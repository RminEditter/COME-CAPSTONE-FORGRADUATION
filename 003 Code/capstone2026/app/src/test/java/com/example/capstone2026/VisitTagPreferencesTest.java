package com.example.capstone2026;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class VisitTagPreferencesTest {
    private VisitRecord record(String cafe, String uid, long date, boolean liked) {
        VisitRecord r = new VisitRecord("카페", 5, "", date);
        r.setCafeId(cafe); r.setUserUid(uid);
        r.setObservedTags(Arrays.asList("DESSERT"));
        r.setLikedTags(liked ? Arrays.asList("DESSERT", "DESSERT") : Collections.emptyList());
        return r;
    }
    @Test public void onlyExplicitLikesFromCurrentUserCountOncePerCafe() {
        Map<Tag,Integer> scores = VisitTagPreferences.scores(Arrays.asList(
                record("a", "me", 1, true), record("a", "me", 2, true),
                record("b", "other", 3, true), record("c", "me", 4, false)), "me");
        assertEquals(Integer.valueOf(1), scores.get(Tag.DESSERT));
        assertEquals(1, scores.size());
    }
    @Test public void clearingEditingAndDeletionAreReflectedWithoutCachedPreferences() {
        VisitRecord old = record("a", "me", 1, true), latest = record("a", "me", 2, false);
        assertTrue(VisitTagPreferences.scores(Arrays.asList(old, latest), "me").isEmpty());
        old.setLikedTags(Collections.emptyList());
        assertTrue(VisitTagPreferences.scores(Arrays.asList(old), "me").isEmpty());
        assertTrue(VisitTagPreferences.scores(Collections.emptyList(), "me").isEmpty());
    }
    @Test public void legacyAndInvalidLikesDoNotBecomePreferences() {
        VisitRecord r = record("a", "me", 1, true);
        r.setObservedTags(Collections.emptyList());
        assertTrue(VisitTagPreferences.scores(Arrays.asList(r), "me").isEmpty());
        assertTrue(new VisitRecord().getLikedTags().isEmpty());
    }
    @Test public void similarCafeMovesUpAndBonusSurvivesOtherFeedbackWithoutAccumulation() {
        Recommender.Recommendation other = new Recommender.Recommendation(
                new Recommender.CafeModel("a", "a", "", new Tag[]{Tag.WORK_FRIENDLY},0,0), 71, "기본", 0);
        Recommender.Recommendation similar = new Recommender.Recommendation(
                new Recommender.CafeModel("b", "b", "", new Tag[]{Tag.DESSERT},0,0), 70, "기본", 0);
        List<Recommender.Recommendation> list = new ArrayList<>(Arrays.asList(other,similar));
        VisitTagPreferences.apply(list, Collections.singletonMap(Tag.DESSERT, 100));
        assertSame(similar,list.get(0)); assertEquals(85,similar.score);
        assertTrue(similar.reason.contains("디저트"));
        Recommender.applyFeedbackScores(list, Collections.emptyMap());
        assertEquals(85,similar.score);
    }
}
