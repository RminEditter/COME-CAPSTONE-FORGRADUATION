package com.example.capstone2026;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class CafeDiscoveryPolicyTest {
    private Map<String, Object> cafe(String name, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        if (category != null) data.put("category", category);
        return data;
    }

    @Test public void excludesBonjukAndBranchNameVariantsEvenWhenWronglyTaggedCafe() {
        for (String name : new String[]{"본죽", "본죽 충남대점", "본죽대전궁동점", "본 죽 유성점",
                "본죽&비빔밥", "본죽앤비빔밥 대전점", "<b>본죽</b> 유성점", "본죽(궁동)",
                "한솥도시락 궁동점", "맥도날드 대전점", "도미노피자 궁동점"}) {
            assertFalse(name, CafeDiscoveryPolicy.isDiscoverable(cafe(name, "카페")));
        }
    }
    @Test public void retainsCoffeeFranchisesAndIndependentCafes() {
        for (String name : new String[]{"스타벅스 충남대점", "메가MGC커피 궁동점", "컴포즈커피",
                "빽다방", "카페 온유", "메가바이트 커피", "피자와 커피", "본죽이야기 카페"}) {
            assertTrue(name, CafeDiscoveryPolicy.isDiscoverable(cafe(name, null)));
        }
    }
    @Test public void restaurantCategoriesAreExcludedButCafeSubcategoriesAreRetained() {
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(cafe("한그릇", "음식점>한식>죽")));
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(cafe("열공", "교육>스터디카페")));
        assertTrue(CafeDiscoveryPolicy.isDiscoverable(cafe("소담", "음식점>카페,디저트>베이커리")));
        assertTrue(CafeDiscoveryPolicy.isDiscoverable(cafe("브런치 카페", "음식점>양식>카페")));
    }
    @Test public void supportsKakaoAndImportedFieldNamesAndExplicitExclusion() {
        Map<String, Object> data = cafe("한그릇", null);
        data.put("category_group_code", "FD6");
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(data));
        data.put("category_name", "음식점 > 카페");
        assertTrue(CafeDiscoveryPolicy.isDiscoverable(data));
        data.put("excludedFromDiscovery", true);
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(data));
        data = cafe("카페 온유", null);
        data.put("isCafe", false);
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(data));
    }
    @Test public void missingCategoriesRetainCafesButMissingNamesAreNotDiscoverable() {
        assertTrue(CafeDiscoveryPolicy.isDiscoverable(cafe("처음 만난 카페", null)));
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(cafe(null, "카페")));
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(cafe("  ", null)));
        assertFalse(CafeDiscoveryPolicy.isDiscoverable(null));
    }
}
