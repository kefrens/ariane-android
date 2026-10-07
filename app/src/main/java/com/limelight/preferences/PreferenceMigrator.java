package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Moves settings saved by older versions of the app to their current keys and
 * formats, and writes the defaults the settings screen needs to show.
 */
final class PreferenceMigrator {
    /** A resolution and frame rate read from the legacy combined setting. */
    static final class StreamMode {
        final int width;
        final int height;
        final float fps;

        StreamMode(int width, int height, float fps) {
            this.width = width;
            this.height = height;
            this.fps = fps;
        }
    }

    private PreferenceMigrator() {}

    /**
     * Returns the stream mode from the legacy combined resolution and frame rate
     * setting if there was one (it has been moved to the current keys), or null.
     */
    static StreamMode migrate(Context context, SharedPreferences prefs) {
        if (prefs.contains(PreferenceConfiguration.LEGACY_ENABLE_51_SURROUND_PREF_STRING)) {
            if (prefs.getBoolean(PreferenceConfiguration.LEGACY_ENABLE_51_SURROUND_PREF_STRING, false)) {
                prefs.edit()
                        .remove(PreferenceConfiguration.LEGACY_ENABLE_51_SURROUND_PREF_STRING)
                        .putString(PreferenceConfiguration.AUDIO_CONFIG_PREF_STRING, "51")
                        .apply();
            }
        }

        StreamMode legacyMode = null;
        String str = prefs.getString(PreferenceConfiguration.LEGACY_RES_FPS_PREF_STRING, null);
        if (str != null) {
            legacyMode = parseLegacyResFps(str);

            prefs.edit()
                    .remove(PreferenceConfiguration.LEGACY_RES_FPS_PREF_STRING)
                    .putString(PreferenceConfiguration.RESOLUTION_PREF_STRING,
                            PreferenceConfiguration.getResolutionString(legacyMode.width, legacyMode.height))
                    .putString(PreferenceConfiguration.FPS_PREF_STRING, "" + legacyMode.fps)
                    .apply();
        }
        else {
            // Convert legacy resolution strings to the new style
            String resStr = prefs.getString(PreferenceConfiguration.RESOLUTION_PREF_STRING,
                    PreferenceConfiguration.DEFAULT_RESOLUTION);
            if (!resStr.contains("x")) {
                prefs.edit()
                        .putString(PreferenceConfiguration.RESOLUTION_PREF_STRING,
                                PreferenceConfiguration.convertFromLegacyResolutionString(resStr))
                        .apply();
            }
        }

        if (prefs.contains(PreferenceConfiguration.LEGACY_STRETCH_PREF_STRING)) {
            boolean stretch = prefs.getBoolean(PreferenceConfiguration.LEGACY_STRETCH_PREF_STRING, false);
            prefs.edit()
                    .remove(PreferenceConfiguration.LEGACY_STRETCH_PREF_STRING)
                    .putString(PreferenceConfiguration.VIDEO_SCALE_MODE_PREF_STRING, stretch ? "stretch" : "fit")
                    .apply();
        }

        if (prefs.contains(PreferenceConfiguration.LEGACY_ENFORCE_REFRESH_RATE_STRING)) {
            boolean enforce = prefs.getBoolean(PreferenceConfiguration.LEGACY_ENFORCE_REFRESH_RATE_STRING, false);
            prefs.edit()
                    .remove(PreferenceConfiguration.LEGACY_ENFORCE_REFRESH_RATE_STRING)
                    .putBoolean(PreferenceConfiguration.ENFORCE_DISPLAY_MODE_PREF_STRING, enforce)
                    .apply();
        }

        if (!prefs.contains(PreferenceConfiguration.SMALL_ICONS_PREF_STRING)) {
            // We need to write small icon mode's default to disk for the settings page to display
            // the current state of the option properly
            prefs.edit().putBoolean(PreferenceConfiguration.SMALL_ICONS_PREF_STRING,
                    PreferenceConfiguration.getDefaultSmallMode(context)).apply();
        }

        if (!prefs.contains(PreferenceConfiguration.GAMEPAD_MOTION_SENSORS_PREF_STRING) &&
                Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            // Android 12 has a nasty bug that causes crashes when the app touches the InputDevice's
            // associated InputDeviceSensorManager (just calling getSensorManager() is enough).
            // As a workaround, we will override the default value for the gamepad motion sensor
            // option to disabled on Android 12 to reduce the impact of this bug.
            // https://cs.android.com/android/_/android/platform/frameworks/base/+/8970010a5e9f3dc5c069f56b4147552accfcbbeb
            prefs.edit().putBoolean(PreferenceConfiguration.GAMEPAD_MOTION_SENSORS_PREF_STRING, false).apply();
        }

        return legacyMode;
    }

    static StreamMode parseLegacyResFps(String value) {
        switch (value) {
            case "360p30":
                return new StreamMode(640, 360, 30);
            case "360p60":
                return new StreamMode(640, 360, 60);
            case "720p30":
                return new StreamMode(1280, 720, 30);
            case "720p60":
                return new StreamMode(1280, 720, 60);
            case "1080p30":
                return new StreamMode(1920, 1080, 30);
            case "1080p60":
                return new StreamMode(1920, 1080, 60);
            case "4K30":
                return new StreamMode(3840, 2160, 30);
            case "4K60":
                return new StreamMode(3840, 2160, 60);
            default:
                // Should never get here
                return new StreamMode(1280, 720, 60);
        }
    }
}
