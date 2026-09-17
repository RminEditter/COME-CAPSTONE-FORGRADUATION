package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

final class MapCafeFilter {
    private MapCafeFilter() { }

    static List<CafeMapItem> select(List<CafeMapItem> cafes, String district, List<Tag> tags) {
        List<CafeMapItem> result = new ArrayList<>();
        for (CafeMapItem cafe : cafes) {
            if (!"전체".equals(district) && (cafe.address == null || !cafe.address.contains(district))) continue;
            if (!cafe.rawTags.containsAll(tags)) continue;
            result.add(cafe);
        }
        return result;
    }
}
