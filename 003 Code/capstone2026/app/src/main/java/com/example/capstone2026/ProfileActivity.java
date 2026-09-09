package com.example.capstone2026;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ProfileActivity extends AppCompatActivity {
    private ProfileDashboardView dashboard;
    private int loadGeneration;
    private FirebaseAuth auth;
    private final FirebaseAuth.AuthStateListener authListener = ignored -> {
        if (auth.getCurrentUser() == null) finish();
        else loadDashboard();
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ProfileUi.applyInsets(findViewById(R.id.profileRoot));
        auth = FirebaseAuth.getInstance();
        dashboard = new ProfileDashboardView(findViewById(R.id.profileRoot));
        BottomNavHelper.setup(this);
        View selected = findViewById(R.id.btnNavProfile);
        selected.setSelected(true);
        selected.setContentDescription(getString(R.string.profile_current_tab));
        ViewCompat.setBackgroundTintList(selected,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brown_dark)));
        link(R.id.btnEditProfile, ProfileEditActivity.class);
        link(R.id.btnProfileSurvey, SurveyActivity.class);
        link(R.id.statVisits, VisitHistoryActivity.class);
        link(R.id.btnProfileHistory, VisitHistoryActivity.class);
        link(R.id.statFavorites, FavoriteActivity.class);
        link(R.id.btnProfileFavorites, FavoriteActivity.class);
        link(R.id.statBadges, BadgeActivity.class);
        link(R.id.btnViewBadges, BadgeActivity.class);
        findViewById(R.id.btnRetryProfile).setOnClickListener(v -> loadDashboard());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            loadGeneration++;
            auth.signOut();
            getSharedPreferences("CafeFitLogin", MODE_PRIVATE).edit().putBoolean("auto_login", false).apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void link(int id, Class<?> destination) {
        findViewById(id).setOnClickListener(v -> startActivity(new Intent(this, destination)));
    }

    @Override protected void onStart() {
        super.onStart();
        // Registration delivers the current user, including after returning from another screen.
        auth.addAuthStateListener(authListener);
    }

    @Override protected void onStop() {
        auth.removeAuthStateListener(authListener);
        loadGeneration++;
        super.onStop();
    }

    private void loadDashboard() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.profile_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = user.getUid();
        int generation = ++loadGeneration;
        dashboard.showLoading();
        int favorites = 0;
        for (Object value : AccountPreferences.open(this, "CafeFitFavorites").getAll().values()) {
            if (Boolean.TRUE.equals(value)) favorites++;
        }
        dashboard.showFavoriteCount(favorites);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<DocumentSnapshot> profile = db.collection("users").document(uid).get();
        Task<QuerySnapshot> visits = db.collection("visit_records").whereEqualTo("userUid", uid).get();
        Task<QuerySnapshot> badges = db.collection("users").document(uid).collection("badges").get();
        profile.addOnCompleteListener(task -> {
            if (isCurrent(generation, uid) && task.isSuccessful()) dashboard.showProfile(task.getResult().getData());
        });
        visits.addOnCompleteListener(task -> {
            if (isCurrent(generation, uid) && task.isSuccessful()) dashboard.showVisitCount(task.getResult().size());
        });
        badges.addOnCompleteListener(task -> {
            if (!isCurrent(generation, uid) || !task.isSuccessful()) return;
            int unlocked = 0;
            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                if (Boolean.TRUE.equals(document.get("unlocked"))) unlocked++;
            }
            dashboard.showBadgeCount(unlocked);
        });
        Tasks.whenAllComplete(profile, visits, badges).addOnCompleteListener(task -> {
            if (isCurrent(generation, uid)) {
                dashboard.showCompleted(!profile.isSuccessful() || !visits.isSuccessful() || !badges.isSuccessful());
            }
        });
    }

    private boolean isCurrent(int generation, String uid) {
        FirebaseUser user = auth.getCurrentUser();
        return !isFinishing() && !isDestroyed() && generation == loadGeneration
                && user != null && uid.equals(user.getUid());
    }
}
