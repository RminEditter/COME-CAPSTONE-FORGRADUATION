package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    private EditText editSignupId, editSignupPassword, editSignupPasswordCheck;
    private Button btnRegister, btnBackLogin;

    // 1. FirebaseAuth 변수 추가
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 2. FirebaseAuth 초기화
        mAuth = FirebaseAuth.getInstance();

        androidx.appcompat.widget.AppCompatButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        editSignupId = findViewById(R.id.editSignupId);
        editSignupPassword = findViewById(R.id.editSignupPassword);
        editSignupPasswordCheck = findViewById(R.id.editSignupPasswordCheck);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackLogin = findViewById(R.id.btnBackLogin);

        btnRegister.setOnClickListener(v -> {
            String id = editSignupId.getText().toString().trim();
            String pw = editSignupPassword.getText().toString().trim();
            String pwCheck = editSignupPasswordCheck.getText().toString().trim();

            if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw) || TextUtils.isEmpty(pwCheck)) {
                Toast.makeText(SignupActivity.this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
            } else if (!pw.equals(pwCheck)) {
                Toast.makeText(SignupActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            } else if (pw.length() < 6) {
                // 파이어베이스 보안 정책상 비밀번호는 최소 6자리 이상이어야 합니다.
                Toast.makeText(SignupActivity.this, "비밀번호는 최소 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            } else {

                // 3. 파이어베이스는 ID가 이메일 형식이어야 하므로, 일반 아이디일 경우 가상 도메인을 붙여줍니다.
                String finalEmail = id;
                if (!id.contains("@")) {
                    finalEmail = id + "@cafefit.com";
                }

                // 4. ★ 핵심: Firebase Auth에 유저 등록하기 ★
                mAuth.createUserWithEmailAndPassword(finalEmail, pw)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(SignupActivity.this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String errorMsg = task.getException() != null ? task.getException().getMessage() : "알 수 없는 오류";

                                // [수정] 토스트 대신 확인 버튼을 누를 때까지 안 사라지는 팝업창을 띄웁니다!
                                new androidx.appcompat.app.AlertDialog.Builder(SignupActivity.this)
                                        .setTitle("⚠️ 회원가입 실패")
                                        .setMessage("원인: " + errorMsg)
                                        .setPositiveButton("확인", (dialog, which) -> dialog.dismiss())
                                        .show();
                            }
                        });
            }
        });

        btnBackLogin.setOnClickListener(v -> finish());
    }
}