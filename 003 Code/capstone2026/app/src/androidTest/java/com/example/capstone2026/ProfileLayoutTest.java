package com.example.capstone2026;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.chip.ChipGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.*;

/** Exercises the real XML and binding without logging in or writing to Firebase. */
@RunWith(AndroidJUnit4.class)
public class ProfileLayoutTest {
    @Test public void populatedDashboardBindsSurveyAndStatistics() {
        onMain(() -> {
            View root = dashboard(360, 1f, false);
            assertEquals("12", text(root, R.id.txtVisitCount));
            assertEquals("5", text(root, R.id.txtFavoriteCount));
            assertEquals(3, ((ChipGroup) root.findViewById(R.id.chipProfileTaste)).getChildCount());
            assertTrue(text(root, R.id.txtProfilePriority).contains(Tag.WORK_FRIENDLY.getKoreanLabel()));
            assertEquals(View.GONE, root.findViewById(R.id.btnRetryProfile).getVisibility());
            measure(root, 360);
            checkBounds(root);
            screenshot(root, "dashboard-360-top");
            ScrollView scroll = (ScrollView) ((ViewGroup) root).getChildAt(0);
            scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
            screenshot(root, "dashboard-360-bottom");
        });
    }

    @Test public void missingDataAndFailedLoadsHaveDifferentStates() {
        onMain(() -> {
            View root = inflate(R.layout.activity_profile, 360, 1f, false);
            ProfileDashboardView binding = new ProfileDashboardView(root);
            binding.showLoading();
            binding.showFavoriteCount(0);
            binding.showCompleted(true);
            assertEquals("—", text(root, R.id.txtVisitCount));
            assertEquals("0", text(root, R.id.txtFavoriteCount));
            assertEquals(View.VISIBLE, root.findViewById(R.id.btnRetryProfile).getVisibility());
            measure(root, 360);
            screenshot(root, "dashboard-error");
            binding.showLoading();
            binding.showProfile(Collections.emptyMap());
            binding.showVisitCount(0);
            binding.showFavoriteCount(0);
            binding.showBadgeCount(0);
            binding.showCompleted(false);
            assertEquals("0", text(root, R.id.txtVisitCount));
            assertEquals(View.VISIBLE, root.findViewById(R.id.txtTasteEmpty).getVisibility());
            assertEquals(View.GONE, root.findViewById(R.id.btnRetryProfile).getVisibility());
            measure(root, 360);
            screenshot(root, "dashboard-empty");
        });
    }

    @Test public void reloadClearsThePreviousAccountsProfileAndTags() {
        onMain(() -> {
            View root = dashboard(360, 1f, false);
            ProfileDashboardView binding = new ProfileDashboardView(root);
            binding.showLoading();
            assertEquals("—", text(root, R.id.txtVisitCount));
            assertEquals(0, ((ChipGroup) root.findViewById(R.id.chipProfileTaste)).getChildCount());
            assertNotEquals("카페 탐험가 민지", text(root, R.id.txtProfileName));
            Map<String, Object> next = new HashMap<>();
            next.put("nickname", "두 번째 계정");
            next.put("user_tags", Collections.singletonList("SOLO"));
            binding.showProfile(next);
            assertEquals("두 번째 계정", text(root, R.id.txtProfileName));
            assertEquals(1, ((ChipGroup) root.findViewById(R.id.chipProfileTaste)).getChildCount());
        });
    }

    @Test public void narrowDashboardSupportsLargeTextAndNightMode() {
        onMain(() -> {
            for (boolean night : new boolean[]{false, true}) {
                View root = dashboard(320, 1.6f, night);
                measure(root, 320);
                checkBounds(root);
                screenshot(root, night ? "dashboard-320-night-large" : "dashboard-320-large");
            }
        });
    }

    @Test public void profileEditorFitsNormalAndLargeText() {
        onMain(() -> {
            for (float scale : new float[]{1f, 1.6f}) {
                View root = inflate(R.layout.activity_profile_edit, 360, scale, false);
                ((TextView) root.findViewById(R.id.editProfileNickname)).setText("카페 탐험가 민지");
                root.findViewById(R.id.txtProfileStatus).setVisibility(View.GONE);
                root.findViewById(R.id.btnSaveProfile).setEnabled(true);
                spinner(root, R.id.spinnerProfileGender, R.array.profile_gender_options);
                spinner(root, R.id.spinnerProfileAge, R.array.profile_age_options);
                measure(root, 360);
                checkBounds(root);
                screenshot(root, scale == 1f ? "profile-edit" : "profile-edit-large");
            }
        });
    }

    private View dashboard(int width, float scale, boolean night) {
        View root = inflate(R.layout.activity_profile, width, scale, night);
        ProfileDashboardView binding = new ProfileDashboardView(root);
        binding.showLoading();
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", "카페 탐험가 민지");
        data.put("user_tags", Arrays.asList("BEAN_NUTTY", "WORK_FRIENDLY", "DESSERT"));
        data.put("priority_tag", "WORK_FRIENDLY");
        binding.showProfile(data);
        binding.showVisitCount(12);
        binding.showFavoriteCount(5);
        binding.showBadgeCount(3);
        binding.showCompleted(false);
        return root;
    }

    private View inflate(int layout, int width, float scale, boolean night) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.densityDpi = 160;
        config.fontScale = scale;
        config.screenWidthDp = width;
        config.setLocale(Locale.KOREAN);
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        Context context = new ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Capstone2026);
        return LayoutInflater.from(context).inflate(layout, null, false);
    }

    private void spinner(View root, int id, int entries) {
        Spinner spinner = root.findViewById(id);
        spinner.setAdapter(ArrayAdapter.createFromResource(root.getContext(), entries, R.layout.item_profile_spinner));
    }

    private void measure(View root, int width) {
        for (int i = 0; i < 2; i++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(820, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, 820);
        }
    }

    private void checkBounds(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getLayout() != null) {
                if (text.getMaxLines() == Integer.MAX_VALUE) {
                    assertTrue("Text vertically clipped: " + text.getText(), text.getLayout().getHeight()
                            <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom() + 2);
                }
                for (int i = 0; i < text.getLayout().getLineCount(); i++) {
                    assertTrue("Text clipped: " + text.getText(), text.getLayout().getLineWidth(i)
                            <= text.getWidth() - text.getCompoundPaddingLeft() - text.getCompoundPaddingRight() + 2);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                assertTrue("Child outside parent: " + child.getClass().getSimpleName(),
                        child.getRight() <= group.getWidth() + 1 && child.getLeft() >= -1);
                checkBounds(child);
            }
        }
    }

    private String text(View root, int id) { return ((TextView) root.findViewById(id)).getText().toString(); }
    private void onMain(Runnable runnable) { InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable); }

    private void screenshot(View root, String name) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File directory = new File(context.getExternalFilesDir(null), "profile-previews");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            bitmap.recycle();
        }
    }
}
