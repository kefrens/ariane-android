package com.limelight.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import com.limelight.R;

/**
 * Screen background in the spirit of the PS5 home screen: a cool glow from the top
 * left and a warm one from the bottom right, drifting slowly over the dark base.
 * It only redraws while the window is drawing it, at a low frame rate.
 */
public class AmbientBackgroundDrawable extends Drawable implements Runnable {
    // The drift is slow, so 20 frames a second is smooth enough and cheap at 4K
    private static final long FRAME_MS = 50;
    private static final double CYCLE_MS = 30000;

    private final int baseColor;
    private final int coolColor;
    private final int warmColor;
    private final Paint coolPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint warmPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint grainPaint = DitherNoise.newPaint();
    private final long startMs = SystemClock.uptimeMillis();

    /**
     * Uses this background for the activity's window, on a full 8-bit-per-channel
     * surface: some TVs default to RGB 565, where these gradients band badly.
     */
    public static void install(Activity activity) {
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().setBackgroundDrawable(new AmbientBackgroundDrawable(activity));
    }

    public AmbientBackgroundDrawable(Context context) {
        baseColor = context.getResources().getColor(R.color.ariane_background);
        coolColor = 0x9900497D;  // primary container at 60%
        warmColor = 0x59743500;  // tertiary container at 35%
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float w = b.width();
        float h = b.height();
        if (w <= 0 || h <= 0) {
            return;
        }

        double t = (SystemClock.uptimeMillis() - startMs) / CYCLE_MS * 2 * Math.PI;
        float radius = Math.max(w, h) * 0.8f;

        canvas.drawColor(baseColor);

        float coolX = b.left + w * (0.12f + 0.10f * (float) Math.sin(t));
        float coolY = b.top + h * (0.08f + 0.10f * (float) Math.cos(t * 0.8));
        coolPaint.setShader(new RadialGradient(coolX, coolY, radius,
                coolColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawRect(b, coolPaint);

        float warmX = b.left + w * (0.92f - 0.10f * (float) Math.sin(t * 0.7 + 1));
        float warmY = b.top + h * (0.95f - 0.10f * (float) Math.cos(t * 0.9 + 2));
        warmPaint.setShader(new RadialGradient(warmX, warmY, radius * 0.8f,
                warmColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawRect(b, warmPaint);

        canvas.drawRect(b, grainPaint);

        // Schedule the next frame only after drawing one, so nothing runs while hidden
        unscheduleSelf(this);
        scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_MS);
    }

    @Override
    public void run() {
        invalidateSelf();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (!visible) {
            unscheduleSelf(this);
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public void setAlpha(int alpha) {
        coolPaint.setAlpha(alpha);
        warmPaint.setAlpha(alpha);
        grainPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        coolPaint.setColorFilter(colorFilter);
        warmPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
