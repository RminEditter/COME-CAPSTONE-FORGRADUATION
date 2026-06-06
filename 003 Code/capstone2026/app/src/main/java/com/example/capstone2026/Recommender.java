package com.example.capstone2026;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.location.Location;
public class Recommender {

    // 1. 카페 기본 정보를 담는 클래스
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

    // 2. [중요] 추천 결과와 사유를 담는 클래스 (이게 없어서 빨간 줄이 뜬 거예요!)
    public static class Recommendation {
        public CafeModel cafe;
        public int score;
        public String reason;

        // 실제 거리(m)
        public double distanceMeters;

        public Recommendation(
                CafeModel cafe,
                int score,
                String reason,
                double distanceMeters
        ) {
            this.cafe = cafe;
            this.score = score;
            this.reason = reason;
            this.distanceMeters = distanceMeters;
        }
    }

    // 3. 추천 로직
// [수정] 사용자 현재 위치(lat, lng)를 받아 실제 거리 계산
    public static List<Recommendation> recommend(
            List<CafeModel> allCafes,
            List<Tag> userTags,
            double userLat,
            double userLng
    ) {
        List<Recommendation> results = new ArrayList<>();

        for (CafeModel cafe : allCafes) {
            int finalScore = 0;
            List<Tag> matched = new ArrayList<>();

            // 1. 태그 매칭 점수 계산
            for (Tag ut : userTags) {
                for (Tag ct : cafe.tags) {
                    if (ut == ct) {
                        matched.add(ct);
                    }
                }
            }

            // 2. [수정] 점수 계산 로직 튜닝 (100점 만점 기준)
            if (!userTags.isEmpty()) {
                // (일치 개수 / 내 선택 개수) 비율로 100점 환산
                double ratio = (double) matched.size() / userTags.size();
                finalScore = (int) (ratio * 100);

                // 보너스: 하나라도 맞으면 최소 20점은 보장 (너무 낮으면 기분 안 좋으니까요!)
                if (matched.size() > 0 && finalScore < 20) {
                    finalScore = 20;
                }
            }

            // 3. 추천 사유 생성
            // 3. [추가] 사용자 위치와 카페 위치 간 실제 거리 계산
            float[] distanceResult = new float[1];

            Location.distanceBetween(
                    userLat,      // 사용자 현재 위도
                    userLng,      // 사용자 현재 경도
                    cafe.lat,     // 카페 위도
                    cafe.lng,     // 카페 경도
                    distanceResult
            );

// 계산된 거리(m)
            double distanceMeters = distanceResult[0];
            android.util.Log.d(
                    "DIST_CHECK",
                    "user=" + userLat + "," + userLng
                            + " cafe=" + cafe.lat + "," + cafe.lng
                            + " dist=" + distanceMeters
            );

// 4. 추천 사유 생성
// [수정] 기존 500m 고정값 대신 실제 거리 사용
            String reason = ReasonGenerator.buildReason(
                    matched.toArray(new Tag[0]),
                    distanceMeters,
                    2
            );

            // 정수화된 finalScore를 넣어줍니다.
            results.add(
                    new Recommendation(
                            cafe,
                            finalScore,
                            reason,
                            distanceMeters
                    )
            );
        }

        // 4. 점수 높은 순으로 정렬
        Collections.sort(results, (a, b) -> b.score - a.score);

        return results;
    }
    public static List<Recommendation> recommend(
        List<CafeModel> allCafes,
        List<Tag> userTags
) {
    return recommend(
            allCafes,
            userTags,
            36.3622,
            127.3568
    );
}
}