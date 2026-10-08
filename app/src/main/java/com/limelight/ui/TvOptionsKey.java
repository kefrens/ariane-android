package com.limelight.ui;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;

/**
 * Lets the remote's menu key (or Y or the Menu/Options button on a gamepad) open the options of the focused
 * host or app, so nothing on a TV needs a long press of OK.
 */
public final class TvOptionsKey {
    private TvOptionsKey() {}

    static boolean isOptionsKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BUTTON_Y ||
                keyCode == KeyEvent.KEYCODE_BUTTON_START;
    }

    /**
     * Call from Activity.dispatchKeyEvent before super. Returns true if the event
     * was consumed: the options open on key up, and the matching key down is
     * swallowed so the activity's own menu handling never sees it.
     */
    public static boolean handle(Activity activity, AbsListView list, KeyEvent event) {
        if (!isOptionsKey(event.getKeyCode()) || list == null || !list.hasFocus()) {
            return false;
        }

        View selected = list.getSelectedView();
        if (selected == null) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            ContextMenuPanel.show(activity, list, selected);
        }
        return true;
    }
}
