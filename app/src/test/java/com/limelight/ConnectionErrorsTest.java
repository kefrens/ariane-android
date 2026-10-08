package com.limelight;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class ConnectionErrorsTest {
    private final Resources res = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void blockedPortsSupersedeTheErrorCode() {
        String message = ConnectionErrors.terminatedMessage(res, MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC, 0, 5);
        assertEquals(res.getString(R.string.nettest_text_blocked), message);
    }

    @Test
    public void noVideoHasItsOwnMessage() {
        String message = ConnectionErrors.terminatedMessage(res, MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC, 0,
                MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
        assertEquals(res.getString(R.string.no_video_received_error), message);
    }

    @Test
    public void largeErrorCodesShowAsHex() {
        String message = ConnectionErrors.terminatedMessage(res, 0x80004005, 0, 0);
        assertTrue(message, message.endsWith("80004005"));
    }

    @Test
    public void stageFailureNamesTheStageAndCode() {
        String message = ConnectionErrors.stageFailedMessage(res, "RTSP handshake", -408, 0, 0);
        assertTrue(message, message.contains("RTSP handshake (error -408)"));
        assertTrue(message, message.contains(res.getString(R.string.error_msg_timeout)));
    }

    @Test
    public void waitsWhileHostMayBeWaking() {
        assertTrue(ConnectionErrors.mayStillBeStarting(0, 1, MoonBridge.ML_TEST_RESULT_INCONCLUSIVE));
        assertFalse(ConnectionErrors.mayStillBeStarting(0, 1, 5));
        assertFalse(ConnectionErrors.mayStillBeStarting(-1, 1, 0));
    }
}
