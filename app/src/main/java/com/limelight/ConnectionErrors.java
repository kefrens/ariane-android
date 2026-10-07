package com.limelight;

import android.content.res.Resources;

import com.limelight.nvstream.jni.MoonBridge;

/**
 * Builds the messages shown when a stream fails to start or ends unexpectedly,
 * from the error code, the ports involved and the result of the port test.
 */
final class ConnectionErrors {
    private ConnectionErrors() {}

    /** True if the connectivity test found the ports blocked. */
    static boolean portsBlocked(int portTestResult) {
        return portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0;
    }

    /**
     * True if a failed start is probably the host still waking or unlocking,
     * so the connection should keep trying instead of failing.
     */
    static boolean mayStillBeStarting(int errorCode, int portFlags, int portTestResult) {
        return errorCode == 0 && portFlags != 0 &&
                (portTestResult == MoonBridge.ML_TEST_RESULT_INCONCLUSIVE || portTestResult == 0);
    }

    static String stageFailedMessage(Resources res, String stage, int errorCode,
                                     int portFlags, int portTestResult) {
        String text = res.getString(R.string.conn_error_msg) + " " + stage + " (error " + errorCode + ")";

        switch (errorCode) {
            case 403:
                text += "\n\n" + res.getString(R.string.error_msg_permission_denied) +
                        " (" + res.getString(R.string.permission_launch_app) + ")";
                break;
            case -408:
                text += "\n\n" + res.getString(R.string.error_msg_timeout);
                break;
            default:
                break;
        }

        text += portsSuffix(res, portFlags);

        if (portsBlocked(portTestResult)) {
            text += "\n\n" + res.getString(R.string.nettest_text_blocked);
        }
        return text;
    }

    static String terminatedMessage(Resources res, int errorCode, int portFlags, int portTestResult) {
        String message;

        if (portsBlocked(portTestResult)) {
            // If we got a blocked result, that supersedes any other error message
            message = res.getString(R.string.nettest_text_blocked);
        }
        else {
            switch (errorCode) {
                case MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC:
                    message = res.getString(R.string.no_video_received_error);
                    break;

                case MoonBridge.ML_ERROR_NO_VIDEO_FRAME:
                    message = res.getString(R.string.no_frame_received_error);
                    break;

                case MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION:
                case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
                    message = res.getString(R.string.early_termination_error);
                    break;

                case MoonBridge.ML_ERROR_FRAME_CONVERSION:
                    message = res.getString(R.string.frame_conversion_error);
                    break;

                default:
                    // We'll assume large errors are hex values
                    String errorCodeString = Math.abs(errorCode) > 1000 ?
                            Integer.toHexString(errorCode) : Integer.toString(errorCode);
                    message = res.getString(R.string.conn_terminated_msg) + "\n\n" +
                            res.getString(R.string.error_code_prefix) + " " + errorCodeString;
                    break;
            }
        }

        return message + portsSuffix(res, portFlags);
    }

    private static String portsSuffix(Resources res, int portFlags) {
        if (portFlags == 0) {
            return "";
        }
        return "\n\n" + res.getString(R.string.check_ports_msg) + "\n" +
                MoonBridge.stringifyPortFlags(portFlags, "\n");
    }
}
