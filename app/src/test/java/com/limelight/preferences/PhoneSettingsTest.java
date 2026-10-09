package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Settings on a phone: a list of categories, then one category's options, with back stepping
 * out. Robolectric's default screen is a phone, so StreamSettings picks the drill-down layout.
 */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class PhoneSettingsTest {
    private StreamSettings activity;

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(StreamSettings.class).setup().get();
        ShadowLooper.idleMainLooper();
    }

    private LinearLayout categories() {
        return activity.findViewById(R.id.settingsCategories);
    }

    private boolean shown(int id) {
        View view = activity.findViewById(id);
        assertNotNull(view);
        return view.getVisibility() == View.VISIBLE;
    }

    @Test
    public void startsOnTheListOfCategories() {
        assertNotNull("the phone layout should be in use", activity.findViewById(R.id.settingsListPane));
        assertTrue(shown(R.id.settingsListPane));
        assertFalse(shown(R.id.settingsDetailPane));
        assertFalse(shown(R.id.settingsBack));
        assertEquals(activity.getString(R.string.settings),
                ((TextView) activity.findViewById(R.id.settingsTitle)).getText().toString());
        assertTrue("expected several categories, got " + categories().getChildCount(),
                categories().getChildCount() >= 5);
    }

    @Test
    public void everyCategoryNamesTheSectionsInsideIt() {
        View first = categories().getChildAt(0);
        TextView title = first.findViewById(R.id.settingsRowTitle);
        TextView summary = first.findViewById(R.id.settingsRowSummary);
        assertTrue(title.getText().length() > 0);
        assertEquals(View.VISIBLE, summary.getVisibility());
        assertTrue(summary.getText().length() > 0);
    }

    @Test
    public void tappingACategoryShowsItsOptions() {
        View first = categories().getChildAt(0);
        String name = ((TextView) first.findViewById(R.id.settingsRowTitle)).getText().toString();

        first.performClick();

        assertFalse(shown(R.id.settingsListPane));
        assertTrue(shown(R.id.settingsDetailPane));
        assertTrue(shown(R.id.settingsBack));
        assertEquals(name, ((TextView) activity.findViewById(R.id.settingsTitle)).getText().toString());
    }

    @Test
    public void backGoesFromTheCategoryToTheListAndThenLeaves() {
        categories().getChildAt(0).performClick();

        activity.onBackPressed();
        assertTrue(shown(R.id.settingsListPane));
        assertFalse(shown(R.id.settingsDetailPane));
        assertFalse(activity.isFinishing());

        activity.onBackPressed();
        assertTrue("back on the list leaves Settings", activity.isFinishing());
    }

    @Test
    public void backButtonOnScreenDoesTheSame() {
        categories().getChildAt(1).performClick();
        assertTrue(shown(R.id.settingsDetailPane));

        activity.findViewById(R.id.settingsBack).performClick();

        assertTrue(shown(R.id.settingsListPane));
        assertFalse(shown(R.id.settingsBack));
    }

    @Test
    public void searchShowsResultsAndBackClearsIt() {
        EditText search = activity.findViewById(R.id.settingsSearch);

        search.setText("video");

        assertTrue(shown(R.id.settingsDetailPane));
        assertFalse(shown(R.id.settingsListPane));
        assertEquals(activity.getString(R.string.settings_search_results),
                ((TextView) activity.findViewById(R.id.settingsTitle)).getText().toString());

        activity.onBackPressed();

        assertEquals("", search.getText().toString());
        assertTrue("clearing a search started from the list returns to the list",
                shown(R.id.settingsListPane));
        assertFalse(activity.isFinishing());
    }

    private int headerLayoutOf(String categoryKey) {
        androidx.preference.PreferenceFragmentCompat fragment = (androidx.preference.PreferenceFragmentCompat)
                activity.getSupportFragmentManager().findFragmentById(R.id.stream_settings);
        assertNotNull(fragment);
        return fragment.getPreferenceScreen().findPreference(categoryKey).getLayoutResource();
    }

    @Test
    public void aCategoryDoesNotRepeatItsNameAboveItsOptions() {
        categories().getChildAt(0).performClick();

        assertEquals("the title bar already says Video",
                R.layout.settings_category_hidden, headerLayoutOf("category_video_settings"));
    }

    @Test
    public void searchResultsKeepTheCategoryHeaders() {
        categories().getChildAt(0).performClick();
        int hidden = headerLayoutOf("category_video_settings");

        ((EditText) activity.findViewById(R.id.settingsSearch)).setText("bitrate");

        assertTrue("headers show where each match lives",
                headerLayoutOf("category_video_settings") != hidden);
    }

    @Test
    public void clearingASearchReturnsToTheCategoryYouWereIn() {
        EditText search = activity.findViewById(R.id.settingsSearch);
        View second = categories().getChildAt(1);
        String name = ((TextView) second.findViewById(R.id.settingsRowTitle)).getText().toString();
        second.performClick();

        search.setText("a");
        activity.onBackPressed();

        assertTrue(shown(R.id.settingsDetailPane));
        assertEquals(name, ((TextView) activity.findViewById(R.id.settingsTitle)).getText().toString());
    }
}
