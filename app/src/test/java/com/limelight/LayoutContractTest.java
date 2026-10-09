package com.limelight;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The Java code looks views up by id and does not null-check most of them, so a layout
 * that drops one crashes at launch. Each screen lists the ids its code needs, and every
 * variant of that layout has to carry them. Variants are found by name: a phone or tablet
 * layout is checked as soon as a file called NAME_phone or NAME_tablet exists.
 */
@Config(sdk = {33})
@RunWith(RobolectricTestRunner.class)
public class LayoutContractTest {
    private static final String[] VARIANT_SUFFIXES = {"", "_phone", "_tablet"};

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Test
    public void homeScreen() {
        check("activity_pc_view",
                "settingsButton", "helpButton", "profilesButton", "pcFragmentContainer",
                "no_pc_found_layout", "homeRunningBanner", "homeRunningHost", "homeRunningApp",
                "homeRunningResume", "homeRunningQuit");
    }

    @Test
    public void hostList() {
        checkList("pc_grid_view");
        check("pc_grid_item", "grid_image", "grid_overlay", "grid_text", "grid_spinner");
        check("pc_add_item");
    }

    @Test
    public void appsScreen() {
        check("activity_app_view",
                "profilesButton", "appSearchButton", "appListText", "appFragmentContainer",
                "runningBanner", "runningAppName", "runningResume", "runningQuit");
    }

    @Test
    public void appList() {
        checkList("app_grid_view");
        checkList("app_grid_view_small");
        check("app_grid_item", "grid_image", "grid_mask", "grid_overlay", "grid_text");
        check("app_grid_item_small", "grid_image", "grid_mask", "grid_overlay", "grid_text");
    }

    @Test
    public void settingsScreen() {
        check("activity_stream_settings", "stream_settings");
    }

    @Test
    public void phoneSettingsHasEverythingTheDrillDownNeeds() {
        // The wide layout shares most of these ids, the phone one adds the two panes and the back button
        check("activity_stream_settings_phone",
                "stream_settings", "settingsCategories", "settingsSearch", "settingsSearchEmpty",
                "settingsSections", "settingsProfile", "settingsListPane", "settingsDetailPane",
                "settingsBack", "settingsTitle");
        check("settings_category_row", "settingsRowIcon", "settingsRowTitle", "settingsRowSummary");
    }

    private static void checkList(String layoutName) {
        for (java.util.Map.Entry<String, View> variant : inflateVariants(layoutName).entrySet()) {
            View list = variant.getValue().findViewById(idOf("fragmentView"));
            assertNotNull(variant.getKey() + " needs fragmentView", list);
            assertTrue(variant.getKey() + ": fragmentView must be an AbsListView, the adapters need one",
                    list instanceof AbsListView);
        }
    }

    private static void check(String layoutName, String... requiredIds) {
        for (java.util.Map.Entry<String, View> variant : inflateVariants(layoutName).entrySet()) {
            for (String id : requiredIds) {
                assertNotNull(variant.getKey() + " is missing @id/" + id,
                        variant.getValue().findViewById(idOf(id)));
            }
        }
    }

    private static java.util.Map<String, View> inflateVariants(String layoutName) {
        Context base = ApplicationProvider.getApplicationContext();
        Context context = new androidx.appcompat.view.ContextThemeWrapper(base,
                com.limelight.R.style.AppTheme);
        Resources res = context.getResources();
        java.util.Map<String, View> views = new java.util.LinkedHashMap<>();
        for (String suffix : VARIANT_SUFFIXES) {
            int layoutId = res.getIdentifier(layoutName + suffix, "layout", context.getPackageName());
            if (layoutId == 0) {
                assertNotEquals("the base layout " + layoutName + " must exist", "", suffix);
                continue;
            }
            views.put(layoutName + suffix, LayoutInflater.from(context).inflate(layoutId, null));
        }
        return views;
    }

    private static int idOf(String name) {
        Context context = ApplicationProvider.getApplicationContext();
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }
}
