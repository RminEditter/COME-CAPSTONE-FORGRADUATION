package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private EditText editSignupId, editSignupPassword, editSignupPasswordCheck;
    private Button btnRegister, btnBackLogin;
    private android.widget.TextView btnFindPassword; //  TextView로 독립 분리 선언!

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

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


        btnFindPassword = findViewById(R.id.btnFindPassword);
        if (btnFindPassword != null) {
            btnFindPassword.setOnClickListener(v -> showFindPasswordDialog());
        }

        btnRegister.setOnClickListener(v -> {
            String emailInput = editSignupId.getText().toString().trim();
            String pw = editSignupPassword.getText().toString().trim();
            String pwCheck = editSignupPasswordCheck.getText().toString().trim();


            if (TextUtils.isEmpty(emailInput) || TextUtils.isEmpty(pw) || TextUtils.isEmpty(pwCheck)) {
                Toast.makeText(SignupActivity.this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }


            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                Toast.makeText(SignupActivity.this, "실제 사용 가능한 올바른 이메일 형식(예: user@gmail.com)을 입력해 주세요.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!pw.equals(pwCheck)) {
                Toast.makeText(SignupActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pw.length() < 6) {
                Toast.makeText(SignupActivity.this, "비밀번호는 최소 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 중복 클릭으로 인한 다중 요청 방지 잠금
            btnRegister.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(emailInput, pw)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {

                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            btnRegister.setEnabled(true);
                                            if (verifyTask.isSuccessful()) {


                                                mAuth.signOut();

                                                new AlertDialog.Builder(SignupActivity.this)
                                                        .setTitle("📧 이메일 인증 발송 완료")
                                                        .setMessage("가입이 접수되었습니다!\n\n입력하신 메일함(" + emailInput + ")에 들어가서 구글 인증 링크를 반드시 클릭해 주셔야 로그인이 가능합니다.")
                                                        .setPositiveButton("확인", (dialog, which) -> {
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        })
                                                        .setCancelable(false) // 임의로 창을 닫지 못하게 제한
                                                        .show();
                                            } else {
                                                Toast.makeText(SignupActivity.this, "인증 메일 발송 중 오류: " + verifyTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "알 수 없는 오류";

                            new AlertDialog.Builder(SignupActivity.this)
                                    .setTitle("⚠️ 회원가입 실패")
                                    .setMessage("원인: " + errorMsg)
                                    .setPositiveButton("확인", (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    });
        });

        btnBackLogin.setOnClickListener(v -> finish());
    }

    // 5. [추가] 비밀번호 분기 시 이메일로 패스워드 재설정 웹 링크를 바로 전송해 주는 다이얼로그 팝업 함수
    private void showFindPasswordDialog() {
        final EditText edEmail = new EditText(this);
        edEmail.setHint("example@naver.com");
        edEmail.setPadding(60, 40, 60, 40);

        new AlertDialog.Builder(this)
                .setTitle("🔑 비밀번호 찾기")
                .setMessage("가입하셨던 실제 이메일 주소를 입력해 주시면 비밀번호 재설정 링크를 발송해 드립니다.")
                .setView(edEmail)
                .setPositiveButton("발송", (dialog, which) -> {
                    String email = edEmail.getText().toString().trim();
                    if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(SignupActivity.this, "올바른 이메일 형식을 적어주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this, "비밀번호 재설정 이메일을 보냈습니다. 메일함을 확인하세요! 📩", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(SignupActivity.this, "오류: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .show();
    }
}