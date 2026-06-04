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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText editId, editPassword;
    private Button btnLogin, btnSignup;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // [추가] 최초 가입자 판별을 위한 Firestore 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // [추가] Firestore 초기화

        androidx.appcompat.widget.AppCompatButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        editId = findViewById(R.id.editId);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> {
            String email = editId.getText().toString().trim();
            String pw = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pw)) {
                Toast.makeText(LoginActivity.this, "이메일 주소와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 중복 클릭 방지 잠금
            btnLogin.setEnabled(false);

            // Firebase 서버에 로그인 요청하기
            mAuth.signInWithEmailAndPassword(email, pw)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // 1. [이메일 인증 확인] 링크를 진짜로 누른 사람만 통과
                                if (user.isEmailVerified()) {

                                    // 2. ★ [핵심 기능] Firestore 서버에서 이 유저의 취향/프로필 설정 데이터가 있는지 조회 ★
                                    checkUserSetupStatus(user.getUid());

                                } else {
                                    btnLogin.setEnabled(true);
                                    // [인증 미완료자 차단]
                                    new AlertDialog.Builder(LoginActivity.this)
                                            .setTitle("📧 이메일 인증 미완료")
                                            .setMessage("아직 이메일 인증이 완료되지 않았습니다.\n가입하신 메일함에 접속하셔서 구글 인증 링크를 반드시 클릭해 주세요!")
                                            .setPositiveButton("확인", (dialog, which) -> dialog.dismiss())
                                            .show();

                                    mAuth.signOut();
                                }
                            }
                        } else {
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "이메일 또는 비밀번호가 올바르지 않거나 가입되지 않은 회원입니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 💡 [추가] 로그인 성공 유저의 UID 문서를 조회하여 설문/프로필 화면 또는 메인 화면으로 내비게이션하는 함수
     */
    private void checkUserSetupStatus(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true); // 로직이 끝나면 버튼 상태 해제

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        // 문서가 존재하고, 핵심 데이터(예: nickname이나 bean_tag)가 채워져 있는지 검사
                        if (document.exists() && document.contains("bean_tag") && document.contains("nickname")) {

                            // [기존 유저 Case]: 취향 설문과 프로필이 모두 완료된 유저는 바로 메인 홈화면 진입!
                            Toast.makeText(LoginActivity.this, "로그인 성공! 환영합니다. ✨", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // [신규 가입자 또는 미설정 유저 Case]: 데이터가 없으므로 최초 설문/프로필 설정창으로 강제 안내!
                            Toast.makeText(LoginActivity.this, "첫 방문을 환영합니다! 취향 설문과 프로필 설정을 시작합니다. ☕", Toast.LENGTH_LONG).show();

                            // 💡 Tip: 보통 설문(SurveyActivity)을 먼저 시키고 완료되면 프로필로 보내거나,
                            // 동료분이 만들어둔 FirstProfileActivity가 있다면 거기로 먼저 보내주시면 됩니다.
                            // 여기서는 가장 보편적인 'SurveyActivity'를 첫 관문으로 설정해 드립니다.
                            // (FirstProfileActivity로 먼저 보내고 싶다면 아래 클래스명만 바꾸시면 됩니다!)
                            Intent intent = new Intent(LoginActivity.this, SurveyActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // 서버 통신 장애 등 예외 처리 발생 시 안전하게 메인으로 보내거나 토스트 안내
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }
}