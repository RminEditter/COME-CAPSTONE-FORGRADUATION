package com.example.capstone2026;

import java.util.*;

final class VisitTagPreferences {
    static boolean selectable(Tag tag) { return tag != null && tag != Tag.MOOD && tag != Tag.DISTANCE; }
    static Map<Tag, Integer> scores(List<VisitRecord> records, String uid) {
        Map<String, VisitRecord> latest = new TreeMap<>();
        for (VisitRecord record : records) {
            if (!uid.equals(record.getUserUid()) || !CafeIdentity.hasId(record.getCafeId())) continue;
            VisitRecord old = latest.get(record.getCafeId());
            if (old == null || record.getVisitedAt() > old.getVisitedAt()
                    || (record.getVisitedAt() == old.getVisitedAt()
                    && String.valueOf(record.getId()).compareTo(String.valueOf(old.getId())) > 0)) latest.put(record.getCafeId(), record);
        }
        Map<Tag, Integer> result = new EnumMap<>(Tag.class);
        for (VisitRecord record : latest.values()) {
            Set<Tag> liked = new HashSet<>();
            for (String value : record.getLikedTags()) {
                Tag tag = SurveyPreferences.parseTag(value);
                if (selectable(tag) && record.getObservedTags().contains(value)) liked.add(tag);
            }
            for (Tag tag : liked) result.put(tag, result.getOrDefault(tag, 0) + 1);
        }
        return result;
    }
    static void apply(List<Recommender.Recommendation> recommendations, Map<Tag, Integer> scores) {
        for (Recommender.Recommendation recommendation : recommendations) {
            int bonus = 0;
            List<String> matches = new ArrayList<>();
            Set<Tag> unique = new HashSet<>(Arrays.asList(recommendation.cafe.tags));
            for (Tag tag : Tag.values()) {
                if (unique.contains(tag) && scores.getOrDefault(tag, 0) > 0) {
                    bonus += 3 * Math.min(5, scores.get(tag));
                    matches.add(tag.getKoreanLabel());
                }
            }
            recommendation.visitTagBonus = Math.min(15, bonus);
            if (!matches.isEmpty()) recommendation.reason += "\n방문 평가에서 마음에 든 " + String.join("·", matches) + " 태그가 있는 카페예요.";
        }
        Recommender.applyFeedbackScores(recommendations, Collections.emptyMap());
    }
}
