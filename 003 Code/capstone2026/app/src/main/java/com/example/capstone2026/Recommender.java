package com.example.capstone2026;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        public Recommendation(CafeModel cafe, int score, String reason) {
            this.cafe = cafe;
            this.score = score;
            this.reason = reason;
        }
    }

    // 3. 추천 로직
    public static List<Recommendation> recommend(List<CafeModel> allCafes, List<Tag> userTags) {
        List<Recommendation> results = new ArrayList<>();

        for (CafeModel cafe : allCafes) {
            int score = 0;
            List<Tag> matched = new ArrayList<>();

            // 태그 매칭 점수 계산
            for (Tag ut : userTags) {
                for (Tag ct : cafe.tags) {
                    if (ut == ct) {
                        score++;
                        matched.add(ct);
                    }
                }
            }

            // 추천 사유 생성 (ReasonGenerator 활용)
            // 임시로 거리 500m, 가격대 2(보통)로 설정
            String reason = ReasonGenerator.buildReason(matched.toArray(new Tag[0]), 500.0, 2);

            results.add(new Recommendation(cafe, score, reason));
        }

        // 점수 높은 순으로 정렬
        Collections.sort(results, (a, b) -> b.score - a.score);

        return results;
    }
}