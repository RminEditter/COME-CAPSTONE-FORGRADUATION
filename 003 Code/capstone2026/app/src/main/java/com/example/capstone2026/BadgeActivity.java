package com.example.capstone2026;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BadgeActivity extends AppCompatActivity {

    private LinearLayout layoutBadgeList;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final String[] badgeCodes = {
            "NEWBIE",
            "REGULAR",
            "CRITIC",
            "ACIDIC_LOVER",
            "WORK_MASTER",
            "DESSERT_EXPLORER",
            "PHOTO_HUNTER",
            "HIP_SEEKER"
    };

    private final Map<String, BadgeData> savedBadgeMap =
            new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badge);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "로그인이 필요합니다.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        setupBackButton();

        layoutBadgeList =
                findViewById(R.id.layoutBadgeList);

        loadBadges();
    }

    private void setupBackButton() {
        AppCompatButton btnBack =
                findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadBadges() {

        db.collection("users")
                .document(currentUser.getUid())
                .collection("badges")
                .get()
                .addOnCompleteListener(task -> {

                    savedBadgeMap.clear();

                    if (task.isSuccessful() &&
                            task.getResult() != null) {

                        for (DocumentSnapshot document :
                                task.getResult().getDocuments()) {

                            String badgeCode =
                                    document.getString(
                                            "badgeCode"
                                    );

                            String badgeName =
                                    document.getString(
                                            "badgeName"
                                    );

                            Long progressLong =
                                    document.getLong(
                                            "progress"
                                    );

                            Long goalLong =
                                    document.getLong(
                                            "goal"
                                    );

                            Boolean unlockedBoolean =
                                    document.getBoolean(
                                            "unlocked"
                                    );

                            if (badgeCode == null) {
                                continue;
                            }

                            int progress =
                                    progressLong != null
                                            ? progressLong.intValue()
                                            : 0;

                            int goal =
                                    goalLong != null
                                            ? goalLong.intValue()
                                            : BadgeManager.getBadgeGoal(
                                            badgeCode
                                    );

                            boolean unlocked =
                                    Boolean.TRUE.equals(
                                            unlockedBoolean
                                    );

                            BadgeData badgeData =
                                    new BadgeData(
                                            badgeCode,
                                            badgeName,
                                            progress,
                                            goal,
                                            unlocked
                                    );

                            savedBadgeMap.put(
                                    badgeCode,
                                    badgeData
                            );
                        }

                    } else {

                        Toast.makeText(
                                this,
                                "배지 정보를 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    showAllBadges();
                });
    }

    private void showAllBadges() {

        layoutBadgeList.removeAllViews();

        for (String badgeCode : badgeCodes) {

            BadgeData badgeData =
                    savedBadgeMap.get(badgeCode);

            if (badgeData == null) {

                badgeData = new BadgeData(
                        badgeCode,
                        BadgeManager.getBadgeName(
                                badgeCode
                        ),
                        0,
                        BadgeManager.getBadgeGoal(
                                badgeCode
                        ),
                        false
                );
            }

            addBadgeView(badgeData);
        }
    }

    private void addBadgeView(BadgeData badgeData) {

        View badgeView =
                getLayoutInflater().inflate(
                        R.layout.item_badge,
                        layoutBadgeList,
                        false
                );

        TextView txtBadgeIcon =
                badgeView.findViewById(
                        R.id.txtBadgeIcon
                );

        TextView txtBadgeName =
                badgeView.findViewById(
                        R.id.txtBadgeName
                );

        TextView txtBadgeDescription =
                badgeView.findViewById(
                        R.id.txtBadgeDescription
                );

        TextView txtBadgeProgress =
                badgeView.findViewById(
                        R.id.txtBadgeProgress
                );

        TextView txtBadgeStatus =
                badgeView.findViewById(
                        R.id.txtBadgeStatus
                );

        ProgressBar progressBadge =
                badgeView.findViewById(
                        R.id.progressBadge
                );

        txtBadgeIcon.setText(
                getBadgeIcon(
                        badgeData.badgeCode
                )
        );

        txtBadgeName.setText(
                badgeData.badgeName
        );

        txtBadgeDescription.setText(
                getBadgeDescription(
                        badgeData.badgeCode
                )
        );

        txtBadgeProgress.setText(
                badgeData.progress
                        + " / "
                        + badgeData.goal
        );

        progressBadge.setMax(
                badgeData.goal
        );

        progressBadge.setProgress(
                badgeData.progress
        );

        if (badgeData.unlocked) {

            txtBadgeStatus.setText(
                    "획득 완료 ✓"
            );

            txtBadgeIcon.setAlpha(1.0f);
            txtBadgeName.setAlpha(1.0f);
            txtBadgeDescription.setAlpha(1.0f);

        } else {

            txtBadgeStatus.setText(
                    "진행 중"
            );

            txtBadgeIcon.setAlpha(0.45f);
            txtBadgeName.setAlpha(0.65f);
            txtBadgeDescription.setAlpha(0.65f);
        }

        layoutBadgeList.addView(
                badgeView
        );
    }

    private String getBadgeIcon(String badgeCode) {

        switch (badgeCode) {

            case "NEWBIE":
                return "🌱";

            case "REGULAR":
                return "☕";

            case "CRITIC":
                return "⭐";

            case "ACIDIC_LOVER":
                return "🍋";

            case "WORK_MASTER":
                return "💻";

            case "DESSERT_EXPLORER":
                return "🍰";

            case "PHOTO_HUNTER":
                return "📷";

            case "HIP_SEEKER":
                return "✨";

            default:
                return "🏅";
        }
    }

    private String getBadgeDescription(String badgeCode) {

        switch (badgeCode) {

            case "NEWBIE":
                return "첫 방문 기록을 남겨보세요.";

            case "REGULAR":
                return "카페 방문 기록을 10번 남겨보세요.";

            case "CRITIC":
                return "별점을 포함한 방문 기록을 10번 남겨보세요.";

            case "ACIDIC_LOVER":
                return "산미 있는 원두 카페를 5번 방문해보세요.";

            case "WORK_MASTER":
                return "카공하기 좋은 카페를 10번 방문해보세요.";

            case "DESSERT_EXPLORER":
                return "디저트 카페를 7번 방문해보세요.";

            case "PHOTO_HUNTER":
                return "인테리어가 예쁜 카페를 5번 방문해보세요.";

            case "HIP_SEEKER":
                return "힙한 감성의 카페를 7번 방문해보세요.";

            default:
                return "";
        }
    }

    private static class BadgeData {

        String badgeCode;
        String badgeName;
        int progress;
        int goal;
        boolean unlocked;

        BadgeData(
                String badgeCode,
                String badgeName,
                int progress,
                int goal,
                boolean unlocked
        ) {

            this.badgeCode = badgeCode;
            this.badgeName = badgeName;
            this.progress = progress;
            this.goal = goal;
            this.unlocked = unlocked;
        }
    }
}