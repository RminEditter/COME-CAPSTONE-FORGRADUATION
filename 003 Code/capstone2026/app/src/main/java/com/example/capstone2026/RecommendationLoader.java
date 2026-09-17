package com.example.capstone2026;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

/** Rebuilds recommendations from persistent account data on every screen entry. */
public final class RecommendationLoader {
    // Firestore callbacks default to the UI thread; bulk processing must not run there.
    static final java.util.concurrent.Executor COMPUTATION =
            java.util.concurrent.Executors.newFixedThreadPool(2);
    public static final class Result {
        public final List<Recommender.Recommendation> recommendations;
        public final boolean usedDefaultLocation;
        public final boolean hasSurvey;

        Result(List<Recommender.Recommendation> recommendations, boolean usedDefaultLocation,
               boolean hasSurvey) {
            this.recommendations = recommendations;
            this.usedDefaultLocation = usedDefaultLocation;
            this.hasSurvey = hasSurvey;
        }
    }

    public static Task<Result> load(Context context, String uid) {
        return load(context, uid, null);
    }

    public static Task<Result> load(Context context, String uid, HomeSituation situation) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<DocumentSnapshot> survey = db.collection("users").document(uid).get();
        Task<QuerySnapshot> cafes = db.collection("cafes").get();
        Task<double[]> location = locate(context.getApplicationContext());
        return Tasks.whenAll(survey, cafes, location).continueWith(COMPUTATION, task -> {
            if (!task.isSuccessful()) throw task.getException();
            SurveyPreferences preferences = SurveyPreferences.from(survey.getResult().getData());
            double[] coordinates = location.getResult();
            List<Recommender.Recommendation> recommendations = Recommender.recommend(
                    models(cafes.getResult()),
                    situation == null ? preferences.tags : situation.withSurvey(preferences.tags),
                    situation == null ? preferences.priority : situation.priority,
                    coordinates[0], coordinates[1]);
            if (coordinates[2] == 1) {
                for (Recommender.Recommendation recommendation : recommendations) {
                    recommendation.reason = recommendation.reason.replace("현재 위치", "대전 궁동·어은동 위치")
                            .replace("기준 위치", "대전 궁동·어은동");
                }
            }
            return new Result(recommendations, coordinates[2] == 1, !preferences.tags.isEmpty());
        });
    }

    public static List<Recommender.CafeModel> models(QuerySnapshot snapshot) {
        List<Recommender.CafeModel> result = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            if (CafeDiscoveryPolicy.isDiscoverable(document.getData())) result.add(model(document));
        }
        return result;
    }

    public static Recommender.CafeModel model(DocumentSnapshot document) {
        List<Tag> tags = new ArrayList<>();
        Object raw = document.get("tags");
        if (raw instanceof Iterable<?>) {
            for (Object value : (Iterable<?>) raw) {
                Tag tag = SurveyPreferences.parseTag(value);
                if (tag != null && !tags.contains(tag)) tags.add(tag);
            }
        }
        Double lat = document.getDouble("latitude");
        Double lng = document.getDouble("longitude");
        return new Recommender.CafeModel(document.getId(), document.getString("name"),
                document.getString("address"), tags.toArray(new Tag[0]),
                lat == null ? 0 : lat, lng == null ? 0 : lng);
    }

    private static Task<double[]> locate(Context context) {
        double[] fallback = {36.3622, 127.3568, 1};
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return Tasks.forResult(fallback);

        TaskCompletionSource<double[]> result = new TaskCompletionSource<>();
        CancellationTokenSource cancellation = new CancellationTokenSource();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = () -> {
            result.trySetResult(fallback);
            cancellation.cancel();
        };
        handler.postDelayed(timeout, 10000);
        try {
            LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(fine ? Priority.PRIORITY_HIGH_ACCURACY
                            : Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.getToken())
                    .addOnCompleteListener(task -> {
                        handler.removeCallbacks(timeout);
                        if (task.isSuccessful() && task.getResult() != null) {
                            result.trySetResult(new double[]{task.getResult().getLatitude(),
                                    task.getResult().getLongitude(), 0});
                        } else {
                            result.trySetResult(fallback);
                        }
                    });
        } catch (SecurityException e) {
            handler.removeCallbacks(timeout);
            cancellation.cancel();
            result.trySetResult(fallback);
        }
        return result.getTask();
    }
}
