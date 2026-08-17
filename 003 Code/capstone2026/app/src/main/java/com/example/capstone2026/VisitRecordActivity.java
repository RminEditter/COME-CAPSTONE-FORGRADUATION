package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisitRecordActivity extends AppCompatActivity {

    private TextView tvCafeName;
    private RatingBar ratingBar;
    private EditText etMemo;
    private Button btnSaveVisit;

    private String cafeName;
    private String mode;
    private String recordId;
    private long visitedAt;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_record);

        setupBackButton();
        BottomNavHelper.setup(this);

        db = FirebaseFirestore.getInstance();

        tvCafeName = findViewById(R.id.tvCafeName);
        ratingBar = findViewById(R.id.ratingBar);
        etMemo = findViewById(R.id.etMemo);
        btnSaveVisit = findViewById(R.id.btnSaveVisit);

        mode = getIntent().getStringExtra("mode");
        cafeName = getIntent().getStringExtra("cafeName");

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLICKED_CAFE_NAME")) {
            String filterCafeName = intent.getStringExtra("CLICKED_CAFE_NAME");

            Toast.makeText(this, filterCafeName + " 기록을 불러옵니다!", Toast.LENGTH_SHORT).show();
        }

        if (cafeName != null) {
            tvCafeName.setText(cafeName);
        }

        if ("edit".equals(mode)) {
            recordId = getIntent().getStringExtra("id");
            float rating = getIntent().getFloatExtra("rating", 0.0f);
            String memo = getIntent().getStringExtra("memo");
            visitedAt = getIntent().getLongExtra("visitedAt", 0);

            ratingBar.setRating(rating);
            etMemo.setText(memo);
            btnSaveVisit.setText("수정하기");
        }

        btnSaveVisit.setOnClickListener(v -> {
            if ("edit".equals(mode)) {
                updateVisitRecord();
            } else {
                saveVisitRecord();
            }
        });
    }

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void saveVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> record = new HashMap<>();
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", memo);
        record.put("visitedAt", System.currentTimeMillis());
        record.put("userUid", currentUid);

        db.collection("visit_records")
                .add(record)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(this, "방문 기록이 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show();

                    applyBadgesAfterVisit(
                            currentUid,
                            rating
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateVisitRecord() {
        float rating = ratingBar.getRating();
        String memo = etMemo.getText().toString().trim();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> record = new HashMap<>();
        record.put("cafeName", cafeName);
        record.put("rating", rating);
        record.put("memo", memo);
        record.put("visitedAt", visitedAt);
        record.put("userUid", currentUid);

        db.collection("visit_records")
                .document(recordId)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "방문 기록이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    goToHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void applyBadgesAfterVisit(
            String currentUid,
            float rating
    ) {

        if (cafeName == null || cafeName.isEmpty()) {

            Recommender.CafeModel fallbackCafe =
                    new Recommender.CafeModel(
                            "",
                            cafeName,
                            "",
                            new Tag[0],
                            0.0,
                            0.0
                    );

            applyBadgeManager(
                    currentUid,
                    fallbackCafe,
                    rating
            );

            return;
        }

        db.collection("cafes")
                .whereEqualTo("name", cafeName)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() &&
                            task.getResult() != null &&
                            !task.getResult().isEmpty()) {

                        QueryDocumentSnapshot document =
                                (QueryDocumentSnapshot)
                                        task.getResult()
                                                .getDocuments()
                                                .get(0);

                        String cafeId =
                                document.getString("id");

                        String name =
                                document.getString("name");

                        String address =
                                document.getString("address");

                        Double latitudeDouble =
                                document.getDouble("latitude");

                        Double longitudeDouble =
                                document.getDouble("longitude");

                        double latitude =
                                latitudeDouble != null
                                        ? latitudeDouble
                                        : 0.0;

                        double longitude =
                                longitudeDouble != null
                                        ? longitudeDouble
                                        : 0.0;

                        List<String> tagStrings =
                                (List<String>) document.get("tags");

                        List<Tag> tagList =
                                new ArrayList<>();

                        if (tagStrings != null) {

                            for (String tagString : tagStrings) {

                                if (tagString == null) {
                                    continue;
                                }

                                try {

                                    Tag tag =
                                            Tag.valueOf(
                                                    tagString
                                                            .trim()
                                                            .toUpperCase()
                                            );

                                    tagList.add(tag);

                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }

                        Recommender.CafeModel cafe =
                                new Recommender.CafeModel(
                                        cafeId != null
                                                ? cafeId
                                                : document.getId(),
                                        name != null
                                                ? name
                                                : cafeName,
                                        address != null
                                                ? address
                                                : "",
                                        tagList.toArray(
                                                new Tag[0]
                                        ),
                                        latitude,
                                        longitude
                                );

                        applyBadgeManager(
                                currentUid,
                                cafe,
                                rating
                        );

                    } else {

                        Recommender.CafeModel fallbackCafe =
                                new Recommender.CafeModel(
                                        "",
                                        cafeName,
                                        "",
                                        new Tag[0],
                                        0.0,
                                        0.0
                                );

                        applyBadgeManager(
                                currentUid,
                                fallbackCafe,
                                rating
                        );
                    }
                });
    }

    private void applyBadgeManager(
            String currentUid,
            Recommender.CafeModel cafe,
            float rating
    ) {

        BadgeManager.applyVisitBadges(
                currentUid,
                cafe,
                rating,
                new BadgeManager.BadgeUpdateListener() {

                    @Override
                    public void onComplete(
                            List<String> newlyUnlockedBadges
                    ) {

                        if (newlyUnlockedBadges != null &&
                                !newlyUnlockedBadges.isEmpty()) {

                            StringBuilder badgeNames =
                                    new StringBuilder();

                            for (String badgeCode :
                                    newlyUnlockedBadges) {

                                if (badgeNames.length() > 0) {
                                    badgeNames.append(", ");
                                }

                                badgeNames.append(
                                        BadgeManager.getBadgeName(
                                                badgeCode
                                        )
                                );
                            }

                            Toast.makeText(
                                    VisitRecordActivity.this,
                                    "🏅 새 배지 획득: "
                                            + badgeNames,
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        goToHistory();
                    }

                    @Override
                    public void onFailure(Exception e) {

                        Toast.makeText(
                                VisitRecordActivity.this,
                                "배지 처리 중 오류가 발생했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        goToHistory();
                    }
                }
        );
    }

    private void goToHistory() {
        Intent intent = new Intent(this, VisitHistoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}