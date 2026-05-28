package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class ProfileActivity extends AppCompatActivity {

    private EditText editProfileNickname;
    private Spinner spinnerProfileGender, spinnerProfileAge;
    private Button btnSaveProfile, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBackButton();
        BottomNavHelper.setup(this);

        editProfileNickname = findViewById(R.id.editProfileNickname);
        spinnerProfileGender = findViewById(R.id.spinnerProfileGender);
        spinnerProfileAge = findViewById(R.id.spinnerProfileAge);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        String[] genderItems = {"여성", "남성", "선택 안 함"};
        String[] ageItems = {"10대", "20대", "30대", "40대 이상"};

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genderItems
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileGender.setAdapter(genderAdapter);

        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ageItems
        );
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileAge.setAdapter(ageAdapter);

        SharedPreferences prefs = getSharedPreferences("CafeFitProfile", MODE_PRIVATE);

        String nickname = prefs.getString("nickname", "");
        String gender = prefs.getString("gender", "선택 안 함");
        String age = prefs.getString("age", "20대");

        editProfileNickname.setText(nickname);
        setSpinnerSelection(spinnerProfileGender, genderItems, gender);
        setSpinnerSelection(spinnerProfileAge, ageItems, age);

        btnSaveProfile.setOnClickListener(v -> {
            String newNickname = editProfileNickname.getText().toString().trim();
            String newGender = spinnerProfileGender.getSelectedItem().toString();
            String newAge = spinnerProfileAge.getSelectedItem().toString();

            if (newNickname.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nickname", newNickname);
            editor.putString("gender", newGender);
            editor.putString("age", newAge);
            editor.apply();

            Toast.makeText(ProfileActivity.this, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setSpinnerSelection(Spinner spinner, String[] items, String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}