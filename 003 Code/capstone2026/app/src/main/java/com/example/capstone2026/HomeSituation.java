package com.example.capstone2026;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A temporary exploration intent; never written back to the user's survey. */
public enum HomeSituation {
    STUDY("공부하기", Tag.WORK_FRIENDLY, Tag.OUTLET_MANY, Tag.LAPTOP_OK),
    DATE("데이트", Tag.COUPLE, Tag.INTERIOR_PRETTY, Tag.DESSERT),
    SOLO("혼자 쉬기", Tag.SOLO, Tag.DRINK_TASTY, Tag.SMALL_CAFE);

    public final String label;
    public final Tag priority;
    private final List<Tag> tags;

    HomeSituation(String label, Tag... tags) {
        this.label = label;
        this.priority = tags[0];
        this.tags = Arrays.asList(tags);
    }

    public List<Tag> withSurvey(List<Tag> survey) {
        List<Tag> combined = new ArrayList<>(survey);
        combined.removeAll(Arrays.asList(Tag.SOLO, Tag.COUPLE, Tag.FRIEND, Tag.FAMILY, Tag.COLLEAGUE));
        for (Tag tag : tags) if (!combined.contains(tag)) combined.add(tag);
        return combined;
    }

    public static HomeSituation parse(String value) {
        try { return value == null ? null : valueOf(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
