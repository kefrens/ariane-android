package com.limelight.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

/**
 * The Theme setting: follow the system, or force light or dark. The stream always stays dark,
 * whatever this says.
 */
public final class ThemeMode {
    public static final String THEME_MODE_PREF_STRING = "list_theme_mode";
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";

    private ThemeMode() {}

    /** Sets the app-wide night mode from the preference. Visible screens repaint themselves. */
    public static void apply(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        AppCompatDelegate.setDefaultNightMode(nightModeFor(
                prefs.getString(THEME_MODE_PREF_STRING, MODE_SYSTEM),
                UiClass.of(context) == UiClass.TV));
    }

    /** The decision itself, without Android lookups, so it can be tested. */
    static int nightModeFor(String mode, boolean television) {
        if (MODE_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (MODE_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        // TVs rarely report a night mode, and the TV design is made for dark
        return television ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    /** Light screens want dark status and navigation bar icons, dark screens want light ones. */
    static boolean useDarkSystemBarIcons(int uiMode) {
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Sets the status and navigation bar icons to suit the screen's theme. The theme alone
     * leaves them white on a light screen on some Android versions.
     */
    public static void applySystemBarIcons(Activity activity) {
        boolean dark = useDarkSystemBarIcons(activity.getResources().getConfiguration().uiMode);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(dark);
        controller.setAppearanceLightNavigationBars(dark);
    }

    /** Keeps one screen dark regardless of the setting. Call before super.onCreate(). */
    public static void pinDark(AppCompatActivity activity) {
        activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
