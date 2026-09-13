package com.example.capstone2026;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class HomeSituationTest {
    @Test public void temporarySituationPreservesSavedSurvey() {
        List<Tag> saved = Arrays.asList(Tag.COUPLE, Tag.BEAN_NUTTY, Tag.DESSERT);
        List<Tag> active = HomeSituation.SOLO.withSurvey(saved);
        assertTrue(active.contains(Tag.SOLO));
        assertFalse(active.contains(Tag.COUPLE));
        assertTrue(active.contains(Tag.BEAN_NUTTY));
        assertEquals(Arrays.asList(Tag.COUPLE, Tag.BEAN_NUTTY, Tag.DESSERT), saved);
    }
    @Test public void studyDoesNotDuplicateTagsAndInvalidIntentsAreIgnored() {
        List<Tag> result = HomeSituation.STUDY.withSurvey(Arrays.asList(Tag.WORK_FRIENDLY));
        assertEquals(3, result.size());
        assertEquals(Tag.WORK_FRIENDLY, HomeSituation.STUDY.priority);
        assertNull(HomeSituation.parse("BAD_VALUE"));
        assertNull(HomeSituation.parse(null));
        assertEquals(HomeSituation.DATE, HomeSituation.parse("DATE"));
    }
}
