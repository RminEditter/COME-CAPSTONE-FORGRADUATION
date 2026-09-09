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
import java.util.Arrays;
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

public class MainActivity extends AppCompatActivity {

    private EditText editSearch;
    private AppCompatButton btnRecommendCafe, btnHomeMenu;
    private TextView txtRecommendCafeName, txtRecommendCafeDesc, txtRecentCafe;
    private TextView txtTopCafe1, txtTopCafe2, txtTopCafe3;

    private int recommendationRequest = 0;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
        BottomNavHelper.setup(this);
        //cleanupFranchiseDataAllInOne();
        //updateAllCafeTags(); 태그부여 함수 건들지말것.
        //reanalyzeAllCafeTags();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences recentPrefs = AccountPreferences.open(this, "CafeFitRecent");
        String recentCafeName = recentPrefs.getString("recentCafe", "최근 본 카페가 없습니다.");
        txtRecentCafe.setText(recentCafeName);

        if (!recentCafeName.equals("최근 본 카페가 없습니다.") && !recentCafeName.isEmpty()) {
            txtRecentCafe.setOnClickListener(v -> {
                queryCafeAndGoDetail(recentPrefs.getString("recentCafeId", null), recentCafeName);
            });
        } else {
            txtRecentCafe.setOnClickListener(null);
        }

        fetchAllUsersHighestRatedCafes();

        // 💡 필요할 때 주석을 해제하여 네이버 리뷰 크롤링 및 태그 분석 함수를 작동시킵니다.
        // updateAllCafeTags();

        txtRecommendCafeName.setText("취향 분석 중...");
        txtRecommendCafeDesc.setText("서버에서 계정 설문 정보를 가져오고 있습니다 🔍");

        requestCurrentLocation();
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
                int scheduledDelayIndex = 0; // 스킵되지 않고 실제 크롤링할 카페들의 딜레이 인덱스

