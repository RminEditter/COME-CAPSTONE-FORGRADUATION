package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

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
        String bean = getSelectedText(rgBean);
        String style = getSelectedText(rgStyle);
        String size = getSelectedText(rgSize);
        String companion = getSelectedText(rgCompanion);

        if (bean.isEmpty() || style.isEmpty() || size.isEmpty() || companion.isEmpty()) {
            Toast.makeText(this, "모든 항목을 선택해 주세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean dessert = switchDessert.isChecked();
        boolean specialty = switchSpecialty.isChecked();

        List<Tag> userTags = SurveyTagConverter.convertSurveyToTags(
                bean, style, dessert, specialty, size, companion
        );

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

        if (id == -1) {
            return "";
        }

        RadioButton rb = findViewById(id);
        return rb.getText().toString();
    }
}