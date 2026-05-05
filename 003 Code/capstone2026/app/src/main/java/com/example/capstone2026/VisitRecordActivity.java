package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class VisitRecordActivity extends AppCompatActivity {

    private TextView tvCafeName;
    private RatingBar ratingBar;
    private EditText etMemo;
    private Button btnSaveVisit;

    private String cafeName;
    private String mode;
    private int recordId;
    private long visitedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record);

        tvCafeName = findViewById(R.id.tvCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        etMemo = findViewById(R.id.etMemo);
        btnSaveVisit = findViewById(R.id.btnSaveVisit);

        mode = getIntent().getStringExtra("mode");
        cafeName = getIntent().getStringExtra("cafeName");

        if (cafeName == null) {
            cafeName = "알 수 없는 카페";
        }

        tvCafeName.setText(cafeName);

        if ("edit".equals(mode)) {
            recordId = getIntent().getIntExtra("id", -1);
            float rating = getIntent().getFloatExtra("rating", 3.0f);
            String memo = getIntent().getStringExtra("memo");
            visitedAt = getIntent().getLongExtra("visitedAt", System.currentTimeMillis());

            ratingBar.setRating(rating);
            etMemo.setText(memo);
            btnSaveVisit.setText("방문 기록 수정");
        } else {
            visitedAt = System.currentTimeMillis();
            btnSaveVisit.setText("방문 기록 저장");
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

        VisitRecord record = new VisitRecord(
                cafeName,
                rating,
                memo,
                System.currentTimeMillis()
        );

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.visitRecordDao().insert(record);

            runOnUiThread(() -> {
                Toast.makeText(this, "방문 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, VisitHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    private void updateVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        VisitRecord record = new VisitRecord(
                cafeName,
                rating,
                memo,
                visitedAt
        );

        record.setId(recordId);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.visitRecordDao().update(record);

            runOnUiThread(() -> {
                Toast.makeText(this, "방문 기록이 수정되었습니다.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, VisitHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }).start();
    }
}