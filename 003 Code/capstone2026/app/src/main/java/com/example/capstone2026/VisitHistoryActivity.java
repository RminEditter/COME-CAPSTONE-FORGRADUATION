package com.example.capstone2026;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VisitHistoryActivity extends AppCompatActivity {

    private RecyclerView rvVisitHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_history);

        rvVisitHistory = findViewById(R.id.rvVisitHistory);
        rvVisitHistory.setLayoutManager(new LinearLayoutManager(this));

        loadVisitRecords();
    }

    private void loadVisitRecords() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<VisitRecord> records = db.visitRecordDao().getAllRecords();

            runOnUiThread(() -> {
                VisitHistoryAdapter adapter = new VisitHistoryAdapter(records);
                rvVisitHistory.setAdapter(adapter);
            });
        }).start();
    }
}