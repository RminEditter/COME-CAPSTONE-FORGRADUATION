package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

public class TagAssigner {

    public static Tag[] assignTags(String storeNo, String name, String address) {
        List<Tag> tags = new ArrayList<>();

        String n = safeLower(name);
        String a = safeLower(address);

        // ===== 1) 키워드 기반 확정 태그 (새로운 Tag Enum 매핑) =====
        if (containsAny(n, "스터디", "공부", "study", "랩", "작업", "노트북", "콘센트")) {
            addIfAbsent(tags, Tag.WORK_FRIENDLY); // WORK 대신
            // QUIET, WIFI, OUTLET 등이 없으므로 의미가 가장 가까운 태그 부여
            addIfAbsent(tags, Tag.SOLO);
        }

        if (containsAny(n, "디저트", "케이크", "베이커리", "빵", "크로플", "마카롱")) {
            addIfAbsent(tags, Tag.DESSERT);
        }

        if (containsAny(n, "로스터", "로스팅", "roaster", "roasting", "핸드드립")) {
            addIfAbsent(tags, Tag.SPECIALTY_DRIP);
            addIfAbsent(tags, pickTasteTag(storeNo));
        }

        if (containsAny(n, "인테리어", "이쁜", "분위기", "감성")) {
            addIfAbsent(tags, Tag.INTERIOR_PRETTY);
            addIfAbsent(tags, Tag.HIP);
        }

        // ===== 2) 기본 태그(없으면 고정 랜덤 부여) =====
        // 맛 태그가 없으면 랜덤 부여
        if (!containsTaste(tags)) addIfAbsent(tags, pickTasteTag(storeNo));

        // 규모/성향 태그가 없으면 랜덤 부여
        if (!containsMoodOrSize(tags)) addIfAbsent(tags, pickMoodOrSizeTag(storeNo));

        // 동반자/목적 태그가 없으면 랜덤 부여
        if (!containsCompanion(tags)) addIfAbsent(tags, pickCompanionTag(storeNo));

        return tags.toArray(new Tag[0]);
    }

    // ---------- helpers ----------
    private static void addIfAbsent(List<Tag> list, Tag t) {
        if (t != null && !list.contains(t)) list.add(t);
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
        return tags.contains(Tag.BEAN_ACIDIC) || tags.contains(Tag.BEAN_NUTTY);
    }

    private static boolean containsMoodOrSize(List<Tag> tags) {
        return tags.contains(Tag.HIP) || tags.contains(Tag.INTERIOR_PRETTY) ||
                tags.contains(Tag.SMALL_CAFE) || tags.contains(Tag.LARGE_CAFE);
    }

    private static boolean containsCompanion(List<Tag> tags) {
        return tags.contains(Tag.SOLO) || tags.contains(Tag.COUPLE) ||
                tags.contains(Tag.FRIEND) || tags.contains(Tag.FAMILY) || tags.contains(Tag.COLLEAGUE);
    }

    private static Tag pickTasteTag(String seed) {
        int r = Math.abs(hash(seed)) % 2;
        if (r == 0) return Tag.BEAN_ACIDIC;
        return Tag.BEAN_NUTTY;
    }

    private static Tag pickMoodOrSizeTag(String seed) {
        int r = Math.abs(hash(seed + "_m")) % 4;
        if (r == 0) return Tag.HIP;
        if (r == 1) return Tag.INTERIOR_PRETTY;
        if (r == 2) return Tag.SMALL_CAFE;
        return Tag.LARGE_CAFE;
    }

    private static Tag pickCompanionTag(String seed) {
        int r = Math.abs(hash(seed + "_u")) % 5;
        if (r == 0) return Tag.SOLO;
        if (r == 1) return Tag.COUPLE;
        if (r == 2) return Tag.FRIEND;
        if (r == 3) return Tag.FAMILY;
        return Tag.COLLEAGUE;
    }

    private static int hash(String s) {
        return (s == null) ? 0 : s.hashCode();
    }
}