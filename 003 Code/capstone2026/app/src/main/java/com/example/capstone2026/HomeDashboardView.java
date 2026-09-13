package com.example.capstone2026;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/** UI binding is separate from Firebase so real layouts can be tested offline. */
final class HomeDashboardView {
    private final View root;
    HomeDashboardView(View root) {
        this.root = root;
        root.findViewById(R.id.cardRecommend).setClipToOutline(true);
        ((EditText) root.findViewById(R.id.editSearch)).setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_home_search, 0, 0, 0);
        tint(R.id.btnNavMain, R.color.beige_soft);
        tint(R.id.btnHomeMap, R.color.beige_soft);
        tint(R.id.btnRecommendCafe, R.color.brown_main);
        if (root.getResources().getConfiguration().fontScale > 1.2f) {
            LinearLayout actions = root.findViewById(R.id.homeActions);
            actions.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < actions.getChildCount(); i++) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) params.topMargin = (int) (8 * root.getResources().getDisplayMetrics().density);
                actions.getChildAt(i).setLayoutParams(params);
            }
        }
    }

    private void tint(int id, int color) {
        ViewCompat.setBackgroundTintList(root.findViewById(id), ColorStateList.valueOf(
                ContextCompat.getColor(root.getContext(), color)));
    }

    private void text(int id, String value) { ((TextView) root.findViewById(id)).setText(value); }
    void loading() {
        text(R.id.txtRecommendCafeName, "취향에 맞는 카페를 찾고 있어요");
        text(R.id.txtRecommendCafeDesc, "저장된 취향과 가까운 위치를 함께 살펴볼게요.");
        text(R.id.txtHomeBadge, "취향에 맞는 추천");
        root.findViewById(R.id.txtHomeAddress).setVisibility(View.GONE);
        ((ChipGroup) root.findViewById(R.id.homeCafeTags)).removeAllViews();
        action("불러오는 중…", false, false);
        favorite(false, false);
    }

    void message(String title, String description, String action) {
        loading();
        text(R.id.txtRecommendCafeName, title);
        text(R.id.txtRecommendCafeDesc, description);
        action(action, true, false);
    }

    void recommendation(Recommender.Recommendation best, boolean fallback, HomeSituation situation) {
        text(R.id.txtRecommendCafeName, best.cafe.name);
        text(R.id.txtHomeBadge, situation == null ? "취향에 맞는 추천" : situation.label + " · 오늘의 추천");
        text(R.id.txtHomeAddress, best.cafe.address == null ? "주소 정보 없음" : best.cafe.address);
        root.findViewById(R.id.txtHomeAddress).setVisibility(View.VISIBLE);
        text(R.id.txtRecommendCafeDesc, best.reason
                + (fallback ? "\n대전 궁동·어은동 기준 추천" : ""));
        ChipGroup group = root.findViewById(R.id.homeCafeTags);
        group.removeAllViews();
        if (best.cafe.tags != null) {
            for (Tag tag : best.cafe.tags) {
                if (tag == null || group.getChildCount() >= 3) continue;
                Chip chip = new Chip(root.getContext());
                chip.setText("#" + tag.getKoreanLabel());
                chip.setTextColor(ContextCompat.getColor(root.getContext(), R.color.brown_dark));
                chip.setChipBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(root.getContext(), R.color.ivory_bg)));
                chip.setCheckable(false);
                chip.setClickable(false);
                group.addView(chip);
            }
        }
        action("카페 자세히 보기", true, true);
    }

    void favorite(boolean saved, boolean enabled) {
        ImageButton button = root.findViewById(R.id.btnHomeFavorite);
        button.setEnabled(enabled);
        button.setImageResource(saved ? R.drawable.ic_home_bookmark_on : R.drawable.ic_home_bookmark);
        button.setContentDescription(saved ? "즐겨찾기 해제" : "즐겨찾기 추가");
        button.setSelected(saved);
    }

    private void action(String label, boolean enabled, boolean map) {
        text(R.id.btnRecommendCafe, label);
        root.findViewById(R.id.btnRecommendCafe).setEnabled(enabled);
        root.findViewById(R.id.btnHomeMap).setEnabled(map);
    }
}
