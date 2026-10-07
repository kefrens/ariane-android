package com.limelight.ui;

import android.os.Build;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.limelight.R;

/**
 * Makes the D-pad focused item of a host or app grid obvious on a TV: a white
 * outline (see focus_ring.xml) and a slight zoom. Touch users never see either,
 * since grids only select items when the device is not in touch mode.
 */
public final class FocusHighlighter {
    static final float FOCUSED_SCALE = 1.06f;
    private static final int ANIM_MS = 120;

    private FocusHighlighter() {}

    public static void install(final AbsListView list) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Each item draws the ring as its foreground, so it zooms with the item.
            // The list's own selector would stay at the unzoomed bounds.
            list.setSelector(android.R.color.transparent);
        }
        else {
            // No view foregrounds before Android 6: let the grid draw the ring instead
            list.setSelector(R.drawable.focus_ring);
            list.setDrawSelectorOnTop(true);
        }

        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private View current;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (current != view) {
                    zoom(current, false);
                    current = view;
                }
                zoom(view, list.hasFocus());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                zoom(current, false);
                current = null;
            }
        });

        list.setOnFocusChangeListener((v, hasFocus) -> zoom(list.getSelectedView(), hasFocus));
    }

    static void zoom(View view, boolean focused) {
        if (view == null) {
            return;
        }
        float target = focused && !view.isInTouchMode() ? FOCUSED_SCALE : 1f;
        view.animate().scaleX(target).scaleY(target).setDuration(ANIM_MS).start();
    }
}
