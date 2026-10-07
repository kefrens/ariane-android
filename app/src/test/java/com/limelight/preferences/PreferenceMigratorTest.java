package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class PreferenceMigratorTest {
    private Context ctx;
    private SharedPreferences prefs;

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setup() {
        ctx = ApplicationProvider.getApplicationContext();
        prefs = ctx.getSharedPreferences("migrator_test", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void legacyResFpsMovesToResolutionAndFps() {
        prefs.edit().putString("list_resolution_fps", "1080p60").commit();

        PreferenceMigrator.StreamMode mode = PreferenceMigrator.migrate(ctx, prefs);

        assertNotNull(mode);
        assertEquals(1920, mode.width);
        assertEquals(1080, mode.height);
        assertEquals(60f, mode.fps, 0f);
        assertFalse(prefs.contains("list_resolution_fps"));
        assertEquals("1920x1080", prefs.getString("list_resolution", null));
        assertEquals(60f, Float.parseFloat(prefs.getString("list_fps", null)), 0f);
    }

    @Test
    public void unknownLegacyResFpsFallsBackTo720p60() {
        PreferenceMigrator.StreamMode mode = PreferenceMigrator.parseLegacyResFps("bogus");
        assertEquals(1280, mode.width);
        assertEquals(720, mode.height);
        assertEquals(60f, mode.fps, 0f);
    }

    @Test
    public void oldStyleResolutionIsRewritten() {
        prefs.edit().putString("list_resolution", "4K").commit();

        assertNull(PreferenceMigrator.migrate(ctx, prefs));
        assertEquals("3840x2160", prefs.getString("list_resolution", null));
    }

    @Test
    public void currentResolutionIsLeftAlone() {
        prefs.edit().putString("list_resolution", "2560x1440").commit();

        assertNull(PreferenceMigrator.migrate(ctx, prefs));
        assertEquals("2560x1440", prefs.getString("list_resolution", null));
    }

    @Test
    public void legacySurroundAndStretchAreMoved() {
        prefs.edit()
                .putBoolean("checkbox_51_surround", true)
                .putBoolean("checkbox_stretch_video", true)
                .putBoolean("checkbox_enforce_refresh_rate", true)
                .commit();

        PreferenceMigrator.migrate(ctx, prefs);

        assertEquals("51", prefs.getString("list_audio_config", null));
        assertFalse(prefs.contains("checkbox_51_surround"));
        assertEquals("stretch", prefs.getString("list_video_scale_mode", null));
        assertFalse(prefs.contains("checkbox_stretch_video"));
        assertTrue(prefs.getBoolean("checkbox_enforce_display_mode", false));
        assertFalse(prefs.contains("checkbox_enforce_refresh_rate"));
    }

    @Test
    public void smallIconDefaultIsWritten() {
        PreferenceMigrator.migrate(ctx, prefs);
        assertTrue(prefs.contains("checkbox_small_icon_mode"));
    }

    @Test
    public void readPreferencesUsesMigratedLegacyMode() {
        prefs.edit().putString("list_resolution_fps", "720p30").commit();

        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(ctx, prefs);

        assertEquals(1280, config.width);
        assertEquals(720, config.height);
        assertEquals(30f, config.fps, 0f);
    }
}
