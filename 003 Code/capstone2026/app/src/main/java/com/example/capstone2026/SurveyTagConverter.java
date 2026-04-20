package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;


public class SurveyTagConverter {

    public static List<Tag> convertSurveyToTags(
            String bean,        // 1. 원두 (고소 / 산미 / 둘다)
            String style,       // 2. 성향 (인테리어가 이쁨 / 음료가 맛있음 / 힙함 / 카공)
            boolean dessert,    // 3. 디저트 (true: 네 / false: 아니요)
            boolean specialty,  // 4. 스페셜티 (true: 네 / false: 아니요)
            String size,        // 5. 규모 (소형 / 대형)
            String companion    // 6. 동반자 (혼자 / 커플 / 친구 / 가족 / 직장동료)
    ) {
        List<Tag> tags = new ArrayList<>();

        /* =============================================
           1. 원두 취향 (복수 선택 처리)
        ============================================= */
        if ("고소".equals(bean)) {
            tags.add(Tag.BEAN_NUTTY);
        } else if ("산미".equals(bean)) {
            tags.add(Tag.BEAN_ACIDIC);
        } else if ("둘다".equals(bean)) {
            // '둘다' 선택 시 시스템상 두 가지 속성을 모두 가진 카페를 찾도록 함
            tags.add(Tag.BEAN_NUTTY);
            tags.add(Tag.BEAN_ACIDIC);
        }

        /* =============================================
           2. 선호하는 카페 스타일
        ============================================= */
        if ("인테리어가 이쁨".equals(style)) {
            tags.add(Tag.INTERIOR_PRETTY);
        } else if ("음료가 맛있음".equals(style)) {
            tags.add(Tag.DRINK_TASTY);
        } else if ("힙함".equals(style)) {
            tags.add(Tag.HIP);
        } else if ("카공".equals(style)) {
            tags.add(Tag.WORK_FRIENDLY);
        }

        /* =============================================
           3. 디저트 여부 (Switch/Checkbox 기반)
        ============================================= */
        if (dessert) {
            tags.add(Tag.DESSERT);
        }

        /* =============================================
           4. 스페셜티 드립커피 선호도
        ============================================= */
        if (specialty) {
            tags.add(Tag.SPECIALTY_DRIP);
        }

        /* =============================================
           5. 카페 규모 선호도
        ============================================= */
        if ("소형".equals(size)) {
            tags.add(Tag.SMALL_CAFE);
        } else if ("대형".equals(size)) {
            tags.add(Tag.LARGE_CAFE);
        }

        /* =============================================
           6. 누구와 이용하시나요?
        ============================================= */
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