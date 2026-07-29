package com.example.capstone2026;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.location.Location;

public class Recommender {

    public static class CafeModel {
        public String id;
        public String name;
        public String address;
        public Tag[] tags;
        public double lat, lng;

        public CafeModel(String id, String name, String address, Tag[] tags, double lat, double lng) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.tags = tags;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public static class Recommendation {
        public CafeModel cafe;
        public int score;
        public String reason;
        public double distanceMeters;

        public Recommendation(CafeModel cafe, int score, String reason, double distanceMeters) {
            this.cafe = cafe;
            this.score = score;
            this.reason = reason;
            this.distanceMeters = distanceMeters;
        }
    }

    public static List<Recommendation> recommend(
            List<CafeModel> allCafes,
            List<Tag> userTags,
            Tag priorityTag,
            double userLat,
            double userLng
    ) {
        List<Recommendation> results = new ArrayList<>();

        if (allCafes == null || allCafes.isEmpty()) {
            return results;
        }

        if (userTags == null) {
            userTags = new ArrayList<>();
        }

        for (CafeModel cafe : allCafes) {
            int finalScore = 0;
            int weightedScore = 0;
            int maxScore = 0;
            List<Tag> matched = new ArrayList<>();

            // 1. 유저 태그 매칭 및 가중치 계산
            for (Tag userTag : userTags) {
                if (userTag == null) continue;

                int weight = getWeight(userTag, priorityTag);
                maxScore += weight;

                if (hasTag(cafe.tags, userTag)) {
                    matched.add(userTag);
                    weightedScore += weight;
                }
            }

            // 2. 기본 매칭 점수 계산 (최대 80점 비중 할당)
            double matchRatio = 0.0;
            if (maxScore > 0) {
                matchRatio = (double) weightedScore / maxScore;
            }

            // 태그 매칭 기본점수를 최대 85점으로 잡음
            int baseScore = (int) (matchRatio * 85);
            if (!matched.isEmpty() && baseScore < 20) {
                baseScore = 20; // 하나라도 맞으면 최소 20점 보장
            }

            // 3. 거리 계산 (위도/경도)
            float[] distanceResult = new float[1];
            Location.distanceBetween(
                    userLat,
                    userLng,
                    cafe.lat,
                    cafe.lng,
                    distanceResult
            );
            double distanceMeters = distanceResult[0];

            // 4. 거리 보너스 점수 계산 (최대 15점 비중 할당)
            int distanceBonus = getDistanceBonus(distanceMeters);

            if (priorityTag == Tag.DISTANCE) {
                // 거리 우선 옵션일 경우 거리 보너스 가중치 2배 (+최대 30점)
                finalScore = baseScore + (distanceBonus * 2);
            } else {
                finalScore = baseScore + distanceBonus;
            }

            // 5. 점수 상한선 100점 제한
            if (finalScore > 100) {
                finalScore = 100;
            }

            // 6. 추천 사유(Reason) 생성
            String reason = ReasonGenerator.buildReason(
                    matched.toArray(new Tag[0]),
                    distanceMeters,
                    priorityTag
            );

            results.add(new Recommendation(
                    cafe,
                    finalScore,
                    reason,
                    distanceMeters
            ));
        }

        // 7. 정렬 로직 고도화 (1차: 점수 내림차순, 2차: 동점일 경우 거리 오름차순)
        Collections.sort(results, (a, b) -> {
            if (b.score != a.score) {
                return b.score - a.score; // 점수 높은 순
            }
            return Double.compare(a.distanceMeters, b.distanceMeters); // 점수 같으면 가까운 순
        });

        return results;
    }

    public static List<Recommendation> recommend(
            List<CafeModel> allCafes,
            List<Tag> userTags,
            double userLat,
            double userLng
    ) {
        return recommend(allCafes, userTags, null, userLat, userLng);
    }

    // 기본 위치 설정 (대전 유성구 궁동/어은동 기본 좌표 유지)
    public static List<Recommendation> recommend(
            List<CafeModel> allCafes,
            List<Tag> userTags
    ) {
        return recommend(
                allCafes,
                userTags,
                null,
                36.3622,
                127.3568
        );
    }

    private static boolean hasTag(Tag[] cafeTags, Tag targetTag) {
        if (cafeTags == null || targetTag == null) {
            return false;
        }

        for (Tag cafeTag : cafeTags) {
            if (cafeTag == targetTag) {
                return true;
            }
        }

        return false;
    }

    private static int getWeight(Tag userTag, Tag priorityTag) {
        if (priorityTag == null || userTag == null) {
            return 1;
        }

        // 분위기 우선시 관련 태그 가중치 확대
        if (priorityTag == Tag.MOOD) {
            if (userTag == Tag.INTERIOR_PRETTY ||
                    userTag == Tag.HIP ||
                    userTag == Tag.WORK_FRIENDLY ||
                    userTag == Tag.SMALL_CAFE ||
                    userTag == Tag.LARGE_CAFE) {
                return 3;
            }
        }

        if (userTag == priorityTag) {
            return 3;
        }

        return 1;
    }

    private static int getDistanceBonus(double distanceMeters) {
        if (distanceMeters <= 500) { // 500m 이내
            return 15;
        } else if (distanceMeters <= 1000) { // 1km 이내
            return 12;
        } else if (distanceMeters <= 2000) { // 2km 이내
            return 8;
        } else if (distanceMeters <= 3000) { // 3km 이내
            return 5;
        } else {
            return 2;
        }
    }
}