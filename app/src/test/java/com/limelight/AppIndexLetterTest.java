package com.limelight;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class AppIndexLetterTest {

    @Test
    public void filesAppsUnderTheirFirstLetter() {
        assertEquals("H", AppView.indexLetter("Hades II"));
        assertEquals("S", AppView.indexLetter("steam Big Picture"));
        assertEquals("D", AppView.indexLetter("  Desktop"));
    }

    @Test
    public void dropsAccents() {
        assertEquals("E", AppView.indexLetter("Élden Ring"));
    }

    @Test
    public void filesEverythingElseUnderHash() {
        assertEquals("#", AppView.indexLetter("2XKO"));
        assertEquals("#", AppView.indexLetter(""));
        assertEquals("#", AppView.indexLetter(null));
        assertEquals("#", AppView.indexLetter("原神"));
    }
}
