package com.example.capstone2026;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {
    /** Keep an in-flight save across rotation so a second write is not submitted. */
    public static class EditState extends ViewModel {
        String uid;
        String nickname = "";
        String gender;
        String age;
        boolean loaded;
        Task<DocumentSnapshot> load;
        Task<Void> save;
    }

    private EditState state;
    private EditText nickname;
    private Spinner gender;
    private Spinner age;
    private Button save;
    private View retry;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        ProfileUi.applyInsets(findViewById(R.id.profileRoot));
        nickname = findViewById(R.id.editProfileNickname);
        gender = findViewById(R.id.spinnerProfileGender);
        age = findViewById(R.id.spinnerProfileAge);
        save = findViewById(R.id.btnSaveProfile);
        retry = findViewById(R.id.btnRetryProfile);
        status = findViewById(R.id.txtProfileStatus);
        setupSpinner(gender, R.array.profile_gender_options);
        setupSpinner(age, R.array.profile_age_options);
        state = new ViewModelProvider(this).get(EditState.class);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || (state.uid != null && !state.uid.equals(user.getUid()))) {
            finish();
            return;
        }
        state.uid = user.getUid();
        if (savedInstanceState != null && state.uid.equals(savedInstanceState.getString("profileUid"))
                && savedInstanceState.getBoolean("profileLoaded")) {
            state.loaded = true;
            state.nickname = savedInstanceState.getString("profileNickname", "");
            state.gender = savedInstanceState.getString("profileGender");
            state.age = savedInstanceState.getString("profileAge");
        }
        restoreForm();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        retry.setOnClickListener(v -> { state.load = null; loadProfile(); });
        save.setOnClickListener(v -> saveProfile());
    }

    @Override protected void onStart() {
        super.onStart();
        if (state == null || !sameAccount()) { finish(); return; }
        if (state.save != null) observeSave();
        else if (state.loaded) showReady();
        else loadProfile();
    }

    @Override protected void onStop() {
        rememberForm();
        super.onStop();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        rememberForm();
        if (state != null) {
            out.putString("profileUid", state.uid);
            out.putBoolean("profileLoaded", state.loaded);
            out.putString("profileNickname", state.nickname);
            out.putString("profileGender", state.gender);
            out.putString("profileAge", state.age);
        }
        super.onSaveInstanceState(out);
    }

    private void loadProfile() {
        if (!sameAccount()) { finish(); return; }
        setEditable(false);
        status.setText(R.string.profile_loading);
        status.setVisibility(View.VISIBLE);
        retry.setVisibility(View.GONE);
        if (state.load == null) {
            state.load = FirebaseFirestore.getInstance().collection("users").document(state.uid).get();
        }
        Task<DocumentSnapshot> request = state.load;
        request.addOnCompleteListener(this, task -> {
            if (!sameAccount() || isFinishing() || isDestroyed() || state.load != request) return;
            state.load = null;
            if (!task.isSuccessful()) {
                status.setText(R.string.profile_load_failed);
                retry.setVisibility(View.VISIBLE);
                return;
            }
            Map<String, Object> data = task.getResult().getData();
            state.nickname = string(data, "nickname");
            state.gender = string(data, "gender");
            state.age = string(data, "age");
            state.loaded = true;
            restoreForm();
            showReady();
        });
    }

    private void saveProfile() {
        if (!sameAccount()) { finish(); return; }
        if (!state.loaded || state.save != null) return;
        String name = nickname.getText().toString().trim();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > 20) {
            nickname.setError(getString(R.string.profile_nickname_invalid));
            nickname.requestFocus();
            return;
        }
        rememberForm();
        state.nickname = name;
        Map<String, Object> fields = new HashMap<>();
        fields.put("nickname", name);
        fields.put("gender", state.gender);
        fields.put("age", state.age);
        // Merge only editable profile fields; retain survey answers, priority and other user data.
        state.save = FirebaseFirestore.getInstance().collection("users").document(state.uid)
                .set(fields, SetOptions.merge());
        observeSave();
    }

    private void observeSave() {
        setEditable(false);
        status.setText(R.string.profile_saving);
        status.setVisibility(View.VISIBLE);
        retry.setVisibility(View.GONE);
        Task<Void> request = state.save;
        request.addOnCompleteListener(this, task -> {
            if (!sameAccount() || isFinishing() || isDestroyed() || state.save != request) return;
            state.save = null;
            if (task.isSuccessful()) {
                Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                showReady();
                status.setText(R.string.profile_save_failed);
                status.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupSpinner(Spinner spinner, int arrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayId, R.layout.item_profile_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void restoreForm() {
        nickname.setText(state.nickname);
        select(gender, state.gender);
        select(age, state.age);
    }

    private void select(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value)) { spinner.setSelection(i); return; }
        }
        spinner.setSelection(0);
    }

    private void rememberForm() {
        if (state == null || !state.loaded) return;
        state.nickname = nickname.getText().toString();
        state.gender = String.valueOf(gender.getSelectedItem());
        state.age = String.valueOf(age.getSelectedItem());
    }

    private void showReady() {
        setEditable(true);
        status.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
    }

    private void setEditable(boolean enabled) {
        save.setEnabled(enabled);
        nickname.setEnabled(enabled);
        gender.setEnabled(enabled);
        age.setEnabled(enabled);
    }

    private boolean sameAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && state != null && user.getUid().equals(state.uid);
    }

    private static String string(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        return value instanceof String ? (String) value : "";
    }
}
