package com.example.capstone2026;
import java.util.ArrayList;
import java.util.List;

public class ReasonGenerator {

    public static String buildReason(Tag[] matchedTags, double distanceMeters, int priceLevel) {
        List<String> parts = new ArrayList<>();

        // 매칭 태그 상위 2개
        if (matchedTags != null && matchedTags.length > 0) {
            int limit = Math.min(2, matchedTags.length);
            for (int i = 0; i < limit; i++) {
                parts.add(matchedTags[i].labelKo());
            }
        }

        // 거리
        if (distanceMeters <= 300) parts.add("가까움");
        else if (distanceMeters <= 700) parts.add("적당히 가까움");

        // 가격대(선택)
        if (priceLevel <= 2) parts.add("가성비");
        else if (priceLevel >= 4) parts.add("프리미엄");

        if (parts.isEmpty()) return "취향 기반으로 추천했어요";
        return String.join(" · ", parts) + " 포인트가 있어요";
    }
}
