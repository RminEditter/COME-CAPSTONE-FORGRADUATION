package com.example.capstone2026;

import java.util.HashMap;
import java.util.Map;

public enum Tag {

    // 1. 원두 취향
    BEAN_NUTTY,        // 고소
    BEAN_ACIDIC,       // 산미

    // 2. 카페 성향
    INTERIOR_PRETTY,   // 인테리어 예쁨
    DRINK_TASTY,       // 음료 맛집
    HIP,               // 힙한 감성
    WORK_FRIENDLY,     // 카공 가능

    // 3. 디저트
    DESSERT,

    // 4. 스페셜티
    SPECIALTY_DRIP,

    // 5. 카페 규모
    SMALL_CAFE,
    LARGE_CAFE,

    // 6. 동반자
    SOLO,
    COUPLE,
    FRIEND,
    FAMILY,
    COLLEAGUE;

    /* =========================
       한글 라벨 매핑
    ========================== */

    private static final Map<Tag, String> KO_LABEL = new HashMap<>();

    static {
        KO_LABEL.put(Tag.BEAN_NUTTY, "고소");
        KO_LABEL.put(Tag.BEAN_ACIDIC, "산미");

        KO_LABEL.put(Tag.INTERIOR_PRETTY, "인테리어 예쁨");
        KO_LABEL.put(Tag.DRINK_TASTY, "음료 맛집");
        KO_LABEL.put(Tag.HIP, "힙한 감성");
        KO_LABEL.put(Tag.WORK_FRIENDLY, "카공 가능");

        KO_LABEL.put(Tag.DESSERT, "디저트");
        KO_LABEL.put(Tag.SPECIALTY_DRIP, "스페셜티 드립");

        KO_LABEL.put(Tag.SMALL_CAFE, "소형 카페");
        KO_LABEL.put(Tag.LARGE_CAFE, "대형 카페");

        KO_LABEL.put(Tag.SOLO, "혼자");
        KO_LABEL.put(Tag.COUPLE, "커플");
        KO_LABEL.put(Tag.FRIEND, "친구");
        KO_LABEL.put(Tag.FAMILY, "가족");
        KO_LABEL.put(Tag.COLLEAGUE, "직장동료");
    }

    public String getKoreanLabel() {
        return KO_LABEL.get(this);
    }
}