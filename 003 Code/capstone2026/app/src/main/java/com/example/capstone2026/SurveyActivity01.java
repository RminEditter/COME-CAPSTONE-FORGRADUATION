package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SurveyActivity01 extends AppCompatActivity {

    RadioGroup rgBean, rgStyle, rgSize, rgCompanion;
    Switch switchDessert, switchSpecialty;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        // 뷰 초기화
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
        String bean = getSelectedText(rgBean);
        String style = getSelectedText(rgStyle);
        String size = getSelectedText(rgSize);
        String companion = getSelectedText(rgCompanion);

        // 모든 항목 선택 여부 검사
        if (bean.isEmpty() || style.isEmpty() || size.isEmpty() || companion.isEmpty()) {
            Toast.makeText(this, "모든 항목을 선택해 주세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean dessert = switchDessert.isChecked();
        boolean specialty = switchSpecialty.isChecked();

        // 컨버터를 통해 태그 변환
        List<Tag> userTags = SurveyTagConverter.convertSurveyToTags(
                bean, style, dessert, specialty, size, companion
        );

        // 결과 전달
        Intent intent = new Intent(this, MainActivity01.class);
        ArrayList<String> tagStrings = new ArrayList<>();
        for (Tag t : userTags) {
            tagStrings.add(t.name());
        }
        intent.putStringArrayListExtra("selected_tags", tagStrings);

        startActivity(intent);
        finish();
    }

    private String getSelectedText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        return rb.getText().toString();
    }
}