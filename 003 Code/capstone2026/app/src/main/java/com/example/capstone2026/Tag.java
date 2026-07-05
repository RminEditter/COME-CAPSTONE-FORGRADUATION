package com.example.capstone2026;

import java.util.HashMap;
import java.util.Map;

public enum Tag {

    // =========================
    // 1. 원두 취향
    // =========================
    BEAN_NUTTY,        // 고소한 원두
    BEAN_ACIDIC,       // 산미 있는 원두

    // =========================
    // 2. 카페 성향
    // =========================
    INTERIOR_PRETTY,   // 인테리어
    DRINK_TASTY,       // 커피 맛
    HIP,               // 힙한 감성
    WORK_FRIENDLY,     // 카공 가능

    // =========================
    // 3. 디저트
    // =========================
    DESSERT,

    // =========================
    // 4. 스페셜티
    // =========================
    SPECIALTY_DRIP,

    // =========================
    // 5. 카페 규모
    // =========================
    SMALL_CAFE,
    LARGE_CAFE,

    // =========================
    // 6. 동반자
    // =========================
    SOLO,
    COUPLE,
    FRIEND,
    FAMILY,
    COLLEAGUE,

    // =========================
    // 7. 추천 우선순위(가중치)
    // =========================
    MOOD,
    DISTANCE;

    /* =========================
       한글 라벨
    ========================= */

    private static final Map<Tag, String> KO_LABEL = new HashMap<>();

    static {

        // 원두
        KO_LABEL.put(BEAN_NUTTY, "고소한 원두");
        KO_LABEL.put(BEAN_ACIDIC, "산미 있는 원두");

        // 카페 성향
        KO_LABEL.put(INTERIOR_PRETTY, "인테리어");
        KO_LABEL.put(DRINK_TASTY, "커피 맛");
        KO_LABEL.put(HIP, "힙한 감성");
        KO_LABEL.put(WORK_FRIENDLY, "카공");

        // 디저트
        KO_LABEL.put(DESSERT, "디저트");

        // 스페셜티
        KO_LABEL.put(SPECIALTY_DRIP, "스페셜티");

        // 규모
        KO_LABEL.put(SMALL_CAFE, "소형 카페");
        KO_LABEL.put(LARGE_CAFE, "대형 카페");

        // 방문 인원
        KO_LABEL.put(SOLO, "혼자");
        KO_LABEL.put(COUPLE, "연인");
        KO_LABEL.put(FRIEND, "친구");
        KO_LABEL.put(FAMILY, "가족");
        KO_LABEL.put(COLLEAGUE, "직장동료");

        // 추천 우선순위
        KO_LABEL.put(MOOD, "분위기");
        KO_LABEL.put(DISTANCE, "거리");
    }

    public String getKoreanLabel() {
        return KO_LABEL.getOrDefault(this, name());
    }
}