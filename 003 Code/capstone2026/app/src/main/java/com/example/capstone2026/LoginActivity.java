package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editId, editPassword;
    private Button btnLogin, btnSignup;

    // 1. FirebaseAuth 변수 선언
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 2. FirebaseAuth 초기화
        mAuth = FirebaseAuth.getInstance();

        androidx.appcompat.widget.AppCompatButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

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

            // 가입할 때와 마찬가지로 이메일 형식이 아니면 가상 도메인을 붙여줍니다.
            if (!id.contains("@")) {
                id = id + "@cafefit.com";
            }

            // 3. ★ 핵심: Firebase 서버에 로그인 요청하기 ★
            mAuth.signInWithEmailAndPassword(id, pw)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // 로그인 성공 시, 프로필 설정 여부는 기존 폰 내부 SharedPreferences 방식을 유지합니다.
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
                            // 로그인 실패 (아이디 없음, 비밀번호 틀림 등)
                            Toast.makeText(LoginActivity.this, "아이디 또는 비밀번호가 올바르지 않거나 가입되지 않은 회원입니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }
}