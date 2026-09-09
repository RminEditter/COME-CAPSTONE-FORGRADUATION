package com.example.capstone2026;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SurveyPreferencesTest {
    @Test public void newSurveyOverridesLegacyAnswersAndPreservesPriority() {
        Map<String, Object> data = new HashMap<>();
        data.put("bean_tag", "BEAN_NUTTY");
        data.put("user_tags", Arrays.asList(" bean_acidic ", "DESSERT", "DESSERT", "unknown", null));
        data.put("priority_tag", "distance");
        SurveyPreferences preferences = SurveyPreferences.from(data);
        assertEquals(Arrays.asList(Tag.BEAN_ACIDIC, Tag.DESSERT), preferences.tags);
        assertEquals(Tag.DISTANCE, preferences.priority);
    }

    @Test public void existingLegacyAccountStillHasPreferences() {
        Map<String, Object> data = new HashMap<>();
        data.put("bean_tag", "BEAN_NUTTY");
        data.put("style_tag", "WORK_FRIENDLY");
        data.put("companion_tag", "SOLO");
        assertEquals(Arrays.asList(Tag.BEAN_NUTTY, Tag.WORK_FRIENDLY, Tag.SOLO),
                SurveyPreferences.from(data).tags);
    }

    @Test public void emptyNewSurveyDoesNotResurrectOldPreferences() {
        Map<String, Object> data = new HashMap<>();
        data.put("user_tags", Collections.emptyList());
        data.put("bean_tag", "BEAN_NUTTY");
        assertTrue(SurveyPreferences.from(data).tags.isEmpty());
    }

    @Test public void missingOrMalformedProfileDoesNotCrash() {
        assertTrue(SurveyPreferences.from(null).tags.isEmpty());
        Map<String, Object> data = new HashMap<>();
        data.put("user_tags", 42);
        data.put("priority_tag", "removed_tag");
        SurveyPreferences preferences = SurveyPreferences.from(data);
        assertTrue(preferences.tags.isEmpty());
        assertNull(preferences.priority);
    }
}
