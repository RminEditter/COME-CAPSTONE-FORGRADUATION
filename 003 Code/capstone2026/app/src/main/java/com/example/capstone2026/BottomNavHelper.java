package com.example.capstone2026;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.widget.AppCompatButton;

public class BottomNavHelper {

    public static void setup(Activity activity) {
        AppCompatButton btnMap = activity.findViewById(R.id.btnNavMap);
        AppCompatButton btnMain = activity.findViewById(R.id.btnNavMain);
        AppCompatButton btnProfile = activity.findViewById(R.id.btnNavProfile);

        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                if (!(activity instanceof RecommendCafeActivity)) {
                    activity.startActivity(new Intent(activity, RecommendCafeActivity.class));
                }
            });
        }

        if (btnMain != null) {
            btnMain.setOnClickListener(v -> {
                if (!(activity instanceof MainActivity)) {
                    activity.startActivity(new Intent(activity, MainActivity.class));
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

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (!(activity instanceof ProfileActivity)) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                }
            });
        }

    }
}