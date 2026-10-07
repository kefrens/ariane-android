package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;
import com.limelight.ui.OptionPanel;

/**
 * A list setting that shows its current value at the end of the row. With a remote,
 * Left and Right step through the values in place; OK still opens the full list.
 */
public class InlineListPreference extends ListPreference {
    public InlineListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public InlineListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public InlineListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InlineListPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_widget_inline_value);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView valueView = (TextView) holder.findViewById(R.id.inline_value);
        if (valueView != null) {
            CharSequence entry = getEntry();
            valueView.setText(entry != null ? entry : "");
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

    @Override
    protected void onClick() {
        // OK opens the choices in a side panel instead of the old centred dialog
        OptionPanel.show(getContext(), getTitle(), getEntries(), findIndexOfValue(getValue()),
                index -> setValueFromUser(getEntryValues()[index].toString()));
    }

    private void setValueFromUser(String newValue) {
        if (callChangeListener(newValue)) {
            setValue(newValue);
        }
    }

    // Moves to the previous or next value. At either end the key is left alone, so
    // Left on the first value still moves focus back to the categories.
    private boolean step(int direction) {
        CharSequence[] values = getEntryValues();
        if (values == null || values.length == 0) {
            return false;
        }

        int next = findIndexOfValue(getValue()) + direction;
        if (next < 0 || next >= values.length) {
            return false;
        }

        setValueFromUser(values[next].toString());
        return true;
    }
}
