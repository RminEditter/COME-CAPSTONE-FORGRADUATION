package com.example.capstone2026;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    // 개인화 추천용 태그 점수
    private Map<Tag, Integer> personalizationTagScores = new HashMap<>();

    private boolean feedbackLoaded = false;
    private boolean ratingPreferenceLoaded = false;

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

        // 개인화 추천 데이터 로드
        loadPersonalizationData();
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
            } else if (checkedId == R.id.chipSortReviews) {
                // 4. 리뷰 많은 순 (방문 기록 개수 내림차순)
                sortRecommendationsByReviewCount();
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

            return Double.compare(
                    a.distanceMeters,
                    b.distanceMeters
            ); // 점수 같으면 가까운 순
        });
    }

    // 2. 거리순 정렬
    private void sortRecommendationsByDistance() {
        if (displayList == null || displayList.isEmpty()) return;

        Collections.sort(displayList, (a, b) ->
                Double.compare(
                        a.distanceMeters,
                        b.distanceMeters
                ) // 가까운 순
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

                if (statsA != null) {
                    ratingA = statsA.avgRating;
                }
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.name)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.name);

                if (statsB != null) {
                    ratingB = statsB.avgRating;
                }
            }

            // 평점이 높은 순으로 정렬 (같으면 추천 점수 순)
            if (ratingB != ratingA) {
                return Float.compare(
                        ratingB,
                        ratingA
                );
            }

            return Integer.compare(
                    b.score,
                    a.score
            );
        });
    }

    // 4. 리뷰 많은 순 정렬 (visit_records의 카페별 리뷰 개수 기준)
    private void sortRecommendationsByReviewCount() {
        if (displayList == null || displayList.isEmpty()) return;

        Collections.sort(displayList, (a, b) -> {

            int reviewCountA = 0;
            int reviewCountB = 0;

            if (a.cafe != null && ratingStatsMap.containsKey(a.cafe.name)) {
                CafeRatingStats statsA = ratingStatsMap.get(a.cafe.name);

                if (statsA != null) {
                    reviewCountA = statsA.visitCount;
                }
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.name)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.name);

                if (statsB != null) {
                    reviewCountB = statsB.visitCount;
                }
            }

            // 리뷰 수가 많은 순으로 정렬
            if (reviewCountB != reviewCountA) {
                return Integer.compare(
                        reviewCountB,
                        reviewCountA
                );
            }

            // 리뷰 수가 같으면 평균 평점이 높은 순
            float ratingA = 0.0f;
            float ratingB = 0.0f;

            if (a.cafe != null && ratingStatsMap.containsKey(a.cafe.name)) {
                CafeRatingStats statsA = ratingStatsMap.get(a.cafe.name);

                if (statsA != null) {
                    ratingA = statsA.avgRating;
                }
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.name)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.name);

                if (statsB != null) {
                    ratingB = statsB.avgRating;
                }
            }

            if (ratingB != ratingA) {
                return Float.compare(
                        ratingB,
                        ratingA
                );
            }

            // 리뷰 수와 평점까지 같으면 추천 점수가 높은 순
            return Integer.compare(
                    b.score,
                    a.score
            );
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

    // 개인화 추천 데이터 로드
    private void loadPersonalizationData() {

        personalizationTagScores.clear();

        // 즐겨찾기 데이터는 로컬에서 바로 반영
        applyFavoritePreferences();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            feedbackLoaded = true;
            ratingPreferenceLoaded = true;
            applyPersonalizationIfReady();
            return;
        }

        String uid = currentUser.getUid();

        loadRecommendationFeedback(uid);
        loadMyRatingPreferences(uid);
    }

    // 즐겨찾기한 카페의 태그를 선호 태그로 반영
    private void applyFavoritePreferences() {

        SharedPreferences prefs = getSharedPreferences(
                "CafeFitFavorites",
                MODE_PRIVATE
        );

        if (recommendationList == null) {
            return;
        }

        for (Recommender.Recommendation recommendation : recommendationList) {

            if (recommendation == null ||
                    recommendation.cafe == null ||
                    recommendation.cafe.id == null) {
                continue;
            }

            boolean isFavorite = prefs.getBoolean(
                    recommendation.cafe.id,
                    false
            );

            if (!isFavorite) {
                continue;
            }

            addCafeTagsToPersonalizationScore(
                    recommendation.cafe,
                    1
            );
        }
    }

    // 추천 정확도 피드백 데이터를 불러와 개인화 점수에 반영
    private void loadRecommendationFeedback(String uid) {

        FirebaseFirestore.getInstance()
                .collection("recommendation_feedback")
                .whereEqualTo("userUid", uid)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {

                        for (QueryDocumentSnapshot document : task.getResult()) {

                            String cafeId = document.getString("cafeId");
                            String feedback = document.getString("feedback");

                            if (cafeId == null || feedback == null) {
                                continue;
                            }

                            Recommender.CafeModel cafe =
                                    findCafeById(cafeId);

                            if (cafe == null) {
                                continue;
                            }

                            if ("LIKE".equals(feedback)) {

                                addCafeTagsToPersonalizationScore(
                                        cafe,
                                        1
                                );

                            } else if ("DISLIKE".equals(feedback)) {

                                addCafeTagsToPersonalizationScore(
                                        cafe,
                                        -1
                                );
                            }
                        }
                    }

                    feedbackLoaded = true;
                    applyPersonalizationIfReady();
                });
    }

    // 현재 로그인 사용자의 과거 별점을 불러와 개인화 점수에 반영
    private void loadMyRatingPreferences(String uid) {

        FirebaseFirestore.getInstance()
                .collection("visit_records")
                .whereEqualTo("userUid", uid)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {

                        for (QueryDocumentSnapshot document : task.getResult()) {

                            String cafeName = document.getString("cafeName");
                            Double ratingDouble = document.getDouble("rating");

                            if (cafeName == null || ratingDouble == null) {
                                continue;
                            }

                            float rating = ratingDouble.floatValue();

                            Recommender.CafeModel cafe =
                                    findCafeByName(cafeName);

                            if (cafe == null) {
                                continue;
                            }

                            // 4점 이상은 사용자가 선호한 카페로 판단
                            if (rating >= 4.0f) {

                                addCafeTagsToPersonalizationScore(
                                        cafe,
                                        1
                                );

                                // 2점 이하는 사용자가 선호하지 않은 카페로 판단
                            } else if (rating <= 2.0f) {

                                addCafeTagsToPersonalizationScore(
                                        cafe,
                                        -1
                                );
                            }
                        }
                    }

                    ratingPreferenceLoaded = true;
                    applyPersonalizationIfReady();
                });
    }

    // 카페가 가진 태그 전체에 개인화 선호 점수를 누적
    private void addCafeTagsToPersonalizationScore(
            Recommender.CafeModel cafe,
            int value
    ) {

        if (cafe == null || cafe.tags == null) {
            return;
        }

        for (Tag tag : cafe.tags) {

            if (tag == null) {
                continue;
            }

            int currentScore = 0;

            if (personalizationTagScores.containsKey(tag)) {
                Integer savedScore =
                        personalizationTagScores.get(tag);

                if (savedScore != null) {
                    currentScore = savedScore;
                }
            }

            personalizationTagScores.put(
                    tag,
                    currentScore + value
            );
        }
    }

    // 추천 피드백과 별점 데이터를 모두 읽은 뒤 실제 추천 점수에 반영
    private void applyPersonalizationIfReady() {

        if (!feedbackLoaded || !ratingPreferenceLoaded) {
            return;
        }

        Recommender.applyFeedbackScores(
                recommendationList,
                personalizationTagScores
        );

        rebuildDisplayList();

        sortRecommendationsByScore();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // 개인화 추천 점수 적용 후 검색 조건을 유지한 채 화면 목록 재생성
    private void rebuildDisplayList() {

        displayList.clear();

        String searchQuery =
                getIntent().getStringExtra("SEARCH_QUERY");

        if (!TextUtils.isEmpty(searchQuery)) {

            String finalQuery =
                    searchQuery.toLowerCase().trim();

            for (Recommender.Recommendation rec :
                    recommendationList) {

                if (rec.cafe != null &&
                        rec.cafe.name != null &&
                        rec.cafe.name
                                .toLowerCase()
                                .contains(finalQuery)) {

                    displayList.add(rec);
                }
            }

        } else {

            displayList.addAll(recommendationList);
        }
    }

    private Recommender.CafeModel findCafeById(String cafeId) {

        if (recommendationList == null ||
                cafeId == null) {
            return null;
        }

        for (Recommender.Recommendation recommendation :
                recommendationList) {

            if (recommendation == null ||
                    recommendation.cafe == null ||
                    recommendation.cafe.id == null) {
                continue;
            }

            if (cafeId.equals(recommendation.cafe.id)) {
                return recommendation.cafe;
            }
        }

        return null;
    }

    private Recommender.CafeModel findCafeByName(String cafeName) {

        if (recommendationList == null ||
                cafeName == null) {
            return null;
        }

        for (Recommender.Recommendation recommendation :
                recommendationList) {

            if (recommendation == null ||
                    recommendation.cafe == null ||
                    recommendation.cafe.name == null) {
                continue;
            }

            if (cafeName.equals(recommendation.cafe.name)) {
                return recommendation.cafe;
            }
        }

        return null;
    }
}