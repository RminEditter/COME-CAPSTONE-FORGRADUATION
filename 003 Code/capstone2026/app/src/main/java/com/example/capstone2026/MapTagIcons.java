package com.example.capstone2026;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import com.google.android.material.chip.Chip;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Vector-like Canvas icons: no photo hosting, image assets or paid Firebase features required. */
final class MapTagIcons {
    private final Context context;
    private final Map<String, Icon> cache = new HashMap<>();
    MapTagIcons(Context context) { this.context = context; }

    static void styleChip(Chip chip, MapTagStyle style) {
        int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{}};
        chip.setChipBackgroundColor(new ColorStateList(states, new int[]{style.color, Color.WHITE}));
        chip.setTextColor(new ColorStateList(states, new int[]{Color.WHITE, style.color}));
        chip.setChipStrokeColor(ColorStateList.valueOf(style.color));
        chip.setChipStrokeWidth(chip.getResources().getDisplayMetrics().density);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(style.color);
        chip.setChipIcon(dot);
        chip.setChipIconTint(new ColorStateList(states, new int[]{Color.WHITE, style.color}));
        chip.setChipIconSize(10 * chip.getResources().getDisplayMetrics().density);
        chip.setChipIconVisible(true);
        chip.setCheckedIconVisible(false);
    }

    Icon icon(List<Tag> tags, List<Tag> selected) {
        List<MapTagStyle> styles = MapTagStyle.markerStyles(tags, selected);
        MapTagStyle primary = styles.get(0);
        String key = primary.name();
        Icon cached = cache.get(key);
        if (cached != null) return cached;

        float density = context.getResources().getDisplayMetrics().density;
        int width = 34, height = 45;
        Bitmap bitmap = Bitmap.createBitmap(Math.round(width * density), Math.round(height * density), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(density, density);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = width / 2f;
        p.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(0, 0, 34, 34), 12, 12, p);
        p.setColor(primary.color);
        canvas.drawRoundRect(new RectF(2, 2, 32, 32), 10, 10, p);
        Path tip = new Path();
        tip.moveTo(10, 29); tip.lineTo(cx, 44); tip.lineTo(24, 29); tip.close();
        canvas.drawPath(tip, p);
        canvas.save();
        canvas.translate(cx, 16);
        drawSymbol(canvas, p, primary.symbol);
        canvas.restore();
        Icon result = IconFactory.getInstance(context).fromBitmap(bitmap);
        cache.put(key, result);
        return result;
    }

    private static void line(Canvas c, Paint p, float... xy) {
        Path path = new Path(); path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i+1]);
        c.drawPath(path, p);
    }

    private static void drawSymbol(Canvas c, Paint p, String symbol) {
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.9f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        switch (symbol) {
            case "laptop":
                c.drawRoundRect(new RectF(-9,-8,9,5), 1.4f,1.4f,p);
                line(c,p,-11,8,11,8,9,5,-9,5,-11,8);
                break;
            case "computer":
                c.drawRoundRect(new RectF(-8,-9,8,4),1.5f,1.5f,p);
                line(c,p,0,4,0,8,-5,8,5,8);
                break;
            case "plug":
                line(c,p,-4,-10,-4,-5,4,-5,4,-10);
                c.drawRoundRect(new RectF(-7,-5,7,4),2,2,p);
                line(c,p,0,4,0,10);
                break;
            case "cake":
                line(c,p,-9,8,9,8,9,-1,-9,-1,-9,8);
                line(c,p,-9,3,-5,1,-1,3,3,1,7,3,9,2);
                line(c,p,0,-2,0,-7);
                c.drawOval(new RectF(-1.4f,-10,1.4f,-7),p);
                break;
            case "coffee":
            case "cup":
                line(c,p,-8,-4,-8,6,6,6,6,-4,-8,-4);
                c.drawArc(new RectF(4,-4,12,4),-90,180,false,p);
                line(c,p,-10,9,8,9);
                if (symbol.equals("coffee")) {
                    line(c,p,-4,-7,-5,-10);
                    line(c,p,1,-7,2,-10);
                }
                break;
            default:
                c.drawCircle(0,0,6,p);
        }
        p.setStyle(Paint.Style.FILL);
    }
}
