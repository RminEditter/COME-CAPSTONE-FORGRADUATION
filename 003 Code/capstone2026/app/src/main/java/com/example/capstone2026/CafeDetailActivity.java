package com.example.capstone2026;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CafeDetailActivity extends AppCompatActivity {

    private TextView txtCafeName, txtAddress, txtTags, txtReason;
    private Button btnFavorite, btnAddVisitRecord;

    private RecyclerView rvCafeVisitHistory;
    private FirebaseFirestore firestoreDb;

    private String cafeId;
    private String cafeName;

    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_detail);

        firestoreDb = FirebaseFirestore.getInstance();

        txtCafeName = findViewById(R.id.txtDetailCafeName);
        txtAddress = findViewById(R.id.txtDetailAddress);
        txtTags = findViewById(R.id.txtDetailTags);
        txtReason = findViewById(R.id.txtDetailReason);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnAddVisitRecord = findViewById(R.id.btnAddVisitRecord);

        rvCafeVisitHistory = findViewById(R.id.rvCafeVisitHistory);
        if (rvCafeVisitHistory != null) {
            rvCafeVisitHistory.setLayoutManager(new LinearLayoutManager(this));
        }

        cafeId = getIntent().getStringExtra("cafe_id");
        cafeName = getIntent().getStringExtra("cafe_name");

        String address = getIntent().getStringExtra("cafe_address");
        String tags = getIntent().getStringExtra("cafe_tags");
        String reason = getIntent().getStringExtra("cafe_reason");

        txtCafeName.setText(cafeName);
        txtAddress.setText(address);
        txtTags.setText(tags);
        txtReason.setText(reason);

        loadFavoriteState();

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnAddVisitRecord.setOnClickListener(v -> {
            Intent intent = new Intent(CafeDetailActivity.this, VisitRecordActivity.class);
            intent.putExtra("mode", "add");
            intent.putExtra("cafeName", cafeName);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadThisCafeVisitRecords();
    }

    // [핵심 수정] userUid 필터링을 제거하여 이 카페의 다른 사람 리뷰까지 다 긁어옴
    private void loadThisCafeVisitRecords() {
        if (cafeName == null || rvCafeVisitHistory == null) return;

        // 오직 카페 이름이 일치하는 조건만 걸어 모든 유저의 글을 통합 검색합니다.
        firestoreDb.collection("visit_records")
                .whereEqualTo("cafeName", cafeName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<VisitRecord> thisCafeRecords = new ArrayList<>();

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
        String address = getIntent().getStringExtra("cafe_address");
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