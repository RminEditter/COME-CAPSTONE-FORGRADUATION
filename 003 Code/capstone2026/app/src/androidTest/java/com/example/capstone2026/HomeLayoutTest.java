package com.example.capstone2026;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.chip.ChipGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import static org.junit.Assert.*;

/** Real XML rendered without network calls or changing an existing Firebase account. */
@RunWith(AndroidJUnit4.class)
public class HomeLayoutTest {
    @Test public void recommendationAndFavoriteAndLoadingStates() {
        main(() -> {
            View root = inflate(360, 1f, false);
            HomeDashboardView binding = populate(root);
            assertEquals(3, ((ChipGroup) root.findViewById(R.id.homeCafeTags)).getChildCount());
            assertTrue(root.findViewById(R.id.btnHomeMap).isEnabled());
            binding.favorite(true, true);
            assertEquals("즐겨찾기 해제", root.findViewById(R.id.btnHomeFavorite).getContentDescription());
            measure(root, 360, 820);
            checkBounds(root);
            capture(root, "home-360-top");
            ScrollView scroll = root.findViewById(R.id.homeScroll);
            scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
            capture(root, "home-360-bottom");
            binding.loading();
            assertFalse(root.findViewById(R.id.btnHomeMap).isEnabled());
            assertFalse(root.findViewById(R.id.btnRecommendCafe).isEnabled());
            assertEquals(0, ((ChipGroup) root.findViewById(R.id.homeCafeTags)).getChildCount());
        });
    }

    @Test public void surveyAndErrorActionsRemainAvailable() {
        main(() -> {
            View root = inflate(360, 1f, false);
            HomeDashboardView binding = new HomeDashboardView(root);
            binding.message("나만의 카페를 찾아보세요", "취향을 알려주시면 어울리는 카페를 추천해드려요.", "내 취향 설정하기");
            assertTrue(root.findViewById(R.id.btnRecommendCafe).isEnabled());
            assertFalse(root.findViewById(R.id.btnHomeFavorite).isEnabled());
            binding.message("추천을 불러오지 못했어요", "연결 상태를 확인하고 다시 시도해주세요.", "다시 불러오기");
            assertEquals("다시 불러오기", ((TextView) root.findViewById(R.id.btnRecommendCafe)).getText().toString());
            measure(root, 360, 820);
            checkBounds(root);
            capture(root, "home-error");
        });
    }

    @Test public void narrowLargeFontAndNightLayoutDoNotClip() {
        main(() -> {
            for (boolean night : new boolean[]{false, true}) {
                View root = inflate(320, 1.6f, night);
                populate(root);
                measure(root, 320, 820);
                checkBounds(root);
                capture(root, night ? "home-large-night" : "home-large");
            }
        });
    }

    @Test public void clearingPhotoResetsImageAndCredits() {
        main(() -> {
            View root = inflate(360, 1f, false);
            HomePhotoLoader loader = new HomePhotoLoader(new android.app.Activity(), root.findViewById(R.id.imgHomeCafe),
                    root.findViewById(R.id.txtHomePhotoState), root.findViewById(R.id.homePhotoCredits));
            loader.clear();
            assertNull(loader.googleMapsUrl());
            assertEquals(View.GONE, root.findViewById(R.id.homePhotoCredits).getVisibility());
            assertEquals(View.VISIBLE, root.findViewById(R.id.txtHomePhotoState).getVisibility());
        });
    }

    private HomeDashboardView populate(View root) {
        HomeDashboardView binding = new HomeDashboardView(root);
        Recommender.CafeModel cafe = new Recommender.CafeModel("preview", "카페 온유", "대전 유성구 궁동",
                new Tag[]{Tag.WORK_FRIENDLY, Tag.OUTLET_MANY, Tag.DRINK_TASTY}, 36.36, 127.35);
        binding.recommendation(new Recommender.Recommendation(cafe, 85,
                "카공과 콘센트가 있는 공간을 선호하는 취향에 잘 맞아요.", 300), false, HomeSituation.STUDY);
        binding.favorite(false, true);
        ((TextView) root.findViewById(R.id.txtRecentCafe)).setText("카페 느린오후");
        ((TextView) root.findViewById(R.id.txtHomeRecentHint)).setText("눌러서 카페를 다시 만나보세요.");
        ((TextView) root.findViewById(R.id.txtTopCafe1)).setText("1. 카페 모퉁이  ★4.8");
        ((TextView) root.findViewById(R.id.txtTopCafe2)).setText("2. 카페 느린오후  ★4.7");
        ((TextView) root.findViewById(R.id.txtTopCafe3)).setText("3. 카페 온유  ★4.6");
        ((ChipGroup) root.findViewById(R.id.homeSituations)).check(R.id.chipHomeStudy);
        return binding;
    }

    private View inflate(int width, float scale, boolean night) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.densityDpi = 160; config.fontScale = scale; config.screenWidthDp = width;
        config.setLocale(Locale.KOREAN);
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return LayoutInflater.from(new ContextThemeWrapper(base.createConfigurationContext(config),
                R.style.Theme_Capstone2026)).inflate(R.layout.activity_main, null, false);
    }

    private void measure(View root, int width, int height) {
        for (int i = 0; i < 2; i++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
        }
    }

    private void checkBounds(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getLayout() != null) {
                assertTrue("Vertical clipping: " + text.getText(), text.getLayout().getHeight()
                        <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom() + 2);
                for (int i = 0; i < text.getLayout().getLineCount(); i++) {
                    assertTrue("Horizontal clipping: " + text.getText(), text.getLayout().getLineWidth(i)
                            <= text.getWidth() - text.getCompoundPaddingLeft() - text.getCompoundPaddingRight() + 2);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                assertTrue("Child exceeds width", child.getLeft() >= -1 && child.getRight() <= group.getWidth() + 1);
                checkBounds(child);
            }
        }
    }

    private void main(Runnable task) { InstrumentationRegistry.getInstrumentation().runOnMainSync(task); }
    private void capture(View root, String name) {
        File folder = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null), "home-previews");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try (FileOutputStream stream = new FileOutputStream(new File(folder, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (Exception error) { throw new AssertionError(error); }
        finally { bitmap.recycle(); }
    }
}
