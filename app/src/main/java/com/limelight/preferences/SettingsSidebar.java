package com.limelight.preferences;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.limelight.R;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Left pane of the two-pane settings used on TVs and other wide screens. It lists
 * the preference categories, and the options pane shows only the selected one.
 * The search field above the categories shows matching options from every category.
 */
final class SettingsSidebar {
    private final LinearLayout container;
    private final EditText searchField;
    private final TextView searchEmpty;
    private final List<PreferenceCategory> categories = new ArrayList<>();
    // Visibility of each option before a search, so clearing it restores the options
    // the settings screen hides on purpose (unsupported on this device, for example)
    private final Map<Preference, Boolean> visibleBeforeSearch = new HashMap<>();
    private PreferenceFragmentCompat fragment;
    private View activeItem;

    private SettingsSidebar(LinearLayout container, EditText searchField, TextView searchEmpty) {
        this.container = container;
        this.searchField = searchField;
        this.searchEmpty = searchEmpty;

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

        if (searchField != null) {
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    search(s.toString());
                }
            });
            // The keyboard's search key jumps straight to the first result
            searchField.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    if (fragment != null && fragment.getListView() != null) {
                        fragment.getListView().requestFocus();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    /** Returns null when the layout has no sidebar (single-pane settings on phones). */
    static SettingsSidebar attach(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.settingsCategories);
        return container != null ? new SettingsSidebar(container,
                activity.findViewById(R.id.settingsSearch),
                activity.findViewById(R.id.settingsSearchEmpty)) : null;
    }

    void bind(PreferenceFragmentCompat fragment) {
        this.fragment = fragment;
        container.removeAllViews();
        categories.clear();
        visibleBeforeSearch.clear();
        activeItem = null;
        if (searchField != null && searchField.length() > 0) {
            searchField.setText("");
        }

        PreferenceScreen screen = fragment.getPreferenceScreen();
        if (screen == null) {
            return;
        }

        // Rebind rows in place when a value changes, so focus stays on the row
        if (fragment.getListView() != null) {
            fragment.getListView().setItemAnimator(null);
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
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(iconFor(category.getKey()), 0, 0, 0);
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

    private static int iconFor(String categoryKey) {
        if (categoryKey == null) {
            return R.drawable.ic_settings_more;
        }
        switch (categoryKey) {
            case "category_video_settings":
                return R.drawable.ic_settings_video;
            case "category_audio_settings":
                return R.drawable.ic_settings_audio;
            case "category_gamepad_settings":
                return R.drawable.ic_settings_gamepad;
            case "category_input_settings":
                return R.drawable.ic_settings_mouse;
            case "category_host_settings":
                return R.drawable.ic_settings_host;
            case "category_general_settings":
                return R.drawable.ic_settings_general;
            case "category_ui_settings":
                return R.drawable.ic_settings_appearance;
            case "category_onscreen_controls":
            case "category_virtual_trackpad_settings":
                return R.drawable.ic_settings_touch;
            case "category_special_key_layout":
                return R.drawable.ic_settings_keys;
            case "category_perf_monitor_settings":
                return R.drawable.ic_settings_stats;
            case "category_advanced_settings":
                return R.drawable.ic_settings_advanced;
            default:
                return R.drawable.ic_settings_more;
        }
    }

    private void select(View item) {
        // Picking a category leaves the search
        if (searchField != null && searchField.length() > 0) {
            // Clearing the search re-selects the active item, so make that this one
            activeItem = item;
            searchField.setText("");
            return;
        }
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

    private void search(String query) {
        String needle = normalize(query.trim());

        if (needle.isEmpty()) {
            if (!visibleBeforeSearch.isEmpty()) {
                for (Map.Entry<Preference, Boolean> entry : visibleBeforeSearch.entrySet()) {
                    entry.getKey().setVisible(entry.getValue());
                }
                visibleBeforeSearch.clear();
            }
            if (searchEmpty != null) {
                searchEmpty.setVisibility(View.GONE);
            }
            // Back to showing only the category that was on show
            View previous = activeItem;
            activeItem = null;
            if (previous != null) {
                select(previous);
            } else if (container.getChildCount() > 0) {
                select(container.getChildAt(0));
            }
            return;
        }

        boolean anyMatch = false;
        for (PreferenceCategory category : categories) {
            boolean categoryMatches = filter(category, needle);
            category.setVisible(categoryMatches);
            anyMatch |= categoryMatches;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setActivated(false);
        }

        if (searchEmpty != null) {
            searchEmpty.setText(container.getContext().getString(R.string.settings_search_empty, query.trim()));
            searchEmpty.setVisibility(anyMatch ? View.GONE : View.VISIBLE);
        }
        if (fragment != null && fragment.getListView() != null) {
            fragment.getListView().scrollToPosition(0);
        }
    }

    // Shows the options of the group that match, and returns whether any did
    private boolean filter(PreferenceGroup group, String needle) {
        boolean any = false;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (!visibleBeforeSearch.containsKey(pref)) {
                visibleBeforeSearch.put(pref, pref.isVisible());
            }
            if (!visibleBeforeSearch.get(pref)) {
                continue;
            }

            boolean matches;
            if (pref instanceof PreferenceGroup) {
                matches = filter((PreferenceGroup) pref, needle);
            } else {
                matches = contains(pref.getTitle(), needle) || contains(pref.getSummary(), needle);
            }
            pref.setVisible(matches);
            any |= matches;
        }
        return any;
    }

    private static boolean contains(CharSequence text, String needle) {
        return text != null && normalize(text.toString()).contains(needle);
    }

    // Lower case without accents, so "video" finds "Vidéo"
    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
