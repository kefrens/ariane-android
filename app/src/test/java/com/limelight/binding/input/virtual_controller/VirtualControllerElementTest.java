package com.limelight.binding.input.virtual_controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class VirtualControllerElementTest {
    private static class Host implements ElementHost {
        ControllerMode mode = ControllerMode.Active;
        final List<VirtualControllerElement<Host>> elements = new ArrayList<>();

        @Override
        public ControllerMode getControllerMode() {
            return mode;
        }

        @Override
        public List<VirtualControllerElement<Host>> getElements() {
            return elements;
        }
    }

    private static class Element extends VirtualControllerElement<Host> {
        Element(Host host, Context context) {
            super(host, context, 7);
        }

        @Override
        protected void onElementDraw(Canvas canvas) {
        }

        @Override
        public boolean onElementTouchEvent(MotionEvent event) {
            return true;
        }
    }

    private Element newElement(Host host) {
        Element element = new Element(host, ApplicationProvider.getApplicationContext());
        element.setLayoutParams(new FrameLayout.LayoutParams(100, 50));
        host.elements.add(element);
        return element;
    }

    @Test
    public void numericIdsAreKeptAsTheSavedKey() {
        assertEquals("7", newElement(new Host()).elementId);
    }

    @Test
    public void configurationRoundTripsWithHiddenFlag() throws Exception {
        Host host = new Host();
        Element element = newElement(host);
        element.enabled = false;
        element.hidden = true;
        JSONObject saved = element.getConfiguration();

        Element loaded = newElement(host);
        loaded.loadConfiguration(saved);

        assertFalse(loaded.enabled);
        assertTrue(loaded.hidden);
        assertEquals(View.GONE, loaded.getVisibility());
        assertEquals(100, loaded.getLayoutParams().width);
    }

    @Test
    public void layoutsSavedBeforeHiddenExistedStillLoad() throws Exception {
        Element element = newElement(new Host());
        JSONObject saved = new JSONObject()
                .put("LEFT", 10).put("TOP", 20).put("WIDTH", 30).put("HEIGHT", 40).put("ENABLED", true);

        element.loadConfiguration(saved);

        assertFalse(element.hidden);
        assertEquals(View.VISIBLE, element.getVisibility());
    }

    @Test
    public void disabledElementsStayVisibleWhileTogglingThem() throws Exception {
        Host host = new Host();
        host.mode = ControllerMode.DisableEnableButtons;
        Element element = newElement(host);
        JSONObject saved = new JSONObject()
                .put("LEFT", 0).put("TOP", 0).put("WIDTH", 30).put("HEIGHT", 40).put("ENABLED", false);

        element.loadConfiguration(saved);

        assertEquals(View.VISIBLE, element.getVisibility());
    }

    @Test
    public void pressedColorComesFromTheHost() {
        Host host = new Host() {
            @Override
            public int getPressedColor() {
                return 0x12345678;
            }
        };
        assertEquals(0x12345678, newElement(host).pressedColor);
    }
}
