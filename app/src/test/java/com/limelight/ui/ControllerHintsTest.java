package com.limelight.ui;

import static org.junit.Assert.assertEquals;

import com.limelight.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class ControllerHintsTest {

    @Test
    public void sonyPadsGetPlayStationLabels() {
        assertEquals(ControllerHints.Pad.PLAYSTATION, ControllerHints.padForVendor(0x054c));
        assertEquals(R.string.home_hint_playstation, ControllerHints.homeHint(ControllerHints.Pad.PLAYSTATION));
    }

    @Test
    public void otherPadsGetXboxLabels() {
        assertEquals(ControllerHints.Pad.XBOX, ControllerHints.padForVendor(0x045e));
        assertEquals(ControllerHints.Pad.XBOX, ControllerHints.padForVendor(0x2dc8));
        assertEquals(R.string.home_hint_xbox, ControllerHints.homeHint(ControllerHints.Pad.XBOX));
    }

    @Test
    public void noPadKeepsRemoteHint() {
        assertEquals(R.string.home_hint, ControllerHints.homeHint(ControllerHints.Pad.NONE));
    }
}
