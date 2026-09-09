package com.example.capstone2026;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves old name-only records only when the catalog proves the name unique. */
public final class CafeIdentity {
    private final Map<String, String> uniqueNames = new HashMap<>();
    private final Set<String> ambiguousNames = new HashSet<>();

    public CafeIdentity(List<Recommender.CafeModel> cafes) {
        for (Recommender.CafeModel cafe : cafes) {
            if (!hasId(cafe.id) || cafe.name == null) continue;
            String previous = uniqueNames.putIfAbsent(cafe.name, cafe.id);
            if (previous != null && !previous.equals(cafe.id)) ambiguousNames.add(cafe.name);
        }
    }

    public static boolean hasId(String id) {
        return id != null && !id.trim().isEmpty() && !"temp_id".equals(id);
    }

    public String resolve(String id, String name) {
        if (hasId(id)) return id;
        return ambiguousNames.contains(name) ? null : uniqueNames.get(name);
    }

    public Map<String, CafeRatingStats> aggregate(List<VisitRecord> records) {
        Map<String, CafeRatingStats> result = new HashMap<>();
        for (VisitRecord record : records) {
            String id = resolve(record.getCafeId(), record.getCafeName());
            if (id == null) continue;
            CafeRatingStats stats = result.get(id);
            if (stats == null) {
                stats = new CafeRatingStats();
                stats.cafeName = record.getCafeName();
                result.put(id, stats);
            }
            stats.avgRating = (stats.avgRating * stats.visitCount + record.getRating())
                    / (stats.visitCount + 1);
            stats.visitCount++;
        }
        return result;
    }
}
