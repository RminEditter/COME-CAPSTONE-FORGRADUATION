package com.example.capstone2026;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Fetches only the visible hero. No prefetch, disk cache, Google key, or HTTP retries. */
final class HomePhotoLoader {
    private static final int MAX_RESPONSE = 3 * 1024 * 1024;
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .callTimeout(40, TimeUnit.SECONDS).build();
    private final Activity activity;
    private final ImageView image;
    private final TextView status;
    private final LinearLayout credits;
    private volatile int generation;
    private Call pending;
    private String googleMapsUrl;

    HomePhotoLoader(Activity activity, ImageView image, TextView status, LinearLayout credits) {
        this.activity = activity; this.image = image; this.status = status; this.credits = credits;
    }

    String googleMapsUrl() { return googleMapsUrl; }

    void clear() {
        generation++;
        if (pending != null) pending.cancel();
        pending = null;
        googleMapsUrl = null;
        image.setScaleType(ImageView.ScaleType.CENTER);
        image.setImageResource(R.drawable.ic_profile_cup);
        status.setText(R.string.home_photo_empty);
        status.setVisibility(View.VISIBLE);
        credits.removeAllViews();
        credits.setVisibility(View.GONE);
    }

    void load(String cafeId) {
        clear();
        String endpoint = BuildConfig.CAFE_PHOTO_ENDPOINT;
        if (endpoint.isEmpty() || !CafeIdentity.hasId(cafeId)) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        int request = generation;
        String uid = user.getUid();
        status.setText("카페 사진을 불러오는 중…");
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!current(request, uid)) return;
            if (!task.isSuccessful() || task.getResult().getToken() == null) { unavailable(); return; }
            try {
                JSONObject body = new JSONObject().put("cafeId", cafeId);
                Request http = new Request.Builder().url(endpoint)
                        .header("Authorization", "Bearer " + task.getResult().getToken())
                        .header("Cache-Control", "no-store")
                        .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                        .build();
                pending = HTTP.newCall(http);
                pending.enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException error) {
                        activity.runOnUiThread(() -> { if (current(request, uid)) unavailable(); });
                    }
                    @Override public void onResponse(Call call, Response response) {
                        try (Response closeable = response) {
                            if (!response.isSuccessful() || response.body() == null) throw new IOException();
                            JSONObject result = new JSONObject(readBounded(response.body().byteStream()));
                            if (!"ok".equals(result.optString("status"))) throw new IOException();
                            byte[] bytes = Base64.decode(result.getString("imageBase64"), Base64.DEFAULT);
                            BitmapFactory.Options bounds = new BitmapFactory.Options();
                            bounds.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                            if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                                    || bounds.outWidth > 1600 || bounds.outHeight > 1600) throw new IOException();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (bitmap == null) throw new IOException();
                            activity.runOnUiThread(() -> {
                                if (!current(request, uid)) { bitmap.recycle(); return; }
                                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                image.setImageBitmap(bitmap);
                                status.setVisibility(View.GONE);
                                googleMapsUrl = safeGoogleMap(result.optString("googleMapsUrl"));
                                credits.removeAllViews();
                                addCredit("Google Maps", googleMapsUrl);
                                addCredits(result.optJSONArray("authors"));
                                addCredits(result.optJSONArray("providers"));
                                credits.setVisibility(View.VISIBLE);
                            });
                        } catch (Exception ignored) {
                            activity.runOnUiThread(() -> { if (current(request, uid)) unavailable(); });
                        }
                    }
                });
            } catch (Exception ignored) { unavailable(); }
        });
    }

    private static String readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count;
        while ((count = input.read(chunk)) != -1) {
            if (output.size() + count > MAX_RESPONSE) throw new IOException("Response too large");
            output.write(chunk, 0, count);
        }
        return output.toString("UTF-8");
    }

    private boolean current(int request, String uid) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return request == generation && !activity.isDestroyed() && !activity.isFinishing()
                && user != null && uid.equals(user.getUid());
    }

    private void unavailable() {
        status.setText("사진을 표시할 수 없어 기본 이미지로 보여드려요.");
        status.setVisibility(View.VISIBLE);
    }

    private String safeGoogleMap(String value) {
        Uri uri = Uri.parse(value);
        return "https".equals(uri.getScheme()) && "www.google.com".equals(uri.getHost())
                && uri.getPath() != null && uri.getPath().startsWith("/maps/") ? value : null;
    }

    private void addCredits(JSONArray entries) {
        if (entries == null) return;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry != null && !entry.optString("name").isEmpty()) addCredit(entry.optString("name"), entry.optString("url"));
        }
    }

    private void addCredit(String name, String url) {
        TextView view = new TextView(activity);
        view.setText(name);
        view.setTextSize(14);
        view.setTextColor(ContextCompat.getColor(activity, R.color.brown_dark));
        int padding = (int) (8 * activity.getResources().getDisplayMetrics().density);
        view.setPadding(0, padding, 0, padding);
        if (url != null && "https".equals(Uri.parse(url).getScheme())) {
            view.setMinHeight((int) (48 * activity.getResources().getDisplayMetrics().density));
            view.setContentDescription(name + ", 출처 보기");
            view.setOnClickListener(v -> {
                try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (android.content.ActivityNotFoundException ignored) { }
            });
        }
        credits.addView(view);
    }
}
