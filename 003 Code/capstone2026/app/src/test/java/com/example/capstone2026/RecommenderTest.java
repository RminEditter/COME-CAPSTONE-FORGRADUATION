package com.example.capstone2026;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class RecommenderTest {
    private Recommender.CafeModel cafe(String id, Tag... tags) {
        return new Recommender.CafeModel(id, id, "", tags, 36.3622, 127.3568);
    }

    private List<Recommender.Recommendation> recommend(List<Recommender.CafeModel> cafes,
            List<Tag> tags, Tag priority, double distance) {
        return Recommender.recommend(cafes, tags, priority, 36.3622, 127.3568,
                (a, b, c, d) -> distance);
    }

    @Test public void changedSurveyChangesTheWinningCafe() {
        List<Recommender.CafeModel> cafes = Arrays.asList(cafe("work", Tag.WORK_FRIENDLY), cafe("sweet", Tag.DESSERT));
        assertEquals("work", recommend(cafes, Collections.singletonList(Tag.WORK_FRIENDLY), null, 500).get(0).cafe.id);
        assertEquals("sweet", recommend(cafes, Collections.singletonList(Tag.DESSERT), null, 500).get(0).cafe.id);
    }

    @Test public void priorityChangesRankingForOtherwiseEqualMatches() {
        List<Recommender.CafeModel> cafes = Arrays.asList(cafe("sweet", Tag.DESSERT), cafe("work", Tag.WORK_FRIENDLY));
        List<Tag> tags = Arrays.asList(Tag.DESSERT, Tag.WORK_FRIENDLY);
        List<Recommender.Recommendation> ranked = recommend(cafes, tags, Tag.WORK_FRIENDLY, 500);
        assertEquals("work", ranked.get(0).cafe.id);
        assertEquals(78, ranked.get(0).score);
        assertEquals(36, ranked.get(1).score);
    }

    @Test public void distancePriorityDoublesBonusAndScoreRemainsCapped() {
        List<Recommender.CafeModel> cafes = Collections.singletonList(cafe("a", Tag.DESSERT));
        List<Tag> tags = Arrays.asList(Tag.DESSERT, Tag.SOLO);
        assertEquals(57, recommend(cafes, tags, null, 500).get(0).score);
        assertEquals(72, recommend(cafes, tags, Tag.DISTANCE, 500).get(0).score);
        assertEquals(100, recommend(cafes, Collections.singletonList(Tag.DESSERT), Tag.DISTANCE, 500).get(0).score);
    }

    @Test public void duplicateAnswersCannotChangeTagWeights() {
        List<Recommender.CafeModel> cafes = Collections.singletonList(cafe("a", Tag.DESSERT));
        int once = recommend(cafes, Arrays.asList(Tag.DESSERT, Tag.SOLO), null, 500).get(0).score;
        int twice = recommend(cafes, Arrays.asList(Tag.DESSERT, Tag.DESSERT, Tag.SOLO), null, 500).get(0).score;
        assertEquals(once, twice);
    }

    @Test public void equalScoresPreferTheNearerCafe() {
        Recommender.CafeModel far = cafe("far", Tag.DESSERT);
        Recommender.CafeModel near = cafe("near", Tag.DESSERT);
        far.lat = 37;
        List<Recommender.Recommendation> ranked = Recommender.recommend(Arrays.asList(far, near),
                Collections.singletonList(Tag.DESSERT), null, 36, 127,
                (a, b, c, d) -> c == 37 ? 400 : 100);
        assertEquals("near", ranked.get(0).cafe.id);
    }

    @Test public void feedbackIsBoundedAndDoesNotAccumulateOnRefresh() {
        List<Recommender.Recommendation> ranked = recommend(Collections.singletonList(cafe("a", Tag.DESSERT)),
                Arrays.asList(Tag.DESSERT, Tag.SOLO), null, 500);
        int base = ranked.get(0).score;
        Recommender.applyFeedbackScores(ranked, Collections.singletonMap(Tag.DESSERT, 100));
        assertEquals(base + 15, ranked.get(0).score);
        Recommender.applyFeedbackScores(ranked, Collections.singletonMap(Tag.DESSERT, 100));
        assertEquals(base + 15, ranked.get(0).score);
        Recommender.applyFeedbackScores(ranked, Collections.singletonMap(Tag.DESSERT, -100));
        assertEquals(base - 15, ranked.get(0).score);
        Recommender.applyFeedbackScores(ranked, Collections.emptyMap());
        assertEquals(base, ranked.get(0).score);
    }

    @Test public void missingCoordinatesDoNotReceiveDistanceBonus() {
        Recommender.CafeModel cafe = cafe("a", Tag.DESSERT);
        cafe.lat = cafe.lng = 0;
        Recommender.Recommendation result = recommend(Collections.singletonList(cafe),
                Collections.singletonList(Tag.DESSERT), Tag.DISTANCE, 0).get(0);
        assertEquals(85, result.score);
        assertEquals(Double.POSITIVE_INFINITY, result.distanceMeters, 0);
        assertFalse(result.reason.contains("Infinity"));
    }
}
