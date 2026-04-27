package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText editSignupId, editSignupPassword, editSignupPasswordCheck;
    private Button btnRegister, btnBackLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

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
            } else {
                SharedPreferences prefs = getSharedPreferences("CafeFitUser", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString("userId", id);
                editor.putString("userPw", pw);
                editor.apply();

                Toast.makeText(SignupActivity.this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnBackLogin.setOnClickListener(v -> finish());
    }
}