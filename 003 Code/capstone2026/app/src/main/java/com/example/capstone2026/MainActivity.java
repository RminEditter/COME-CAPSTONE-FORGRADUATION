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
    private int rankingRequest = 0;
    private HomeDashboardView home;
    private HomePhotoLoader photos;
    private HomeSituation situation;
    private Recommender.Recommendation featured;
    private String featuredUid;
    private Runnable featuredAction;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ProfileUi.applyInsets(findViewById(R.id.homeRoot));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        home = new HomeDashboardView(findViewById(R.id.homeRoot));
        photos = new HomePhotoLoader(this, findViewById(R.id.imgHomeCafe),
                findViewById(R.id.txtHomePhotoState), findViewById(R.id.homePhotoCredits));
        situation = HomeSituation.parse(savedInstanceState == null ? null : savedInstanceState.getString("situation"));
        com.google.android.material.chip.ChipGroup situations = findViewById(R.id.homeSituations);
        if (situation != null) situations.check(situation == HomeSituation.STUDY ? R.id.chipHomeStudy
                : situation == HomeSituation.DATE ? R.id.chipHomeDate : R.id.chipHomeSolo);
        situations.setOnCheckedStateChangeListener((group, ids) -> {
            situation = ids.isEmpty() ? null : ids.get(0) == R.id.chipHomeStudy ? HomeSituation.STUDY
                    : ids.get(0) == R.id.chipHomeDate ? HomeSituation.DATE : HomeSituation.SOLO;
            fetchCafesAndRecommendAfterLocation();
        });
        setupClickListeners();
        BottomNavHelper.setup(this);
        findViewById(R.id.btnNavMain).setSelected(true);
        findViewById(R.id.btnNavMain).setContentDescription("메인, 현재 탭");
        //cleanupFranchiseDataAllInOne();
        //updateAllCafeTags(); 태그부여 함수 건들지말것.
        //reanalyzeAllCafeTags();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        SharedPreferences recentPrefs = AccountPreferences.open(this, "CafeFitRecent");
        String recentCafeName = recentPrefs.getString("recentCafe", "최근 본 카페가 없습니다.");
        txtRecentCafe.setText(recentCafeName);

        if (!recentCafeName.equals("최근 본 카페가 없습니다.")
                && CafeDiscoveryPolicy.isDiscoverableName(recentCafeName)) {
            txtRecentCafe.setOnClickListener(v -> {
                queryCafeAndGoDetail(recentPrefs.getString("recentCafeId", null), recentCafeName);
            });
        } else {
            txtRecentCafe.setOnClickListener(null);
            txtRecentCafe.setText(R.string.home_recent_empty);
        }
        ((TextView) findViewById(R.id.txtHomeRecentHint)).setText(txtRecentCafe.hasOnClickListeners()
                ? "눌러서 카페를 다시 만나보세요." : getString(R.string.home_recent_hint));

        fetchAllUsersHighestRatedCafes();

        // 💡 필요할 때 주석을 해제하여 네이버 리뷰 크롤링 및 태그 분석 함수를 작동시킵니다.
        // updateAllCafeTags();

        home.loading();

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
        int request = ++rankingRequest;
        String uid = mAuth.getCurrentUser() == null ? "" : mAuth.getCurrentUser().getUid();
        TextView[] views = {txtTopCafe1, txtTopCafe2, txtTopCafe3};
        for (TextView view : views) {
            view.setText("전체 평점 계산 중...");
            view.setOnClickListener(null);
        }
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> cafes =
                db.collection("cafes").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> visits =
                db.collection("visit_records").get();
        com.google.android.gms.tasks.Tasks.whenAll(cafes, visits)
                .continueWith(RecommendationLoader.COMPUTATION, task -> {
            if (!task.isSuccessful()) throw task.getException();
            List<Recommender.CafeModel> models = RecommendationLoader.models(cafes.getResult());
            Map<String, CafeRatingStats> stats = new CafeIdentity(models)
                    .aggregate(VisitRecordRepository.records(visits.getResult()));
            List<Recommender.CafeModel> ranked = new ArrayList<>();
            for (Recommender.CafeModel cafe : models) {
                if (stats.containsKey(cafe.id)) ranked.add(cafe);
            }
            ranked.sort((a, b) -> Float.compare(stats.get(b.id).avgRating, stats.get(a.id).avgRating));
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(3, ranked.size()); i++) {
                Recommender.CafeModel cafe = ranked.get(i);
                rows.add(new String[]{cafe.id, cafe.name, String.format(java.util.Locale.getDefault(),
                        "%d. %s  ★%.1f", i + 1, cafe.name, stats.get(cafe.id).avgRating)});
            }
            return rows;
        }).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed() || request != rankingRequest
                    || mAuth.getCurrentUser() == null || !uid.equals(mAuth.getCurrentUser().getUid())) return;
            if (!task.isSuccessful()) {
                views[0].setText("평점을 불러오지 못했어요. 눌러서 다시 시도");
                views[0].setOnClickListener(v -> fetchAllUsersHighestRatedCafes());
                views[1].setText("");
                views[2].setText("");
                return;
            }
            List<String[]> rows = task.getResult();
            for (int i = 0; i < views.length; i++) {
                if (i >= rows.size()) {
                    views[i].setText(i == 0 ? "아직 평가가 등록된 카페가 없어요." : "");
                    continue;
                }
                String[] row = rows.get(i);
                views[i].setText(row[2]);
                views[i].setOnClickListener(v -> queryCafeAndGoDetail(row[0], row[1]));
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
        if (!CafeDiscoveryPolicy.isDiscoverable(document.getData())) {
            Toast.makeText(this, "카페가 아닌 매장으로 분류되어 추천에서 제외됐어요.", Toast.LENGTH_SHORT).show();
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
        featured = null;
        featuredUid = null;
        featuredAction = null;
        home.loading();
        photos.clear();
        RecommendationLoader.load(this, uid, situation).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed() || request != recommendationRequest
                    || mAuth.getCurrentUser() == null
                    || !uid.equals(mAuth.getCurrentUser().getUid())) return;
            if (!task.isSuccessful()) {
                home.message("추천을 불러오지 못했어요", "연결 상태를 확인하고 다시 시도해주세요.", "다시 불러오기");
                featuredAction = this::fetchCafesAndRecommendAfterLocation;
                return;
            }
            RecommendationLoader.Result result = task.getResult();
            if (!result.hasSurvey && situation == null) {
                home.message("나만의 카페를 찾아보세요", "몇 가지 취향을 알려주시면 어울리는 카페를 추천해드려요.", "내 취향 설정하기");
                featuredAction = () -> startActivity(new Intent(this, SurveyActivity.class));
            } else if (!result.recommendations.isEmpty()) {
                Recommender.Recommendation best = result.recommendations.get(0);
                featured = best;
                featuredUid = uid;
                home.recommendation(best, result.usedDefaultLocation, situation);
                updateHomeFavorite();
                featuredAction = () -> queryCafeAndGoDetail(best.cafe.id, best.cafe.name);
                photos.load(best.cafe.id);
            } else {
                home.message("추천할 카페가 아직 없어요", "잠시 후 다시 불러오거나 취향을 수정해보세요.", "다시 불러오기");
                featuredAction = this::fetchCafesAndRecommendAfterLocation;
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
        rankingRequest++;
        photos.clear();
        featured = null;
        featuredAction = null;
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (situation != null) outState.putString("situation", situation.name());
    }

    private boolean hasCurrentFeature() {
        FirebaseUser user = mAuth.getCurrentUser();
        return featured != null && user != null && user.getUid().equals(featuredUid);
    }

    private void updateHomeFavorite() {
        if (!hasCurrentFeature()) { home.favorite(false, false); return; }
        home.favorite(AccountPreferences.open(this, "CafeFitFavorites").getBoolean(featured.cafe.id, false),
                CafeIdentity.hasId(featured.cafe.id));
    }

    private void toggleHomeFavorite() {
        if (!hasCurrentFeature() || !CafeIdentity.hasId(featured.cafe.id)) return;
        Recommender.CafeModel cafe = featured.cafe;
        SharedPreferences prefs = AccountPreferences.open(this, "CafeFitFavorites");
        boolean saved = !prefs.getBoolean(cafe.id, false);
        StringBuilder tags = new StringBuilder();
        for (Tag tag : cafe.tags) tags.append('#').append(tag.getKoreanLabel()).append(' ');
        prefs.edit().putBoolean(cafe.id, saved).putString(cafe.id + "_name", cafe.name)
                .putString(cafe.id + "_address", cafe.address).putString(cafe.id + "_tags", tags.toString())
                .putString(cafe.id + "_reason", featured.reason).apply();
        updateHomeFavorite();
        Toast.makeText(this, saved ? "즐겨찾기에 추가했어요." : "즐겨찾기에서 해제했어요.", Toast.LENGTH_SHORT).show();
    }

    private void openHomeMap() {
        if (!hasCurrentFeature()) return;
        // Google photo content links to Google Maps, never to the independent MapLibre map.
        String url = photos.googleMapsUrl();
        if (url == null) url = "https://www.google.com/maps/search/?api=1&query="
                + android.net.Uri.encode(featured.cafe.name + " " + featured.cafe.address);
        try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
        catch (android.content.ActivityNotFoundException ignored) {
            Toast.makeText(this, "지도를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAllRecommendations() {
        Intent intent = new Intent(this, RecommendCafeActivity.class);
        if (situation != null) intent.putExtra("HOME_SITUATION", situation.name());
        startActivity(intent);
    }

    private void setupClickListeners() {
        btnRecommendCafe.setOnClickListener(v -> {
            if (featuredAction != null) featuredAction.run();
        });
        findViewById(R.id.btnHomeSurvey).setOnClickListener(v -> startActivity(new Intent(this, SurveyActivity.class)));
        findViewById(R.id.btnHomeMap).setOnClickListener(v -> openHomeMap());
        findViewById(R.id.btnHomeFavorite).setOnClickListener(v -> toggleHomeFavorite());
        findViewById(R.id.btnHomeAll).setOnClickListener(v -> openAllRecommendations());

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
            boolean searchAction = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH;
            boolean enterKey = event != null
                    && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN;
            if (!searchAction && !enterKey) return false;
            String query = editSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, RecommendCafeActivity.class);
                intent.putExtra("SEARCH_QUERY", query);
                if (situation != null) intent.putExtra("HOME_SITUATION", situation.name());
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
