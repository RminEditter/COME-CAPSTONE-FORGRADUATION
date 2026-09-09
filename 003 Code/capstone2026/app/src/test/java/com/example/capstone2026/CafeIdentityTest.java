package com.example.capstone2026;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import static org.junit.Assert.*;

public class CafeIdentityTest {
    private Recommender.CafeModel cafe(String id, String name) {
        return new Recommender.CafeModel(id, name, "", new Tag[0], 36, 127);
    }

    private VisitRecord visit(String id, String name, float rating) {
        VisitRecord record = new VisitRecord(name, rating, "", 1);
        record.setCafeId(id);
        return record;
    }

    @Test public void sameNameBranchesKeepSeparateRatings() {
        CafeIdentity identity = new CafeIdentity(Arrays.asList(cafe("a", "카페"), cafe("b", "카페")));
        Map<String, CafeRatingStats> stats = identity.aggregate(Arrays.asList(
                visit("a", "카페", 5), visit("b", "카페", 1), visit(null, "카페", 3)));
        assertEquals(2, stats.size());
        assertEquals(5, stats.get("a").avgRating, 0.001);
        assertEquals(1, stats.get("b").avgRating, 0.001);
        assertEquals(1, stats.get("a").visitCount);
        assertNull(identity.resolve(null, "카페"));
    }

    @Test public void unambiguousLegacyVisitsRemainInAverage() {
        CafeIdentity identity = new CafeIdentity(Collections.singletonList(cafe("a", "유일한 카페")));
        CafeRatingStats stats = identity.aggregate(Arrays.asList(
                visit(null, "유일한 카페", 3), visit("a", "유일한 카페", 5))).get("a");
        assertEquals(4, stats.avgRating, 0.001);
        assertEquals(2, stats.visitCount);
    }

    @Test public void renamedCafeKeepsItsOwnRecordsById() {
        CafeIdentity identity = new CafeIdentity(Arrays.asList(cafe("a", "새 이름"), cafe("b", "옛 이름")));
        Map<String, CafeRatingStats> stats = identity.aggregate(Collections.singletonList(visit("a", "옛 이름", 5)));
        assertNotNull(stats.get("a"));
        assertFalse(stats.containsKey("b"));
    }

    @Test public void unknownIdMustNotBeReassignedByName() {
        CafeIdentity identity = new CafeIdentity(Collections.singletonList(cafe("a", "카페")));
        assertEquals("deleted", identity.resolve("deleted", "카페"));
        assertEquals("a", identity.resolve(null, "카페"));
        assertFalse(CafeIdentity.hasId("temp_id"));
    }
}
