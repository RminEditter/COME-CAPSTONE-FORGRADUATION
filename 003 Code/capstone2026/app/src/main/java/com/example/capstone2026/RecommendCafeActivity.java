package com.example.capstone2026;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendCafeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CafeAdapter adapter;

    public static List<Recommender.Recommendation> recommendationList;

    //  화면에 표시할 카페 리스트 및 평점 통계 맵을 멤버 변수로 승격하여 정렬 시 참조
    private List<Recommender.Recommendation> displayList = new ArrayList<>();
    private Map<String, CafeRatingStats> ratingStatsMap = new HashMap<>();

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

        displayList.clear();

        if (!TextUtils.isEmpty(searchQuery)) {
            //  Case 1: 검색어가 존재할 경우 -> 전체 추천 리스트 중 카페 이름에 검색어가 포함된 것만 필터링
            String finalQuery = searchQuery.toLowerCase().trim();

            for (Recommender.Recommendation rec : recommendationList) {
                if (rec.cafe != null && rec.cafe.name != null) {
                    if (rec.cafe.name.toLowerCase().contains(finalQuery)) {
                        displayList.add(rec);
                    }
                }
            }

            if (displayList.isEmpty()) {
                Toast.makeText(this, "'" + searchQuery + "' 검색 결과와 일치하는 카페가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            //  Case 2: 검색어가 없을 경우 (일반 추천 진입) -> 추천 전체 목록 노출
            displayList.addAll(recommendationList);
        }

        // 초기 어댑터 연결
        adapter = new CafeAdapter(displayList);
        recyclerView.setAdapter(adapter);

        // ChipGroup 정렬 리스너 설정
        setupSortChipGroup();

        // Firestore 평점 통계 데이터 로드
        loadRatingStats();
    }

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    //  정렬 칩(ChipGroup) 선택 이벤트 처리
    private void setupSortChipGroup() {
        ChipGroup chipGroupSort = findViewById(R.id.chipGroupSort);
        if (chipGroupSort == null) return;

        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipSortRecommend) {
                // 1. 추천순 (알고리즘 추천 점수 내림차순 -> 동점 시 거리 가까운 순)
                sortRecommendationsByScore();
            } else if (checkedId == R.id.chipSortDistance) {
                // 2. 거리순 (사용자 현재 위치와 가까운 순)
                sortRecommendationsByDistance();
            } else if (checkedId == R.id.chipSortRating) {
                // 3. 평점순 (방문 기록 기준 평균 평점 내림차순)
                sortRecommendationsByRating();
            }

            // 목록 재정렬 반영
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    // 1. 추천순 정렬
    private void sortRecommendationsByScore() {
        if (displayList == null || displayList.isEmpty()) return;
        Collections.sort(displayList, (a, b) -> {
            if (b.score != a.score) {
                return Integer.compare(b.score, a.score); // 점수 높은 순
            }
            return Double.compare(a.distanceMeters, b.distanceMeters); // 점수 같으면 가까운 순
        });
    }

    // 2. 거리순 정렬
    private void sortRecommendationsByDistance() {
        if (displayList == null || displayList.isEmpty()) return;
        Collections.sort(displayList, (a, b) ->
                Double.compare(a.distanceMeters, b.distanceMeters) // 가까운 순
        );
    }

    // 3. 평점순 정렬 (visit_records 기반 실시간 평점 활용)
    private void sortRecommendationsByRating() {
        if (displayList == null || displayList.isEmpty()) return;
        Collections.sort(displayList, (a, b) -> {
            float ratingA = 0.0f;
            float ratingB = 0.0f;

            if (a.cafe != null && ratingStatsMap.containsKey(a.cafe.name)) {
                CafeRatingStats statsA = ratingStatsMap.get(a.cafe.name);
                if (statsA != null) ratingA = statsA.avgRating;
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.name)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.name);
                if (statsB != null) ratingB = statsB.avgRating;
            }

            // 평점이 높은 순으로 정렬 (같으면 추천 점수 순)
            if (ratingB != ratingA) {
                return Float.compare(ratingB, ratingA);
            }
            return Integer.compare(b.score, a.score);
        });
    }

    private void loadRatingStats() {
        FirebaseFirestore.getInstance().collection("visit_records")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ratingStatsMap.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String cafeName = document.getString("cafeName");
                            Double ratingDouble = document.getDouble("rating");
                            float rating = ratingDouble != null ? ratingDouble.floatValue() : 0.0f;

                            if (cafeName == null) {
                                continue;
                            }

                            if (ratingStatsMap.containsKey(cafeName)) {
                                CafeRatingStats stats = ratingStatsMap.get(cafeName);

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
                                ratingStatsMap.put(cafeName, stats);
                            }
                        }

                        if (adapter != null) {
                            adapter.setRatingStatsMap(ratingStatsMap);
                        }
                    } else {
                        Toast.makeText(this, "통계 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}