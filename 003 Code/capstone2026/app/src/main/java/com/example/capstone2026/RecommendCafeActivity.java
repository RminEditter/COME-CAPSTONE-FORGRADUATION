package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    private void loadRatingStats() {
        // [수정] Room DB 대신 파이어베이스에서 전체 방문 기록을 가져와 별점 통계 계산
        FirebaseFirestore.getInstance().collection("visit_records")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, CafeRatingStats> map = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String cafeName = document.getString("cafeName");
                            Double ratingDouble = document.getDouble("rating");
                            float rating = ratingDouble != null ? ratingDouble.floatValue() : 0.0f;

                            if (cafeName == null) continue;

                            // 맵에 이미 해당 카페 통계가 있다면 갱신, 없다면 새로 만들기
                            if (map.containsKey(cafeName)) {
                                CafeRatingStats stats = map.get(cafeName);
                                if (stats != null) {
                                    // 기존 평균 점수와 개수를 이용해 새로운 평균 계산 (임시 계산 방식)
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

                        // 어댑터에 계산된 통계 맵 적용
                        if (adapter != null) {
                            adapter.setRatingStatsMap(map);
                        }
                    } else {
                        Toast.makeText(this, "통계 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}