package com.limelight;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.limelight.profiles.ProfilesManager;
import com.limelight.utils.ExternalDisplayControlActivity;
import com.limelight.utils.ThemeMode;
import com.limelight.utils.UiClass;

public class ArtemisApplication extends Application {
    // Held here because SharedPreferences keeps its listeners weakly
    private SharedPreferences.OnSharedPreferenceChangeListener themePrefListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // Light, dark or follow the system. Changing the setting repaints the open screens.
        ThemeMode.apply(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        themePrefListener = (sharedPrefs, key) -> {
            if (ThemeMode.THEME_MODE_PREF_STRING.equals(key) || UiClass.INTERFACE_MODE_PREF_STRING.equals(key)) {
                ThemeMode.apply(this);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(themePrefListener);

        // Status and navigation bar icons that suit the theme of each screen. The stream and
        // the external display controls are always dark, so they keep their own.
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                if (!(activity instanceof Game) && !(activity instanceof ExternalDisplayControlActivity)) {
                    ThemeMode.applySystemBarIcons(activity);
                }
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });

        // On Android 12+ take the app colours from the wallpaper. The stream activity
        // keeps its own fixed overlay colours.
        DynamicColors.applyToActivitiesIfAvailable(this,
                new DynamicColorsOptions.Builder()
                        .setPrecondition((activity, theme) -> !(activity instanceof Game))
                        .build());

        ProfilesManager profilesManager = ProfilesManager.getInstance();
        if (!profilesManager.load(this)) {
            Toast.makeText(this, R.string.profile_manager_failed_to_load, Toast.LENGTH_LONG).show();
        }
    }
}
