package com.example.capstone2026;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;
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
    private TextView pointBalanceView;
    private final PointManager pointManager = new PointManager();
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
        installPointCard();
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
        loadPoints(generation, uid);
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


    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void installPointCard() {
        View root = findViewById(R.id.profileRoot);
        if (!(root instanceof LinearLayout)) return;
        LinearLayout rootLayout = (LinearLayout) root;
        if (!(rootLayout.getChildAt(0) instanceof android.widget.ScrollView)) return;
        android.widget.ScrollView scroll = (android.widget.ScrollView) rootLayout.getChildAt(0);
        if (!(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout content = (LinearLayout) scroll.getChildAt(0);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xFFFFFFFF);
        background.setCornerRadius(dp(16));
        card.setBackground(background);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(12);
        cardParams.bottomMargin = dp(12);

        TextView title = new TextView(this);
        title.setText("내 CafeFit 포인트");
        title.setTextColor(0xFF49382D);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        card.addView(title);

        pointBalanceView = new TextView(this);
        pointBalanceView.setText("포인트 조회 중...");
        pointBalanceView.setTextColor(0xFF65452F);
        pointBalanceView.setTypeface(null, Typeface.BOLD);
        pointBalanceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        balanceParams.topMargin = dp(8);
        card.addView(pointBalanceView, balanceParams);

        TextView hint = new TextView(this);
        hint.setText("방문 태그 평가에 참여하고 포인트를 모아보세요.");
        hint.setTextColor(0xFF77716C);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        card.addView(hint);
        TextView historyButton = new TextView(this);
        historyButton.setText("포인트 적립·사용 내역 보기  ›");
        historyButton.setTextColor(0xFF65452F);
        historyButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        historyButton.setPadding(dp(8), dp(14), dp(8), dp(14));
        historyButton.setClickable(true);
        historyButton.setFocusable(true);
        historyButton.setOnClickListener(v -> startActivity(
                new Intent(ProfileActivity.this, PointHistoryActivity.class)));
        card.addView(historyButton);

        TextView shopButton = new TextView(this);
        shopButton.setText("기프티콘 교환소 가기  ›");
        shopButton.setTextColor(0xFF65452F);
        shopButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        shopButton.setPadding(dp(8), dp(14), dp(8), dp(14));
        shopButton.setClickable(true);
        shopButton.setFocusable(true);
        shopButton.setOnClickListener(v -> startActivity(
                new Intent(ProfileActivity.this, RewardShopActivity.class)));
        card.addView(shopButton);
        content.addView(card, Math.min(3, content.getChildCount()), cardParams);
    }

    private void loadPoints(int generation, String uid) {
        if (pointBalanceView == null) return;
        pointBalanceView.setText("포인트 조회 중...");
        pointManager.getMyPoints(new PointManager.PointCallback() {
            @Override public void onSuccess(long points) {
                if (isCurrent(generation, uid)) {
                    pointBalanceView.setText(String.format(java.util.Locale.KOREA, "%,d P", points));
                }
            }
            @Override public void onFailure(String message) {
                if (isCurrent(generation, uid)) {
                    pointBalanceView.setText("포인트 조회 불가");
                }
            }
        });
    }

    private boolean isCurrent(int generation, String uid) {
        FirebaseUser user = auth.getCurrentUser();
        return !isFinishing() && !isDestroyed() && generation == loadGeneration
                && user != null && uid.equals(user.getUid());
    }
}
