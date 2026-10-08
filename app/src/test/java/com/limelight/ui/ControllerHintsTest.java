package com.limelight.ui;

import static org.junit.Assert.assertArrayEquals;
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
        ControllerHints.Hint[] hints = ControllerHints.homeHints(ControllerHints.Pad.PLAYSTATION);
        assertArrayEquals(new int[]{R.drawable.btn_ps_cross}, hints[0].buttons);
        assertArrayEquals(new int[]{R.drawable.btn_menu, R.drawable.btn_ps_triangle}, hints[1].buttons);
    }

    @Test
    public void otherPadsGetXboxLabels() {
        assertEquals(ControllerHints.Pad.XBOX, ControllerHints.padForVendor(0x045e));
        assertEquals(ControllerHints.Pad.XBOX, ControllerHints.padForVendor(0x2dc8));
        ControllerHints.Hint[] hints = ControllerHints.homeHints(ControllerHints.Pad.XBOX);
        assertArrayEquals(new int[]{R.drawable.btn_xbox_a}, hints[0].buttons);
        assertArrayEquals(new int[]{R.drawable.btn_menu, R.drawable.btn_xbox_y}, hints[1].buttons);
    }

    @Test
    public void noPadKeepsRemoteHint() {
        ControllerHints.Hint[] hints = ControllerHints.homeHints(ControllerHints.Pad.NONE);
        assertArrayEquals(new int[]{R.drawable.btn_remote_ok}, hints[0].buttons);
        assertEquals(R.string.hint_open, hints[0].label);
    }
}
