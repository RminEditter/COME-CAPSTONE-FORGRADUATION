package com.example.capstone2026;

import java.util.HashMap;
import java.util.Map;

public enum Tag {
    // Taste
    ACIDIC, NUTTY, BITTER,
    // Mood
    QUIET, COZY, LIVELY,
    // Purpose
    WORK, DATE, REST,
    // Menu
    DESSERT, NONCOFFEE,
    // Convenience / vibe
    OUTLET, WIFI, PHOTO;

    private static final Map<Tag, String> KO_LABEL = new HashMap<>();

    static {
        KO_LABEL.put(ACIDIC, "산미");
        KO_LABEL.put(NUTTY, "고소");
        KO_LABEL.put(BITTER, "쌉싸름");

        KO_LABEL.put(QUIET, "조용함");
        KO_LABEL.put(COZY, "아늑함");
        KO_LABEL.put(LIVELY, "활기참");

        KO_LABEL.put(WORK, "작업");
        KO_LABEL.put(DATE, "데이트");
        KO_LABEL.put(REST, "휴식");

        KO_LABEL.put(DESSERT, "디저트");
        KO_LABEL.put(NONCOFFEE, "논커피");

        KO_LABEL.put(OUTLET, "콘센트");
        KO_LABEL.put(WIFI, "와이파이");
        KO_LABEL.put(PHOTO, "사진맛집");
    }

    public String labelKo() {
        return KO_LABEL.getOrDefault(this, name());
    }
}
