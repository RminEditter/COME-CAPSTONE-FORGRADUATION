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
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirstProfileActivity extends AppCompatActivity {

    private EditText editNickname;
    private Spinner spinnerGender, spinnerAge;
    private Button btnNext;

    // 💡 위치 권한 팝업이 끝나면 설문창이 아니라 최종 홈 화면(MainActivity)으로 보내도록 수정
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "위치 권한이 거부되었습니다. 나중에 다시 설정할 수 있어요.", Toast.LENGTH_SHORT).show();
                }

                moveToMain();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_profile);

        setupBackButton();

        editNickname = findViewById(R.id.editNickname);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerAge = findViewById(R.id.spinnerAge);
        btnNext = findViewById(R.id.btnNext);

        String[] genderItems = {"성별 선택", "여성", "남성", "선택 안 함"};
        String[] ageItems = {"나이대 선택", "10대", "20대", "30대", "40대 이상"};

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genderItems
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ageItems
        );
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

            // [기존 유지] 혹시 모를 로컬 로직 파탄을 막기 위해 SharedPreferences 동기화 유지
            SharedPreferences prefs = getSharedPreferences("CafeFitProfile", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nickname", nickname);
            editor.putString("gender", gender);
            editor.putString("age", age);
            editor.putBoolean("isFirstProfileDone", true);
            editor.apply();

            // 💡 [핵심 추가] 파이어베이스 Firestore 클라우드 서버에 프로필 데이터 적재
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "로그인 유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentUid = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 설문조사 데이터가 들어있는 동일한 내 UID 문서에 닉네임, 성별, 나이를 덮어쓰지 않고 '누적 합병(merge)' 합니다.
            Map<String, Object> userProfileData = new HashMap<>();
            userProfileData.put("nickname", nickname);
            userProfileData.put("gender", gender);
            userProfileData.put("age", age);

            btnNext.setEnabled(false); // 중복 클릭 차단

            db.collection("users").document(currentUid)
                    .set(userProfileData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(FirstProfileActivity.this, "모든 프로필 설정이 완료되었습니다! 🎉", Toast.LENGTH_SHORT).show();
                        // 데이터 저장 성공 후 위치 권한 프로세스 진행
                        requestLocationPermission();
                    })
                    .addOnFailureListener(e -> {
                        btnNext.setEnabled(true);
                        Toast.makeText(FirstProfileActivity.this, "프로필 서버 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            moveToMain();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // 💡 [이름 및 타겟 변경] moveToSurvey() 노선을 끊고 메인 화면으로 당당하게 진입하게 합니다.
    private void moveToMain() {
        Intent intent = new Intent(FirstProfileActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // 뒤로가기를 눌러도 다시 프로필 설정창으로 못 오게 액티비티 파괴
    }
}