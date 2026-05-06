package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SurveyActivity extends AppCompatActivity {

    private RadioGroup rgBean, rgStyle, rgSize, rgCompanion;
    private Switch switchDessert, switchSpecialty;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        rgBean = findViewById(R.id.rgBean);
        rgStyle = findViewById(R.id.rgStyle);
        rgSize = findViewById(R.id.rgSize);
        rgCompanion = findViewById(R.id.rgCompanion);

        switchDessert = findViewById(R.id.switchDessert);
        switchSpecialty = findViewById(R.id.switchSpecialty);

        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> submitSurvey());
    }

    private void submitSurvey() {
        if (rgBean.getCheckedRadioButtonId() == -1 ||
                rgStyle.getCheckedRadioButtonId() == -1 ||
                rgSize.getCheckedRadioButtonId() == -1 ||
                rgCompanion.getCheckedRadioButtonId() == -1) {

            Toast.makeText(this, "모든 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

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

        SharedPreferences prefs = getSharedPreferences("CafeFitSurvey", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("bean_tag", beanTag);
        editor.putString("style_tag", styleTag);
        editor.putString("size_tag", sizeTag);
        editor.putString("companion_tag", companionTag);
        editor.putString("dessert_tag", dessertTag);
        editor.putString("specialty_tag", specialtyTag);

        editor.apply();

        Toast.makeText(this, "취향 분석 완료!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(SurveyActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}