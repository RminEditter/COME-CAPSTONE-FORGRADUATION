package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText editProfileNickname;
    private Spinner spinnerProfileGender, spinnerProfileAge;
    private Button btnSaveProfile, btnLogout;

    // [추가] 파이어베이스 인스턴스 변수 정의
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String[] genderItems = {"여성", "남성", "선택 안 함"};
    private String[] ageItems = {"10대", "20대", "30대", "40대 이상"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // [추가] 파이어베이스 초기화 및 로그인 체크
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupBackButton();
        BottomNavHelper.setup(this);

        editProfileNickname = findViewById(R.id.editProfileNickname);
        spinnerProfileGender = findViewById(R.id.spinnerProfileGender);
        spinnerProfileAge = findViewById(R.id.spinnerProfileAge);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // [유지] 기존 스피너 어댑터 바인딩 로직
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

        // [수정] SharedPreferences 로직을 제거하고, 서버(Firestore)에서 내 프로필 로드
        loadUserProfileFromServer();

        // [수정] 저장 버튼 클릭 시 파이어베이스 클라우드 데이터 병합 저장
        btnSaveProfile.setOnClickListener(v -> {
            String newNickname = editProfileNickname.getText().toString().trim();
            String newGender = spinnerProfileGender.getSelectedItem().toString();
            String newAge = spinnerProfileAge.getSelectedItem().toString();

            if (newNickname.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 중복 클릭 방지 잠금
            btnSaveProfile.setEnabled(false);

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("nickname", newNickname);
            profileData.put("gender", newGender);
            profileData.put("age", newAge);

            // 💡 SetOptions.merge() 장치를 달아 설문조사 태그 데이터 손상을 방지합니다.
            db.collection("users").document(currentUser.getUid())
                    .set(profileData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "프로필이 계정에 동기화되었습니다. ✨", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "서버 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // [수정] 로그아웃 버튼 클릭 시 파이어베이스 계정 세션까지 완전 해제
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // 💡 구글 인증 세션 로그아웃 처리

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // [추가] 내 UID 문서에서 프로필을 실시간으로 읽어와 UI 셋팅하는 함수
    private void loadUserProfileFromServer() {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // 서버 필드에서 값 추출
                            String nickname = document.getString("nickname");
                            String gender = document.getString("gender");
                            String age = document.getString("age");

                            // 데이터가 있을 때만 화면에 반영 (없으면 기본값 또는 공백)
                            if (nickname != null) {
                                editProfileNickname.setText(nickname);
                            }

                            if (gender != null) {
                                setSpinnerSelection(spinnerProfileGender, genderItems, gender);
                            } else {
                                setSpinnerSelection(spinnerProfileGender, genderItems, "선택 안 함");
                            }

                            if (age != null) {
                                setSpinnerSelection(spinnerProfileAge, ageItems, age);
                            } else {
                                setSpinnerSelection(spinnerProfileAge, ageItems, "20대");
                            }
                        }
                    } else {
                        Toast.makeText(this, "프로필 로드 실패", Toast.LENGTH_SHORT).show();
                    }
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