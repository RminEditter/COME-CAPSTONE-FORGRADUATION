package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editSearch;
    private AppCompatButton btnRecommendCafe, btnHomeMenu;
    private TextView txtRecommendCafeName, txtRecommendCafeDesc, txtRecentCafe;

    private List<Tag> selectedTags = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();

        BottomNavHelper.setup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserPreferences();

        if (selectedTags.isEmpty()) {
            txtRecommendCafeName.setText("나만의 카페를 찾아보세요!");
            txtRecommendCafeDesc.setText("오른쪽 상단 메뉴에서 취향 설문을 시작해주세요.");
        } else {
            txtRecommendCafeName.setText("취향 분석 중...");
            txtRecommendCafeDesc.setText("DB에서 카페를 찾고 있습니다 🔍");
            fetchCafesAndRecommend();
        }
    }

    private void initViews() {
        editSearch = findViewById(R.id.editSearch);
        btnRecommendCafe = findViewById(R.id.btnRecommendCafe);
        btnHomeMenu = findViewById(R.id.btnHomeMenu);

        txtRecommendCafeName = findViewById(R.id.txtRecommendCafeName);
        txtRecommendCafeDesc = findViewById(R.id.txtRecommendCafeDesc);
        txtRecentCafe = findViewById(R.id.txtRecentCafe);

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

    private void fetchCafesAndRecommend() {
        db.collection("cafes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Recommender.CafeModel> cafeModels = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Cafe dbCafe = document.toObject(Cafe.class);
                            dbCafe.setId(document.getId());

                            List<Tag> enumTags = new ArrayList<>();

                            if (dbCafe.getTags() != null) {
                                for (String tagStr : dbCafe.getTags()) {
                                    try {
                                        enumTags.add(Tag.valueOf(tagStr));
                                    } catch (IllegalArgumentException e) {
                                        // 태그 변환 실패 시 무시
                                    }
                                }
                            }

                            Tag[] tagArray = enumTags.toArray(new Tag[0]);

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

                        List<Recommender.Recommendation> results =
                                Recommender.recommend(cafeModels, selectedTags);

                        RecommendCafeActivity.recommendationList = results;

                        if (!results.isEmpty() && results.get(0).score > 0) {
                            Recommender.Recommendation bestMatch = results.get(0);

                            txtRecommendCafeName.setText(bestMatch.cafe.name + " ✨");
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
        btnRecommendCafe.setOnClickListener(v -> {
            String currentTitle = txtRecommendCafeName.getText().toString();

            if (!currentTitle.contains("찾아보세요")
                    && !currentTitle.contains("분석 중")
                    && !currentTitle.contains("없습니다")) {

                String cleanName = currentTitle.replace(" ✨", "");

                getSharedPreferences("CafeFitRecent", MODE_PRIVATE)
                        .edit()
                        .putString("recentCafe", cleanName)
                        .apply();
            }

            Intent intent = new Intent(MainActivity.this, RecommendCafeActivity.class);
            startActivity(intent);
        });

        btnHomeMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, btnHomeMenu);

            popupMenu.getMenu().add("취향 설문 다시 하기");
            popupMenu.getMenu().add("방문 기록 보기");
            popupMenu.getMenu().add("즐겨찾기 목록");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();

                if (title.equals("취향 설문 다시 하기")) {
                    startActivity(new Intent(MainActivity.this, SurveyActivity.class));
                    return true;
                }

                if (title.equals("방문 기록 보기")) {
                    startActivity(new Intent(MainActivity.this, VisitHistoryActivity.class));
                    return true;
                }

                if (title.equals("즐겨찾기 목록")) {
                    startActivity(new Intent(MainActivity.this, FavoriteActivity.class));
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = editSearch.getText().toString().trim();

            if (!query.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, RecommendCafeActivity.class);
                intent.putExtra("SEARCH_QUERY", query);
                startActivity(intent);
                return true;
            } else {
                Toast.makeText(MainActivity.this, "검색어를 입력해주세요!", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    private void updateAllCafeTags() {
        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<QueryDocumentSnapshot> docs = new ArrayList<>();

                for (QueryDocumentSnapshot d : task.getResult()) {
                    docs.add(d);
                }

                for (int i = 0; i < docs.size(); i++) {
                    final int index = i;

                    new android.os.Handler().postDelayed(() -> {
                        QueryDocumentSnapshot document = docs.get(index);
                        String name = document.getString("name");
                        String addr = document.getString("address");

                        NaverReviewAnalyzer.analyzeCafe(name, addr, tags -> {
                            List<String> tagStrings = new ArrayList<>();

                            for (Tag t : tags) {
                                tagStrings.add(t.name());
                            }

                            db.collection("cafes")
                                    .document(document.getId())
                                    .update("tags", tagStrings);

                            Log.e("CafeFit", "성공: " + name);
                        });

                    }, i * 500);
                }
            }
        });
    }
}