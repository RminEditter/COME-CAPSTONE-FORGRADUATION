package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CafeDetailActivity extends AppCompatActivity {

    // =====================================================
    // 기본 UI
    // =====================================================

    private TextView txtCafeName;
    private TextView txtAddress;
    private TextView txtReason;
    private TextView txtAverageRating;
    private TextView txtReviewCount;
    private TextView txtNoReviews;

    // =====================================================
    // 태그 평가 UI
    // =====================================================

    private TextView txtEvaluationStatus;
    private ChipGroup chipGroupTags;
    private ChipGroup chipGroupEvaluation;

    private Button btnSubmitTagEvaluation;
    private Button btnDeleteTagEvaluation;

    // =====================================================
    // 교차검증 UI
    // =====================================================

    private TextView txtVerificationSummary;
    private LinearLayout layoutVerificationResults;

    // =====================================================
    // 버튼
    // =====================================================

    private Button btnFavorite;
    private Button btnAddVisitRecord;

    private ImageButton btnBack;
    private ImageButton btnCall;
    private ImageButton btnNavigation;
    private ImageButton btnShare;

    // =====================================================
    // 리뷰
    // =====================================================

    private RecyclerView rvCafeVisitHistory;

    // =====================================================
    // Firebase
    // =====================================================

    private FirebaseFirestore firestoreDb;

    // =====================================================
    // 카페 데이터
    // =====================================================

    private String cafeId;
    private String cafeName;
    private String address;
    private String phone;

    private boolean isFavorite;
    private boolean hasTagEvaluation = false;

    private final List<Tag> evaluationTags =
            new ArrayList<>();


    // =====================================================
    // onCreate
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_cafe_detail
        );

        firestoreDb =
                FirebaseFirestore.getInstance();


        // =================================================
        // Intent
        // =================================================

        cafeId =
                getIntent()
                        .getStringExtra(
                                "cafe_id"
                        );

        cafeName =
                getIntent()
                        .getStringExtra(
                                "cafe_name"
                        );

        address =
                getIntent()
                        .getStringExtra(
                                "cafe_address"
                        );

        String tags =
                getIntent()
                        .getStringExtra(
                                "cafe_tags"
                        );

        String reason =
                getIntent()
                        .getStringExtra(
                                "cafe_reason"
                        );


        if (
                getIntent()
                        .hasExtra(
                                "cafe_phone"
                        )
        ) {

            phone =
                    getIntent()
                            .getStringExtra(
                                    "cafe_phone"
                            );
        }


        // =================================================
        // View
        // =================================================

        initViews();


        txtCafeName.setText(
                cafeName != null
                        ? cafeName
                        : "카페 이름"
        );


        txtAddress.setText(
                address != null
                        ? address
                        : "주소 정보 없음"
        );


        txtReason.setText(
                reason != null
                        ? reason
                        : "추천 사유 정보가 없습니다."
        );


        // 기존 태그 표시
        setupTagChips(tags);

        // 평가 태그 생성
        setupEvaluationTags();

        // 내 평가 불러오기
        loadMyTagEvaluation();

        // 교차검증
        loadTagVerification();

        // 즐겨찾기
        loadFavoriteState();

        // 버튼
        setupClickListeners();


        boolean knownCafe =
                CafeIdentity.hasId(cafeId);


        btnFavorite.setEnabled(
                knownCafe
        );

        btnAddVisitRecord.setEnabled(
                knownCafe
        );

        btnSubmitTagEvaluation.setEnabled(
                knownCafe
        );


        if (!knownCafe) {

            btnDeleteTagEvaluation
                    .setVisibility(
                            View.GONE
                    );
        }


        if (knownCafe) {

            AccountPreferences
                    .open(
                            this,
                            "CafeFitRecent"
                    )
                    .edit()
                    .putString(
                            "recentCafe",
                            cafeName
                    )
                    .putString(
                            "recentCafeId",
                            cafeId
                    )
                    .apply();


            firestoreDb
                    .collection(
                            "cafes"
                    )
                    .document(
                            cafeId
                    )
                    .get()
                    .addOnSuccessListener(
                            document -> {

                                if (
                                        isFinishing()
                                                || isDestroyed()
                                ) {

                                    return;
                                }


                                if (
                                        document.exists()
                                                && document
                                                .getString(
                                                        "phone"
                                                )
                                                != null
                                ) {

                                    phone =
                                            document
                                                    .getString(
                                                            "phone"
                                                    );
                                }


                                updateCallButton();
                            }
                    );
        }


        updateCallButton();


        if (rvCafeVisitHistory != null) {

            rvCafeVisitHistory
                    .setLayoutManager(
                            new LinearLayoutManager(
                                    this
                            )
                    );
        }
    }


    // =====================================================
    // View 초기화
    // =====================================================

    private void initViews() {

        txtCafeName =
                findViewById(
                        R.id.txtDetailCafeName
                );

        txtAddress =
                findViewById(
                        R.id.txtDetailAddress
                );

        txtReason =
                findViewById(
                        R.id.txtDetailReason
                );

        txtAverageRating =
                findViewById(
                        R.id.txtAverageRating
                );

        txtReviewCount =
                findViewById(
                        R.id.txtReviewCount
                );

        txtNoReviews =
                findViewById(
                        R.id.txtNoReviews
                );


        txtEvaluationStatus =
                findViewById(
                        R.id.txtEvaluationStatus
                );


        txtVerificationSummary =
                findViewById(
                        R.id.txtVerificationSummary
                );

        layoutVerificationResults =
                findViewById(
                        R.id.layoutVerificationResults
                );


        btnFavorite =
                findViewById(
                        R.id.btnFavorite
                );

        btnAddVisitRecord =
                findViewById(
                        R.id.btnAddVisitRecord
                );

        btnSubmitTagEvaluation =
                findViewById(
                        R.id.btnSubmitTagEvaluation
                );

        btnDeleteTagEvaluation =
                findViewById(
                        R.id.btnDeleteTagEvaluation
                );


        btnBack =
                findViewById(
                        R.id.btnDetailBack
                );

        btnCall =
                findViewById(
                        R.id.btnActionCall
                );

        btnNavigation =
                findViewById(
                        R.id.btnActionNavi
                );

        btnShare =
                findViewById(
                        R.id.btnActionShare
                );


        chipGroupTags =
                findViewById(
                        R.id.chipGroupDetailTags
                );

        chipGroupEvaluation =
                findViewById(
                        R.id.chipGroupEvaluation
                );


        rvCafeVisitHistory =
                findViewById(
                        R.id.rvCafeVisitHistory
                );
    }


    // =====================================================
    // 버튼
    // =====================================================

    private void setupClickListeners() {

        if (btnBack != null) {

            btnBack.setOnClickListener(
                    v -> finish()
            );
        }


        if (btnFavorite != null) {

            btnFavorite.setOnClickListener(
                    v -> toggleFavorite()
            );
        }


        if (btnAddVisitRecord != null) {

            btnAddVisitRecord
                    .setOnClickListener(
                            v -> {

                                Intent intent =
                                        new Intent(
                                                CafeDetailActivity.this,
                                                VisitRecordActivity.class
                                        );

                                intent.putExtra(
                                        "mode",
                                        "add"
                                );

                                intent.putExtra(
                                        "cafeName",
                                        cafeName
                                );

                                intent.putExtra(
                                        "cafeId",
                                        cafeId
                                );

                                startActivity(
                                        intent
                                );
                            }
                    );
        }


        if (btnSubmitTagEvaluation != null) {

            btnSubmitTagEvaluation
                    .setOnClickListener(
                            v -> saveTagEvaluation()
                    );
        }


        if (btnDeleteTagEvaluation != null) {

            btnDeleteTagEvaluation
                    .setOnClickListener(
                            v ->
                                    showDeleteEvaluationDialog()
                    );
        }


        if (btnCall != null) {

            btnCall.setOnClickListener(
                    v -> {

                        if (
                                phone == null
                                        || phone
                                        .trim()
                                        .isEmpty()
                        ) {

                            return;
                        }


                        Intent intent =
                                new Intent(
                                        Intent.ACTION_DIAL,
                                        Uri.fromParts(
                                                "tel",
                                                phone,
                                                null
                                        )
                                );


                        startActivity(
                                intent
                        );
                    }
            );
        }


        if (btnNavigation != null) {

            btnNavigation
                    .setOnClickListener(
                            v -> {

                                String searchQuery =
                                        cafeName
                                                + " "
                                                + (
                                                address != null
                                                        ? address
                                                        : ""
                                        );


                                String url =
                                        "https://map.naver.com/v5/search/"
                                                + Uri.encode(
                                                searchQuery
                                        );


                                Intent intent =
                                        new Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(
                                                        url
                                                )
                                        );


                                startActivity(
                                        intent
                                );
                            }
                    );
        }


        if (btnShare != null) {

            btnShare.setOnClickListener(
                    v -> {

                        String shareText =
                                String.format(
                                        Locale.getDefault(),
                                        "[CafeFit] %s\n📍 주소: %s\n함께 방문해보세요!",
                                        cafeName,
                                        address
                                );


                        Intent shareIntent =
                                new Intent(
                                        Intent.ACTION_SEND
                                );


                        shareIntent.setType(
                                "text/plain"
                        );


                        shareIntent.putExtra(
                                Intent.EXTRA_TEXT,
                                shareText
                        );


                        startActivity(
                                Intent.createChooser(
                                        shareIntent,
                                        "카페 정보 공유하기"
                                )
                        );
                    }
            );
        }
    }


    // =====================================================
    // 기존 카페 태그
    // =====================================================

    private void setupTagChips(
            String tags
    ) {

        if (
                chipGroupTags == null
                        || tags == null
                        || tags
                        .trim()
                        .isEmpty()
        ) {

            return;
        }


        chipGroupTags.removeAllViews();


        String[] tagArray =
                tags.split(" ");


        for (String tag : tagArray) {

            if (
                    tag
                            .trim()
                            .isEmpty()
            ) {

                continue;
            }


            Chip chip =
                    new Chip(
                            this
                    );


            chip.setText(
                    tag.startsWith("#")
                            ? tag
                            : "#" + tag
            );


            chip.setClickable(
                    false
            );

            chip.setCheckable(
                    false
            );


            chipGroupTags.addView(
                    chip
            );
        }
    }


    // =====================================================
    // 평가용 태그
    // =====================================================

    private void setupEvaluationTags() {

        if (chipGroupEvaluation == null) {
            return;
        }


        chipGroupEvaluation
                .removeAllViews();


        evaluationTags.clear();


        evaluationTags.add(
                Tag.BEAN_NUTTY
        );

        evaluationTags.add(
                Tag.BEAN_ACIDIC
        );

        evaluationTags.add(
                Tag.INTERIOR_PRETTY
        );

        evaluationTags.add(
                Tag.DRINK_TASTY
        );

        evaluationTags.add(
                Tag.HIP
        );

        evaluationTags.add(
                Tag.WORK_FRIENDLY
        );

        evaluationTags.add(
                Tag.DESSERT
        );

        evaluationTags.add(
                Tag.SPECIALTY_DRIP
        );

        evaluationTags.add(
                Tag.SMALL_CAFE
        );

        evaluationTags.add(
                Tag.LARGE_CAFE
        );

        evaluationTags.add(
                Tag.OUTLET_MANY
        );

        evaluationTags.add(
                Tag.WIFI_FAST
        );

        evaluationTags.add(
                Tag.LAPTOP_OK
        );


        for (Tag tag : evaluationTags) {

            Chip chip =
                    new Chip(
                            this
                    );


            chip.setText(
                    tag.getKoreanLabel()
            );


            chip.setCheckable(
                    true
            );

            chip.setClickable(
                    true
            );


            chip.setTag(
                    tag.name()
            );


            chipGroupEvaluation
                    .addView(
                            chip
                    );
        }
    }


    // =====================================================
    // 평가 저장 / 수정
    // =====================================================

    private void saveTagEvaluation() {

        if (!CafeIdentity.hasId(cafeId)) {

            Toast.makeText(
                    this,
                    "카페 정보를 확인할 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (user == null) {

            Toast.makeText(
                    this,
                    "로그인 후 평가할 수 있습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        List<String> selectedTags =
                getSelectedEvaluationTags();


        if (selectedTags.isEmpty()) {

            if (hasTagEvaluation) {

                showDeleteEvaluationDialog();

            } else {

                Toast.makeText(
                        this,
                        "카페의 특징을 하나 이상 선택해주세요.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }


        String uid =
                user.getUid();


        Map<String, Object> evaluation =
                new HashMap<>();


        evaluation.put(
                "cafeId",
                cafeId
        );

        evaluation.put(
                "cafeName",
                cafeName
        );

        evaluation.put(
                "userId",
                uid
        );

        evaluation.put(
                "tags",
                selectedTags
        );

        evaluation.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );


        String documentId =
                cafeId
                        + "_"
                        + uid;


        setEvaluationButtonsEnabled(
                false
        );


        btnSubmitTagEvaluation
                .setText(
                        "저장 중..."
                );


        firestoreDb
                .collection(
                        "tag_evaluations"
                )
                .document(
                        documentId
                )
                .set(
                        evaluation
                )
                .addOnSuccessListener(
                        unused -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            hasTagEvaluation =
                                    true;


                            setEvaluationButtonsEnabled(
                                    true
                            );


                            btnSubmitTagEvaluation
                                    .setText(
                                            "태그 평가 수정"
                                    );


                            btnDeleteTagEvaluation
                                    .setVisibility(
                                            View.VISIBLE
                                    );


                            txtEvaluationStatus
                                    .setText(
                                            "✓ 평가가 저장되었습니다."
                                    );


                            Toast.makeText(
                                    this,
                                    "카페 평가가 저장되었습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();


                            // 저장 직후 교차검증 다시 계산
                            loadTagVerification();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            setEvaluationButtonsEnabled(
                                    true
                            );


                            btnSubmitTagEvaluation
                                    .setText(
                                            hasTagEvaluation
                                                    ? "태그 평가 수정"
                                                    : "태그 평가 저장"
                                    );


                            Toast.makeText(
                                    this,
                                    "평가 저장에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }


    // =====================================================
    // 선택 태그
    // =====================================================

    private List<String> getSelectedEvaluationTags() {

        List<String> selectedTags =
                new ArrayList<>();


        if (chipGroupEvaluation == null) {
            return selectedTags;
        }


        for (
                int i = 0;
                i < chipGroupEvaluation
                        .getChildCount();
                i++
        ) {

            View child =
                    chipGroupEvaluation
                            .getChildAt(
                                    i
                            );


            if (!(child instanceof Chip)) {
                continue;
            }


            Chip chip =
                    (Chip) child;


            if (
                    chip.isChecked()
                            && chip.getTag()
                            != null
            ) {

                selectedTags.add(
                        chip
                                .getTag()
                                .toString()
                );
            }
        }


        return selectedTags;
    }


    // =====================================================
    // 내 평가 로드
    // =====================================================

    private void loadMyTagEvaluation() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (
                user == null
                        || !CafeIdentity.hasId(
                        cafeId
                )
        ) {

            hasTagEvaluation =
                    false;


            btnDeleteTagEvaluation
                    .setVisibility(
                            View.GONE
                    );

            return;
        }


        String documentId =
                cafeId
                        + "_"
                        + user.getUid();


        firestoreDb
                .collection(
                        "tag_evaluations"
                )
                .document(
                        documentId
                )
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            if (!document.exists()) {

                                hasTagEvaluation =
                                        false;


                                btnSubmitTagEvaluation
                                        .setText(
                                                "태그 평가 저장"
                                        );


                                btnDeleteTagEvaluation
                                        .setVisibility(
                                                View.GONE
                                        );


                                txtEvaluationStatus
                                        .setText(
                                                ""
                                        );


                                clearEvaluationChips();

                                return;
                            }


                            hasTagEvaluation =
                                    true;


                            Object tagsObject =
                                    document.get(
                                            "tags"
                                    );


                            if (
                                    tagsObject
                                            instanceof List
                            ) {

                                List<?> savedTags =
                                        (List<?>)
                                                tagsObject;


                                for (
                                        int i = 0;
                                        i < chipGroupEvaluation
                                                .getChildCount();
                                        i++
                                ) {

                                    View child =
                                            chipGroupEvaluation
                                                    .getChildAt(
                                                            i
                                                    );


                                    if (
                                            !(child
                                                    instanceof Chip)
                                    ) {

                                        continue;
                                    }


                                    Chip chip =
                                            (Chip) child;


                                    if (
                                            chip.getTag()
                                                    != null
                                    ) {

                                        String tagName =
                                                chip
                                                        .getTag()
                                                        .toString();


                                        chip.setChecked(
                                                savedTags.contains(
                                                        tagName
                                                )
                                        );
                                    }
                                }
                            }


                            btnSubmitTagEvaluation
                                    .setText(
                                            "태그 평가 수정"
                                    );


                            btnDeleteTagEvaluation
                                    .setVisibility(
                                            View.VISIBLE
                                    );


                            txtEvaluationStatus
                                    .setText(
                                            "이전에 평가한 기록이 있습니다."
                                    );
                        }
                );
    }


    // =====================================================
    // 삭제 확인
    // =====================================================

    private void showDeleteEvaluationDialog() {

        if (!hasTagEvaluation) {

            Toast.makeText(
                    this,
                    "삭제할 평가가 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "태그 평가 삭제"
                )
                .setMessage(
                        "이 카페에 남긴 태그 평가를 삭제하시겠습니까?"
                )
                .setNegativeButton(
                        "취소",
                        null
                )
                .setPositiveButton(
                        "삭제",
                        (
                                dialog,
                                which
                        ) ->
                                deleteTagEvaluation()
                )
                .show();
    }


    // =====================================================
    // 평가 삭제
    // =====================================================

    private void deleteTagEvaluation() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (
                user == null
                        || !CafeIdentity
                        .hasId(
                                cafeId
                        )
        ) {

            return;
        }


        String documentId =
                cafeId
                        + "_"
                        + user.getUid();


        setEvaluationButtonsEnabled(
                false
        );


        btnDeleteTagEvaluation
                .setText(
                        "삭제 중..."
                );


        firestoreDb
                .collection(
                        "tag_evaluations"
                )
                .document(
                        documentId
                )
                .delete()
                .addOnSuccessListener(
                        unused -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            hasTagEvaluation =
                                    false;


                            clearEvaluationChips();


                            btnSubmitTagEvaluation
                                    .setText(
                                            "태그 평가 저장"
                                    );


                            btnDeleteTagEvaluation
                                    .setText(
                                            "평가 삭제"
                                    );


                            btnDeleteTagEvaluation
                                    .setVisibility(
                                            View.GONE
                                    );


                            setEvaluationButtonsEnabled(
                                    true
                            );


                            txtEvaluationStatus
                                    .setText(
                                            "평가가 삭제되었습니다."
                                    );


                            Toast.makeText(
                                    this,
                                    "태그 평가를 삭제했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();


                            // 삭제 후 교차검증 다시 계산
                            loadTagVerification();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            btnDeleteTagEvaluation
                                    .setText(
                                            "평가 삭제"
                                    );


                            setEvaluationButtonsEnabled(
                                    true
                            );


                            Toast.makeText(
                                    this,
                                    "평가 삭제에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }


    // =====================================================
    // 교차검증 핵심
    // =====================================================

    private void loadTagVerification() {

        if (!CafeIdentity.hasId(cafeId)) {

            txtVerificationSummary
                    .setText(
                            "카페 정보를 확인할 수 없습니다."
                    );

            return;
        }


        layoutVerificationResults
                .removeAllViews();


        txtVerificationSummary
                .setText(
                        "사용자 평가 데이터를 분석하고 있습니다."
                );


        // 먼저 cafes의 원본 태그를 읽음
        firestoreDb
                .collection(
                        "cafes"
                )
                .document(
                        cafeId
                )
                .get()
                .addOnSuccessListener(
                        cafeDocument -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            Set<String> originalTags =
                                    new HashSet<>();


                            Object originalTagsObject =
                                    cafeDocument.get(
                                            "tags"
                                    );


                            if (
                                    originalTagsObject
                                            instanceof List
                            ) {

                                List<?> list =
                                        (List<?>)
                                                originalTagsObject;


                                for (Object value : list) {

                                    if (value != null) {

                                        originalTags.add(
                                                value
                                                        .toString()
                                                        .trim()
                                                        .toUpperCase(
                                                                Locale.ROOT
                                                        )
                                        );
                                    }
                                }
                            }


                            // 해당 카페의 모든 사용자 평가 조회
                            firestoreDb
                                    .collection(
                                            "tag_evaluations"
                                    )
                                    .whereEqualTo(
                                            "cafeId",
                                            cafeId
                                    )
                                    .get()
                                    .addOnSuccessListener(
                                            evaluations -> {

                                                if (
                                                        isFinishing()
                                                                || isDestroyed()
                                                ) {

                                                    return;
                                                }


                                                int totalEvaluations =
                                                        evaluations.size();


                                                Map<String, Integer> counts =
                                                        new HashMap<>();


                                                for (
                                                        QueryDocumentSnapshot document
                                                        : evaluations
                                                ) {

                                                    Object evaluationTagsObject =
                                                            document.get(
                                                                    "tags"
                                                            );


                                                    if (
                                                            !(evaluationTagsObject
                                                                    instanceof List)
                                                    ) {

                                                        continue;
                                                    }


                                                    /*
                                                     * 한 사용자의 한 평가에서
                                                     * 같은 태그가 중복되어도
                                                     * 한 번만 계산
                                                     */
                                                    Set<String> uniqueTags =
                                                            new HashSet<>();


                                                    List<?> list =
                                                            (List<?>)
                                                                    evaluationTagsObject;


                                                    for (Object value : list) {

                                                        if (value != null) {

                                                            uniqueTags.add(
                                                                    value
                                                                            .toString()
                                                                            .trim()
                                                                            .toUpperCase(
                                                                                    Locale.ROOT
                                                                            )
                                                            );
                                                        }
                                                    }


                                                    for (
                                                            String tagName
                                                            : uniqueTags
                                                    ) {

                                                        Integer oldCount =
                                                                counts.get(
                                                                        tagName
                                                                );


                                                        counts.put(
                                                                tagName,
                                                                oldCount == null
                                                                        ? 1
                                                                        : oldCount + 1
                                                        );
                                                    }
                                                }


                                                showVerificationResults(
                                                        originalTags,
                                                        counts,
                                                        totalEvaluations
                                                );
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> {

                                                txtVerificationSummary
                                                        .setText(
                                                                "사용자 평가 데이터를 불러오지 못했습니다."
                                                        );
                                            }
                                    );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            txtVerificationSummary
                                    .setText(
                                            "기존 카페 태그를 불러오지 못했습니다."
                                    );
                        }
                );
    }


    // =====================================================
    // 검증 결과 화면 출력
    // =====================================================

    private void showVerificationResults(
            Set<String> originalTags,
            Map<String, Integer> counts,
            int totalEvaluations
    ) {

        layoutVerificationResults
                .removeAllViews();


        if (totalEvaluations == 0) {

            txtVerificationSummary
                    .setText(
                            "아직 사용자 평가가 없습니다.\n첫 번째 태그 평가를 남겨보세요."
                    );

            return;
        }


        txtVerificationSummary
                .setText(
                        String.format(
                                Locale.getDefault(),
                                "현재 %d명이 이 카페의 특징을 평가했습니다.",
                                totalEvaluations
                        )
                );


        List<TagVerification> results =
                new ArrayList<>();


        /*
         * 사용자가 평가할 수 있는 모든 태그를 대상으로
         * 기존 크롤링 태그와 사용자 평가를 비교
         */
        for (Tag tag : evaluationTags) {

            int count =
                    counts.containsKey(
                            tag.name()
                    )
                            ? counts.get(
                            tag.name()
                    )
                            : 0;


            boolean original =
                    originalTags.contains(
                            tag.name()
                    );


            /*
             * 기존 태그이거나
             * 최소 한 명이라도 선택한 태그만 표시
             */
            if (
                    original
                            || count > 0
            ) {

                results.add(
                        new TagVerification(
                                tag,
                                count,
                                totalEvaluations,
                                original
                        )
                );
            }
        }


        if (results.isEmpty()) {

            TextView emptyView =
                    createVerificationTextView();


            emptyView.setText(
                    "표시할 검증 결과가 없습니다."
            );


            layoutVerificationResults
                    .addView(
                            emptyView
                    );

            return;
        }


        /*
         * 비율 높은 순으로 표시
         */
        results.sort(
                (
                        a,
                        b
                ) -> {

                    int ratioCompare =
                            Double.compare(
                                    b.getRatio(),
                                    a.getRatio()
                            );


                    if (ratioCompare != 0) {

                        return ratioCompare;
                    }


                    /*
                     * 비율이 같으면 기존 크롤링 태그를
                     * 먼저 표시
                     */
                    if (
                            a.isOriginalTag()
                                    != b.isOriginalTag()
                    ) {

                        return a.isOriginalTag()
                                ? -1
                                : 1;
                    }


                    return a
                            .getTag()
                            .name()
                            .compareTo(
                                    b
                                            .getTag()
                                            .name()
                            );
                }
        );


        for (
                TagVerification result
                : results
        ) {

            addVerificationResultView(
                    result
            );
        }
    }


    // =====================================================
    // 태그 검증 결과 한 줄
    // =====================================================

    private void addVerificationResultView(
            TagVerification result
    ) {

        LinearLayout row =
                new LinearLayout(
                        this
                );


        row.setOrientation(
                LinearLayout.VERTICAL
        );


        row.setPadding(
                0,
                dpToPx(7),
                0,
                dpToPx(7)
        );


        TextView title =
                createVerificationTextView();


        title.setTypeface(
                null,
                Typeface.BOLD
        );


        String originalText =
                result.isOriginalTag()
                        ? " · 기존 태그"
                        : "";


        title.setText(
                String.format(
                        Locale.getDefault(),
                        "%s   %d%%",
                        result
                                .getTag()
                                .getKoreanLabel(),
                        result
                                .getPercentage()
                )
        );


        TextView detail =
                createVerificationTextView();


        detail.setText(
                String.format(
                        Locale.getDefault(),
                        "%d/%d명 선택 · %s%s",
                        result.getSelectedCount(),
                        result.getTotalEvaluationCount(),
                        result.getStatusText(),
                        originalText
                )
        );


        detail.setTextSize(
                12
        );


        row.addView(
                title
        );


        row.addView(
                detail
        );


        layoutVerificationResults
                .addView(
                        row
                );
    }


    // =====================================================
    // 검증 TextView 생성
    // =====================================================

    private TextView createVerificationTextView() {

        TextView textView =
                new TextView(
                        this
                );


        textView.setTextSize(
                14
        );


        textView.setTextColor(
                getColor(
                        R.color.text_dark
                )
        );


        return textView;
    }


    // =====================================================
    // dp 변환
    // =====================================================

    private int dpToPx(
            int dp
    ) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;


        return Math.round(
                dp * density
        );
    }


    // =====================================================
    // 태그 체크 해제
    // =====================================================

    private void clearEvaluationChips() {

        if (chipGroupEvaluation == null) {
            return;
        }


        for (
                int i = 0;
                i < chipGroupEvaluation
                        .getChildCount();
                i++
        ) {

            View child =
                    chipGroupEvaluation
                            .getChildAt(
                                    i
                            );


            if (
                    child
                            instanceof Chip
            ) {

                ((Chip) child)
                        .setChecked(
                                false
                        );
            }
        }
    }


    // =====================================================
    // 평가 버튼 활성화
    // =====================================================

    private void setEvaluationButtonsEnabled(
            boolean enabled
    ) {

        if (btnSubmitTagEvaluation != null) {

            btnSubmitTagEvaluation
                    .setEnabled(
                            enabled
                    );
        }


        if (btnDeleteTagEvaluation != null) {

            btnDeleteTagEvaluation
                    .setEnabled(
                            enabled
                    );
        }
    }


    // =====================================================
    // onResume
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        loadThisCafeVisitRecords();
    }


    // =====================================================
    // 리뷰
    // =====================================================

    private void loadThisCafeVisitRecords() {

        if (
                cafeName == null
                        || rvCafeVisitHistory
                        == null
        ) {

            return;
        }


        VisitRecordRepository
                .forCafe(
                        cafeId,
                        cafeName
                )
                .addOnCompleteListener(
                        task -> {

                            if (
                                    isFinishing()
                                            || isDestroyed()
                            ) {

                                return;
                            }


                            if (task.isSuccessful()) {

                                List<VisitRecord> records =
                                        task.getResult();


                                if (records == null) {

                                    records =
                                            new ArrayList<>();
                                }


                                float totalRating =
                                        0.0f;


                                for (
                                        VisitRecord record
                                        : records
                                ) {

                                    totalRating +=
                                            record
                                                    .getRating();
                                }


                                int reviewCount =
                                        records.size();


                                if (reviewCount > 0) {

                                    float avgRating =
                                            totalRating
                                                    / reviewCount;


                                    txtAverageRating
                                            .setText(
                                                    String.format(
                                                            Locale.getDefault(),
                                                            "⭐ %.1f",
                                                            avgRating
                                                    )
                                            );


                                    txtReviewCount
                                            .setText(
                                                    String.format(
                                                            Locale.getDefault(),
                                                            "(%d개 리뷰)",
                                                            reviewCount
                                                    )
                                            );


                                    txtNoReviews
                                            .setVisibility(
                                                    View.GONE
                                            );


                                    rvCafeVisitHistory
                                            .setVisibility(
                                                    View.VISIBLE
                                            );

                                } else {

                                    txtAverageRating
                                            .setText(
                                                    "⭐ 0.0"
                                            );


                                    txtReviewCount
                                            .setText(
                                                    "(0개 리뷰)"
                                            );


                                    txtNoReviews
                                            .setVisibility(
                                                    View.VISIBLE
                                            );


                                    rvCafeVisitHistory
                                            .setVisibility(
                                                    View.GONE
                                            );
                                }


                                VisitHistoryAdapter adapter =
                                        new VisitHistoryAdapter(
                                                records
                                        );


                                rvCafeVisitHistory
                                        .setAdapter(
                                                adapter
                                        );

                            } else {

                                Toast.makeText(
                                        this,
                                        "방문 기록을 불러오지 못했습니다.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // 즐겨찾기
    // =====================================================

    private void loadFavoriteState() {

        SharedPreferences prefs =
                AccountPreferences.open(
                        this,
                        "CafeFitFavorites"
                );


        isFavorite =
                CafeIdentity.hasId(
                        cafeId
                )
                        && prefs.getBoolean(
                        cafeId,
                        false
                );


        updateFavoriteButton();
    }


    // =====================================================
    // 전화 버튼
    // =====================================================

    private void updateCallButton() {

        if (btnCall == null) {
            return;
        }


        boolean available =
                phone != null
                        && !phone
                        .trim()
                        .isEmpty();


        btnCall.setEnabled(
                available
        );


        btnCall.setAlpha(
                available
                        ? 1.0f
                        : 0.4f
        );


        btnCall.setContentDescription(
                available
                        ? "전화 걸기"
                        : "등록된 전화번호 없음"
        );
    }


    // =====================================================
    // 즐겨찾기 변경
    // =====================================================

    private void toggleFavorite() {

        if (
                !CafeIdentity.hasId(
                        cafeId
                )
        ) {

            return;
        }


        isFavorite =
                !isFavorite;


        String tags =
                getIntent()
                        .getStringExtra(
                                "cafe_tags"
                        );


        String reason =
                getIntent()
                        .getStringExtra(
                                "cafe_reason"
                        );


        SharedPreferences prefs =
                AccountPreferences.open(
                        this,
                        "CafeFitFavorites"
                );


        prefs.edit()
                .putBoolean(
                        cafeId,
                        isFavorite
                )
                .putString(
                        cafeId
                                + "_name",
                        cafeName
                )
                .putString(
                        cafeId
                                + "_address",
                        address
                )
                .putString(
                        cafeId
                                + "_tags",
                        tags
                )
                .putString(
                        cafeId
                                + "_reason",
                        reason
                )
                .apply();


        updateFavoriteButton();


        Toast.makeText(
                this,
                isFavorite
                        ? "즐겨찾기에 추가했어요."
                        : "즐겨찾기에서 해제했어요.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =====================================================
    // 즐겨찾기 UI
    // =====================================================

    private void updateFavoriteButton() {

        if (btnFavorite == null) {
            return;
        }


        btnFavorite.setText(
                isFavorite
                        ? "♥ 즐겨찾기 해제"
                        : "♡ 즐겨찾기 추가"
        );
    }
}