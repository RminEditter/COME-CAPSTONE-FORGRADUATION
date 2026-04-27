package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SurveyActivity extends AppCompatActivity {

    private RadioGroup radioGroupBean, radioGroupScale, radioGroupMood, radioGroupDessert;
    private RadioButton radioBeanNutty, radioScaleSmall, radioMoodWork, radioMoodInterior, radioMoodHip, radioDessertYes;
    private Button btnSurveySubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // 1. 뷰 연결
        radioGroupBean = findViewById(R.id.radioGroupBean);
        radioGroupScale = findViewById(R.id.radioGroupScale);
        radioGroupMood = findViewById(R.id.radioGroupMood);
        radioGroupDessert = findViewById(R.id.radioGroupDessert);

        radioBeanNutty = findViewById(R.id.radioBeanNutty);
        radioScaleSmall = findViewById(R.id.radioScaleSmall);
        radioMoodWork = findViewById(R.id.radioMoodWork);
        radioMoodInterior = findViewById(R.id.radioMoodInterior);
        radioMoodHip = findViewById(R.id.radioMoodHip);
        radioDessertYes = findViewById(R.id.radioDessertYes);

        btnSurveySubmit = findViewById(R.id.btnSurveySubmit);

        // 2. 제출 버튼 이벤트
        btnSurveySubmit.setOnClickListener(v -> {
            // 모든 문항을 선택했는지 체크
            if (radioGroupBean.getCheckedRadioButtonId() == -1 ||
                    radioGroupScale.getCheckedRadioButtonId() == -1 ||
                    radioGroupMood.getCheckedRadioButtonId() == -1 ||
                    radioGroupDessert.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, "모든 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. 선택한 라디오 버튼에 따라 Tag 매핑
            String beanTag = radioBeanNutty.isChecked() ? "BEAN_NUTTY" : "BEAN_ACIDIC";
            String scaleTag = radioScaleSmall.isChecked() ? "SMALL_CAFE" : "LARGE_CAFE";

            String moodTag = "";
            if (radioMoodWork.isChecked()) moodTag = "WORK_FRIENDLY";
            else if (radioMoodInterior.isChecked()) moodTag = "INTERIOR_PRETTY";
            else if (radioMoodHip.isChecked()) moodTag = "HIP";

            String dessertTag = radioDessertYes.isChecked() ? "DESSERT" : ""; // 음료만 마시면 디저트 태그 없음

            // 4. 저장하기
            SharedPreferences prefs = getSharedPreferences("CafeFitSurvey", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("bean_tag", beanTag);
            editor.putString("scale_tag", scaleTag);
            editor.putString("mood_tag", moodTag);
            editor.putString("dessert_tag", dessertTag);
            editor.apply();

            Toast.makeText(this, "취향 분석 완료!", Toast.LENGTH_SHORT).show();

            // 5. 메인 화면으로 이동
            Intent intent = new Intent(SurveyActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}