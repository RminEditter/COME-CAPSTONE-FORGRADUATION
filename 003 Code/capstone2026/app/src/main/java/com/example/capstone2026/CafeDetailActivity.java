package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CafeDetailActivity extends AppCompatActivity {

    private TextView txtCafeName, txtAddress, txtReason, txtAverageRating, txtReviewCount, txtNoReviews;
    private Button btnFavorite, btnAddVisitRecord;
    private ImageButton btnBack, btnCall, btnNavigation, btnShare;
    private ChipGroup chipGroupTags;

    private RecyclerView rvCafeVisitHistory;
    private FirebaseFirestore firestoreDb;

    private String cafeId;
    private String cafeName;
    private String address;
    private String phone = "02-1234-5678"; // Intent 파라미터로 전달받거나 기본값 세팅

    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_detail);

        firestoreDb = FirebaseFirestore.getInstance();

        // Intent 데이터 받아오기
        cafeId = getIntent().getStringExtra("cafe_id");
        cafeName = getIntent().getStringExtra("cafe_name");
        address = getIntent().getStringExtra("cafe_address");
        String tags = getIntent().getStringExtra("cafe_tags");
        String reason = getIntent().getStringExtra("cafe_reason");
        if (getIntent().hasExtra("cafe_phone")) {
            phone = getIntent().getStringExtra("cafe_phone");
        }

        // View 뷰 바인딩
        initViews();

        // 텍스트 데이터 바인딩
        txtCafeName.setText(cafeName != null ? cafeName : "카페 이름");
        txtAddress.setText(address != null ? address : "주소 정보 없음");
        txtReason.setText(reason != null ? reason : "추천 사유 정보가 없습니다.");

        // 태그 칩 동적 생성
        setupTagChips(tags);

        // 즐겨찾기 상태 불러오기
        loadFavoriteState();

        // 버튼 클릭 리스너 설정
        setupClickListeners();

        // 리뷰 리사이클러뷰 레이아웃 매니저 설정
        if (rvCafeVisitHistory != null) {
            rvCafeVisitHistory.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void initViews() {
        txtCafeName = findViewById(R.id.txtDetailCafeName);
        txtAddress = findViewById(R.id.txtDetailAddress);
        txtReason = findViewById(R.id.txtDetailReason);
        txtAverageRating = findViewById(R.id.txtAverageRating);
        txtReviewCount = findViewById(R.id.txtReviewCount);
        txtNoReviews = findViewById(R.id.txtNoReviews);

        btnFavorite = findViewById(R.id.btnFavorite);
        btnAddVisitRecord = findViewById(R.id.btnAddVisitRecord);
        btnBack = findViewById(R.id.btnDetailBack);
        btnCall = findViewById(R.id.btnActionCall);
        btnNavigation = findViewById(R.id.btnActionNavi);
        btnShare = findViewById(R.id.btnActionShare);

        chipGroupTags = findViewById(R.id.chipGroupDetailTags);
        rvCafeVisitHistory = findViewById(R.id.rvCafeVisitHistory);
    }

    private void setupClickListeners() {
        // 뒤로가기
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 즐겨찾기
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // 방문 기록(리뷰) 작성
        btnAddVisitRecord.setOnClickListener(v -> {
            Intent intent = new Intent(CafeDetailActivity.this, VisitRecordActivity.class);
            intent.putExtra("mode", "add");
            intent.putExtra("cafeName", cafeName);
            startActivity(intent);
        });

        // 📞 전화걸기 기능
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(intent);
            });
        }

        // 🗺️ 길찾기 (카카오맵/네이버 지도 검색 연동)
        if (btnNavigation != null) {
            btnNavigation.setOnClickListener(v -> {
                String searchQuery = cafeName + " " + (address != null ? address : "");
                String url = "https://map.naver.com/v5/search/" + Uri.encode(searchQuery);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        }

        // 🔗 카페 정보 공유하기 기능
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                String shareText = String.format("[CafeFit] %s\n📍 주소: %s\n함께 방문해보세요!", cafeName, address);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "카페 정보 공유하기"));
            });
        }
    }

    // 태그 문자열(예: "#카공 #콘센트많음")을 분리하여 예쁜 Chip으로 생성
    private void setupTagChips(String tags) {
        if (chipGroupTags == null || tags == null || tags.trim().isEmpty()) return;

        chipGroupTags.removeAllViews();
        String[] tagArray = tags.split(" ");

        for (String tag : tagArray) {
            if (tag.trim().isEmpty()) continue;
            Chip chip = new Chip(this);
            chip.setText(tag.startsWith("#") ? tag : "#" + tag);
            chip.setClickable(false);
            chip.setCheckable(false);
            chipGroupTags.addView(chip);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadThisCafeVisitRecords();
    }

    // 카페 전체 방문 기록 로드 및 평균 평점 요약 계산
    private void loadThisCafeVisitRecords() {
        if (cafeName == null || rvCafeVisitHistory == null) return;

        firestoreDb.collection("visit_records")
                .whereEqualTo("cafeName", cafeName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<VisitRecord> thisCafeRecords = new ArrayList<>();
                        float totalRating = 0.0f;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("cafeName");
                            Double ratingDouble = document.getDouble("rating");
                            float rating = ratingDouble != null ? ratingDouble.floatValue() : 0.0f;
                            String memo = document.getString("memo");
                            Long visitedAtLong = document.getLong("visitedAt");
                            long visitedAt = visitedAtLong != null ? visitedAtLong : 0L;
                            String uid = document.getString("userUid");

                            VisitRecord record = new VisitRecord(name, rating, memo, visitedAt);
                            record.setId(document.getId());
                            record.setUserUid(uid);

                            thisCafeRecords.add(record);
                            totalRating += rating;
                        }

                        // UI 업데이트 (평균 평점 및 리뷰 개수 반영)
                        int reviewCount = thisCafeRecords.size();
                        if (reviewCount > 0) {
                            float avgRating = totalRating / reviewCount;
                            if (txtAverageRating != null) {
                                txtAverageRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", avgRating));
                            }
                            if (txtReviewCount != null) {
                                txtReviewCount.setText(String.format(Locale.getDefault(), "(%d개 리뷰)", reviewCount));
                            }
                            if (txtNoReviews != null) txtNoReviews.setVisibility(View.GONE);
                            rvCafeVisitHistory.setVisibility(View.VISIBLE);
                        } else {
                            if (txtAverageRating != null) txtAverageRating.setText("⭐ 0.0");
                            if (txtReviewCount != null) txtReviewCount.setText("(0개 리뷰)");
                            if (txtNoReviews != null) txtNoReviews.setVisibility(View.VISIBLE);
                            rvCafeVisitHistory.setVisibility(View.GONE);
                        }

                        VisitHistoryAdapter adapter = new VisitHistoryAdapter(thisCafeRecords);
                        rvCafeVisitHistory.setAdapter(adapter);
                    }
                });
    }

    private void loadFavoriteState() {
        SharedPreferences prefs = getSharedPreferences("CafeFitFavorites", MODE_PRIVATE);
        isFavorite = prefs.getBoolean(cafeId, false);
        updateFavoriteButton();
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        String tags = getIntent().getStringExtra("cafe_tags");
        String reason = getIntent().getStringExtra("cafe_reason");

        SharedPreferences prefs = getSharedPreferences("CafeFitFavorites", MODE_PRIVATE);
        prefs.edit()
                .putBoolean(cafeId, isFavorite)
                .putString(cafeId + "_name", cafeName)
                .putString(cafeId + "_address", address)
                .putString(cafeId + "_tags", tags)
                .putString(cafeId + "_reason", reason)
                .apply();

        updateFavoriteButton();

        if (isFavorite) {
            Toast.makeText(this, "즐겨찾기에 추가했어요.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "즐겨찾기에서 해제했어요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            btnFavorite.setText("♥ 즐겨찾기 해제");
        } else {
            btnFavorite.setText("♡ 즐겨찾기 추가");
        }
    }
}