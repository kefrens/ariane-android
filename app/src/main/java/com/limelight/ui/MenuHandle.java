package com.limelight.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;

import java.util.Collections;

/**
 * A small tab on the right edge of the stream. Tapping it opens the stream menu, and dragging
 * it up or down moves it out of the way. It sits above the video as its own view, so a touch on
 * it never reaches the host, and it asks Android not to treat touches on it as the Back gesture.
 *
 * It fades to a faint outline when left alone.
 */
public class MenuHandle extends View {
    static final String POSITION_PREF = "menu_handle_position";
    private static final float IDLE_ALPHA = 0.35f;
    private static final long FADE_DELAY_MS = 2500;
    private static final float TAB_WIDTH_DP = 10f;
    private static final float TAB_HEIGHT_DP = 64f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tab = new RectF();
    private final float density;
    private final int touchSlop;

    private float downRawY;
    private float downTranslation;
    private boolean dragging;
    // Where the tab sits, from -1 (top of the screen) to 1 (bottom), 0 being the middle
    private float position;
    private boolean positionRestored;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable fadeOut = () -> animate().alpha(IDLE_ALPHA).setDuration(300).start();

    public MenuHandle(Context context) {
        this(context, null);
    }

    public MenuHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFFFFFFF);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(1.5f * density);
        outline.setColor(0x99000000);

        setAlpha(IDLE_ALPHA);
        setClickable(true);
    }

    /** Where a drag leaves the tab: the offset from the middle, kept inside the room there is. */
    static float clampOffset(float offset, float maxOffset) {
        return Math.max(-maxOffset, Math.min(maxOffset, offset));
    }

    /** The same place as a number from -1 to 1, so it holds on any screen size or rotation. */
    static float toPosition(float offset, float maxOffset) {
        return maxOffset <= 0 ? 0 : clampOffset(offset, maxOffset) / maxOffset;
    }

    /** How far the tab can move from the middle before it leaves the screen. */
    private float maxOffset() {
        View parent = getParent() instanceof View ? (View) getParent() : null;
        if (parent == null) {
            return 0;
        }
        return Math.max(0, (parent.getHeight() - getHeight()) / 2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = TAB_WIDTH_DP * density;
        float height = Math.min(TAB_HEIGHT_DP * density, getHeight());
        float top = (getHeight() - height) / 2f;
        // Flush with the right edge, rounded on the side that faces the screen
        tab.set(getWidth() - width, top, getWidth() + width, top + height);
        float radius = width;
        canvas.drawRoundRect(tab, radius, radius, fill);
        canvas.drawRoundRect(tab, radius, radius, outline);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!positionRestored && getParent() != null && maxOffset() > 0) {
            positionRestored = true;
            position = readPosition();
            setTranslationY(position * maxOffset());
        }

        // Touches on the handle are never the system Back gesture. Only the handle's own area
        // is excluded, which is far below the 200dp that Android 10 and 11 allow per edge.
        ViewCompat.setSystemGestureExclusionRects(this,
                Collections.singletonList(new Rect(0, 0, right - left, bottom - top)));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawY = event.getRawY();
                downTranslation = getTranslationY();
                dragging = false;
                handler.removeCallbacks(fadeOut);
                animate().cancel();
                setAlpha(1f);
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE: {
                float moved = event.getRawY() - downRawY;
                if (!dragging && Math.abs(moved) > touchSlop) {
                    dragging = true;
                }
                if (dragging) {
                    float max = maxOffset();
                    setTranslationY(clampOffset(downTranslation + moved, max));
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    float max = maxOffset();
                    position = toPosition(getTranslationY(), max);
                    savePosition(position);
                } else {
                    performClick();
                }
                dragging = false;
                handler.postDelayed(fadeOut, FADE_DELAY_MS);
                return true;

            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                handler.postDelayed(fadeOut, FADE_DELAY_MS);
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && visibility == VISIBLE) {
            // Easy to find for a moment, then out of the way
            handler.removeCallbacks(fadeOut);
            setAlpha(1f);
            handler.postDelayed(fadeOut, FADE_DELAY_MS * 2);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(fadeOut);
        super.onDetachedFromWindow();
    }

    private float readPosition() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return Math.max(-1f, Math.min(1f, prefs.getFloat(POSITION_PREF, 0f)));
    }

    private void savePosition(float value) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putFloat(POSITION_PREF, value).apply();
    }
}
