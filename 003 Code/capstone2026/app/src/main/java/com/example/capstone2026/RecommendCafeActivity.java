package com.example.capstone2026;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

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

    private List<Recommender.Recommendation> recommendationList = new ArrayList<>();
    private int loadGeneration = 0;
    private android.widget.TextView statusView;
    private boolean defaultLocation;

    //  화면에 표시할 카페 리스트 및 평점 통계 맵을 멤버 변수로 승격하여 정렬 시 참조
    private List<Recommender.Recommendation> displayList = new ArrayList<>();
    private Map<String, CafeRatingStats> ratingStatsMap = new HashMap<>();

    // 개인화 추천용 태그 점수
    private Map<Tag, Integer> personalizationTagScores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_cafe);

        setupBackButton();
        BottomNavHelper.setup(this);

        recyclerView = findViewById(R.id.recyclerViewCafes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        statusView = findViewById(R.id.textRecommendationStatus);
        statusView.setOnClickListener(v -> reloadRecommendations());
        adapter = new CafeAdapter(displayList);
        recyclerView.setAdapter(adapter);
        setupSortChipGroup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadRecommendations();
    }

    @Override
    protected void onStop() {
        super.onStop();
        loadGeneration++;
    }

    private boolean isCurrentLoad(int generation, String uid) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return !isFinishing() && !isDestroyed() && generation == loadGeneration
                && user != null && uid.equals(user.getUid());
    }

    private void reloadRecommendations() {
        int generation = ++loadGeneration;
        recommendationList.clear();
        displayList.clear();
        ratingStatsMap.clear();
        adapter.notifyDataSetChanged();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            statusView.setText("로그인 후 추천을 확인해주세요.");
            return;
        }
        String uid = user.getUid();
        statusView.setText("카페와 취향 정보를 불러오는 중...");
        RecommendationLoader.load(this, uid,
                HomeSituation.parse(getIntent().getStringExtra("HOME_SITUATION"))).addOnCompleteListener(task -> {
            if (!isCurrentLoad(generation, uid)) return;
            if (!task.isSuccessful()) {
                statusView.setText("추천을 불러오지 못했습니다. 여기를 눌러 다시 시도해주세요.");
                return;
            }
            recommendationList = task.getResult().recommendations;
            defaultLocation = task.getResult().usedDefaultLocation;
            rebuildDisplayList();
            applySelectedSort();
            adapter.notifyDataSetChanged();
            updateStatus();
            loadPersonalizationData(generation, uid);
        });
    }

    private void updateStatus() {
        HomeSituation situation = HomeSituation.parse(getIntent().getStringExtra("HOME_SITUATION"));
        if (displayList.isEmpty()) {
            statusView.setText(TextUtils.isEmpty(getIntent().getStringExtra("SEARCH_QUERY"))
                    ? "등록된 카페가 없습니다. 눌러서 다시 불러오기"
                    : "검색어와 일치하는 카페가 없습니다.");
        } else {
            statusView.setText((situation == null ? "" : situation.label + " · ") + (defaultLocation
                    ? "위치 확인 불가: 대전 궁동·어은동을 기준으로 추천합니다."
                    : "현재 위치와 취향을 기준으로 추천합니다."));
        }
    }

    private void applySelectedSort() {
        ChipGroup group = findViewById(R.id.chipGroupSort);
        int checked = group.getCheckedChipId();
        if (checked == R.id.chipSortDistance) sortRecommendationsByDistance();
        else if (checked == R.id.chipSortRating) sortRecommendationsByRating();
        else if (checked == R.id.chipSortReviews) sortRecommendationsByReviewCount();
        else sortRecommendationsByScore();
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

            if (a.cafe != null && ratingStatsMap.containsKey(a.cafe.id)) {
                CafeRatingStats statsA = ratingStatsMap.get(a.cafe.id);

                if (statsA != null) {
                    ratingA = statsA.avgRating;
                }
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.id)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.id);

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

            if (a.cafe != null && ratingStatsMap.containsKey(a.cafe.id)) {
                CafeRatingStats statsA = ratingStatsMap.get(a.cafe.id);

                if (statsA != null) {
                    reviewCountA = statsA.visitCount;
                }
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.id)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.id);

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

            if (a.cafe != null && ratingStatsMap.containsKey(a.cafe.id)) {
                CafeRatingStats statsA = ratingStatsMap.get(a.cafe.id);

                if (statsA != null) {
                    ratingA = statsA.avgRating;
                }
            }

            if (b.cafe != null && ratingStatsMap.containsKey(b.cafe.id)) {
                CafeRatingStats statsB = ratingStatsMap.get(b.cafe.id);

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

    private void loadPersonalizationData(int generation, String uid) {
        personalizationTagScores.clear();
        SharedPreferences favorites = AccountPreferences.open(this, "CafeFitFavorites");
        List<Recommender.CafeModel> cafes = new ArrayList<>();
        for (Recommender.Recommendation recommendation : recommendationList) {
            cafes.add(recommendation.cafe);
            if (favorites.getBoolean(recommendation.cafe.id, false)) {
                addCafeTagsToPersonalizationScore(recommendation.cafe, 1);
            }
        }
        CafeIdentity identity = new CafeIdentity(cafes);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> visits =
                db.collection("visit_records").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> feedback =
                db.collection("recommendation_feedback").whereEqualTo("userUid", uid).get();
        com.google.android.gms.tasks.Tasks.whenAll(visits, feedback).addOnCompleteListener(done -> {
            if (!isCurrentLoad(generation, uid)) return;
            if (visits.isSuccessful()) {
                List<VisitRecord> records = VisitRecordRepository.records(visits.getResult());
                ratingStatsMap = identity.aggregate(records);
                adapter.setRatingStatsMap(ratingStatsMap);
                for (VisitRecord record : records) {
                    if (!uid.equals(record.getUserUid())) continue;
                    Recommender.CafeModel cafe = findCafeById(
                            identity.resolve(record.getCafeId(), record.getCafeName()));
                    if (cafe == null) continue;
                    if (record.getRating() >= 4) addCafeTagsToPersonalizationScore(cafe, 1);
                    else if (record.getRating() <= 2) addCafeTagsToPersonalizationScore(cafe, -1);
                }
            }
            if (feedback.isSuccessful()) {
                for (QueryDocumentSnapshot document : feedback.getResult()) {
                    Recommender.CafeModel cafe = findCafeById(document.getString("cafeId"));
                    if (cafe == null) continue;
                    String value = document.getString("feedback");
                    if ("LIKE".equals(value)) addCafeTagsToPersonalizationScore(cafe, 1);
                    else if ("DISLIKE".equals(value)) addCafeTagsToPersonalizationScore(cafe, -1);
                }
            }
            Recommender.applyFeedbackScores(recommendationList, personalizationTagScores);
            rebuildDisplayList();
            applySelectedSort();
            adapter.notifyDataSetChanged();
            if (!visits.isSuccessful() || !feedback.isSuccessful()) {
                statusView.setText("일부 평점·피드백을 불러오지 못했습니다. 눌러서 다시 시도해주세요.");
            }
        });
    }

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

}
