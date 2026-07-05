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

            for (Tag userTag : userTags) {
                if (userTag == null) continue;

                int weight = getWeight(userTag, priorityTag);
                maxScore += weight;

                if (hasTag(cafe.tags, userTag)) {
                    matched.add(userTag);
                    weightedScore += weight;
                }
            }

            if (maxScore > 0) {
                double ratio = (double) weightedScore / maxScore;
                finalScore = (int) (ratio * 100);

                if (!matched.isEmpty() && finalScore < 20) {
                    finalScore = 20;
                }
            }

            float[] distanceResult = new float[1];

            Location.distanceBetween(
                    userLat,
                    userLng,
                    cafe.lat,
                    cafe.lng,
                    distanceResult
            );

            double distanceMeters = distanceResult[0];

            int distanceBonus = getDistanceBonus(distanceMeters);

            if (priorityTag == Tag.DISTANCE) {
                finalScore += distanceBonus * 2;
            } else {
                finalScore += distanceBonus;
            }

            if (finalScore > 100) {
                finalScore = 100;
            }

            String reason = ReasonGenerator.buildReason(
                    matched.toArray(new Tag[0]),
                    distanceMeters,
                    2
            );

            if (priorityTag != null) {
                if (priorityTag == Tag.DISTANCE) {
                    reason += " 거리 우선 조건을 반영했어요.";
                } else if (matched.contains(priorityTag)) {
                    reason += " 중요하게 선택한 조건과도 잘 맞아요.";
                }
            }

            results.add(new Recommendation(
                    cafe,
                    finalScore,
                    reason,
                    distanceMeters
            ));
        }

        Collections.sort(results, (a, b) -> b.score - a.score);

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

        if (priorityTag == Tag.MOOD) {
            if (userTag == Tag.INTERIOR_PRETTY ||
                    userTag == Tag.HIP ||
                    userTag == Tag.WORK_FRIENDLY) {
                return 3;
            }
        }

        if (userTag == priorityTag) {
            return 3;
        }

        return 1;
    }

    private static int getDistanceBonus(double distanceMeters) {
        if (distanceMeters <= 500) {
            return 10;
        } else if (distanceMeters <= 1000) {
            return 8;
        } else if (distanceMeters <= 2000) {
            return 5;
        } else if (distanceMeters <= 3000) {
            return 3;
        } else {
            return 1;
        }
    }
}