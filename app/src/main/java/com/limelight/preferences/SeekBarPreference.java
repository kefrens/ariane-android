package com.limelight.preferences;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;

import java.util.Locale;

/**
 * A slider setting. The row shows the current value between arrows, and with a remote
 * Left and Right change it in place. OK opens a side panel with a larger slider.
 */
public class SeekBarPreference extends Preference
{
    private static final String ANDROID_SCHEMA_URL = "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR_SCHEMA_URL = "http://schemas.moonlight-stream.com/apk/res/seekbar";

    private static final int PANEL_WIDTH_DP = 440;

    private SeekBar seekBar;
    private final Context context;

    private final String dialogMessage;
    private final String suffix;
    private final int defaultValue;
    private final int maxValue;
    private final int minValue;
    private final int stepSize;
    private final int keyStepSize;
    private final int divisor;
    private int currentValue;

    private final int seekbarMax;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Read the message from XML
        int dialogMessageId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "dialogMessage", 0);
        if (dialogMessageId == 0) {
            dialogMessage = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "dialogMessage");
        }
        else {
            dialogMessage = context.getString(dialogMessageId);
        }

        // Get the suffix for the number displayed in the dialog
        int suffixId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "text", 0);
        if (suffixId == 0) {
            suffix = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "text");
        }
        else {
            suffix = context.getString(suffixId);
        }

        // Get default, min, and max seekbar values
        defaultValue = attrs.getAttributeIntValue(ANDROID_SCHEMA_URL, "defaultValue", PreferenceConfiguration.getDefaultBitrate(context));
        maxValue = attrs.getAttributeIntValue(ANDROID_SCHEMA_URL, "max", 100);
        minValue = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "min", 1);
        stepSize = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "step", 1);
        divisor = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "divisor", 1);
        keyStepSize = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "keyStep", 0);
        seekbarMax = maxValue - minValue;

        setWidgetLayoutResource(R.layout.preference_widget_inline_value);
    }

    // "80.0 Mbps", "100%": the value as the row and the panel show it
    private String format(int value) {
        String t;
        if (divisor != 1) {
            t = String.format((Locale) null, "%.1f", value / (float) divisor);
        }
        else {
            t = String.valueOf(value);
        }
        return suffix == null ? t : t.concat(suffix.length() > 1 ? " " + suffix : suffix);
    }

    private int clampToStep(int value) {
        int rounded = Math.round((float) value / stepSize) * stepSize;
        return Math.max(minValue, Math.min(maxValue, rounded));
    }

    private void saveValue(int value) {
        currentValue = value;
        if (shouldPersist()) {
            persistInt(currentValue);
            callChangeListener(currentValue);
        }
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView valueView = (TextView) holder.findViewById(R.id.inline_value);
        if (valueView != null) {
            valueView.setText(format(currentValue));
        }

        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN || !isEnabled()) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                return step(-1);
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                return step(1);
            }
            return false;
        });
    }

    // Moves one key step. At either end the key is left alone, so Left on the
    // lowest value still moves focus back to the categories.
    private boolean step(int direction) {
        int increment = keyStepSize != 0 ? keyStepSize : stepSize;
        int next = clampToStep(currentValue + direction * increment);
        if (next == currentValue) {
            return false;
        }
        saveValue(next);
        return true;
    }

    public void showDialog() {
        final Dialog dialog = new Dialog(context, R.style.Ariane_SidePanel);
        View root = LayoutInflater.from(dialog.getContext()).inflate(R.layout.seekbar_panel, null);

        ((TextView) root.findViewById(R.id.seekPanelTitle)).setText(getTitle());
        TextView message = root.findViewById(R.id.seekPanelMessage);
        if (dialogMessage != null) {
            message.setText(dialogMessage);
        }
        else {
            message.setVisibility(View.GONE);
        }

        final TextView valueText = root.findViewById(R.id.seekPanelValue);
        seekBar = root.findViewById(R.id.seekPanelBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = clampToStep(progress + minValue);
                if (value - minValue != progress) {
                    seekBar.setProgress(value - minValue);
                    return;
                }
                valueText.setText(format(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (shouldPersist()) {
            currentValue = getPersistedInt(defaultValue);
        }
        updateSeekbar();
        valueText.setText(format(clampToStep(currentValue)));

        Runnable save = () -> {
            saveValue(seekBar.getProgress() + minValue);
            dialog.dismiss();
        };
        // OK on the slider saves, like the hint says
        seekBar.setOnKeyListener((v, keyCode, event) -> {
            if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_BUTTON_A) && event.getAction() == KeyEvent.ACTION_UP) {
                save.run();
                return true;
            }
            return false;
        });
        root.findViewById(R.id.seekPanelSave).setOnClickListener(v -> save.run());
        root.findViewById(R.id.seekPanelCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            float density = context.getResources().getDisplayMetrics().density;
            window.setGravity(Gravity.END);
            window.setLayout((int) (PANEL_WIDTH_DP * density), ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        seekBar.requestFocus();
    }

    protected void updateSeekbar() {
        seekBar.setMax(seekbarMax);
        if (keyStepSize != 0) {
            seekBar.setKeyProgressIncrement(keyStepSize);
        }
        seekBar.setProgress(currentValue - minValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            currentValue = shouldPersist() ? getPersistedInt(this.defaultValue) : 0;
        }
        else {
            currentValue = (Integer) defaultValue;
        }
    }

    public void setProgress(int progress) {
        this.currentValue = progress;
        if (seekBar != null) {
            seekBar.setProgress(progress - minValue);
        }
        notifyChanged();
    }
    public int getProgress() {
        return currentValue + minValue;
    }

    @Override
    protected void onClick() {
        super.onClick();
        showDialog();
    }
}
