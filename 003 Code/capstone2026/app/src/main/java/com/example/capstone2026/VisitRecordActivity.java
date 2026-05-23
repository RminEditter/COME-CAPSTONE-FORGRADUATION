package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class VisitRecordActivity extends AppCompatActivity {

    private TextView tvCafeName;
    private RatingBar ratingBar;
    private EditText etMemo;
    private Button btnSaveVisit;

    private String cafeName;
    private String mode;
    private String recordId; // [수정] Firestore는 문서 ID가 문자열(String)입니다.
    private long visitedAt;

    private FirebaseFirestore db; // [추가] 파이어베이스 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record);
        BottomNavHelper.setup(this);

        db = FirebaseFirestore.getInstance(); // 초기화

        androidx.appcompat.widget.AppCompatButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvCafeName = findViewById(R.id.tvCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        etMemo = findViewById(R.id.etMemo);
        btnSaveVisit = findViewById(R.id.btnSaveVisit);

        mode = getIntent().getStringExtra("mode");
        cafeName = getIntent().getStringExtra("cafeName");

        if (cafeName != null) {
            tvCafeName.setText(cafeName);
        }

        if ("edit".equals(mode)) {
            recordId = getIntent().getStringExtra("id"); // String으로 받음
            float rating = getIntent().getFloatExtra("rating", 0.0f);
            String memo = getIntent().getStringExtra("memo");
            visitedAt = getIntent().getLongExtra("visitedAt", 0);

            ratingBar.setRating(rating);
            etMemo.setText(memo);
            btnSaveVisit.setText("수정하기");
        }

        btnSaveVisit.setOnClickListener(v -> {
            if ("edit".equals(mode)) {
                updateVisitRecord();
            } else {
                saveVisitRecord();
            }
        });
    }

    private void saveVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        // 현재 로그인한 유저의 고유 UID 가져오기
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> record = new HashMap<>();
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", memo);
        record.put("visitedAt", System.currentTimeMillis());
        record.put("userUid", currentUid); // 👈 유저 ID 도장 찍기!

        db.collection("visit_records")
                .add(record)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "방문 기록이 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    goToHistory();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> record = new HashMap<>();
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", memo);
        record.put("visitedAt", visitedAt);
        record.put("userUid", currentUid); // 👈 수정할 때도 유저 ID 유지

        db.collection("visit_records").document(recordId)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "방문 기록이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    goToHistory();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goToHistory() {
        Intent intent = new Intent(this, VisitHistoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}