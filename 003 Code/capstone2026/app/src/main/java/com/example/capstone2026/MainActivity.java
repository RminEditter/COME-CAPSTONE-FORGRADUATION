package com.example.capstone2026;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView; // SearchView 임포트 확인

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseRepository repository;
    private CafeAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;

    // [중요] 검색을 위해 모든 카페 모델을 보관하는 전역 리스트
    private List<Recommender.CafeModel> fullCafeModels = new ArrayList<>();

    // 현재 위치 (기본값: 대전유성구청)
    private double myLat = 36.3604;
    private double myLon = 127.3598;
    private String currentQuery = null; // 현재 입력된 검색어 저장용

    private List<Tag> selectedTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 리사이클러뷰 설정
        RecyclerView rvCafe = findViewById(R.id.rv_cafe);
        rvCafe.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CafeAdapter(new ArrayList<>());
        rvCafe.setAdapter(adapter);

        // 검색 버튼 연결
        EditText etSearch = findViewById(R.id.et_search);
        Button btnSearch = findViewById(R.id.btn_search);
        // 선호태그 설정 버튼 연결
        Button btnFilter = findViewById(R.id.btn_filter);
        btnFilter.setOnClickListener(v -> showTagFilterDialog());

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 사용자가 글자를 입력하거나 지울 때마다 호출됨
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 버튼을 눌렀을 때 검색 실행
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString();
            performSearch(query); // 이 함수는 이전에 제가 드린 코드에 있습니다!
        });


        // 3. 서비스 초기화
        repository = new FirebaseRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
    }

    // 검색 실행 로직
    private void performSearch(String query) {
        this.currentQuery = query;
        // 이미 로드된 데이터들(fullCafeModels)을 대상으로 추천 엔진 재가동
        runRecommendationEngine(new ArrayList<>(fullCafeModels));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        } else {
            getMyLocationAndLoadData();
        }
    }

    private void getMyLocationAndLoadData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    myLat = location.getLatitude();
                    myLon = location.getLongitude();
                }
                loadAndRecommend();
            });
        }
    }

    private void loadAndRecommend() {
        repository.getCafeList(new OnDataFetchedListener<List<Cafe>>() {
            @Override
            public void onSuccess(List<Cafe> firebaseData) {
                fullCafeModels.clear(); // 데이터 꼬임 방지 초기화

                for (Cafe f : firebaseData) {
                    // 1. DB에 이미 태그가 있는 경우
                    if (f.getTags() != null && !f.getTags().isEmpty()) {
                        Tag[] tags = convertStringListToTags(f.getTags());
                        addModelAndRefresh(f, tags);
                    }
                    // 2. 태그가 없는 경우 분석 실행
                    else {
                        NaverReviewAnalyzer.analyzeCafe(f.getName(), f.getAddress(), analyzedTags -> {
                            if (analyzedTags.isEmpty()) analyzedTags.add(Tag.COZY);
                            repository.updateCafeTags(f.getId(), analyzedTags);

                            Tag[] tagArray = analyzedTags.toArray(new Tag[0]);
                            runOnUiThread(() -> addModelAndRefresh(f, tagArray));
                        });
                    }
                }
            }
            @Override public void onFailure(Exception e) { e.printStackTrace(); }
        });
    }

    // 데이터를 모델로 변환하고 전역 리스트에 추가
    private void addModelAndRefresh(Cafe f, Tag[] tags) {
        long id = (long) f.getId().hashCode();
        Recommender.CafeModel newModel = new Recommender.CafeModel(
                id, f.getName(), f.getLatitude(), f.getLongitude(), tags, 3, 0f
        );

        // 중복 추가 방지 후 전역 리스트에 저장
        boolean exists = false;
        for(Recommender.CafeModel m : fullCafeModels) {
            if(m.name.equals(newModel.name)) { exists = true; break; }
        }
        if(!exists) fullCafeModels.add(newModel);

        // 분석/로드 되는 즉시 엔진 실행 (검색어 반영 포함)
        runRecommendationEngine(new ArrayList<>(fullCafeModels));
    }

    private void runRecommendationEngine(List<Recommender.CafeModel> models) {
        Tag[] myPreferredTags = selectedTags.toArray(new Tag[0]);

        if (myPreferredTags.length == 0) {
            myPreferredTags = new Tag[]{Tag.COZY};
        }

        Recommender.UserPreference pref = new Recommender.UserPreference(myPreferredTags, 1, 5);

        // [수정] 5번째 자리에 null(VisitStatsProvider용)을 넣고, 6번째에 검색어를 넣습니다.
        List<Recommender.RecommendResult> results = Recommender.recommend(
                pref,
                models,
                myLat,
                myLon,
                null,         // VisitStatsProvider가 없으므로 null 전달
                currentQuery   // 검색어 자리
        );

        runOnUiThread(() -> adapter.updateList(results));
    }

    private Tag[] convertStringListToTags(List<String> tagStrings) {
        Tag[] tags = new Tag[tagStrings.size()];
        for (int i = 0; i < tagStrings.size(); i++) {
            try {
                tags[i] = Tag.valueOf(tagStrings.get(i));
            } catch (Exception e) {
                tags[i] = Tag.COZY;
            }
        }
        return tags;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMyLocationAndLoadData();
        } else {
            loadAndRecommend();
        }
    }

    private void showTagFilterDialog() {
        Tag[] allTags = Tag.values(); // 모든 태그 종류 가져오기
        String[] tagNames = new String[allTags.length];
        boolean[] checkedItems = new boolean[allTags.length];

        for (int i = 0; i < allTags.length; i++) {
            tagNames[i] = allTags[i].labelKo(); // 태그 이름을 문자열로 변환
            if (selectedTags.contains(allTags[i])) {
                checkedItems[i] = true; // 이미 선택된 건 체크 표시
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("원하는 태그를 선택하세요")
                .setMultiChoiceItems(tagNames, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedTags.add(allTags[which]);
                    } else {
                        selectedTags.remove(allTags[which]);
                    }
                })
                .setPositiveButton("적용", (dialog, which) -> {
                    runRecommendationEngine(new ArrayList<>(fullCafeModels));
                })
                .setNegativeButton("취소", null)
                .show();
    }
}