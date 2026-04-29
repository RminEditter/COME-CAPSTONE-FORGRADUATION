package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editSearch;
    private Button btnSurveyStart, btnRecommendCafe, btnProfile;
    private TextView txtRecommendCafeName, txtRecommendCafeDesc, txtRecentCafe;

    private List<Tag> selectedTags = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 파이어베이스 초기화
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
    }
    //테스트
    @Override
    protected void onResume() {
        super.onResume();
        loadUserPreferences();

        if (selectedTags.isEmpty()) {
            txtRecommendCafeName.setText("나만의 카페를 찾아보세요!");
            txtRecommendCafeDesc.setText("아래 '취향 설문' 버튼을 눌러 설문을 시작해주세요.");
        } else {
            txtRecommendCafeName.setText("취향 분석 중...");
            txtRecommendCafeDesc.setText("DB에서 카페를 찾고 있습니다 🔍");
            fetchCafesAndRecommend();
        }
    }

    private void initViews() {
        editSearch = findViewById(R.id.editSearch);
        btnSurveyStart = findViewById(R.id.btnSurveyStart);
        btnRecommendCafe = findViewById(R.id.btnRecommendCafe);
        txtRecommendCafeName = findViewById(R.id.txtRecommendCafeName);
        txtRecommendCafeDesc = findViewById(R.id.txtRecommendCafeDesc);
        txtRecentCafe = findViewById(R.id.txtRecentCafe);
        btnProfile = findViewById(R.id.btnProfile);

        SharedPreferences recentPrefs = getSharedPreferences("CafeFitRecent", MODE_PRIVATE);
        txtRecentCafe.setText(recentPrefs.getString("recentCafe", "최근 본 카페가 없습니다."));
    }

    private void loadUserPreferences() {
        SharedPreferences prefs = getSharedPreferences("CafeFitSurvey", MODE_PRIVATE);
        String bean = prefs.getString("bean_tag", "");
        String scale = prefs.getString("scale_tag", "");
        String mood = prefs.getString("mood_tag", "");
        String dessert = prefs.getString("dessert_tag", "");

        selectedTags.clear();
        addTagToList(bean);
        addTagToList(scale);
        addTagToList(mood);
        addTagToList(dessert);
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

    // 🚀 DB에서 가져온 데이터를 Recommender.CafeModel로 변환 후 추천!
    private void fetchCafesAndRecommend() {
        db.collection("cafes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Recommender.CafeModel> cafeModels = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Cafe dbCafe = document.toObject(Cafe.class);
                            dbCafe.setId(document.getId());

                            // 1. DB의 String 태그들을 Tag Enum 배열로 변환
                            List<Tag> enumTags = new ArrayList<>();
                            for (String tagStr : dbCafe.getTags()) {
                                try { enumTags.add(Tag.valueOf(tagStr)); }
                                catch (IllegalArgumentException e) { /* 무시 */ }
                            }
                            Tag[] tagArray = enumTags.toArray(new Tag[0]);

                            // 2. 태섭님의 CafeModel 형식으로 포장
                            Recommender.CafeModel model = new Recommender.CafeModel(
                                    dbCafe.getId(),
                                    dbCafe.getName(),
                                    dbCafe.getAddress(),
                                    tagArray,
                                    dbCafe.getLatitude(),
                                    dbCafe.getLongitude()
                            );
                            cafeModels.add(model);
                        }

                        // 3. 알고리즘 실행 (결과 리스트 받기)
                        List<Recommender.Recommendation> results = Recommender.recommend(cafeModels, selectedTags);

                        // 4. 결과가 있고, 취향이 1개라도 일치(score > 0)하는 카페가 있다면 1등 표시
                        if (!results.isEmpty() && results.get(0).score > 0) {
                            Recommender.Recommendation bestMatch = results.get(0);

                            txtRecommendCafeName.setText(bestMatch.cafe.name + " ✨");

                            // 주소와 함께 ReasonGenerator가 만들어준 '추천 사유'까지 같이 띄워줍니다!
                            txtRecommendCafeDesc.setText(bestMatch.cafe.address + "\n" + bestMatch.reason);
                        } else {
                            txtRecommendCafeName.setText("추천 카페가 없습니다.");
                            txtRecommendCafeDesc.setText("조건에 맞는 카페를 찾지 못했어요 😢");
                        }

                    } else {
                        Log.d("MainActivity", "DB 가져오기 실패: ", task.getException());
                        Toast.makeText(MainActivity.this, "DB 연결 에러", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        // 1. 취향 설문 시작 버튼
        btnSurveyStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
            startActivity(intent);
        });

        // 2. 프로필(마이페이지) 버튼
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // 3. 추천 카페 보기 버튼 (전체 추천 목록으로 이동)
        btnRecommendCafe.setOnClickListener(v -> {
            // 메인에 떠있는 1등 카페 이름을 '최근 본 카페'로 저장
            String currentTitle = txtRecommendCafeName.getText().toString();
            if (!currentTitle.contains("찾아보세요") && !currentTitle.contains("분석 중") && !currentTitle.contains("없습니다")) {
                String cleanName = currentTitle.replace(" ✨", "");
                getSharedPreferences("CafeFitRecent", MODE_PRIVATE)
                        .edit().putString("recentCafe", cleanName).apply();
            }

            // 전체 추천 리스트 화면으로 이동
            Intent intent = new Intent(MainActivity.this, RecommendCafeActivity.class);
            startActivity(intent);
        });

        // 4. 검색창(EditText) 검색 기능 구현
        // 키보드에서 '돋보기' 혹은 '엔터' 아이콘을 눌렀을 때 작동합니다.
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = editSearch.getText().toString().trim();

            if (!query.isEmpty()) {
                // 검색어를 Intent에 담아서 RecommendCafeActivity로 보냅니다.
                Intent intent = new Intent(MainActivity.this, RecommendCafeActivity.class);
                intent.putExtra("SEARCH_QUERY", query);
                startActivity(intent);
                return true; // 이벤트 처리 완료
            } else {
                Toast.makeText(MainActivity.this, "검색어를 입력해주세요!", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}