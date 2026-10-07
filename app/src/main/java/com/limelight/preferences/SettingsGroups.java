package com.limelight.preferences;

import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

/**
 * Regroups the settings into the categories of the TV design: Video, Audio,
 * Gamepads & remote, Mouse & keyboard, Stats overlay, Host, Appearance and
 * Advanced, plus Touch controls on touch screens.
 *
 * Smaller categories move inside a bigger one as a titled section, so every key
 * and every category key stays the same: profiles, saved values and the code that
 * looks categories up by key keep working.
 */
final class SettingsGroups {
    // Top-level categories in the order they appear
    private static final String[] ORDER = {
            "category_video_settings",
            "category_audio_settings",
            "category_gamepad_settings",
            "category_input_settings",
            "category_perf_monitor_settings",
            "category_host_settings",
            "category_ui_settings",
            "category_onscreen_controls",
            "category_advanced_settings",
    };

    private SettingsGroups() {}

    static void regroup(PreferenceScreen screen, PackageManager pm) {
        // The virtual trackpad is touch only, like the on-screen controls the
        // settings screen already hides on TVs
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            Preference trackpad = screen.findPreference("category_virtual_trackpad_settings");
            if (trackpad != null) {
                screen.removePreference(trackpad);
            }
        }

        // The language picker belongs with the other look-and-feel options
        moveInto(screen, "list_languages", "category_ui_settings");

        nest(screen, "category_special_key_layout", "category_input_settings");
        nest(screen, "category_general_settings", "category_host_settings");
        nest(screen, "category_virtual_trackpad_settings", "category_onscreen_controls");
        nest(screen, "category_settings_misc", "category_advanced_settings");

        // Re-add the top-level categories in the design's order. Every dependency
        // between options is inside one category, so re-attaching a whole
        // category at a time keeps them resolvable.
        PreferenceCategory[] ordered = new PreferenceCategory[ORDER.length];
        for (int i = 0; i < ORDER.length; i++) {
            Preference pref = screen.findPreference(ORDER[i]);
            if (pref instanceof PreferenceCategory && pref.getParent() == screen) {
                ordered[i] = (PreferenceCategory) pref;
                screen.removePreference(pref);
            }
        }
        for (PreferenceCategory category : ordered) {
            if (category != null) {
                category.setOrder(Preference.DEFAULT_ORDER);
                screen.addPreference(category);
            }
        }
    }

    // Moves a whole category inside another one, where it shows as a section title
    private static void nest(PreferenceScreen screen, String childKey, String parentKey) {
        Preference child = screen.findPreference(childKey);
        Preference parent = screen.findPreference(parentKey);
        if (!(child instanceof PreferenceGroup) || !(parent instanceof PreferenceGroup) ||
                child.getParent() != screen) {
            return;
        }
        screen.removePreference(child);
        child.setOrder(Preference.DEFAULT_ORDER);
        ((PreferenceGroup) parent).addPreference(child);
    }

    // Moves one option to the start of another category
    private static void moveInto(PreferenceScreen screen, String key, String categoryKey) {
        Preference pref = screen.findPreference(key);
        Preference target = screen.findPreference(categoryKey);
        if (pref == null || !(target instanceof PreferenceGroup) || pref.getParent() == null) {
            return;
        }
        pref.getParent().removePreference(pref);
        pref.setOrder(-1);
        ((PreferenceGroup) target).addPreference(pref);
    }
}
