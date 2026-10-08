package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.XmlResourceParser;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class SettingsSectionsTest {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    // Options added by builds that are not in preferences.xml yet
    private static final Set<String> OPTIONAL_KEYS = new HashSet<>();
    static {
        Collections.addAll(OPTIONAL_KEYS, "list_passthrough_format", "list_passthrough_buffer");
    }

    private Context context() {
        return new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme);
    }

    // Key of every option in preferences.xml, mapped to the top-level category it sits in
    private Map<String, String> categoryOfEachKey(Context context) throws Exception {
        Map<String, String> result = new HashMap<>();
        Deque<String> categories = new ArrayDeque<>();
        XmlResourceParser parser = context.getResources().getXml(R.xml.preferences);
        int depth = 0;
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                depth++;
                String key = parser.getAttributeValue(ANDROID_NS, "key");
                if (depth == 2 && parser.getName().endsWith("PreferenceCategory")) {
                    categories.push(key == null ? "" : key);
                } else if (key != null && !categories.isEmpty()) {
                    result.put(key, categories.peek());
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (depth == 2 && parser.getName().endsWith("PreferenceCategory")) {
                    categories.pop();
                }
                depth--;
            }
        }
        return result;
    }

    @Test
    public void everySectionKeyIsAnOptionOfItsCategory() throws Exception {
        Map<String, String> categoryOf = categoryOfEachKey(context());
        Set<String> seen = new HashSet<>();
        for (SettingsSections.Category category : SettingsSections.CATEGORIES) {
            for (SettingsSections.Section section : category.sections) {
                for (String key : section.keys) {
                    assertTrue("listed twice: " + key, seen.add(key));
                    if (OPTIONAL_KEYS.contains(key) && !categoryOf.containsKey(key)) {
                        continue;
                    }
                    // list_languages lives under About & help and moves to Appearance at runtime
                    if (key.equals("list_languages")) {
                        continue;
                    }
                    assertEquals("category of " + key, category.key, categoryOf.get(key));
                }
            }
        }
    }

    @Test
    public void optionsAreOrderedUnderTheirSectionHeader() {
        Context context = context();
        PreferenceManager manager = new PreferenceManager(context);
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        PreferenceCategory video = new PreferenceCategory(context);
        video.setKey("category_video_settings");
        screen.addPreference(video);
        // Declared out of section order on purpose
        for (String key : new String[]{"custom_refresh_rate", "checkbox_game_mode", "list_resolution", "unknown_option"}) {
            CheckBoxPreference pref = new CheckBoxPreference(context);
            pref.setKey(key);
            video.addPreference(pref);
        }

        SettingsSections.apply(screen);

        List<PreferenceCategory> sections = SettingsSections.sectionsOf(video);
        assertEquals(3, sections.size());
        assertEquals("section_video_quality", sections.get(0).getKey());
        assertEquals("section_video_latency", sections.get(1).getKey());
        assertEquals("section_video_custom", sections.get(2).getKey());

        List<Preference> ordered = new ArrayList<>();
        for (int i = 0; i < video.getPreferenceCount(); i++) {
            ordered.add(video.getPreference(i));
        }
        Collections.sort(ordered, (a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        List<String> keys = new ArrayList<>();
        for (Preference pref : ordered) {
            keys.add(pref.getKey());
        }
        assertEquals(java.util.Arrays.asList(
                "section_video_quality", "list_resolution",
                "section_video_latency", "checkbox_game_mode",
                "section_video_custom", "custom_refresh_rate", "unknown_option"), keys);
        assertNotNull(sections.get(0).getIcon());
    }

    @Test
    public void threeDSlidersOnlyShowIn3d() {
        Context context = context();
        PreferenceManager manager = new PreferenceManager(context);
        manager.getSharedPreferences().edit().putString("render_mode_list", "0").commit();
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        PreferenceCategory video = new PreferenceCategory(context);
        video.setKey("category_video_settings");
        screen.addPreference(video);
        ListPreference mode = new ListPreference(context);
        mode.setKey("render_mode_list");
        mode.setEntries(new CharSequence[]{"2D", "3D"});
        mode.setEntryValues(new CharSequence[]{"0", "1"});
        video.addPreference(mode);
        Preference depth = new Preference(context);
        depth.setKey("parallax_depth");
        video.addPreference(depth);

        SettingsSections.apply(screen);
        assertFalse(depth.isVisible());

        assertTrue(mode.callChangeListener("1"));
        assertTrue(depth.isVisible());
        assertTrue(mode.callChangeListener("0"));
        assertFalse(depth.isVisible());
    }
}
