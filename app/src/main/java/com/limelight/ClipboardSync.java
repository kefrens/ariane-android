package com.limelight;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.PersistableBundle;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.limelight.nvstream.http.NvHTTP;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Copies text between the device clipboard and the host's while streaming.
 * Clips this class writes are tagged, so they are never sent straight back.
 */
class ClipboardSync {
    static final String CLIPBOARD_IDENTIFIER = "ArtemisStreaming";

    private final Activity activity;
    private final ClipboardManager clipboardManager;
    private final PreferenceConfiguration prefConfig;
    private volatile boolean fetchRunning = false;

    ClipboardSync(Activity activity, ClipboardManager clipboardManager, PreferenceConfiguration prefConfig) {
        this.activity = activity;
        this.clipboardManager = clipboardManager;
        this.prefConfig = prefConfig;
    }

    /** Sends the device clipboard to the host. Returns false if there was nothing to send. */
    boolean send(NvHTTP httpConn, boolean force) {
        String clipboardText = getClipboardContent(force);
        if (clipboardText == null) {
            return false;
        }

        new Thread() {
            public void run() {
                try {
                    if (!httpConn.sendClipboard(clipboardText)) {
                        toast(activity.getString(R.string.clipboard_sync_unsupported));
                    } else {
                        toast(activity.getString(R.string.send_clipboard_success));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    toast(activity.getString(R.string.send_clipboard_failed) + e.getMessage());
                }
            }
        }.start();

        return true;
    }

    /** Copies the host clipboard to the device after the given delay in milliseconds. */
    void fetch(NvHTTP httpConn, int delay) {
        new Thread() {
            public void run() {
                if (fetchRunning) {
                    return;
                }

                fetchRunning = true;
                try {
                    if (delay > 0) {
                        sleep(delay);
                    }
                    String clipboardContent = httpConn.getClipboard();
                    ClipData clipData = ClipData.newPlainText(CLIPBOARD_IDENTIFIER, clipboardContent);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ClipDescription clipDescription = clipData.getDescription();
                        PersistableBundle newExtras = new PersistableBundle();
                        newExtras.putBoolean(CLIPBOARD_IDENTIFIER, true);
                        if (prefConfig.hideClipboardContent) {
                            // We don't know if the message is sensitive or not, to be safe mark them all as sensitive.
                            newExtras.putBoolean("android.content.extra.IS_SENSITIVE", true);
                        }
                        clipDescription.setExtras(newExtras);
                    }

                    clipboardManager.setPrimaryClip(clipData);
                    toast(activity.getString(R.string.get_clipboard_success));
                } catch (Exception e) {
                    e.printStackTrace();
                    toast(activity.getString(R.string.get_clipboard_failed) + e.getMessage());
                }
                fetchRunning = false;
            }
        }.start();
    }

    private void toast(String message) {
        if (prefConfig.smartClipboardSyncToast) {
            activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
        }
    }

    private String getClipboardContent(boolean force) {
        // Check if there is any clipboard data
        if (clipboardManager.hasPrimaryClip()) {
            ClipDescription clipDescription = clipboardManager.getPrimaryClipDescription();
            if (!force && clipDescription != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    PersistableBundle extras = clipDescription.getExtras();
                    if (extras != null && extras.getBoolean(CLIPBOARD_IDENTIFIER)) {
                        // We're getting the clipboard data we just set/read a while ago
                        return null;
                    }
                } else {
                    CharSequence clipLabel = clipDescription.getLabel();
                    if (clipLabel != null && clipLabel.equals(CLIPBOARD_IDENTIFIER)) {
                        // We're getting the clipboard data we set a while ago
                        return null;
                    }
                }
            }

            ClipData clipData = clipboardManager.getPrimaryClip();

            if (clipData != null && clipData.getItemCount() > 0) {
                // Get the first item from the clipboard data
                ClipData.Item item = clipData.getItemAt(0);

                // Mark the clip as visited
                if (clipDescription != null) {
                    ClipData clonedClip = cloneClipData(clipDescription, item);
                    clipboardManager.setPrimaryClip(clonedClip);
                }

                // Get the text data from the clipboard item
                CharSequence clipText = item.getText();
                if (clipText == null) {
                    return null;
                }
                return clipText.toString();
            }
        }

        return null;
    }

    private static @NonNull ClipData cloneClipData(ClipDescription clipDescription, ClipData.Item item) {
        ClipDescription clonedDescription = new ClipDescription(clipDescription);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PersistableBundle extras = clipDescription.getExtras();
            if (extras == null) {
                extras = new PersistableBundle();
            }
            extras.putBoolean(CLIPBOARD_IDENTIFIER, true);
            clonedDescription.setExtras(extras);
        }

        return new ClipData(clonedDescription, item);
    }
}
