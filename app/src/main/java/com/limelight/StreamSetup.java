package com.limelight;

import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * The decisions made from the settings and the device before a stream starts:
 * which video formats to offer, which gamepads to report, and the frame rate to
 * ask the host for.
 */
final class StreamSetup {
    private StreamSetup() {}

    static int supportedVideoFormats(MediaCodecDecoderRenderer decoder, boolean willStreamHdr) {
        // H.264 is always supported
        int formats = MoonBridge.VIDEO_FORMAT_H264;
        if (decoder.isHevcSupported()) {
            formats |= MoonBridge.VIDEO_FORMAT_H265;
            if (willStreamHdr && decoder.isHevcMain10Hdr10Supported()) {
                formats |= MoonBridge.VIDEO_FORMAT_H265_MAIN10;
            }
        }
        if (decoder.isAv1Supported()) {
            formats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;
            if (willStreamHdr && decoder.isAv1Main10Supported()) {
                formats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN10;
            }
        }
        return formats;
    }

    static int gamepadMask(int attachedMask, boolean multiController, boolean onscreenController) {
        int mask = attachedMask;
        if (!multiController) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            mask = 1;
        }
        if (onscreenController) {
            // If we're using OSC, always set at least gamepad 1.
            mask |= 1;
        }
        return mask;
    }

    /**
     * Returns the frame rate to request from the host for the display's refresh
     * rate. In "cap FPS" pacing this may switch the pacing mode to balanced when
     * capping can't work, so it updates prefConfig.framePacing.
     */
    static float chooseFrameRate(PreferenceConfiguration prefConfig, float displayRefreshRate) {
        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        int roundedRefreshRate = Math.round(displayRefreshRate);
        float chosenFrameRate = prefConfig.fps;
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    // Use frame drops when rendering above the screen frame rate
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Using drop mode for FPS > Hz");
                } else if (roundedRefreshRate <= 49) {
                    // Let's avoid clearly bogus refresh rates and fall back to legacy rendering
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Bogus refresh rate: " + roundedRefreshRate);
                }
                else {
                    chosenFrameRate = roundedRefreshRate - 1;
                    LimeLog.info("Adjusting FPS target for screen to " + chosenFrameRate);
                }
            }
        }

        if (prefConfig.framePacingWarpFactor > 0) {
            chosenFrameRate *= prefConfig.framePacingWarpFactor;
        }
        return chosenFrameRate;
    }
}
