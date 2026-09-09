package com.example.capstone2026;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Canonical survey fields, with read compatibility for older user documents. */
public final class SurveyPreferences {
    public final List<Tag> tags;
    public final Tag priority;

    private SurveyPreferences(List<Tag> tags, Tag priority) {
        this.tags = tags;
        this.priority = priority;
    }

    public static SurveyPreferences from(Map<String, Object> data) {
        Set<Tag> tags = new LinkedHashSet<>();
        Tag priority = null;
        if (data != null) {
            // An explicitly empty new survey must not resurrect legacy answers.
            if (data.containsKey("user_tags")) {
                Object raw = data.get("user_tags");
                if (raw instanceof Iterable<?>) {
                    for (Object value : (Iterable<?>) raw) add(tags, value);
                }
            } else {
                for (String field : new String[]{"bean_tag", "style_tag", "size_tag",
                        "companion_tag", "dessert_tag", "specialty_tag"}) {
                    add(tags, data.get(field));
                }
            }
            priority = parseTag(data.get("priority_tag"));
        }
        return new SurveyPreferences(new ArrayList<>(tags), priority);
    }

    public static Tag parseTag(Object value) {
        if (!(value instanceof String)) return null;
        try {
            return Tag.valueOf(((String) value).trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void add(Set<Tag> tags, Object raw) {
        Tag tag = parseTag(raw);
        if (tag != null) tags.add(tag);
    }
}
