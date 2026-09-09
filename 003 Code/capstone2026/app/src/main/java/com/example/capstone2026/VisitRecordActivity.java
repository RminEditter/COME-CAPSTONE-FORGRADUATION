package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisitRecordActivity extends AppCompatActivity {
    private RatingBar ratingBar;
    private EditText etMemo;
    private Button btnSaveVisit;
    private String cafeId;
    private String cafeName;
    private String mode;
    private String recordId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record);
        BottomNavHelper.setup(this);
        AppCompatButton back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());
        db = FirebaseFirestore.getInstance();
        TextView name = findViewById(R.id.tvCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        etMemo = findViewById(R.id.etMemo);
        btnSaveVisit = findViewById(R.id.btnSaveVisit);
        mode = getIntent().getStringExtra("mode");
        cafeId = getIntent().getStringExtra("cafeId");
        cafeName = getIntent().getStringExtra("cafeName");
        name.setText(cafeName);
        if ("edit".equals(mode)) {
            recordId = getIntent().getStringExtra("id");
            ratingBar.setRating(getIntent().getFloatExtra("rating", 0));
            etMemo.setText(getIntent().getStringExtra("memo"));
            btnSaveVisit.setText("수정하기");
        }
        btnSaveVisit.setOnClickListener(v -> {
            if ("edit".equals(mode)) updateVisitRecord();
            else saveVisitRecord();
        });
    }

    private void saveVisitRecord() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showMessage("로그인이 필요합니다.");
            return;
        }
        if (!CafeIdentity.hasId(cafeId) || cafeName == null) {
            showMessage("카페 상세 화면에서 매장을 다시 선택해주세요.");
            return;
        }
        String uid = user.getUid();
        float rating = ratingBar.getRating();
        Map<String, Object> record = new HashMap<>();
        record.put("cafeId", cafeId);
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", etMemo.getText().toString().trim());
        record.put("visitedAt", System.currentTimeMillis());
        record.put("userUid", uid);
        btnSaveVisit.setEnabled(false);
        db.collection("visit_records").add(record)
                .addOnSuccessListener(reference -> {
                    showMessage("방문 기록이 서버에 저장되었습니다.");
                    applyBadgesAfterVisit(uid, rating);
                })
                .addOnFailureListener(e -> {
                    btnSaveVisit.setEnabled(true);
                    showMessage("저장에 실패했습니다. 다시 시도해주세요.");
                });
    }

    private void updateVisitRecord() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || recordId == null || recordId.isEmpty()) {
            showMessage("로그인과 방문 기록을 확인해주세요.");
            return;
        }
        String uid = user.getUid();
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();
        DocumentReference reference = db.collection("visit_records").document(recordId);
        btnSaveVisit.setEnabled(false);
        // Preserve the stored cafe ID, owner and visit date, including legacy records.
        db.runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(reference);
            if (!existing.exists() || !uid.equals(existing.getString("userUid"))) {
                throw new IllegalStateException("본인이 작성한 기록만 수정할 수 있습니다.");
            }
            transaction.update(reference, "rating", rating, "memo", memo);
            return null;
        }).addOnSuccessListener(unused -> {
            showMessage("방문 기록이 수정되었습니다.");
            goToHistory();
        }).addOnFailureListener(e -> {
            btnSaveVisit.setEnabled(true);
            showMessage("수정에 실패했습니다. 작성자와 네트워크 상태를 확인해주세요.");
        });
    }

    private void applyBadgesAfterVisit(String uid, float rating) {
        db.collection("cafes").document(cafeId).get().addOnCompleteListener(task -> {
            Recommender.CafeModel cafe = task.isSuccessful() && task.getResult().exists()
                    ? RecommendationLoader.model(task.getResult())
                    : new Recommender.CafeModel(cafeId, cafeName, "", new Tag[0], 0, 0);
            BadgeManager.applyVisitBadges(uid, cafe, rating, new BadgeManager.BadgeUpdateListener() {
                @Override
                public void onComplete(List<String> unlocked) {
                    if (unlocked != null && !unlocked.isEmpty()) {
                        StringBuilder names = new StringBuilder();
                        for (String code : unlocked) {
                            if (names.length() > 0) names.append(", ");
                            names.append(BadgeManager.getBadgeName(code));
                        }
                        showMessage("🏅 새 배지 획득: " + names);
                    }
                    goToHistory();
                }

                @Override
                public void onFailure(Exception e) {
                    showMessage("기록은 저장되었지만 배지를 갱신하지 못했습니다.");
                    goToHistory();
                }
            });
        });
    }

    private void showMessage(String message) {
        if (!isDestroyed()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void goToHistory() {
        if (isFinishing() || isDestroyed()) return;
        Intent intent = new Intent(this, VisitHistoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}