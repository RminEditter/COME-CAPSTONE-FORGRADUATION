package com.example.capstone2026;

import android.content.Context;
import android.view.View;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.*;

final class VisitTagSelector {
    private final Map<Tag, Chip> observed = new LinkedHashMap<>(), liked = new LinkedHashMap<>();
    VisitTagSelector(Context context, ChipGroup observations, ChipGroup favorites) {
        for (Tag tag : Tag.values()) {
            if (!VisitTagPreferences.selectable(tag)) continue;
            Chip first = chip(context, tag), second = chip(context, tag);
            observations.addView(first); favorites.addView(second);
            observed.put(tag, first); liked.put(tag, second);
            second.setVisibility(View.GONE);
            first.setOnCheckedChangeListener((button, checked) -> {
                if (!checked) second.setChecked(false);
                second.setVisibility(checked ? View.VISIBLE : View.GONE);
            });
        }
    }
    private Chip chip(Context context, Tag tag) {
        Chip chip = new Chip(context);
        chip.setId(View.generateViewId());
        chip.setSaveEnabled(false);
        chip.setText(tag.getKoreanLabel()); chip.setCheckable(true);
        return chip;
    }
    ArrayList<String> observations() { return selected(observed); }
    ArrayList<String> favorites() { return selected(liked); }
    private ArrayList<String> selected(Map<Tag, Chip> chips) {
        ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<Tag, Chip> entry : chips.entrySet()) if (entry.getValue().isChecked()) result.add(entry.getKey().name());
        return result;
    }
    void restore(List<String> observations, List<String> favorites) {
        for (Tag tag : observed.keySet()) {
            boolean selected = observations != null && observations.contains(tag.name());
            observed.get(tag).setChecked(selected);
            liked.get(tag).setChecked(selected && favorites != null && favorites.contains(tag.name()));
        }
    }
}
