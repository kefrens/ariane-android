package com.limelight.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import java.util.Random;

/**
 * A faint, fixed grain laid over dark gradients. Each pixel moves by a few levels at
 * most, which is invisible as texture but breaks up the steps an 8-bit panel shows in
 * slow gradients (the "256 colours" look).
 */
public final class DitherNoise {
    private static final int TILE = 128;
    // About 1.5% of each grain pixel, so a pixel shifts by 0 to 4 levels
    private static final int ALPHA = 4;

    private static Bitmap tile;

    private DitherNoise() {}

    private static synchronized Bitmap getTile() {
        if (tile == null) {
            int[] pixels = new int[TILE * TILE];
            // Fixed seed so the grain never shimmers between screens
            Random random = new Random(0x41524941L);
            for (int i = 0; i < pixels.length; i++) {
                int g = random.nextInt(256);
                pixels[i] = (ALPHA << 24) | (g << 16) | (g << 8) | g;
            }
            tile = Bitmap.createBitmap(pixels, TILE, TILE, Bitmap.Config.ARGB_8888);
        }
        return tile;
    }

    /** A paint that tiles the grain; draw it over the bounds after the gradients. */
    public static Paint newPaint() {
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(getTile(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        return paint;
    }

    /** The grain as a layer, for stacking over other drawables. */
    public static Drawable newDrawable() {
        return new Drawable() {
            private final Paint paint = newPaint();

            @Override
            public void draw(Canvas canvas) {
                Rect b = getBounds();
                canvas.drawRect(b, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {
                paint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }
}
