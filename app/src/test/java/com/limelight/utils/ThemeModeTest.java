package com.limelight.utils;

import static org.junit.Assert.assertEquals;

import androidx.appcompat.app.AppCompatDelegate;

import org.junit.Test;

public class ThemeModeTest {
    @Test
    public void lightAndDarkAreForcedEverywhere() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.nightModeFor("light", false));
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.nightModeFor("light", true));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.nightModeFor("dark", false));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.nightModeFor("dark", true));
    }

    @Test
    public void systemFollowsTheDeviceExceptOnTv() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.nightModeFor("system", false));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.nightModeFor("system", true));
    }

    @Test
    public void missingOrUnknownValueActsLikeSystem() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.nightModeFor(null, false));
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.nightModeFor("bogus", false));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.nightModeFor(null, true));
    }
}
