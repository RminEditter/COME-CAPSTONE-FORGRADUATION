package com.example.capstone2026;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.widget.AppCompatButton;

public class BottomNavHelper {

    public static void setup(Activity activity) {

        AppCompatButton btnNavMap =
                activity.findViewById(R.id.btnNavMap);

        AppCompatButton btnNavMain =
                activity.findViewById(R.id.btnNavMain);

        AppCompatButton btnNavProfile =
                activity.findViewById(R.id.btnNavProfile);

        AppCompatButton btnNavCommunity =
                activity.findViewById(R.id.btnNavCommunity);

        // 지도 버튼
        if (btnNavMap != null) {
            btnNavMap.setOnClickListener(v -> {

                if (!(activity instanceof MapActivity)) {

                    Intent intent =
                            new Intent(activity, MapActivity.class);

                    activity.startActivity(intent);
                }
            });
        }

        // 메인 버튼
        if (btnNavMain != null) {
            btnNavMain.setOnClickListener(v -> {

                if (!(activity instanceof MainActivity)) {

                    Intent intent =
                            new Intent(activity, MainActivity.class);

                    activity.startActivity(intent);
                }
            });
        }

        // 프로필 버튼
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {

                if (!(activity instanceof ProfileActivity)) {

                    Intent intent =
                            new Intent(activity, ProfileActivity.class);

                    activity.startActivity(intent);
                }
            });
        }

        // 커뮤니티 버튼
        if (btnNavCommunity != null) {
            btnNavCommunity.setOnClickListener(v -> {

                if (!(activity instanceof CommunityActivity)) {

                    Intent intent =
                            new Intent(activity, CommunityActivity.class);

                    activity.startActivity(intent);
                }
            });
        }
    }
}