package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Lays its children out left to right and wraps them onto new rows, like words in a paragraph.
 * Used for the key chips in the in-game panel.
 */
public class FlowLayout extends ViewGroup {
    private final int gap;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = Math.round(8 * context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int w = child.getMeasuredWidth();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            x += w + gap;
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        int height = y + rowHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            int left = getPaddingLeft() + x;
            int top = getPaddingTop() + y;
            child.layout(left, top, left + w, top + h);
            x += w + gap;
            rowHeight = Math.max(rowHeight, h);
        }
    }
}
