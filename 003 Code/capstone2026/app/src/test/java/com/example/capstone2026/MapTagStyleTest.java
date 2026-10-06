package com.example.capstone2026;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class MapTagStyleTest {
    @Test public void representativeIsIndependentOfDatabaseOrder() {
        assertEquals(Arrays.asList(MapTagStyle.WORK), MapTagStyle.markerStyles(
                Arrays.asList(Tag.DESSERT, Tag.WORK_FRIENDLY), Collections.emptyList()));
    }
    @Test public void selectedTagOverridesRepresentative() {
        assertEquals(Arrays.asList(MapTagStyle.DESSERT), MapTagStyle.markerStyles(
                Arrays.asList(Tag.WORK_FRIENDLY, Tag.DESSERT), Arrays.asList(Tag.DESSERT)));
    }
    @Test public void multiSelectionHasAllSelectedColorsInStableOrder() {
        assertEquals(Arrays.asList(MapTagStyle.WORK, MapTagStyle.DESSERT), MapTagStyle.markerStyles(
                Arrays.asList(Tag.DESSERT, Tag.OUTLET_MANY, Tag.WORK_FRIENDLY),
                Arrays.asList(Tag.DESSERT, Tag.WORK_FRIENDLY)));
    }
    @Test public void unknownAndEmptyTagsHaveNeutralMarker() {
        assertEquals(Arrays.asList(MapTagStyle.OTHER), MapTagStyle.markerStyles(
                Arrays.asList(Tag.SOLO), Collections.emptyList()));
        assertEquals(Arrays.asList(MapTagStyle.OTHER), MapTagStyle.markerStyles(
                Collections.emptyList(), Collections.emptyList()));
    }
}
