package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

public class SurveyTagConverter {

    public static List<Tag> convertSurveyToTags(
            String bean,
            String style,
            boolean dessert,
            boolean specialty,
            String size,
            String companion
    ) {

        List<Tag> tags = new ArrayList<>();

        /* =========================
           1️⃣ 원두 취향
        ========================== */
        if ("고소".equals(bean)) {
            tags.add(Tag.BEAN_NUTTY);

        } else if ("산미".equals(bean)) {
            tags.add(Tag.BEAN_ACIDIC);

        } else if ("둘다".equals(bean)) {
            tags.add(Tag.BEAN_NUTTY);
            tags.add(Tag.BEAN_ACIDIC);
        }

        /* =========================
           2️⃣ 카페 성향
        ========================== */
        if ("인테리어가 이쁨".equals(style)) {
            tags.add(Tag.INTERIOR_PRETTY);

        } else if ("음료가 맛있음".equals(style)) {
            tags.add(Tag.DRINK_TASTY);

        } else if ("힙함".equals(style)) {
            tags.add(Tag.HIP);

        } else if ("카공".equals(style)) {
            tags.add(Tag.WORK_FRIENDLY);
        }

        /* =========================
           3️⃣ 디저트
        ========================== */
        if (dessert) {
            tags.add(Tag.DESSERT);
        }

        /* =========================
           4️⃣ 스페셜티 드립
        ========================== */
        if (specialty) {
            tags.add(Tag.SPECIALTY_DRIP);
        }

        /* =========================
           5️⃣ 카페 규모
        ========================== */
        if ("소형".equals(size)) {
            tags.add(Tag.SMALL_CAFE);

        } else if ("대형".equals(size)) {
            tags.add(Tag.LARGE_CAFE);
        }

        /* =========================
           6️⃣ 동반자
        ========================== */
        if ("혼자".equals(companion)) {
            tags.add(Tag.SOLO);

        } else if ("커플".equals(companion)) {
            tags.add(Tag.COUPLE);

        } else if ("친구".equals(companion)) {
            tags.add(Tag.FRIEND);

        } else if ("가족".equals(companion)) {
            tags.add(Tag.FAMILY);

        } else if ("직장동료".equals(companion)) {
            tags.add(Tag.COLLEAGUE);
        }

        return tags;
    }
}