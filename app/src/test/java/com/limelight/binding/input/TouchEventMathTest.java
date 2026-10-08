package com.limelight.binding.input;

import android.view.MotionEvent;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class})
@RunWith(RobolectricTestRunner.class)
public class TouchEventMathTest {
    @Test
    public void polarAndCartesianRoundTrip() {
        float[] point = TouchEventMath.polarToCartesian(5f, (float) (Math.PI / 3));
        assertEquals(2.5f, point[0], 1e-4f);
        assertEquals(4.3301f, point[1], 1e-3f);
        assertEquals(5f, TouchEventMath.cartesianToR(point), 1e-4f);
    }

    @Test
    public void fingerIsNotAPenTool() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0);
        try {
            assertEquals(MoonBridge.LI_TOOL_TYPE_UNKNOWN,
                    TouchEventMath.convertToolTypeToStylusToolType(event, 0));
        } finally {
            event.recycle();
        }
    }

    @Test
    public void touchReportsPressure() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0.75f, 1f, 0, 1f, 1f, 0, 0);
        try {
            assertEquals(0.75f, TouchEventMath.getPressureOrDistance(event, 0), 1e-4f);
        } finally {
            event.recycle();
        }
    }
}
