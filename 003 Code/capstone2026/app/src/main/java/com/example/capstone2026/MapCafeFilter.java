package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

final class MapCafeFilter {
    private MapCafeFilter() { }

    static List<CafeMapItem> select(List<CafeMapItem> cafes, String district, List<Tag> tags) {
        return select(cafes, district, tags, true);
    }

    static List<CafeMapItem> select(List<CafeMapItem> cafes, String district, List<Tag> tags, boolean matchAll) {
        List<CafeMapItem> result = new ArrayList<>();
        for (CafeMapItem cafe : cafes) {
            if (!"전체".equals(district) && (cafe.address == null || !cafe.address.contains(district))) continue;
            if (!tags.isEmpty()) {
                boolean matches = matchAll ? cafe.rawTags.containsAll(tags) : false;
                if (!matchAll) {
                    for (Tag tag : tags) {
                        if (cafe.rawTags.contains(tag)) { matches = true; break; }
                    }
                }
                if (!matches) continue;
            }
            result.add(cafe);
        }
        return result;
    }
}
