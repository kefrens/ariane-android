package com.limelight;

import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class})
@RunWith(RobolectricTestRunner.class)
public class StreamSetupTest {
    private static PreferenceConfiguration prefs(float fps, int pacing) {
        PreferenceConfiguration config = new PreferenceConfiguration();
        config.fps = fps;
        config.framePacing = pacing;
        return config;
    }

    @Test
    public void singleControllerModeAlwaysReportsGamepadOne() {
        assertEquals(1, StreamSetup.gamepadMask(0b110, false, false));
    }

    @Test
    public void multiControllerKeepsAttachedPads() {
        assertEquals(0b110, StreamSetup.gamepadMask(0b110, true, false));
        assertEquals(0b111, StreamSetup.gamepadMask(0b110, true, true));
    }

    @Test
    public void balancedPacingKeepsRequestedFps() {
        PreferenceConfiguration config = prefs(60, PreferenceConfiguration.FRAME_PACING_BALANCED);
        assertEquals(60f, StreamSetup.chooseFrameRate(config, 60f), 0f);
    }

    @Test
    public void capFpsRequestsOneBelowRefreshRate() {
        PreferenceConfiguration config = prefs(60, PreferenceConfiguration.FRAME_PACING_CAP_FPS);
        assertEquals(59f, StreamSetup.chooseFrameRate(config, 60f), 0f);
        assertEquals(PreferenceConfiguration.FRAME_PACING_CAP_FPS, config.framePacing);
    }

    @Test
    public void capFpsFallsBackToBalancedWellAboveRefreshRate() {
        PreferenceConfiguration config = prefs(120, PreferenceConfiguration.FRAME_PACING_CAP_FPS);
        assertEquals(120f, StreamSetup.chooseFrameRate(config, 60f), 0f);
        assertEquals(PreferenceConfiguration.FRAME_PACING_BALANCED, config.framePacing);
    }

    @Test
    public void capFpsIgnoresBogusRefreshRates() {
        PreferenceConfiguration config = prefs(60, PreferenceConfiguration.FRAME_PACING_CAP_FPS);
        assertEquals(60f, StreamSetup.chooseFrameRate(config, 30f), 0f);
        assertEquals(PreferenceConfiguration.FRAME_PACING_BALANCED, config.framePacing);
    }

    @Test
    public void warpFactorMultipliesFrameRate() {
        PreferenceConfiguration config = prefs(60, PreferenceConfiguration.FRAME_PACING_BALANCED);
        config.framePacingWarpFactor = 2;
        assertEquals(120f, StreamSetup.chooseFrameRate(config, 60f), 0f);
    }
}
