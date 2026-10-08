package com.limelight.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.R;

/**
 * Lists the choices of a setting in a panel that slides in from the right, like
 * Google TV's own settings. The current choice is ticked and gets focus first.
 */
public final class OptionPanel {
    private static final int PANEL_WIDTH_DP = 440;

    public interface OnChoiceListener {
        void onChoice(int index);
    }

    private OptionPanel() {}

    public static void show(Context context, CharSequence title, CharSequence[] choices,
                            int checkedIndex, OnChoiceListener listener) {
        if (choices == null || choices.length == 0) {
            return;
        }

        final Dialog dialog = new Dialog(context, R.style.Ariane_SidePanel);
        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(R.layout.option_panel, null);

        TextView titleView = root.findViewById(R.id.optionPanelTitle);
        titleView.setText(title);

        LinearLayout list = root.findViewById(R.id.optionPanelList);
        for (int i = 0; i < choices.length; i++) {
            TextView item = (TextView) inflater.inflate(R.layout.settings_category_item, list, false);
            item.setText(choices[i]);
            item.setActivated(i == checkedIndex);
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
                    i == checkedIndex ? R.drawable.ic_check : 0, 0);

            final int index = i;
            item.setOnClickListener(v -> {
                dialog.dismiss();
                listener.onChoice(index);
            });
            list.addView(item);
        }

        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            float density = context.getResources().getDisplayMetrics().density;
            window.setGravity(Gravity.END);
            window.setLayout((int) (PANEL_WIDTH_DP * density), ViewGroup.LayoutParams.MATCH_PARENT);
        }

        dialog.show();

        View first = list.getChildAt(checkedIndex >= 0 ? checkedIndex : 0);
        if (first != null) {
            first.requestFocus();
        }
    }
}
