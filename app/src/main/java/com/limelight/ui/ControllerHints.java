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

    /** One hint: the buttons that do something, drawn as on the controller, and what they do. */
    public static final class Hint {
        public final int[] buttons;
        public final int label;

        Hint(int label, int... buttons) {
            this.buttons = buttons;
            this.label = label;
        }
    }

    public static Hint[] homeHints(Pad pad) {
        switch (pad) {
            case PLAYSTATION:
                return new Hint[]{
                        new Hint(R.string.hint_open, R.drawable.btn_ps_cross),
                        new Hint(R.string.hint_host_options, R.drawable.btn_menu, R.drawable.btn_ps_triangle),
                        new Hint(R.string.hint_move_hosts, R.drawable.btn_dpad_lr),
                };
            case XBOX:
                return new Hint[]{
                        new Hint(R.string.hint_open, R.drawable.btn_xbox_a),
                        new Hint(R.string.hint_host_options, R.drawable.btn_menu, R.drawable.btn_xbox_y),
                        new Hint(R.string.hint_move_hosts, R.drawable.btn_dpad_lr),
                };
            default:
                return new Hint[]{
                        new Hint(R.string.hint_open, R.drawable.btn_remote_ok),
                        new Hint(R.string.hint_host_options, R.drawable.btn_menu),
                        new Hint(R.string.hint_move_hosts, R.drawable.btn_dpad_lr),
                };
        }
    }
}