                for (int i = 0; i < docs.size(); i++) {
                    final QueryDocumentSnapshot document = docs.get(i);
                    String name = document.getString("name");
                    String addr = document.getString("address");

                    // 1. 프랜차이즈 스킵
                    if (isFranchise(name)) {
                        Log.d("CafeFit", "프랜차이즈 스킵: " + name);
                        continue;
                    }

                    // 💡 2. [핵심] 이미 태그 작업이 끝난 카페인지 검사 (이어서 하기 로직)
                    List<String> existingTags = (List<String>) document.get("tags");
                    if (existingTags != null && !existingTags.isEmpty()) {
                        Log.d("CafeFit", "⏩ 이미 태그가 존재하는 카페 (스킵): " + name);
                        continue; // 이미 태그가 있으므로 크롤링하지 않고 넘어감!
                    }

                    // 3. 아직 태그가 없는 카페만 스케줄러에 등록하여 1초 간격 처리
                    final int delayMultiplier = scheduledDelayIndex++;

                    executor.schedule(() -> {
                        if (name == null) return;

                        Log.d("CafeFit", "🔄 태그 작업 시작: " + name);

                        NaverReviewAnalyzer.analyzeCafe(name, addr, tags -> {
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
                                        .addOnSuccessListener(aVoid -> Log.d("CafeFit", "✅ 성공: " + name + " -> 태그 " + tagStrings.size() + "개"))
                                        .addOnFailureListener(e -> Log.e("CafeFit", "❌ DB 업데이트 실패: " + name, e));
                            } else {
                                Log.w("CafeFit", "⚠️ 태그 수집 실패 또는 데이터 없음: " + name);
                            }
                        });

                    }, delayMultiplier * 1000L, TimeUnit.MILLISECONDS);
                }

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
        TextView[] views = {txtTopCafe1, txtTopCafe2, txtTopCafe3};
        for (TextView view : views) {
            view.setText("전체 평점 계산 중...");
            view.setOnClickListener(null);
        }
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> cafes =
                db.collection("cafes").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> visits =
                db.collection("visit_records").get();
        com.google.android.gms.tasks.Tasks.whenAll(cafes, visits).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) return;
            if (!task.isSuccessful()) {
                for (TextView view : views) view.setText("평점을 불러오지 못했습니다.");
                return;
            }
            List<Recommender.CafeModel> models = RecommendationLoader.models(cafes.getResult());
            Map<String, CafeRatingStats> stats = new CafeIdentity(models)
                    .aggregate(VisitRecordRepository.records(visits.getResult()));
            List<Recommender.CafeModel> ranked = new ArrayList<>();
            for (Recommender.CafeModel cafe : models) {
                if (stats.containsKey(cafe.id)) ranked.add(cafe);
            }
            ranked.sort((a, b) -> Float.compare(stats.get(b.id).avgRating, stats.get(a.id).avgRating));
            for (int i = 0; i < views.length; i++) {
                if (i >= ranked.size()) {
                    views[i].setText((i + 1) + ". 평점 데이터 없음");
                    continue;
                }
                Recommender.CafeModel cafe = ranked.get(i);
                views[i].setText(String.format(java.util.Locale.getDefault(),
                        "%d. %s  ★%.1f", i + 1, cafe.name, stats.get(cafe.id).avgRating));
                views[i].setOnClickListener(v -> queryCafeAndGoDetail(cafe.id, cafe.name));
            }
        });
    }

    private void queryCafeAndGoDetail(String cafeId, String cafeName) {
        if (CafeIdentity.hasId(cafeId)) {
            db.collection("cafes").document(cafeId).get()
                    .addOnSuccessListener(this::openCafeDetail)
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "카페 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show());
        } else if (cafeName != null) {
            db.collection("cafes").whereEqualTo("name", cafeName).limit(2).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.size() == 1) {
                            openCafeDetail(snapshot.getDocuments().get(0));
                        } else {
                            Toast.makeText(this, "카페를 검색해 매장을 선택해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "카페 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show());
        }
    }

    private void openCafeDetail(DocumentSnapshot document) {
        if (isFinishing() || isDestroyed()) return;
        if (!document.exists()) {
            Toast.makeText(this, "카페 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Recommender.CafeModel cafe = RecommendationLoader.model(document);
        StringBuilder tags = new StringBuilder();
        for (Tag tag : cafe.tags) tags.append("#").append(tag.getKoreanLabel()).append(" ");
        Intent intent = new Intent(this, CafeDetailActivity.class);
        intent.putExtra("cafe_id", cafe.id);
        intent.putExtra("cafe_name", cafe.name);
        intent.putExtra("cafe_address", cafe.address);
        intent.putExtra("cafe_tags", tags.toString());
        intent.putExtra("cafe_phone", document.getString("phone"));
        startActivity(intent);
    }

    private void fetchCafesAndRecommendAfterLocation() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        int request = ++recommendationRequest;
        RecommendationLoader.load(this, uid).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed() || request != recommendationRequest
                    || mAuth.getCurrentUser() == null
                    || !uid.equals(mAuth.getCurrentUser().getUid())) return;
            if (!task.isSuccessful()) {
                txtRecommendCafeName.setText("추천을 불러오지 못했습니다.");
                txtRecommendCafeDesc.setText("네트워크 상태를 확인하고 다시 시도해주세요.");
                return;
            }
            RecommendationLoader.Result result = task.getResult();
            if (!result.hasSurvey) {
                txtRecommendCafeName.setText("나만의 카페를 찾아보세요!");
                txtRecommendCafeDesc.setText("오른쪽 상단 메뉴에서 취향 설문을 시작해주세요.");
            } else if (!result.recommendations.isEmpty()) {
                Recommender.Recommendation best = result.recommendations.get(0);
                txtRecommendCafeName.setText(best.cafe.name + " ✨");
                txtRecommendCafeDesc.setText(best.cafe.address + "\n" + best.reason
                        + (result.usedDefaultLocation ? "\n위치 확인 불가: 대전 궁동·어은동 기준" : ""));
            } else {
                txtRecommendCafeName.setText("추천 카페가 없습니다.");
                txtRecommendCafeDesc.setText("등록된 카페 정보를 확인해주세요.");
            }
        });
    }

    private void requestCurrentLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        SharedPreferences permissionPrefs = getSharedPreferences("location_permission", MODE_PRIVATE);
        if (!fine && !coarse && !permissionPrefs.getBoolean("requested", false)) {
            permissionPrefs.edit().putBoolean("requested", true).apply();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            fetchCafesAndRecommendAfterLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) fetchCafesAndRecommendAfterLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recommendationRequest++;
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
        // 1. 지우고자 하는 프랜차이즈 및 피자/패스트푸드 키워드 리스트
        List<String> franchiseNames = Arrays.asList(
                // 기존 카페 프랜차이즈
                "스타벅스", "투썸", "메가", "컴포즈", "빽다방",
                "할리스", "이디야", "파스쿠찌", "엔제리너스",
                "탐앤탐스", "공차", "더리터", "드롭탑", "매머드", "디저트39",

                // 🍕 피자 및 패스트푸드/음식점 키워드 추가
                "피자", "도미노", "피자헛", "알볼로", "미스터피자", "피자스쿨", "59쌀피자",
                "버거", "롯데리아", "맥도날드", "맘스터치", "KFC", "상호명없음", "스터디","분식"
        );

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 💡 메인 카페 목록 컬렉션("cafes")을 맨 앞에 추가해 두셔야 카페 DB에서도 삭제됩니다!
        String[] targetCollections = {"cafes", "visit_records", "reviews", "favorites", "history"};

        for (String collectionName : targetCollections) {
            db.collection(collectionName).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    int deletedCount = 0;

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String cafeName = document.getString("cafeName");
                        if (cafeName == null) {
                            cafeName = document.getString("name");
                        }

                        if (cafeName != null) {
                            for (String franchise : franchiseNames) {
                                if (cafeName.contains(franchise)) {
                                    db.collection(collectionName).document(document.getId()).delete();
                                    Log.d("CLEANUP_ALL", "[" + collectionName + "] 삭제됨: " + cafeName);
                                    deletedCount++;
                                    break;
                                }
                            }
                        }
                    }

                    if (deletedCount > 0) {
                        Toast.makeText(MainActivity.this,
                                collectionName + "에서 " + deletedCount + "건 삭제 완료!",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    private void reanalyzeAllCafeTags() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot d : task.getResult()) {
                    docs.add(d);
                }

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                int scheduledDelayIndex = 0;

                for (int i = 0; i < docs.size(); i++) {
                    final QueryDocumentSnapshot document = docs.get(i);
                    String name = document.getString("name");
                    String addr = document.getString("address");

                    // 1. 프랜차이즈 스킵
                    if (isFranchise(name)) {
                        Log.d("CafeFit", "프랜차이즈 스킵: " + name);
                        continue;
                    }

                    // 💡 [핵심] 기존 태그가 존재하더라도 스킵하지 않고 새로운 Tag 기준으로 덮어씁니다!

                    final int delayMultiplier = scheduledDelayIndex++;

                    executor.schedule(() -> {
                        if (name == null) return;

                        Log.d("CafeFit", "🔄 태그 재분석 시작: " + name);

                        NaverReviewAnalyzer.analyzeCafe(name, addr, tags -> {
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
                                        .addOnSuccessListener(aVoid -> Log.d("CafeFit", "✅ 새 태그 성공: " + name + " -> " + tagStrings))
                                        .addOnFailureListener(e -> Log.e("CafeFit", "❌ DB 업데이트 실패: " + name, e));
                            } else {
                                Log.w("CafeFit", "⚠️ 태그 수집 실패 또는 데이터 없음: " + name);
                            }
                        });

                    }, delayMultiplier * 1000L, TimeUnit.MILLISECONDS);
                }

                executor.shutdown();
            } else {
                Log.e("CafeFit", "카페 목록 불러오기 실패", task.getException());
            }
        });
    }
}