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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private EditText editSearch;
    private AppCompatButton btnRecommendCafe, btnHomeMenu;
    private TextView txtRecommendCafeName, txtRecommendCafeDesc, txtRecentCafe;

    private List<Tag> selectedTags = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private FusedLocationProviderClient fusedLocationClient;

    private double currentLat = 36.3622;   // 기본값: 유성구청 기준
    private double currentLng = 127.3568;
    private boolean isFetching = false;    // 중복 실행 차단 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
        BottomNavHelper.setup(this);

        // 💡 [원하는 시점에 실행]: 주석(//)을 지우면 앱이 켜질 때 전체 카페 태그 업데이트가 시작됩니다!
        // updateAllCafeTags();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        txtRecommendCafeName.setText("취향 분석 중...");
        txtRecommendCafeDesc.setText("서버에서 계정 설문 정보를 가져오고 있습니다 🔍");

        loadUserPreferencesFromFirestore(currentUser.getUid());
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

    /**
     *  절대 지우지 말것 updateAllCafeTags삭제하지말것 절대 지우지말것
     */
    private void updateAllCafeTags() {
        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<QueryDocumentSnapshot> docs = new ArrayList<>();

                for (QueryDocumentSnapshot d : task.getResult()) {
                    docs.add(d);
                }

                for (int i = 0; i < docs.size(); i++) {
                    final int index = i;

                    // 과부하 방지를 위해 0.5초 간격으로 한 개씩 순차 처리
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
                                    .update("tags", tagStrings)
                                    .addOnSuccessListener(aVoid -> Log.d("CafeFit", "태그 업데이트 성공: " + name))
                                    .addOnFailureListener(e -> Log.e("CafeFit", "태그 업데이트 실패: " + name, e));
                        });

                    }, i * 500);
                }
            } else {
                Log.e("CafeFit", "카페 목록 가져오기 실패", task.getException());
            }
        });
    }

    private void loadUserPreferencesFromFirestore(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        selectedTags.clear();

                        if (document.exists()) {
                            addTagToList(document.getString("bean_tag"));
                            addTagToList(document.getString("style_tag"));
                            addTagToList(document.getString("size_tag"));
                            addTagToList(document.getString("companion_tag"));
                            addTagToList(document.getString("dessert_tag"));
                            addTagToList(document.getString("specialty_tag"));
                        }

                        if (selectedTags.isEmpty()) {
                            txtRecommendCafeName.setText("나만의 카페를 찾아보세요!");
                            txtRecommendCafeDesc.setText("오른쪽 상단 메뉴에서 취향 설문을 시작해주세요.");
                        } else {
                            // 💡 위치를 요청하고, 성공 시 추천을 돌리는 안전한 단방향 구조
                            requestCurrentLocation();
                        }
                    } else {
                        Log.e("MainActivity", "서버 프로필 로드 실패", task.getException());
                        txtRecommendCafeName.setText("데이터 로드 실패");
                        txtRecommendCafeDesc.setText("네트워크 상태를 확인해주세요 😢");
                    }
                });
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
        if (isFetching) return; // 무한 루프 렉 원천 차단
        isFetching = true;

        db.collection("cafes")
                .get()
                .addOnCompleteListener(task -> {
                    isFetching = false;
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Recommender.CafeModel> cafeModels = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Cafe dbCafe = document.toObject(Cafe.class);
                            dbCafe.setId(document.getId());

                            List<Tag> enumTags = new ArrayList<>();
                            if (dbCafe.getTags() != null) {
                                for (String tagStr : dbCafe.getTags()) {
                                    try {
                                        enumTags.add(Tag.valueOf(tagStr));
                                    } catch (IllegalArgumentException ignored) {}
                                }
                            }

                            Recommender.CafeModel model = new Recommender.CafeModel(
                                    dbCafe.getId(),
                                    dbCafe.getName(),
                                    dbCafe.getAddress(),
                                    enumTags.toArray(new Tag[0]),
                                    dbCafe.getLatitude(),
                                    dbCafe.getLongitude()
                            );
                            cafeModels.add(model);
                        }

                        // 추천 알고리즘 1회 연산
                        List<Recommender.Recommendation> results =
                                Recommender.recommend(
                                        cafeModels,
                                        selectedTags,
                                        currentLat,
                                        currentLng
                                );

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

    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                Log.d("GPS_CHECK", "GPS 획득 성공: " + currentLat + ", " + currentLng);
            } else {
                Log.d("GPS_CHECK", "GPS 실패: 기본 위치 사용");
            }
            // 💡 무한 재귀호출을 유발하던 구조를 끊고 순수하게 추천 연산만 수행
            fetchCafesAndRecommend();
        }).addOnFailureListener(e -> {
            fetchCafesAndRecommend();
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation();
            } else {
                fetchCafesAndRecommend();
            }
        }
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
}