package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendCafeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CafeAdapter adapter;

    // 이전 화면에서 추천 결과를 임시로 담아두는 변수
    public static List<Recommender.Recommendation> recommendationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_cafe);

        recyclerView = findViewById(R.id.recyclerViewCafes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (recommendationList == null || recommendationList.isEmpty()) {
            Toast.makeText(this, "추천 카페 목록이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new CafeAdapter(recommendationList);
        recyclerView.setAdapter(adapter);

        loadRatingStats();
    }

    private void loadRatingStats() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<CafeRatingStats> statsList = db.visitRecordDao().getCafeRatingStats();

            Map<String, CafeRatingStats> map = new HashMap<>();

            for (CafeRatingStats stats : statsList) {
                map.put(stats.cafeName, stats);
            }

            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setRatingStatsMap(map);
                }
            });

        }).start();
    }
}