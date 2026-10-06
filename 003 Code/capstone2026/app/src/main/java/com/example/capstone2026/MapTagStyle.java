package com.example.capstone2026;

import java.util.ArrayList;
import java.util.List;

/** Fixed order keeps the representative tag stable regardless of database ordering. */
enum MapTagStyle {
    WORK(Tag.WORK_FRIENDLY, 0xFF2463A6, "공", "카공"),
    OUTLET(Tag.OUTLET_MANY, 0xFFB45309, "전", "콘센트 많음"),
    LAPTOP(Tag.LAPTOP_OK, 0xFF7546AB, "노", "노트북 가능"),
    DESSERT(Tag.DESSERT, 0xFFBE356D, "디", "디저트"),
    NUTTY(Tag.BEAN_NUTTY, 0xFF79513A, "콩", "고소한 원두"),
    OTHER(null, 0xFF59636E, "카", "기타 카페");

    final Tag tag;
    final int color;
    final String symbol, label;
    MapTagStyle(Tag tag, int color, String symbol, String label) {
        this.tag = tag; this.color = color; this.symbol = symbol; this.label = label;
    }
    static MapTagStyle forTag(Tag tag) {
        for (MapTagStyle style : values()) if (style.tag == tag) return style;
        return OTHER;
    }
    static List<MapTagStyle> markerStyles(List<Tag> cafeTags, List<Tag> selected) {
        List<MapTagStyle> result = new ArrayList<>();
        for (MapTagStyle style : values()) {
            if (style.tag != null && cafeTags.contains(style.tag)
                    && (selected.isEmpty() || selected.contains(style.tag))) {
                result.add(style);
                if (selected.isEmpty()) break;
            }
        }
        if (result.isEmpty()) result.add(OTHER);
        return result;
    }
}
