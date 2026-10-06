package com.example.capstone2026;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VisitFirestoreTest {
    private <T> T await(Task<T> task) throws Exception { return Tasks.await(task, 20, TimeUnit.SECONDS); }
    @Test public void tagsRoundTripEditDeleteAndPersonalize() throws Exception {
        org.junit.Assume.assumeTrue("Requires local Firestore emulator",
                "true".equals(InstrumentationRegistry.getArguments().getString("firestoreEmulator")));
        // Separate demo app: cannot access the production Firebase project or its auth session.
        FirebaseApp app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                new FirebaseOptions.Builder().setProjectId("demo-cafefit-visits")
                        .setApplicationId("1:123456789:android:fixture").setApiKey("fake-api-key").build(),
                "visit-test-" + UUID.randomUUID());
        FirebaseFirestore db = FirebaseFirestore.getInstance(app);
        db.useEmulator("10.0.2.2", 8181);
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        String uid = "test-" + UUID.randomUUID();
        DocumentReference ref = db.collection("visit_records").document();
        DocumentReference legacy = db.collection("visit_records").document();
        DocumentReference other = db.collection("visit_records").document();
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("userUid", uid); data.put("cafeId", "fixture-cafe"); data.put("cafeName", "테스트 카페");
            data.put("visitedAt", 100L); data.put("rating", 4.5); data.put("memo", "태그 테스트");
            await(legacy.set(data));
            data.put("userUid", uid + "-other"); await(other.set(data)); data.put("userUid",uid);
            data.put("observedTags", Arrays.asList("DESSERT", "WORK_FRIENDLY"));
            data.put("likedTags", Arrays.asList("DESSERT")); data.put("visitedAt",200L);
            await(ref.set(data));
            VisitRecord saved = await(ref.get(Source.SERVER)).toObject(VisitRecord.class);
            assertEquals(Arrays.asList("DESSERT"), saved.getLikedTags());
            assertEquals(2, saved.getObservedTags().size());
            List<VisitRecord> records = VisitRecordRepository.records(await(db.collection("visit_records")
                    .whereEqualTo("userUid",uid).get(Source.SERVER)));
            assertEquals(2,records.size());
            assertEquals(Integer.valueOf(1),VisitTagPreferences.scores(records,uid).get(Tag.DESSERT));
            Recommender.Recommendation cafe = new Recommender.Recommendation(new Recommender.CafeModel(
                    "similar", "유사 카페", "", new Tag[]{Tag.DESSERT},0,0),70,"기본",0);
            VisitTagPreferences.apply(new ArrayList<>(Arrays.asList(cafe)),VisitTagPreferences.scores(records,uid));
            assertEquals(73,cafe.score);
            assertTrue(await(legacy.get(Source.SERVER)).toObject(VisitRecord.class).getLikedTags().isEmpty());
            await(db.runTransaction(tx -> {
                DocumentSnapshot existing = tx.get(ref);
                if (!uid.equals(existing.getString("userUid"))) throw new IllegalStateException("owner");
                tx.update(ref,"rating",3.0,"memo","수정", "observedTags",Arrays.asList("WORK_FRIENDLY"),
                        "likedTags",Collections.emptyList()); return null;
            }));
            VisitRecord edited = await(ref.get(Source.SERVER)).toObject(VisitRecord.class);
            assertEquals("fixture-cafe",edited.getCafeId()); assertEquals(200L,edited.getVisitedAt());
            assertTrue(edited.getLikedTags().isEmpty());
            records = VisitRecordRepository.records(await(db.collection("visit_records").whereEqualTo("userUid",uid).get(Source.SERVER)));
            assertTrue(VisitTagPreferences.scores(records,uid).isEmpty());
            await(ref.delete());
            assertFalse(await(ref.get(Source.SERVER)).exists());
            records = VisitRecordRepository.records(await(db.collection("visit_records").whereEqualTo("userUid",uid).get(Source.SERVER)));
            assertTrue(VisitTagPreferences.scores(records,uid).isEmpty());
        } finally {
            await(db.batch().delete(ref).delete(legacy).delete(other).commit());
            await(db.terminate()); app.delete();
        }
    }
}
