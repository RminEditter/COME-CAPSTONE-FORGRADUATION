package com.example.capstone2026;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import com.google.android.material.chip.Chip;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import java.util.*;

final class MapTagIcons {
    private final Context context;
    private final Map<String, Icon> cache = new HashMap<>();
    MapTagIcons(Context context) { this.context = context; }

    static void styleChip(Chip chip, MapTagStyle style) {
        int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{}};
        chip.setChipBackgroundColor(new ColorStateList(states, new int[]{style.color, 0xFFFFFFFF}));
        chip.setTextColor(new ColorStateList(states, new int[]{0xFFFFFFFF, style.color}));
        chip.setChipStrokeColor(ColorStateList.valueOf(style.color));
        chip.setChipStrokeWidth(chip.getResources().getDisplayMetrics().density);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(style.color);
        chip.setChipIcon(dot);
        chip.setChipIconTint(new ColorStateList(states, new int[]{0xFFFFFFFF, style.color}));
        chip.setChipIconSize(10 * chip.getResources().getDisplayMetrics().density);
        chip.setChipIconVisible(true);
        chip.setCheckedIconVisible(false);
    }

    Icon icon(List<Tag> tags, List<Tag> selected) {
        List<MapTagStyle> styles = MapTagStyle.markerStyles(tags, selected);
        String key = styles.toString();
        Icon cached = cache.get(key);
        if (cached != null) return cached;
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.max(40, styles.size() * 14 + 8);
        Bitmap bitmap = Bitmap.createBitmap(Math.round(width * density), Math.round(54 * density), Bitmap.Config.ARGB_8888);
        bitmap.setDensity(context.getResources().getDisplayMetrics().densityDpi);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(density, density);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = width / 2f;
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(1, 1, width - 1, 43, 12, 12, paint);
        paint.setColor(styles.get(0).color);
        canvas.drawRoundRect(3, 3, width - 3, 41, 10, 10, paint);
        Path tip = new Path(); tip.moveTo(cx - 7, 39); tip.lineTo(cx, 53); tip.lineTo(cx + 7, 39); tip.close();
        canvas.drawPath(tip, paint);
        paint.setColor(Color.WHITE); paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(15);
        canvas.drawText(styles.get(0).symbol, cx, styles.size() > 1 ? 21 : 28, paint);
        if (styles.size() > 1) {
            float start = cx - (styles.size() - 1) * 7;
            for (int i = 0; i < styles.size(); i++) {
                float x = start + i * 14;
                paint.setColor(Color.WHITE); canvas.drawCircle(x, 32, 6, paint);
                paint.setColor(styles.get(i).color); canvas.drawCircle(x, 32, 5, paint);
                paint.setColor(Color.WHITE); paint.setTextSize(7);
                canvas.drawText(styles.get(i).symbol, x, 34.5f, paint);
            }
        }
        Icon icon = IconFactory.getInstance(context).fromBitmap(bitmap);
        cache.put(key, icon);
        return icon;
    }
}
