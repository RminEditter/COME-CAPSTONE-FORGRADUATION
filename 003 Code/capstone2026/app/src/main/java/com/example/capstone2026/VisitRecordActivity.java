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
    private String recordId;
    private long visitedAt;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record);

        setupBackButton();
        BottomNavHelper.setup(this);

        db = FirebaseFirestore.getInstance();

        tvCafeName = findViewById(R.id.tvCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        etMemo = findViewById(R.id.etMemo);
        btnSaveVisit = findViewById(R.id.btnSaveVisit);

        mode = getIntent().getStringExtra("mode");
        cafeName = getIntent().getStringExtra("cafeName");

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLICKED_CAFE_NAME")) {
            String filterCafeName = intent.getStringExtra("CLICKED_CAFE_NAME");

            Toast.makeText(this, filterCafeName + " 기록을 불러옵니다!", Toast.LENGTH_SHORT).show();
        }

        if (cafeName != null) {
            tvCafeName.setText(cafeName);
        }

        if ("edit".equals(mode)) {
            recordId = getIntent().getStringExtra("id");
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

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void saveVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> record = new HashMap<>();
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", memo);
        record.put("visitedAt", System.currentTimeMillis());
        record.put("userUid", currentUid);

        db.collection("visit_records")
                .add(record)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "방문 기록이 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    goToHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> record = new HashMap<>();
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", memo);
        record.put("visitedAt", visitedAt);
        record.put("userUid", currentUid);

        db.collection("visit_records")
                .document(recordId)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "방문 기록이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    goToHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void goToHistory() {
        Intent intent = new Intent(this, VisitHistoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}