package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Config(sdk = {33})
@RunWith(RobolectricTestRunner.class)
public class MenuHandleTest {
    private static final int SCREEN_W = 800;
    private static final int SCREEN_H = 400;
    private static final int HANDLE_W = 100;
    private static final int HANDLE_H = 240;

    private Context context;
    private FrameLayout screen;
    private MenuHandle handle;
    private final AtomicInteger opened = new AtomicInteger();

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        build();
    }

    // A screen with the handle on its right edge, like activity_game.xml
    private void build() {
        screen = new FrameLayout(context);
        handle = new MenuHandle(context);
        handle.setOnClickListener(v -> opened.incrementAndGet());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(HANDLE_W, HANDLE_H);
        params.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;
        screen.addView(handle, params);
        screen.measure(View.MeasureSpec.makeMeasureSpec(SCREEN_W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(SCREEN_H, View.MeasureSpec.EXACTLY));
        screen.layout(0, 0, SCREEN_W, SCREEN_H);
    }

    private void touch(int action, float rawY) {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, 10f, rawY, 0);
        event.setLocation(10f, rawY);
        handle.dispatchTouchEvent(event);
        event.recycle();
    }

    @Test
    public void aTapOpensTheMenu() {
        touch(MotionEvent.ACTION_DOWN, 200);
        touch(MotionEvent.ACTION_UP, 200);

        assertEquals(1, opened.get());
    }

    @Test
    public void aDragMovesTheHandleWithoutOpeningTheMenu() {
        touch(MotionEvent.ACTION_DOWN, 200);
        touch(MotionEvent.ACTION_MOVE, 220);
        touch(MotionEvent.ACTION_MOVE, 240);
        touch(MotionEvent.ACTION_UP, 240);

        assertEquals("the handle follows the finger", 40f, handle.getTranslationY(), 0.5f);
        assertEquals("a drag is not a tap", 0, opened.get());
    }

    @Test
    public void aSmallWobbleStillCountsAsATap() {
        touch(MotionEvent.ACTION_DOWN, 200);
        touch(MotionEvent.ACTION_MOVE, 203);
        touch(MotionEvent.ACTION_UP, 203);

        assertEquals(1, opened.get());
        assertEquals(0f, handle.getTranslationY(), 0.5f);
    }

    @Test
    public void theHandleStaysOnTheScreen() {
        touch(MotionEvent.ACTION_DOWN, 200);
        touch(MotionEvent.ACTION_MOVE, -5000);
        touch(MotionEvent.ACTION_UP, -5000);
        float room = (SCREEN_H - HANDLE_H) / 2f;
        assertEquals("stops at the top", -room, handle.getTranslationY(), 0.5f);

        touch(MotionEvent.ACTION_DOWN, 200);
        touch(MotionEvent.ACTION_MOVE, 5000);
        touch(MotionEvent.ACTION_UP, 5000);
        assertEquals("stops at the bottom", room, handle.getTranslationY(), 0.5f);
    }

    @Test
    public void whereYouLeaveItIsRemembered() {
        touch(MotionEvent.ACTION_DOWN, 200);
        touch(MotionEvent.ACTION_MOVE, 280);
        touch(MotionEvent.ACTION_UP, 280);
        float moved = handle.getTranslationY();

        // A new stream, a new view
        build();

        assertEquals(moved, handle.getTranslationY(), 0.5f);
    }

    @Test
    public void thePositionHoldsOnADifferentScreenSize() {
        assertEquals(0f, MenuHandle.toPosition(0f, 80f), 0.001f);
        assertEquals(1f, MenuHandle.toPosition(80f, 80f), 0.001f);
        assertEquals(-0.5f, MenuHandle.toPosition(-40f, 80f), 0.001f);
        assertEquals("beyond the room is clamped", 1f, MenuHandle.toPosition(500f, 80f), 0.001f);
        assertEquals("no room at all", 0f, MenuHandle.toPosition(30f, 0f), 0.001f);
    }

    @Test
    public void onlyTheHandleIsExcludedFromTheBackGesture() {
        List<Rect> rects = ViewCompat.getSystemGestureExclusionRects(handle);

        assertEquals(1, rects.size());
        assertEquals(new Rect(0, 0, HANDLE_W, HANDLE_H), rects.get(0));
    }

    @Test
    public void itFadesWhenLeftAlone() {
        touch(MotionEvent.ACTION_DOWN, 200);
        assertEquals("easy to see while touched", 1f, handle.getAlpha(), 0.001f);
        touch(MotionEvent.ACTION_UP, 200);

        org.robolectric.shadows.ShadowLooper.idleMainLooper(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue("faint once idle", handle.getAlpha() < 0.5f);
        assertFalse(handle.getAlpha() == 0f);
    }
}
