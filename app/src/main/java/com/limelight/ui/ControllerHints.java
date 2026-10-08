package com.limelight.ui;

import android.view.InputDevice;

import com.limelight.R;

/**
 * Picks the on-screen key hints for whatever is in the user's hand: the TV remote,
 * an Xbox-style gamepad (A, Y and the Menu button) or a PlayStation one (Cross,
 * Triangle and Options). The brand is read from the gamepad's USB vendor ID.
 */
public final class ControllerHints {
    private static final int VENDOR_MICROSOFT = 0x045e;
    private static final int VENDOR_SONY = 0x054c;

    public enum Pad { NONE, XBOX, PLAYSTATION }

    private ControllerHints() {}

    // The first real gamepad connected, or NONE when only the remote is there
    public static Pad connectedPad() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.isVirtual()) {
                continue;
            }
            int sources = device.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD &&
                    (sources & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) {
                continue;
            }
            return padForVendor(device.getVendorId());
        }
        return Pad.NONE;
    }

    // Android names gamepad buttons after the Xbox layout, so anything that isn't
    // a PlayStation pad gets the Xbox labels
    static Pad padForVendor(int vendorId) {
        return vendorId == VENDOR_SONY ? Pad.PLAYSTATION : Pad.XBOX;
    }

    public static int homeHint(Pad pad) {
        switch (pad) {
            case PLAYSTATION:
                return R.string.home_hint_playstation;
            case XBOX:
                return R.string.home_hint_xbox;
            default:
                return R.string.home_hint;
        }
    }
}
