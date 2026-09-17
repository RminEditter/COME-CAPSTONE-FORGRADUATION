package com.example.capstone2026;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MapCafeFilterTest {
    @Test public void allIncludesEveryMatchBeyondOld300MarkerLimit() {
        List<CafeMapItem> cafes = new ArrayList<>();
        for (int i = 0; i < 650; i++) {
            cafes.add(new CafeMapItem("" + i, "카페 " + i, i % 2 == 0 ? "대전 유성구" : "대전 서구", "",
                    i >= 300 ? Arrays.asList(Tag.WORK_FRIENDLY, Tag.OUTLET_MANY) : Collections.emptyList(), 36.3, 127.3));
        }
        List<CafeMapItem> all = MapCafeFilter.select(cafes, "전체", Collections.emptyList());
        List<CafeMapItem> work = MapCafeFilter.select(cafes, "전체", Arrays.asList(Tag.WORK_FRIENDLY));
        assertEquals(650, all.size());
        assertEquals(350, work.size());
        assertTrue(all.containsAll(work));
        assertEquals(all, MapCafeFilter.select(cafes, "전체", Collections.emptyList()));
        List<CafeMapItem> district = MapCafeFilter.select(cafes, "유성구", Arrays.asList(Tag.WORK_FRIENDLY, Tag.OUTLET_MANY));
        assertEquals(175, district.size());
        assertTrue(work.containsAll(district));
        assertTrue(MapCafeFilter.select(cafes, "전체", Arrays.asList(Tag.WORK_FRIENDLY, Tag.DESSERT)).isEmpty());
    }
}
