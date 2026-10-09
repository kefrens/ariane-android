package com.limelight.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

import androidx.annotation.LayoutRes;
import androidx.preference.PreferenceManager;

/**
 * Which kind of device the UI is laid out for. Screens ask this for the layout to inflate,
 * so TV keeps its own layouts while phones and tablets can get touch ones.
 */
public enum UiClass {
    TV,
    TABLET,
    PHONE;

    public static final String INTERFACE_MODE_PREF_STRING = "list_interface_mode";
    public static final String MODE_AUTO = "auto";
    public static final String MODE_TV = "tv";
    public static final String MODE_TOUCH = "touch";

    private static final int TABLET_MIN_SMALLEST_WIDTH_DP = 600;

    /** The class for the current window. Call again after a configuration change. */
    public static UiClass of(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return resolve(prefs.getString(INTERFACE_MODE_PREF_STRING, MODE_AUTO),
                isTelevision(context),
                context.getResources().getConfiguration().smallestScreenWidthDp);
    }

    /** The decision itself, without Android lookups, so it can be tested. */
    static UiClass resolve(String interfaceMode, boolean television, int smallestWidthDp) {
        if (MODE_TV.equals(interfaceMode)) {
            return TV;
        }
        if (!MODE_TOUCH.equals(interfaceMode) && television) {
            return TV;
        }
        return smallestWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP ? TABLET : PHONE;
    }

    /**
     * Picks the layout for this class. A class without its own layout (0) uses the nearest
     * one: a tablet falls back to the phone layout, and either falls back to the TV layout.
     */
    @LayoutRes
    public static int layoutFor(UiClass uiClass, @LayoutRes int tv, @LayoutRes int phone,
                                @LayoutRes int tablet) {
        switch (uiClass) {
            case TABLET:
                if (tablet != 0) return tablet;
                // fall through
            case PHONE:
                if (phone != 0) return phone;
                // fall through
            default:
                return tv;
        }
    }

    @LayoutRes
    public static int layoutFor(Context context, @LayoutRes int tv, @LayoutRes int phone,
                                @LayoutRes int tablet) {
        return layoutFor(of(context), tv, phone, tablet);
    }

    private static boolean isTelevision(Context context) {
        UiModeManager modeMgr = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (modeMgr != null && modeMgr.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
}
