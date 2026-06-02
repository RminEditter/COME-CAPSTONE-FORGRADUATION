package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SurveyActivity extends AppCompatActivity {

    private RadioGroup rgBean, rgStyle, rgSize, rgCompanion;
    private Switch switchDessert, switchSpecialty;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        setupBackButton();
        BottomNavHelper.setup(this);

        rgBean = findViewById(R.id.rgBean);
        rgStyle = findViewById(R.id.rgStyle);
        rgSize = findViewById(R.id.rgSize);
        rgCompanion = findViewById(R.id.rgCompanion);

        switchDessert = findViewById(R.id.switchDessert);
        switchSpecialty = findViewById(R.id.switchSpecialty);

        btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> submitSurvey());
    }

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void submitSurvey() {
        if (rgBean.getCheckedRadioButtonId() == -1 ||
                rgStyle.getCheckedRadioButtonId() == -1 ||
                rgSize.getCheckedRadioButtonId() == -1 ||
                rgCompanion.getCheckedRadioButtonId() == -1) {

            Toast.makeText(this, "모든 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // [유지] 기존 태그 분기 로직 완벽 보존
        String beanTag;
        int beanId = rgBean.getCheckedRadioButtonId();

        if (beanId == R.id.rbNutty) {
            beanTag = "BEAN_NUTTY";
        } else {
            beanTag = "BEAN_ACIDIC";
        }

        String styleTag = "";
        int styleId = rgStyle.getCheckedRadioButtonId();

        if (styleId == R.id.rbWork) {
            styleTag = "WORK_FRIENDLY";
        } else if (styleId == R.id.rbInterior) {
            styleTag = "INTERIOR_PRETTY";
        } else if (styleId == R.id.rbHip) {
            styleTag = "HIP";
        } else if (styleId == R.id.rbDrink) {
            styleTag = "DRINK_TASTY";
        }

        String sizeTag;
        int sizeId = rgSize.getCheckedRadioButtonId();

        if (sizeId == R.id.rbSmall) {
            sizeTag = "SMALL_CAFE";
        } else {
            sizeTag = "LARGE_CAFE";
        }

        String companionTag = "";
        int companionId = rgCompanion.getCheckedRadioButtonId();

        if (companionId == R.id.rbSolo) {
            companionTag = "SOLO";
        } else if (companionId == R.id.rbCouple) {
            companionTag = "COUPLE";
        } else if (companionId == R.id.rbFriend) {
            companionTag = "FRIEND";
        } else if (companionId == R.id.rbFamily) {
            companionTag = "FAMILY";
        }

        String dessertTag = switchDessert.isChecked() ? "DESSERT" : "";
        String specialtyTag = switchSpecialty.isChecked() ? "SPECIALTY_DRIP" : "";

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인 유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userSurveyData = new HashMap<>();
        userSurveyData.put("bean_tag", beanTag);
        userSurveyData.put("style_tag", styleTag);
        userSurveyData.put("size_tag", sizeTag);
        userSurveyData.put("companion_tag", companionTag);
        userSurveyData.put("dessert_tag", dessertTag);
        userSurveyData.put("specialty_tag", specialtyTag);
        userSurveyData.put("email", currentUser.getEmail());

        btnSubmit.setEnabled(false);

        db.collection("users").document(currentUid)
                .set(userSurveyData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SurveyActivity.this, "취향 분석 완료! 프로필 설정을 이어갑니다. 👤", Toast.LENGTH_SHORT).show();

                    // 💡 [수정 핵심 포인트]: 기존 MainActivity 직행 노선을 끊고, FirstProfileActivity로 토스합니다!
                    Intent intent = new Intent(SurveyActivity.this, FirstProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish(); // 현재 설문 화면은 완전히 종료하여 뒤로가기로 못 돌아오게 방어합니다.
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(SurveyActivity.this, "서버 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}