package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

public class TagAssigner {

    public static Tag[] assignTags(String storeNo, String name, String address) {
        List<Tag> tags = new ArrayList<>();

        String n = safeLower(name);
        String a = safeLower(address);

        // ===== 1) 키워드 기반 확정 태그 =====
        if (containsAny(n, "스터디", "공부", "study", "랩", "작업")) {
            addIfAbsent(tags, Tag.WORK);
            addIfAbsent(tags, Tag.QUIET);
            addIfAbsent(tags, Tag.WIFI);
            addIfAbsent(tags, Tag.OUTLET);
        }

        if (containsAny(n, "디저트", "케이크", "베이커리", "빵", "크로플", "마카롱")) {
            addIfAbsent(tags, Tag.DESSERT);
            addIfAbsent(tags, Tag.COZY);
        }

        if (containsAny(n, "로스터", "로스팅", "roaster", "roasting")) {
            addIfAbsent(tags, pickTasteTag(storeNo));
        }

        if (containsAny(n, "와인", "바", "펍", "칵테일")) {
            // 카페 데이터에 이런 게 섞일 수도 있어서 분위기쪽만 가볍게
            addIfAbsent(tags, Tag.LIVELY);
        }

        // ===== 2) 기본 태그(없으면 고정 랜덤) =====
        if (!containsTaste(tags)) addIfAbsent(tags, pickTasteTag(storeNo));
        if (!containsMood(tags)) addIfAbsent(tags, pickMoodTag(storeNo));
        if (!containsPurpose(tags)) addIfAbsent(tags, pickPurposeTag(storeNo));

        // ===== 3) 가끔 PHOTO 추가(고정 랜덤) =====
        if ((Math.abs(hash(storeNo + "_p")) % 5) == 0) addIfAbsent(tags, Tag.PHOTO);

        return tags.toArray(new Tag[0]);
    }

    // ---------- helpers ----------
    private static void addIfAbsent(List<Tag> list, Tag t) {
        if (!list.contains(t)) list.add(t);
    }

    private static boolean containsAny(String s, String... keys) {
        for (String k : keys) {
            if (s.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    private static boolean containsTaste(List<Tag> tags) {
        return tags.contains(Tag.ACIDIC) || tags.contains(Tag.NUTTY) || tags.contains(Tag.BITTER);
    }

    private static boolean containsMood(List<Tag> tags) {
        return tags.contains(Tag.QUIET) || tags.contains(Tag.COZY) || tags.contains(Tag.LIVELY);
    }

    private static boolean containsPurpose(List<Tag> tags) {
        return tags.contains(Tag.WORK) || tags.contains(Tag.DATE) || tags.contains(Tag.REST);
    }

    private static Tag pickTasteTag(String seed) {
        int r = Math.abs(hash(seed)) % 3;
        if (r == 0) return Tag.ACIDIC;
        if (r == 1) return Tag.NUTTY;
        return Tag.BITTER;
    }

    private static Tag pickMoodTag(String seed) {
        int r = Math.abs(hash(seed + "_m")) % 3;
        if (r == 0) return Tag.QUIET;
        if (r == 1) return Tag.COZY;
        return Tag.LIVELY;
    }

    private static Tag pickPurposeTag(String seed) {
        int r = Math.abs(hash(seed + "_u")) % 3;
        if (r == 0) return Tag.WORK;
        if (r == 1) return Tag.DATE;
        return Tag.REST;
    }

    private static int hash(String s) {
        return (s == null) ? 0 : s.hashCode();
    }
}
