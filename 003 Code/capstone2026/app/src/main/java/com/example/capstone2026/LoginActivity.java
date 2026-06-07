package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText editId, editPassword;
    private AppCompatButton btnLogin, btnSignup;
    private CheckBox cbAutoLogin;
    private TextView tvFindAccount;

    private FirebaseAuth mAuth;
    private SharedPreferences loginPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        loginPrefs = getSharedPreferences("CafeFitLogin", MODE_PRIVATE);

        initViews();

        // 💡 [자동 로그인 시스템]
        // 이전에 자동 로그인을 체크했고, 파이어베이스 세션이 살아있다면 바로 메인화면으로 이동!
        if (loginPrefs.getBoolean("auto_login", false) && mAuth.getCurrentUser() != null) {
            // 조원분이 이메일 인증 필수 로직을 짜두셨으므로, 인증된 유저인지 체크해서 진입시킵니다.
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                goToMainActivity();
                return;
            }
        }

        setupClickListeners();
    }

    private void initViews() {
        editId = findViewById(R.id.editId);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup); // XML의 회원가입 버튼 ID
        cbAutoLogin = findViewById(R.id.cbAutoLogin);
        tvFindAccount = findViewById(R.id.tvFindAccount);
    }

    private void setupClickListeners() {
        // 로그인 버튼 클릭 시
        btnLogin.setOnClickListener(v -> {
            String email = editId.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null && user.isEmailVerified()) {
                                // 자동 로그인 체크 여부 로컬 저장소에 저장
                                boolean isAutoChecked = cbAutoLogin != null && cbAutoLogin.isChecked();
                                loginPrefs.edit().putBoolean("auto_login", isAutoChecked).apply();

                                Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                                goToMainActivity();
                            } else {
                                // 인증을 안 했으면 로그아웃 시키고 안내문 출력
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this, "메일함에서 이메일 인증 링크를 클릭하셔야 로그인이 가능합니다! 📧", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        // 비밀번호 찾기 버튼 클릭 시
        if (tvFindAccount != null) {
            tvFindAccount.setOnClickListener(v -> {
                String email = editId.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "ID 입력란에 가입했던 이메일을 적고 다시 눌러주시면 비밀번호 재설정 링크를 보내드립니다!", Toast.LENGTH_LONG).show();
                } else {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "입력하신 이메일로 비밀번호 재설정 링크를 전송했습니다! 📩", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, "전송 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}