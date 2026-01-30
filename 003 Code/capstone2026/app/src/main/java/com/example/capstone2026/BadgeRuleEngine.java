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

        // 태그 기반 배지
        if (hasTag(cafe.tags, Tag.ACIDIC)) deltas.add(new BadgeDelta("ACIDIC_LOVER", 1));       // goal=5
        if (hasTag(cafe.tags, Tag.WORK)) deltas.add(new BadgeDelta("WORK_MASTER", 1));         // goal=10
        if (hasTag(cafe.tags, Tag.DESSERT)) deltas.add(new BadgeDelta("DESSERT_EXPLORER", 1)); // goal=7
        if (hasTag(cafe.tags, Tag.PHOTO)) deltas.add(new BadgeDelta("PHOTO_HUNTER", 1));       // goal=5
        if (hasTag(cafe.tags, Tag.QUIET)) deltas.add(new BadgeDelta("QUIET_SEEKER", 1));       // goal=7

        return deltas;
    }

    private static boolean hasTag(Tag[] tags, Tag t) {
        if (tags == null || t == null) return false;
        for (Tag x : tags) if (x == t) return true;
        return false;
    }
}
