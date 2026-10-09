package com.example.capstone2026;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Read-only point access. All balance changes must be performed by trusted
 * Firebase server code, never from an Android client.
 */
public class PointManager {

    public static final int VISIT_REVIEW_POINTS = 100;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface PointCallback {
        void onSuccess(long points);
        void onFailure(String message);
    }

    public interface RewardCheckCallback {
        void onResult(boolean alreadyRewarded);
        void onFailure(String message);
    }

    public PointManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /** Reads users/{uid}.pointBalance, matching the Cloud Function. */
    public void getMyPoints(PointCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("로그인이 필요합니다.");
            return;
        }
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    Object raw = document.get("pointBalance");
                    if (raw == null) {
                        callback.onSuccess(0L);
                    } else if (raw instanceof Number) {
                        callback.onSuccess(((Number) raw).longValue());
                    } else {
                        callback.onFailure("포인트 데이터 형식이 올바르지 않습니다.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("포인트 조회 실패: " + e.getMessage()));
    }

    /**
     * Checks users/{uid}/point_history/{visitRecordId}.
     * This reflects the actual ledger ID used by awardVisitTagPoints.
     * A false result does NOT mean the visit is eligible for a new reward:
     * the server also enforces one award per cafe via point_awards.
     */
    public void checkVisitReward(String visitRecordId, RewardCheckCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("로그인이 필요합니다.");
            return;
        }
        if (visitRecordId == null || visitRecordId.trim().isEmpty()
                || visitRecordId.contains("/")) {
            callback.onFailure("올바른 방문 기록 ID가 아닙니다.");
            return;
        }
        db.collection("users")
                .document(user.getUid())
                .collection("point_history")
                .document(visitRecordId)
                .get()
                .addOnSuccessListener(document -> callback.onResult(document.exists()))
                .addOnFailureListener(e -> callback.onFailure("적립 여부 조회 실패: " + e.getMessage()));
    }
}
