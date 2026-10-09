package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.grid.PcGridAdapter;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

/** Home and Apps on a phone, against the tablet and TV layouts that stay as they were. */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class PhoneHomeTest {
    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    private static View addHostItem() {
        Context context = ApplicationProvider.getApplicationContext();
        PcGridAdapter adapter = new PcGridAdapter(context, PreferenceConfiguration.readPreferences(context));
        // With no hosts yet the list is only the "Add host" item
        return adapter.getView(0, null, new LinearLayout(context));
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    public void aPhoneListsHostsAsRows() {
        assertEquals("a row: icon beside the text", LinearLayout.HORIZONTAL,
                ((LinearLayout) addHostItem()).getOrientation());
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    public void aTabletKeepsTheHostCards() {
        assertEquals("a card: icon above the text", LinearLayout.VERTICAL,
                ((LinearLayout) addHostItem()).getOrientation());
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    public void aTvKeepsTheHostCards() {
        // A real TV reports television mode, which Robolectric only does when told to
        UiModeManager uiMode = (UiModeManager) ApplicationProvider.getApplicationContext()
                .getSystemService(Context.UI_MODE_SERVICE);
        Shadows.shadowOf(uiMode).currentModeType = Configuration.UI_MODE_TYPE_TELEVISION;

        assertEquals(LinearLayout.VERTICAL, ((LinearLayout) addHostItem()).getOrientation());
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    public void homeOpensOnAPhone() {
        // PcView waits for a GL renderer string on its first run, which a test never gets
        GlPreferences glPrefs = GlPreferences.readPreferences(ApplicationProvider.getApplicationContext());
        glPrefs.savedFingerprint = Build.FINGERPRINT;
        glPrefs.glRenderer = "test";
        glPrefs.writePreferences();

        PcView activity = Robolectric.buildActivity(PcView.class).create().get();

        // The phone banner stacks Resume and Quit under the name, so Quit shares a row with Resume
        View resume = activity.findViewById(R.id.homeRunningResume);
        View quit = activity.findViewById(R.id.homeRunningQuit);
        assertNotNull(resume);
        assertNotNull(quit);
        assertEquals(resume.getParent(), quit.getParent());
        assertFalse(activity.isFinishing());
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    public void appsOpensOnAPhone() {
        android.content.Intent intent = new android.content.Intent();
        intent.putExtra(AppView.NAME_EXTRA, "Desktop");
        intent.putExtra(AppView.UUID_EXTRA, "uuid");
        AppView activity = Robolectric.buildActivity(AppView.class, intent).create().get();

        assertNotNull(activity.findViewById(R.id.appSearchButton));
        assertNotNull(activity.findViewById(R.id.runningBanner));
        // Icon-only search on a phone: the title keeps the room
        assertTrue(((com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton)
                activity.findViewById(R.id.appSearchButton)).getText().length() == 0);
    }

    private static boolean floatingButtonOn() {
        Context context = ApplicationProvider.getApplicationContext();
        return PreferenceConfiguration.readPreferences(context).enableFloatingButton;
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    public void theFloatingMenuButtonIsOnByDefaultOnAPhone() {
        assertTrue(floatingButtonOn());
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    public void theFloatingMenuButtonStaysOffOnATablet() {
        assertFalse(floatingButtonOn());
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    public void theFloatingMenuButtonStaysOffOnATv() {
        assertFalse(floatingButtonOn());
    }
}
