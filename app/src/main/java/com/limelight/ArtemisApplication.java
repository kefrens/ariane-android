package com.limelight;

import android.app.Application;
import android.widget.Toast;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.limelight.profiles.ProfilesManager;

public class ArtemisApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

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
