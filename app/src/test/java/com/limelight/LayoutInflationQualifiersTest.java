package com.limelight;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Every layout must inflate for each class of device, so a layout added for one class
 * cannot break another. The qualifiers match what UiClass treats as phone, tablet and TV.
 */
@Config(sdk = {33})
@RunWith(RobolectricTestRunner.class)
public class LayoutInflationQualifiersTest {
    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    public void phonePortrait() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w740dp-h360dp-land")
    public void phoneLandscape() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port")
    public void tabletPortrait() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    public void tabletLandscape() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    public void television() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port-night")
    public void phonePortraitNight() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-night")
    public void tabletLandscapeNight() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-night")
    public void televisionNight() throws IllegalAccessException {
        LayoutInflationTest.inflateAllLayouts();
    }
}
