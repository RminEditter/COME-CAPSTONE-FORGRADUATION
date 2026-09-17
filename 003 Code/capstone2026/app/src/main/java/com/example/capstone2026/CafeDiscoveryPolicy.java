package com.example.capstone2026;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Conservative discovery filter. Never deletes cafes or users' historical records. */
public final class CafeDiscoveryPolicy {
    private CafeDiscoveryPolicy() { }
    private static final Pattern HTML = Pattern.compile("<[^>]*>");
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{P}\\p{S}]+");

    private static final String[] NON_CAFE_BRANDS = {
            "본죽&비빔밥", "본죽앤비빔밥", "본죽", "본도시락", "본설렁탕", "한솥도시락", "죽이야기",
            "롯데리아", "맥도날드", "버거킹", "맘스터치", "노브랜드버거", "피자헛", "도미노피자",
            "미스터피자", "피자스쿨", "신전떡볶이", "동대문엽기떡볶이", "홍콩반점0410", "홍콩반점"
    };
    private static final Set<String> NON_CAFE_CATEGORIES = new HashSet<>(Arrays.asList(
            "한식", "중식", "일식", "양식", "분식", "죽", "도시락", "치킨", "피자", "햄버거",
            "패스트푸드", "육류,고기요리", "국밥", "설렁탕", "떡볶이", "술집", "주점", "스터디카페"));
    private static final String[] NORMALIZED_BRANDS = new String[NON_CAFE_BRANDS.length];
    private static final String[] COMPACT_BRANDS = new String[NON_CAFE_BRANDS.length];
    static {
        for (int i = 0; i < NON_CAFE_BRANDS.length; i++) {
            NORMALIZED_BRANDS[i] = normalize(NON_CAFE_BRANDS[i]);
            COMPACT_BRANDS[i] = SEPARATORS.matcher(NORMALIZED_BRANDS[i]).replaceAll("");
        }
    }

    public static boolean isDiscoverable(Map<String, Object> data) {
        if (data == null || Boolean.TRUE.equals(data.get("excludedFromDiscovery"))
                || Boolean.FALSE.equals(data.get("isCafe"))) return false;
        String name = string(data.get("name"));
        if (!isDiscoverableName(name)) return false;
        String category = string(data.get("category")) + ">" + string(data.get("category_name"))
                + ">" + string(data.get("categoryName"));
        String normalizedCategory = SPACE.matcher(normalize(category)).replaceAll("");
        if (normalizedCategory.contains("스터디카페")) return false;
        // Explicit cafe subcategories take precedence over a broad parent such as 음식점.
        if (normalizedCategory.contains("카페") || normalizedCategory.contains("커피")
                || normalizedCategory.contains("디저트") || normalizedCategory.contains("베이커리")) return true;
        String group = string(data.get("category_group_code"));
        if (group.isEmpty()) group = string(data.get("categoryGroupCode"));
        if ("CE7".equalsIgnoreCase(group.trim())) return true;
        if ("FD6".equalsIgnoreCase(group.trim())) return false;
        for (String part : normalizedCategory.split(">")) {
            if (NON_CAFE_CATEGORIES.contains(part)) return false;
        }
        // Missing metadata is not evidence of a non-cafe; retain independent cafes.
        return true;
    }

    public static boolean isDiscoverableName(String name) {
        String normalized = normalize(name);
        if (normalized.isEmpty()) return false;
        String compact = SEPARATORS.matcher(normalized).replaceAll("");
        for (int i = 0; i < NORMALIZED_BRANDS.length; i++) {
            String brand = NORMALIZED_BRANDS[i];
            String key = COMPACT_BRANDS[i];
            if (compact.equals(key)) return false;
            if (compact.startsWith(key) && compact.endsWith("점")) return false;
            if (normalized.startsWith(brand)) {
                String tail = normalized.substring(brand.length());
                if (!tail.isEmpty() && (Character.isWhitespace(tail.charAt(0))
                        || "([（［".indexOf(tail.charAt(0)) >= 0)) return false;
            }
        }
        return true;
    }

    private static String string(Object value) { return value instanceof String ? (String) value : ""; }
    private static String normalize(String value) {
        return value == null ? "" : HTML.matcher(Normalizer.normalize(value, Normalizer.Form.NFKC))
                .replaceAll("").replace("&amp;", "&").trim().toLowerCase(Locale.ROOT);
    }
}
