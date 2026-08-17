package com.example.capstone2026;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgeManager {

    public interface BadgeUpdateListener {
        void onComplete(List<String> newlyUnlockedBadges);

        void onFailure(Exception e);
    }

    public static void applyVisitBadges(
            String userUid,
            Recommender.CafeModel cafe,
            float rating,
            BadgeUpdateListener listener
    ) {

        if (userUid == null || userUid.isEmpty()) {
            if (listener != null) {
                listener.onFailure(
                        new IllegalArgumentException("사용자 UID가 없습니다.")
                );
            }
            return;
        }

        if (cafe == null) {
            if (listener != null) {
                listener.onFailure(
                        new IllegalArgumentException("카페 정보가 없습니다.")
                );
            }
            return;
        }

        List<BadgeRuleEngine.BadgeDelta> deltas =
                BadgeRuleEngine.onVisitAdded(
                        cafe,
                        rating
                );

        if (deltas == null || deltas.isEmpty()) {
            if (listener != null) {
                listener.onComplete(new ArrayList<>());
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Task<String>> updateTasks = new ArrayList<>();

        for (BadgeRuleEngine.BadgeDelta delta : deltas) {

            if (delta == null || delta.badgeCode == null) {
                continue;
            }

            Task<String> task = updateSingleBadge(
                    db,
                    userUid,
                    delta.badgeCode,
                    delta.delta
            );

            updateTasks.add(task);
        }

        if (updateTasks.isEmpty()) {
            if (listener != null) {
                listener.onComplete(new ArrayList<>());
            }
            return;
        }

        Tasks.whenAllSuccess(updateTasks)
                .addOnSuccessListener(results -> {

                    List<String> newlyUnlockedBadges =
                            new ArrayList<>();

                    for (Object result : results) {

                        if (result instanceof String) {

                            String badgeCode = (String) result;

                            if (!badgeCode.isEmpty()) {
                                newlyUnlockedBadges.add(badgeCode);
                            }
                        }
                    }

                    if (listener != null) {
                        listener.onComplete(
                                newlyUnlockedBadges
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    private static Task<String> updateSingleBadge(
            FirebaseFirestore db,
            String userUid,
            String badgeCode,
            int delta
    ) {

        DocumentReference badgeRef =
                db.collection("users")
                        .document(userUid)
                        .collection("badges")
                        .document(badgeCode);

        return db.runTransaction(transaction -> {

            int goal = getBadgeGoal(badgeCode);

            long oldProgress = 0;
            boolean oldUnlocked = false;

            com.google.firebase.firestore.DocumentSnapshot snapshot =
                    transaction.get(badgeRef);

            if (snapshot.exists()) {

                Long savedProgress =
                        snapshot.getLong("progress");

                Boolean savedUnlocked =
                        snapshot.getBoolean("unlocked");

                if (savedProgress != null) {
                    oldProgress = savedProgress;
                }

                if (savedUnlocked != null) {
                    oldUnlocked = savedUnlocked;
                }
            }

            long newProgress =
                    oldProgress + delta;

            if (newProgress < 0) {
                newProgress = 0;
            }

            if (newProgress > goal) {
                newProgress = goal;
            }

            boolean newUnlocked =
                    newProgress >= goal;

            Map<String, Object> badgeData =
                    new HashMap<>();

            badgeData.put(
                    "badgeCode",
                    badgeCode
            );

            badgeData.put(
                    "badgeName",
                    getBadgeName(badgeCode)
            );

            badgeData.put(
                    "progress",
                    newProgress
            );

            badgeData.put(
                    "goal",
                    goal
            );

            badgeData.put(
                    "unlocked",
                    newUnlocked
            );

            badgeData.put(
                    "updatedAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(
                    badgeRef,
                    badgeData,
                    SetOptions.merge()
            );

            if (!oldUnlocked && newUnlocked) {
                return badgeCode;
            }

            return "";
        });
    }

    public static int getBadgeGoal(String badgeCode) {

        if (badgeCode == null) {
            return 1;
        }

        switch (badgeCode) {

            case "NEWBIE":
                return 1;

            case "REGULAR":
                return 10;

            case "CRITIC":
                return 10;

            case "ACIDIC_LOVER":
                return 5;

            case "WORK_MASTER":
                return 10;

            case "DESSERT_EXPLORER":
                return 7;

            case "PHOTO_HUNTER":
                return 5;

            case "HIP_SEEKER":
                return 7;

            default:
                return 1;
        }
    }

    public static String getBadgeName(String badgeCode) {

        if (badgeCode == null) {
            return "알 수 없는 배지";
        }

        switch (badgeCode) {

            case "NEWBIE":
                return "첫 발자국";

            case "REGULAR":
                return "카페 단골";

            case "CRITIC":
                return "카페 평론가";

            case "ACIDIC_LOVER":
                return "산미 마스터";

            case "WORK_MASTER":
                return "카공 마스터";

            case "DESSERT_EXPLORER":
                return "디저트 탐험가";

            case "PHOTO_HUNTER":
                return "사진 헌터";

            case "HIP_SEEKER":
                return "힙스터";

            default:
                return badgeCode;
        }
    }
}