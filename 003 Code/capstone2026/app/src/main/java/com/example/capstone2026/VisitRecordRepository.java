package com.example.capstone2026;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VisitRecordRepository {
    public static List<VisitRecord> records(QuerySnapshot snapshot) {
        List<VisitRecord> result = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            VisitRecord record = document.toObject(VisitRecord.class);
            if (record != null) {
                record.setId(document.getId());
                result.add(record);
            }
        }
        return result;
    }

    public static Task<List<VisitRecord>> forCafe(String cafeId, String cafeName) {
        if (!CafeIdentity.hasId(cafeId)) return Tasks.forResult(new ArrayList<>());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> byId = db.collection("visit_records").whereEqualTo("cafeId", cafeId).get();
        if (cafeName == null) return byId.continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return records(task.getResult());
        });
        Task<QuerySnapshot> byName = db.collection("visit_records").whereEqualTo("cafeName", cafeName).get();
        Task<QuerySnapshot> names = db.collection("cafes").whereEqualTo("name", cafeName).limit(2).get();
        return Tasks.whenAll(byId, byName, names).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            Map<String, VisitRecord> matched = new LinkedHashMap<>();
            for (VisitRecord record : records(byId.getResult())) matched.put(record.getId(), record);
            CafeIdentity identity = new CafeIdentity(RecommendationLoader.models(names.getResult()));
            for (VisitRecord record : records(byName.getResult())) {
                if (!CafeIdentity.hasId(record.getCafeId())
                        && cafeId.equals(identity.resolve(null, record.getCafeName()))) {
                    matched.put(record.getId(), record);
                }
            }
            List<VisitRecord> result = new ArrayList<>(matched.values());
            result.sort(Comparator.comparingLong(VisitRecord::getVisitedAt).reversed());
            return result;
        });
    }
}
