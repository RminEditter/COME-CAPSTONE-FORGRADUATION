package com.example.capstone2026;

import android.app.Activity;
import android.content.Intent;
import android.widget.Button;

public class BottomNavHelper {

    public static void setup(Activity activity) {
        Button btnMain = activity.findViewById(R.id.btnNavMain);
        Button btnMap = activity.findViewById(R.id.btnNavMap);
        Button btnProfile = activity.findViewById(R.id.btnNavProfile);

        if (btnMain != null) {
            btnMain.setOnClickListener(v -> {
                if (!(activity instanceof MainActivity)) {
                    activity.startActivity(new Intent(activity, MainActivity.class));
                }
            });
        }

        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                if (!(activity instanceof RecommendCafeActivity)) {
                    activity.startActivity(new Intent(activity, RecommendCafeActivity.class));
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (!(activity instanceof ProfileActivity)) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                }
            });
        }
    }
}