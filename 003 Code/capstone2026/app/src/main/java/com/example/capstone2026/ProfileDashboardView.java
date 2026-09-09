package com.example.capstone2026;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.NumberFormat;
import java.util.Map;

/** Binds both real account data and instrumentation preview fixtures to the same XML. */
final class ProfileDashboardView {
    private final View root;
    private final Context context;
    private final ChipGroup chips;
    private boolean profileLoaded;

    ProfileDashboardView(View root) {
        this.root = root;
        context = root.getContext();
        if (context.getResources().getConfiguration().fontScale >= 1.3f) {
            LinearLayout stats = (LinearLayout) root.findViewById(R.id.statVisits).getParent();
            stats.setOrientation(LinearLayout.VERTICAL);
            int spacing = Math.round(16 * context.getResources().getDisplayMetrics().density);
            for (int i = 0; i < stats.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) stats.getChildAt(i);
                row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                row.setMinimumHeight(spacing * 4);
                row.setOrientation(LinearLayout.HORIZONTAL);
                TextView label = (TextView) row.getChildAt(1);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMarginStart(spacing);
                label.setLayoutParams(params);
                label.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            }
        }
        chips = root.findViewById(R.id.chipProfileTaste);
        chips.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            int width = r - l - chips.getPaddingLeft() - chips.getPaddingRight();
            if (width > 0 && r - l != oldR - oldL) {
                for (int i = 0; i < chips.getChildCount(); i++) ((Chip) chips.getChildAt(i)).setMaxWidth(width);
            }
        });
    }

    void showLoading() {
        profileLoaded = false;
        text(R.id.txtProfileName).setText(R.string.profile_default_name);
        chips.removeAllViews();
        text(R.id.txtProfilePriority).setText(R.string.profile_loading);
        root.findViewById(R.id.txtTasteEmpty).setVisibility(View.GONE);
        text(R.id.btnProfileSurvey).setText(R.string.profile_survey);
        setCount(R.id.statVisits, R.id.txtVisitCount, R.string.profile_visits, null);
        setCount(R.id.statFavorites, R.id.txtFavoriteCount, R.string.profile_favorites, null);
        setCount(R.id.statBadges, R.id.txtBadgeCount, R.string.profile_badges, null);
        text(R.id.txtProfileStatus).setText(R.string.profile_loading);
        root.findViewById(R.id.txtProfileStatus).setVisibility(View.VISIBLE);
        root.findViewById(R.id.btnRetryProfile).setVisibility(View.GONE);
    }

    void showProfile(Map<String, Object> data) {
        profileLoaded = true;
        Object name = data == null ? null : data.get("nickname");
        text(R.id.txtProfileName).setText(name instanceof String && !((String) name).trim().isEmpty()
                ? ((String) name).trim() : context.getString(R.string.profile_default_name));
        SurveyPreferences preferences = SurveyPreferences.from(data);
        chips.removeAllViews();
        for (Tag tag : preferences.tags) {
            Chip chip = new Chip(context);
            chip.setText(tag.getKoreanLabel());
            chip.setCheckable(false);
            chip.setClickable(false);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.beige_soft)));
            chip.setTextColor(ContextCompat.getColor(context, R.color.brown_dark));
            chip.setChipStrokeWidth(0);
            if (chips.getWidth() > 0) chip.setMaxWidth(chips.getWidth());
            chips.addView(chip);
        }
        root.findViewById(R.id.txtTasteEmpty).setVisibility(preferences.tags.isEmpty() ? View.VISIBLE : View.GONE);
        text(R.id.btnProfileSurvey).setText(preferences.tags.isEmpty()
                ? R.string.profile_start_survey : R.string.profile_survey);
        text(R.id.txtProfilePriority).setText(preferences.priority == null
                ? context.getString(R.string.profile_no_priority)
                : context.getString(R.string.profile_priority, preferences.priority.getKoreanLabel()));
    }

    void showVisitCount(int count) { setCount(R.id.statVisits, R.id.txtVisitCount, R.string.profile_visits, count); }
    void showFavoriteCount(int count) { setCount(R.id.statFavorites, R.id.txtFavoriteCount, R.string.profile_favorites, count); }
    void showBadgeCount(int count) { setCount(R.id.statBadges, R.id.txtBadgeCount, R.string.profile_badges, count); }

    void showCompleted(boolean failed) {
        text(R.id.txtProfileStatus).setText(R.string.profile_load_failed);
        root.findViewById(R.id.txtProfileStatus).setVisibility(failed ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.btnRetryProfile).setVisibility(failed ? View.VISIBLE : View.GONE);
        if (failed && !profileLoaded) text(R.id.txtProfilePriority).setText(R.string.profile_taste_failed);
    }

    private void setCount(int containerId, int textId, int labelId, Integer value) {
        String display = value == null ? context.getString(R.string.profile_unknown)
                : NumberFormat.getIntegerInstance().format(value);
        text(textId).setText(display);
        View container = root.findViewById(containerId);
        container.setContentDescription(context.getString(R.string.profile_stat_description,
                context.getString(labelId), value == null ? context.getString(R.string.profile_count_unknown) : display));
        ViewCompat.setScreenReaderFocusable(container, true);
    }

    private TextView text(int id) { return root.findViewById(id); }
}
