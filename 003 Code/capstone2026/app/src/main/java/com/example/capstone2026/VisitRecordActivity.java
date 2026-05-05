package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VisitRecordActivity extends AppCompatActivity {

    private TextView tvCafeName;
    private RatingBar ratingBar;
    private EditText etMemo;
    private Button btnSaveVisit;

    private String cafeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record);

        tvCafeName = findViewById(R.id.tvCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        etMemo = findViewById(R.id.etMemo);
        btnSaveVisit = findViewById(R.id.btnSaveVisit);

        cafeName = getIntent().getStringExtra("cafeName");

        if (cafeName == null) {
            cafeName = "알 수 없는 카페";
        }

        tvCafeName.setText(cafeName);

        btnSaveVisit.setOnClickListener(v -> saveVisitRecord());
    }

    private void saveVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();
        long visitedAt = System.currentTimeMillis();

        VisitRecord record = new VisitRecord(
                cafeName,
                rating,
                memo,
                visitedAt
        );

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.visitRecordDao().insert(record);

            runOnUiThread(() -> {
                Toast.makeText(this, "방문 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();

        android.util.Log.d("TEST", "저장됨: " + cafeName + ", rating=" + rating);
    }
}