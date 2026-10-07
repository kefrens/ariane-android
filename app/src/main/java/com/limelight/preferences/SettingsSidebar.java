package com.limelight.preferences;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.limelight.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Left pane of the two-pane settings used on TVs and other wide screens. It lists
 * the preference categories, and the options pane shows only the selected one.
 */
final class SettingsSidebar {
    private final LinearLayout container;
    private final List<PreferenceCategory> categories = new ArrayList<>();
    private PreferenceFragmentCompat fragment;
    private View activeItem;

    private SettingsSidebar(LinearLayout container) {
        this.container = container;

        // Moving up and down the sidebar switches category, like Google TV's settings.
        // Coming back from the options pane lands on the category on show, not on
        // whichever category happens to sit next to the option.
        container.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            if (newFocus == null || newFocus.getParent() != container) {
                return;
            }
            if (oldFocus != null && oldFocus.getParent() == container) {
                select(newFocus);
            }
            else if (activeItem != null && newFocus != activeItem) {
                final View target = activeItem;
                container.post(target::requestFocus);
            }
        });
    }

    /** Returns null when the layout has no sidebar (single-pane settings on phones). */
    static SettingsSidebar attach(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.settingsCategories);
        return container != null ? new SettingsSidebar(container) : null;
    }

    void bind(PreferenceFragmentCompat fragment) {
        this.fragment = fragment;
        container.removeAllViews();
        categories.clear();
        activeItem = null;

        PreferenceScreen screen = fragment.getPreferenceScreen();
        if (screen == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference pref = screen.getPreference(i);
            if (!(pref instanceof PreferenceCategory) || !pref.isVisible()) {
                continue;
            }

            PreferenceCategory category = (PreferenceCategory) pref;
            // The options pane has room for everything, so skip the "show more" row
            category.setInitialExpandedChildrenCount(Integer.MAX_VALUE);

            TextView item = (TextView) inflater.inflate(R.layout.settings_category_item, container, false);
            item.setText(category.getTitle());
            item.setTag(category);
            item.setOnClickListener(this::select);
            container.addView(item);
            categories.add(category);
        }

        if (container.getChildCount() > 0) {
            View first = container.getChildAt(0);
            select(first);
            first.requestFocus();
        }
    }

    private void select(View item) {
        if (item == activeItem) {
            return;
        }
        activeItem = item;

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setActivated(child == item);
        }
        for (PreferenceCategory category : categories) {
            category.setVisible(category == item.getTag());
        }

        if (fragment != null && fragment.getListView() != null) {
            fragment.getListView().scrollToPosition(0);
        }
    }
}
