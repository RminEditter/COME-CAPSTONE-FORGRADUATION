package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

public class BadgeRuleEngine {

    public static class BadgeDelta {
        public final String badgeCode;
        public final int delta;

        public BadgeDelta(String badgeCode, int delta) {
            this.badgeCode = badgeCode;
            this.delta = delta;
        }
    }

    public static List<BadgeDelta> onVisitAdded(Recommender.CafeModel cafe, float userRatingOr0) {
        List<BadgeDelta> deltas = new ArrayList<>();
        if (cafe == null) return deltas;

        // 방문 자체 배지
        deltas.add(new BadgeDelta("NEWBIE", 1));   // goal=1
        deltas.add(new BadgeDelta("REGULAR", 1));  // goal=10
        if (userRatingOr0 > 0f) deltas.add(new BadgeDelta("CRITIC", 1)); // goal=10

        /* ==========================================================
           태그 기반 배지 (새로운 Tag Enum에 맞게 수정됨)
        ========================================================== */

        // 1. 산미 마스터 (Tag.ACIDIC -> Tag.BEAN_ACIDIC)
        if (hasTag(cafe.tags, Tag.BEAN_ACIDIC)) deltas.add(new BadgeDelta("ACIDIC_LOVER", 1));       // goal=5

        // 2. 카공 마스터 (Tag.WORK -> Tag.WORK_FRIENDLY)
        if (hasTag(cafe.tags, Tag.WORK_FRIENDLY)) deltas.add(new BadgeDelta("WORK_MASTER", 1));         // goal=10

        // 3. 디저트 탐험가 (기존 유지)
        if (hasTag(cafe.tags, Tag.DESSERT)) deltas.add(new BadgeDelta("DESSERT_EXPLORER", 1)); // goal=7

        // 4. 사진 헌터 (Tag.PHOTO -> Tag.INTERIOR_PRETTY)
        if (hasTag(cafe.tags, Tag.INTERIOR_PRETTY)) deltas.add(new BadgeDelta("PHOTO_HUNTER", 1));       // goal=5

        // 5. 힙한 감성 추구자 (기존 QUIET 대체 -> Tag.HIP)
        if (hasTag(cafe.tags, Tag.HIP)) deltas.add(new BadgeDelta("HIP_SEEKER", 1));       // goal=7

        return deltas;
    }

    private static boolean hasTag(Tag[] tags, Tag t) {
        if (tags == null || t == null) return false;
        for (Tag x : tags) if (x == t) return true;
        return false;
    }
}