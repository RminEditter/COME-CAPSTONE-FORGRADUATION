package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendCafeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CafeAdapter adapter;

    public static List<Recommender.Recommendation> recommendationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_cafe);

        setupBackButton();
        BottomNavHelper.setup(this);

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

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadRatingStats() {
        FirebaseFirestore.getInstance().collection("visit_records")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, CafeRatingStats> map = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String cafeName = document.getString("cafeName");
                            Double ratingDouble = document.getDouble("rating");
                            float rating = ratingDouble != null ? ratingDouble.floatValue() : 0.0f;

                            if (cafeName == null) {
                                continue;
                            }

                            if (map.containsKey(cafeName)) {
                                CafeRatingStats stats = map.get(cafeName);

                                if (stats != null) {
                                    float totalRating = (stats.avgRating * stats.visitCount) + rating;
                                    stats.visitCount += 1;
                                    stats.avgRating = totalRating / stats.visitCount;
                                }
                            } else {
                                CafeRatingStats stats = new CafeRatingStats();
                                stats.cafeName = cafeName;
                                stats.avgRating = rating;
                                stats.visitCount = 1;
                                map.put(cafeName, stats);
                            }
                        }

                        if (adapter != null) {
                            adapter.setRatingStatsMap(map);
                        }
                    } else {
                        Toast.makeText(this, "통계 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}