package com.example.capstone2026;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecommendCafeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCafes;
    private TextView textRecommendTitle;
    private FirebaseFirestore db;
    private List<Tag> selectedTags = new ArrayList<>();
    private String searchQuery = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_cafe);

        // 1. 뷰 및 파이어베이스 초기화
        db = FirebaseFirestore.getInstance();
        recyclerViewCafes = findViewById(R.id.recyclerViewCafes);
        textRecommendTitle = findViewById(R.id.textRecommendTitle);

        recyclerViewCafes.setLayoutManager(new LinearLayoutManager(this));

        // 2. 메인에서 보낸 검색어 확인 및 유저 취향 로드
        searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        loadUserPreferences();

        // 3. 상단 타이틀 텍스트 설정
        if (searchQuery != null && !searchQuery.isEmpty()) {
            textRecommendTitle.setText("'" + searchQuery + "' 검색 결과");
        } else {
            textRecommendTitle.setText("추천 카페 결과");
        }

        // 4. 데이터 로드 및 표시
        fetchAndShowRankings();
    }

    private void loadUserPreferences() {
        SharedPreferences prefs = getSharedPreferences("CafeFitSurvey", MODE_PRIVATE);
        addTagToList(prefs.getString("bean_tag", ""));
        addTagToList(prefs.getString("scale_tag", ""));
        addTagToList(prefs.getString("mood_tag", ""));
        addTagToList(prefs.getString("dessert_tag", ""));
    }

    private void addTagToList(String tagStr) {
        if (tagStr != null && !tagStr.isEmpty()) {
            try {
                selectedTags.add(Tag.valueOf(tagStr));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchAndShowRankings() {
        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Recommender.CafeModel> cafeModels = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Cafe dbCafe = document.toObject(Cafe.class);
                    dbCafe.setId(document.getId());

                    // [검색 로직] 이름에 검색어가 포함되어 있는지 확인 (부분 일치)
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        String cafeName = dbCafe.getName().toLowerCase();
                        String keyword = searchQuery.toLowerCase();

                        // 포함되지 않았다면 리스트에 넣지 않고 스킵
                        if (!cafeName.contains(keyword)) {
                            continue;
                        }
                    }

                    // DB의 String 태그 -> Tag Enum 배열로 변환
                    List<Tag> enumTags = new ArrayList<>();
                    if (dbCafe.getTags() != null) {
                        for (String tagStr : dbCafe.getTags()) {
                            try { enumTags.add(Tag.valueOf(tagStr)); }
                            catch (Exception e) { }
                        }
                    }

                    // Recommender용 모델로 변환
                    cafeModels.add(new Recommender.CafeModel(
                            dbCafe.getId(),
                            dbCafe.getName(),
                            dbCafe.getAddress(),
                            enumTags.toArray(new Tag[0]),
                            dbCafe.getLatitude(),
                            dbCafe.getLongitude()
                    ));
                }

                // 태섭님의 알고리즘 실행 (점수 계산 및 정렬)
                List<Recommender.Recommendation> results = Recommender.recommend(cafeModels, selectedTags);

                if (results.isEmpty()) {
                    Toast.makeText(this, "결과가 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    // 네이버 지도 기능이 포함된 태섭님의 CafeAdapter 연결
                    CafeAdapter adapter = new CafeAdapter(results);
                    recyclerViewCafes.setAdapter(adapter);
                }
            } else {
                Toast.makeText(this, "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}