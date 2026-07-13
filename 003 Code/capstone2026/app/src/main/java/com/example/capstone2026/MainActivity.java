package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private TextView txtTopCafe1, txtTopCafe2, txtTopCafe3;

    private List<Tag> selectedTags = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private FusedLocationProviderClient fusedLocationClient;

    private double currentLat = 36.3622;
    private double currentLng = 127.3568;

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
        //cleanupFranchiseDataAllInOne(); 프렌차이즈 지우기
        //updateAllCafeTags(); 태그부여 함수 건들지말것.
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences recentPrefs = getSharedPreferences("CafeFitRecent", MODE_PRIVATE);
        String recentCafeName = recentPrefs.getString("recentCafe", "최근 본 카페가 없습니다.");
        txtRecentCafe.setText(recentCafeName);

        if (!recentCafeName.equals("최근 본 카페가 없습니다.") && !recentCafeName.isEmpty()) {
            txtRecentCafe.setOnClickListener(v -> {
                queryCafeAndGoDetail(recentCafeName);
            });
        } else {
            txtRecentCafe.setOnClickListener(null);
        }

        fetchAllUsersHighestRatedCafes();

        // 💡 필요할 때 주석을 해제하여 네이버 리뷰 크롤링 및 태그 분석 함수를 작동시킵니다.
        // updateAllCafeTags();

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

        txtTopCafe1 = findViewById(R.id.txtTopCafe1);
        txtTopCafe2 = findViewById(R.id.txtTopCafe2);
        txtTopCafe3 = findViewById(R.id.txtTopCafe3);
    }


    private void updateAllCafeTags() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot d : task.getResult()) {
                    docs.add(d);
                }

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                for (int i = 0; i < docs.size(); i++) {
                    final int index = i;
                    final QueryDocumentSnapshot document = docs.get(index);
                    String name = document.getString("name");
                    String addr = document.getString("address");

                    // 💡 프랜차이즈 매장은 스킵하여 네트워크 요청 절약
                    if (isFranchise(name)) {
                        Log.d("CafeFit", "프랜차이즈 스킵: " + name);
                        continue;
                    }

                    // 💡 500ms 간격으로 백그라운드 스레드에서 순차 실행
                    executor.schedule(() -> {
                        if (name == null) return;

                        NaverReviewAnalyzer.analyzeCafe(name, addr, tags -> {
                            // 💡 태그가 정상적으로 수집된 경우에만 DB 갱신 (데이터 소실 방지)
                            if (tags != null && !tags.isEmpty()) {

                                List<String> tagStrings = new ArrayList<>();
                                for (Tag t : tags) {
                                    tagStrings.add(t.name());
                                }

                                Map<String, Object> updateData = new HashMap<>();
                                updateData.put("tags", tagStrings);

                                db.collection("cafes")
                                        .document(document.getId())
                                        .update(updateData)
                                        .addOnSuccessListener(aVoid -> Log.d("CafeFit", "성공: " + name + " -> 태그 " + tagStrings.size() + "개"))
                                        .addOnFailureListener(e -> Log.e("CafeFit", "DB 업데이트 실패: " + name, e));
                            } else {
                                Log.w("CafeFit", "태그 수집 실패 또는 데이터 없음: " + name);
                            }
                        });

                    }, i * 500, TimeUnit.MILLISECONDS);
                }

                // 스케줄러 종료 예약
                executor.shutdown();
            } else {
                Log.e("CafeFit", "카페 목록 불러오기 실패", task.getException());
            }
        });
    }

    // 프랜차이즈 필터링 보조 메서드
    private boolean isFranchise(String cafeName) {
        if (cafeName == null) return false;
        String[] franchises = {"스타벅스", "투썸", "메가", "컴포즈", "빽다방", "할리스", "이디야", "공차", "파스쿠찌"};
        for (String f : franchises) {
            if (cafeName.contains(f)) return true;
        }
        return false;
    }


    private void fetchAllUsersHighestRatedCafes() {
        if (txtTopCafe1 == null || txtTopCafe2 == null || txtTopCafe3 == null) return;

        txtTopCafe1.setText("1. 전체 평점 계산 중...");
        txtTopCafe2.setText("2. 전체 평점 계산 중...");
        txtTopCafe3.setText("3. 전체 평점 계산 중...");

        db.collection("visit_records")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {

                        Map<String, List<Double>> cafeRatingsMap = new HashMap<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String cafeName = doc.getString("cafeName");
                            Object ratingObj = doc.get("rating");

                            if (cafeName != null && ratingObj != null) {
                                double rating = 0.0;
                                try {
                                    if (ratingObj instanceof Number) {
                                        rating = ((Number) ratingObj).doubleValue();
                                    } else {
                                        rating = Double.parseDouble(String.valueOf(ratingObj));
                                    }
                                    if (!cafeRatingsMap.containsKey(cafeName)) {
                                        cafeRatingsMap.put(cafeName, new ArrayList<>());
                                    }
                                    cafeRatingsMap.get(cafeName).add(rating);
                                } catch (Exception ignored) {}
                            }
                        }

                        if (cafeRatingsMap.isEmpty()) {
                            txtTopCafe1.setText("1. 등록된 평점 기록이 없습니다.");
                            txtTopCafe2.setText("2. 평점 데이터 없음");
                            txtTopCafe3.setText("3. 평점 데이터 없음");
                            return;
                        }

                        List<Map.Entry<String, Double>> cafeAverageList = new ArrayList<>();
                        for (Map.Entry<String, List<Double>> entry : cafeRatingsMap.entrySet()) {
                            double sum = 0;
                            for (double r : entry.getValue()) sum += r;
                            double avg = sum / entry.getValue().size();
                            avg = Math.round(avg * 10.0) / 10.0;
                            cafeAverageList.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), avg));
                        }

                        Collections.sort(cafeAverageList, (e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                        if (cafeAverageList.size() > 0) {
                            String name1 = cafeAverageList.get(0).getKey();
                            txtTopCafe1.setText("1. " + name1 + "  ★" + cafeAverageList.get(0).getValue());
                            txtTopCafe1.setOnClickListener(v -> queryCafeAndGoDetail(name1));
                        } else {
                            txtTopCafe1.setText("1. 평점 데이터 없음");
                            txtTopCafe1.setOnClickListener(null);
                        }

                        if (cafeAverageList.size() > 1) {
                            String name2 = cafeAverageList.get(1).getKey();
                            txtTopCafe2.setText("2. " + name2 + "  ★" + cafeAverageList.get(1).getValue());
                            txtTopCafe2.setOnClickListener(v -> queryCafeAndGoDetail(name2));
                        } else {
                            txtTopCafe2.setText("2. 평점 데이터 없음");
                            txtTopCafe2.setOnClickListener(null);
                        }

                        if (cafeAverageList.size() > 2) {
                            String name3 = cafeAverageList.get(2).getKey();
                            txtTopCafe3.setText("3. " + name3 + "  ★" + cafeAverageList.get(2).getValue());
                            txtTopCafe3.setOnClickListener(v -> queryCafeAndGoDetail(name3));
                        } else {
                            txtTopCafe3.setText("3. 평점 데이터 없음");
                            txtTopCafe3.setOnClickListener(null);
                        }

                    } else {
                        txtTopCafe1.setText("종합 순위 로드 실패 😢");
                        txtTopCafe2.setText("네트워크 상태를 확인해 주세요.");
                        txtTopCafe3.setText("네트워크 상태를 확인해 주세요.");
                    }
                });
    }

    /**
     * 🎯 [화면 연동 최적화 엔진]
     */
    private void queryCafeAndGoDetail(String targetCafeName) {
        if (RecommendCafeActivity.recommendationList != null && !RecommendCafeActivity.recommendationList.isEmpty()) {
            for (Recommender.Recommendation r : RecommendCafeActivity.recommendationList) {
                if (r.cafe != null && r.cafe.name != null && r.cafe.name.equals(targetCafeName)) {

                    StringBuilder tagText = new StringBuilder();
                    if (r.cafe.tags != null) {
                        for (Tag tag : r.cafe.tags) {
                            tagText.append("#")
                                    .append(tag.getKoreanLabel())
                                    .append(" ");
                        }
                    }

                    Intent intent = new Intent(MainActivity.this, CafeDetailActivity.class);
                    intent.putExtra("cafe_id", r.cafe.id);
                    intent.putExtra("cafe_name", r.cafe.name);
                    intent.putExtra("cafe_address", r.cafe.address);
                    intent.putExtra("cafe_reason", r.reason);
                    intent.putExtra("cafe_tags", tagText.toString());

                    startActivity(intent);
                    return;
                }
            }
        }

        db.collection("cafes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot matchedDoc = null;
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String dbCafeName = doc.getString("name");
                            if (dbCafeName != null && dbCafeName.equals(targetCafeName)) {
                                matchedDoc = doc;
                                break;
                            }
                        }

                        Intent intent = new Intent(MainActivity.this, CafeDetailActivity.class);

                        if (matchedDoc != null) {
                            String cafeId = matchedDoc.getId();
                            String address = matchedDoc.getString("address");
                            if (address == null) address = "주소 정보 없음";

                            StringBuilder tagText = new StringBuilder();
                            List<String> rawTags = (List<String>) matchedDoc.get("tags");
                            if (rawTags != null) {
                                for (String tagStr : rawTags) {
                                    if (tagStr == null || tagStr.trim().isEmpty()) continue;
                                    try {
                                        String cleanTag = tagStr.trim().toUpperCase().replace(" ", "_");
                                        Tag tag = Tag.valueOf(cleanTag);
                                        tagText.append("#").append(tag.getKoreanLabel()).append(" ");
                                    } catch (IllegalArgumentException e) {
                                        tagText.append("#").append(tagStr.trim()).append(" ");
                                    }
                                }
                            }

                            intent.putExtra("cafe_id", cafeId);
                            intent.putExtra("cafe_name", targetCafeName);
                            intent.putExtra("cafe_address", address);
                            intent.putExtra("cafe_reason", "전체 이용자 평점이 높은 인기 매장입니다 ✨");
                            intent.putExtra("cafe_tags", tagText.toString());
                        } else {
                            intent.putExtra("cafe_id", "temp_id");
                            intent.putExtra("cafe_name", targetCafeName);
                            intent.putExtra("cafe_address", "주소 정보 없음");
                            intent.putExtra("cafe_reason", "실시간 인기 카페");
                            intent.putExtra("cafe_tags", "#카페 ");
                        }

                        startActivity(intent);
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
                            requestCurrentLocation();
                        }
                    } else {
                        txtRecommendCafeName.setText("데이터 로드 실패");
                        txtRecommendCafeDesc.setText("네트워크 상태를 확인해주세요 😢");
                    }
                });
    }

    private void addTagToList(String tagStr) {
        if (tagStr != null && !tagStr.isEmpty()) {
            try { selectedTags.add(Tag.valueOf(tagStr.trim().toUpperCase())); } catch (IllegalArgumentException e) {}
        }
    }

    private void fetchCafesAndRecommendAfterLocation() {
        db.collection("cafes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Recommender.CafeModel> cafeModels = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Cafe dbCafe = document.toObject(Cafe.class);
                            dbCafe.setId(document.getId());
                            List<Tag> enumTags = new ArrayList<>();
                            if (dbCafe.getTags() != null) {
                                for (String tagStr : dbCafe.getTags()) {
                                    try { enumTags.add(Tag.valueOf(tagStr.trim().toUpperCase())); } catch (IllegalArgumentException e) {}
                                }
                            }
                            Recommender.CafeModel model = new Recommender.CafeModel(
                                    dbCafe.getId(), dbCafe.getName(), dbCafe.getAddress(),
                                    enumTags.toArray(new Tag[0]), dbCafe.getLatitude(), dbCafe.getLongitude()
                            );
                            cafeModels.add(model);
                        }
                        List<Recommender.Recommendation> results =
                                Recommender.recommend(cafeModels, selectedTags, currentLat, currentLng);
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
                        Toast.makeText(MainActivity.this, "DB 연결 에러", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                    }
                    fetchCafesAndRecommendAfterLocation();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation();
            } else {
                fetchCafesAndRecommendAfterLocation();
            }
        }
    }

    private void setupClickListeners() {
        btnRecommendCafe.setOnClickListener(v -> {
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
    private void cleanupFranchiseDataAllInOne() {
        // 1. 지우고자 하는 프랜차이즈 대표 키워드 리스트
        List<String> franchiseNames = Arrays.asList(
                "스타벅스", "투썸", "메가", "컴포즈", "빽다방",
                "할리스", "이디야", "파스쿠찌", "엔제리너스",
                "탐앤탐스", "공차", "더리터", "드롭탑", "매머드", "디저트39"
        );

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2. 프랜차이즈 관련 데이터가 남아있을 수 있는 대상 컬렉션 목록
        // (※ 프로젝트의 실제 컬렉션 이름에 맞게 수정/추가하시면 됩니다)
        String[] targetCollections = {"visit_records", "reviews", "favorites", "history"};

        for (String collectionName : targetCollections) {
            db.collection(collectionName).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    int deletedCount = 0;

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // 카페 이름 필드 체크 (cafeName 또는 name 으로 들어있는 경우가 많음)
                        String cafeName = document.getString("cafeName");
                        if (cafeName == null) {
                            cafeName = document.getString("name");
                        }

                        if (cafeName != null) {
                            for (String franchise : franchiseNames) {
                                if (cafeName.contains(franchise)) {
                                    // 해당 프랜차이즈 잔해 문서 삭제
                                    db.collection(collectionName).document(document.getId()).delete();
                                    Log.d("CLEANUP_ALL", "[" + collectionName + "] 컬렉션에서 잔해 삭제: " + cafeName);
                                    deletedCount++;
                                    break;
                                }
                            }
                        }
                    }

                    if (deletedCount > 0) {
                        Toast.makeText(MainActivity.this,
                                collectionName + "에서 프랜차이즈 잔해 " + deletedCount + "건 삭제 완료!",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}