package com.example.capstone2026;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class FirstProfileActivity extends AppCompatActivity {

    private EditText editNickname;
    private Spinner spinnerGender, spinnerAge;
    private Button btnNext;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "위치 권한이 거부되었습니다. 나중에 다시 설정할 수 있어요.", Toast.LENGTH_SHORT).show();
                }
                moveToSurvey();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_profile);

        editNickname = findViewById(R.id.editNickname);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerAge = findViewById(R.id.spinnerAge);
        btnNext = findViewById(R.id.btnNext);

        String[] genderItems = {"성별 선택", "여성", "남성", "선택 안 함"};
        String[] ageItems = {"나이대 선택", "10대", "20대", "30대", "40대 이상"};

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, genderItems);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ageItems);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAge.setAdapter(ageAdapter);

        btnNext.setOnClickListener(v -> {
            String nickname = editNickname.getText().toString().trim();
            String gender = spinnerGender.getSelectedItem().toString();
            String age = spinnerAge.getSelectedItem().toString();

            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (gender.equals("성별 선택")) {
                Toast.makeText(this, "성별을 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (age.equals("나이대 선택")) {
                Toast.makeText(this, "나이대를 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("CafeFitProfile", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nickname", nickname);
            editor.putString("gender", gender);
            editor.putString("age", age);
            editor.putBoolean("isFirstProfileDone", true);
            editor.apply();

            requestLocationPermission();
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            moveToSurvey();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void moveToSurvey() {
        Intent intent = new Intent(FirstProfileActivity.this, SurveyActivity.class);
        startActivity(intent);
        finish();
    }
}
