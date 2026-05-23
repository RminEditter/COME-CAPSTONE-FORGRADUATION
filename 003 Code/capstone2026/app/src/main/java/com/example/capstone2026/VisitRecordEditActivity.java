package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VisitRecordEditActivity extends AppCompatActivity {

    private TextView txtCafeName;
    private RatingBar ratingBar;
    private EditText editMemo;
    private Button btnSaveVisitRecord;

    private String cafeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record_edit);

        txtCafeName = findViewById(R.id.txtCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        editMemo = findViewById(R.id.editMemo);
        btnSaveVisitRecord = findViewById(R.id.btnSaveVisitRecord);

        cafeName = getIntent().getStringExtra("cafe_name");

        if (cafeName == null || cafeName.isEmpty()) {
            cafeName = "카페 이름 없음";
        }

        txtCafeName.setText(cafeName);

        btnSaveVisitRecord.setOnClickListener(v -> saveVisitRecord());
    }

    private void saveVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = editMemo.getText().toString().trim();
        long visitedAt = System.currentTimeMillis();

        if (rating <= 0) {
            Toast.makeText(this, "별점을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        VisitRecord record = new VisitRecord(cafeName, rating, memo, visitedAt);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.visitRecordDao().insert(record);

            runOnUiThread(() -> {
                Toast.makeText(this, "방문 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}