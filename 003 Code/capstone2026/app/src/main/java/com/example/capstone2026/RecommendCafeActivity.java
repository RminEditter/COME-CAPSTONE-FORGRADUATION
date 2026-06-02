package com.example.capstone2026;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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

        String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");

        List<Recommender.Recommendation> displayList = new ArrayList<>();

        if (!TextUtils.isEmpty(searchQuery)) {
            // 🔍 Case 1: 검색어가 존재할 경우 -> 전체 추천 리스트 중 카페 이름에 검색어가 포함된 것만 필터링
            String finalQuery = searchQuery.toLowerCase().trim();

            for (Recommender.Recommendation rec : recommendationList) {
                if (rec.cafe != null && rec.cafe.name != null) {
                    // 대소문자 구분 없이 검색어가 포함되어 있는지 검사
                    if (rec.cafe.name.toLowerCase().contains(finalQuery)) {
                        displayList.add(rec);
                    }
                }
            }

            if (displayList.isEmpty()) {
                Toast.makeText(this, "'" + searchQuery + "' 검색 결과와 일치하는 카페가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // ✨ Case 2: 검색어가 없을 경우 (일반 추천 진입) -> 기존처럼 추천 전체 목록 노출
            displayList.addAll(recommendationList);
        }

        // 💡 기존의 recommendationList 대신, 필터링이 완료된 displayList를 어댑터에 꽂아줍니다!
        adapter = new CafeAdapter(displayList);
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