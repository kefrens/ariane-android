package com.limelight.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UiClassTest {
    @Test
    public void autoPicksByDeviceAndWidth() {
        assertEquals(UiClass.TV, UiClass.resolve("auto", true, 810));
        assertEquals(UiClass.TABLET, UiClass.resolve("auto", false, 800));
        assertEquals(UiClass.TABLET, UiClass.resolve("auto", false, 600));
        assertEquals(UiClass.PHONE, UiClass.resolve("auto", false, 599));
        assertEquals(UiClass.PHONE, UiClass.resolve("auto", false, 360));
    }

    @Test
    public void unknownOrMissingModeBehavesLikeAuto() {
        assertEquals(UiClass.TV, UiClass.resolve(null, true, 810));
        assertEquals(UiClass.PHONE, UiClass.resolve("bogus", false, 360));
    }

    @Test
    public void tvOverrideForcesTvEverywhere() {
        assertEquals(UiClass.TV, UiClass.resolve("tv", false, 360));
        assertEquals(UiClass.TV, UiClass.resolve("tv", false, 800));
    }

    @Test
    public void touchOverrideNeverGivesTvEvenOnATv() {
        assertEquals(UiClass.TABLET, UiClass.resolve("touch", true, 810));
        assertEquals(UiClass.PHONE, UiClass.resolve("touch", true, 360));
    }

    @Test
    public void layoutFallsBackToTheNearestOne() {
        int tv = 1, phone = 2, tablet = 3;
        assertEquals(tv, UiClass.layoutFor(UiClass.TV, tv, phone, tablet));
        assertEquals(phone, UiClass.layoutFor(UiClass.PHONE, tv, phone, tablet));
        assertEquals(tablet, UiClass.layoutFor(UiClass.TABLET, tv, phone, tablet));
        // no tablet layout: tablets use the phone one
        assertEquals(phone, UiClass.layoutFor(UiClass.TABLET, tv, phone, 0));
        // no touch layouts at all: everything uses the TV one
        assertEquals(tv, UiClass.layoutFor(UiClass.TABLET, tv, 0, 0));
        assertEquals(tv, UiClass.layoutFor(UiClass.PHONE, tv, 0, 0));
    }
}
