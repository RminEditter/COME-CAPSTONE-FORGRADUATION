package com.example.capstone2026;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Recommender {

    // ===== Models (UI/DB가 공통으로 쓰기 쉬운 형태) =====
    public static class CafeModel {
        public final long id;
        public final String name;
        public final double lat;
        public final double lng;
        public final Tag[] tags;
        public final int priceLevel;  // 1~5
        public final float ratingAvg; // 표시용 (MVP에선 0)

        public CafeModel(long id, String name, double lat, double lng, Tag[] tags, int priceLevel, float ratingAvg) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.tags = tags == null ? new Tag[0] : tags;
            this.priceLevel = priceLevel;
            this.ratingAvg = ratingAvg;
        }
    }

    public static class UserPreference {
        public final Tag[] preferredTags;
        public final int priceMin; // 1~5
        public final int priceMax;

        public UserPreference(Tag[] preferredTags, int priceMin, int priceMax) {
            this.preferredTags = preferredTags == null ? new Tag[0] : preferredTags;
            this.priceMin = priceMin;
            this.priceMax = priceMax;
        }
    }

    public static class VisitStats {
        public final int cafeVisitCount;  // 해당 카페 방문 횟수
        public final float cafeAvgRating; // 해당 카페 평균 평점

        public VisitStats(int cafeVisitCount, float cafeAvgRating) {
            this.cafeVisitCount = cafeVisitCount;
            this.cafeAvgRating = cafeAvgRating;
        }

        public static VisitStats empty() {
            return new VisitStats(0, 0f);
        }
    }

    public interface VisitStatsProvider {
        VisitStats get(long cafeId);
    }

    public static class RecommendResult {
        public final long cafeId;
        public final String cafeName;
        public final int matchPercent;        // 0~100
        public final int totalScore;          // 정렬용
        public final double distanceMeters;   // 표시용
        public final Tag[] matchedTags;       // 강조 표시용
        public final String reason;           // 추천 이유

        public RecommendResult(long cafeId, String cafeName, int matchPercent, int totalScore,
                               double distanceMeters, Tag[] matchedTags, String reason) {
            this.cafeId = cafeId;
            this.cafeName = cafeName;
            this.matchPercent = matchPercent;
            this.totalScore = totalScore;
            this.distanceMeters = distanceMeters;
            this.matchedTags = matchedTags == null ? new Tag[0] : matchedTags;
            this.reason = reason;
        }
    }

    // ===== Public API =====
    public static List<RecommendResult> recommend(
            UserPreference pref,
            List<CafeModel> cafes,
            double userLat,
            double userLng,
            VisitStatsProvider statsProvider,
            String keyword
    ) {
        if (pref == null || cafes == null || cafes.isEmpty()) return Collections.emptyList();

        List<RecommendResult> out = new ArrayList<>();

        for (CafeModel cafe : cafes) {

            if (keyword != null && !keyword.isEmpty()) {
                if (!cafe.name.toLowerCase().contains(keyword.toLowerCase())) continue;
            }
            // 가격 필터(선택)
            if (cafe.priceLevel < pref.priceMin || cafe.priceLevel > pref.priceMax) continue;

            double distM = haversineMeters(userLat, userLng, cafe.lat, cafe.lng);

            Tag[] matched = matchedTags(pref.preferredTags, cafe.tags);
            int matchPercent = calcMatchPercent(pref.preferredTags.length, matched.length);

            int distanceScore = calcDistanceScore(distM); // 0~100

            VisitStats stats = (statsProvider == null) ? VisitStats.empty() : statsProvider.get(cafe.id);
            if (stats == null) stats = VisitStats.empty();
            int behaviorScore = calcBehaviorScore(stats.cafeVisitCount, stats.cafeAvgRating); // 0~50

            // 총점: 태그(60) + 거리(30) + 행동(10)
            int total = calcTotal(matchPercent, distanceScore, behaviorScore);

            String reason = ReasonGenerator.buildReason(matched, distM, cafe.priceLevel);

            out.add(new RecommendResult(
                    cafe.id,
                    cafe.name,
                    matchPercent,
                    total,
                    distM,
                    matched,
                    reason
            ));
        }

        // 정렬: 총점 desc, 거리 asc
        out.sort(new Comparator<RecommendResult>() {
            @Override
            public int compare(RecommendResult a, RecommendResult b) {
                int s = Integer.compare(b.totalScore, a.totalScore);
                if (s != 0) return s;
                return Double.compare(a.distanceMeters, b.distanceMeters);
            }
        });

        return out;
    }

    // ===== Matching & scoring =====
    public static Tag[] matchedTags(Tag[] userTags, Tag[] cafeTags) {
        if (userTags == null || cafeTags == null) return new Tag[0];

        List<Tag> matched = new ArrayList<>();
        for (Tag ut : userTags) {
            for (Tag ct : cafeTags) {
                if (ut == ct) { matched.add(ut); break; }
            }
        }
        return matched.toArray(new Tag[0]);
    }

    public static int calcMatchPercent(int userTagCount, int matchedCount) {
        if (userTagCount <= 0) return 0;
        return (int) Math.round(matchedCount * 100.0 / userTagCount);
    }

    public static int calcDistanceScore(double meters) {
        // 0~10000m(10km) => 100~0
        double max = 10000.0;
        double clamped = Math.min(Math.max(meters, 0), max);
        return (int) Math.round(100.0 - (clamped / max) * 100.0);
    }

    public static int calcBehaviorScore(int cafeVisitCount, float cafeAvgRating) {
        // 최대 50점: 방문(0~30) + 평점(0~20)
        int v = Math.min(Math.max(cafeVisitCount, 0), 10) * 3;
        float rClamped = Math.max(0f, Math.min(cafeAvgRating, 5f));
        int r = (int) Math.round(rClamped * 4);
        return v + r;
    }

    public static int calcTotal(int matchPercent, int distanceScore, int behaviorScore) {
        double total = matchPercent * 0.6 + distanceScore * 0.3 + behaviorScore * 0.1;
        return (int) Math.round(total);
    }

    // ===== Geo: Haversine =====
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
