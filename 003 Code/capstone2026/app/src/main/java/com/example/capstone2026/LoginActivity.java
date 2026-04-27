package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editId, editPassword;
    private Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editId = findViewById(R.id.editId);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> {
            String id = editId.getText().toString().trim();
            String pw = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw)) {
                Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences userPrefs = getSharedPreferences("CafeFitUser", MODE_PRIVATE);
            String savedId = userPrefs.getString("userId", "");
            String savedPw = userPrefs.getString("userPw", "");

            if (TextUtils.isEmpty(savedId) || TextUtils.isEmpty(savedPw)) {
                Toast.makeText(LoginActivity.this, "먼저 회원가입을 진행하세요.", Toast.LENGTH_SHORT).show();
            } else if (id.equals(savedId) && pw.equals(savedPw)) {
                SharedPreferences profilePrefs = getSharedPreferences("CafeFitProfile", MODE_PRIVATE);
                boolean isFirstProfileDone = profilePrefs.getBoolean("isFirstProfileDone", false);

                Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                Intent intent;
                if (!isFirstProfileDone) {
                    intent = new Intent(LoginActivity.this, FirstProfileActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                }

                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "아이디 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }
}