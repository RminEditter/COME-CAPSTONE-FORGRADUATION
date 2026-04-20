package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

public class ReasonGenerator {

    public static String buildReason(Tag[] matchedTags, double distanceMeters, int priceLevel) {
        List<String> parts = new ArrayList<>();

        // 매칭 태그 상위 2개 (에러 수정 부분)
        if (matchedTags != null && matchedTags.length > 0) {
            int limit = Math.min(2, matchedTags.length);
            for (int i = 0; i < limit; i++) {
                // [수정] labelKo() -> getKoreanLabel() 로 변경
                parts.add(matchedTags[i].getKoreanLabel());
            }
        }

        // 거리 정보 (교수님이 좋아하실만한 세밀한 텍스트)
        if (distanceMeters <= 300) parts.add("도보 5분 이내");
        else if (distanceMeters <= 700) parts.add("가까운 거리");

        // 가격대 정보
        if (priceLevel <= 2) parts.add("가성비 좋음");
        else if (priceLevel >= 4) parts.add("프리미엄 카페");

        if (parts.isEmpty()) return "취향 기반으로 추천했어요";

        // Java 8 이상에서는 String.join을 사용하고,
        // 하위 버전 호환을 위해 안드로이드에서 안전하게 처리합니다.
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            result.append(parts.get(i));
            if (i < parts.size() - 1) result.append(" · ");
        }

        return result.toString() + " 포인트가 있어요";
    }
}