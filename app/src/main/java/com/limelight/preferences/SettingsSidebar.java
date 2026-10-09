package com.limelight.preferences;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 * A column of section icons next to the options jumps to a section of the category.
 *
 * On a phone the same logic drives a drill-down instead: the category list fills the screen,
 * picking a category (or searching) shows its options, and the back button returns to the list.
 */
final class SettingsSidebar {
    private final LinearLayout container;
    private final EditText searchField;
    private final TextView searchEmpty;
    // Section shortcuts, null on layouts without them
    private final LinearLayout jumpContainer;
    private final List<PreferenceCategory> jumpSections = new ArrayList<>();
    private RecyclerView boundList;
    private final List<PreferenceCategory> categories = new ArrayList<>();
    // Visibility of each option before a search, so clearing it restores the options
    // the settings screen hides on purpose (unsupported on this device, for example)
    private final Map<Preference, Boolean> visibleBeforeSearch = new HashMap<>();
    private PreferenceFragmentCompat fragment;
    private View activeItem;

    // Phone drill-down: the category list and the options take turns on the screen.
    // All null on the wide layouts, where both are always shown.
    private final View listPane;
    private final View detailPane;
    private final View backButton;
    private final TextView titleView;
    private final boolean drillDown;

    private SettingsSidebar(LinearLayout container, EditText searchField, TextView searchEmpty,
                            LinearLayout jumpContainer, View listPane, View detailPane,
                            View backButton, TextView titleView) {
        this.container = container;
        this.searchField = searchField;
        this.searchEmpty = searchEmpty;
        this.jumpContainer = jumpContainer;
        this.listPane = listPane;
        this.detailPane = detailPane;
        this.backButton = backButton;
        this.titleView = titleView;
        this.drillDown = listPane != null && detailPane != null;
        if (backButton != null) {
            backButton.setOnClickListener(v -> handleBack());
        }

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

    /** Returns null when the layout has no category list (the bare single-pane settings). */
    static SettingsSidebar attach(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.settingsCategories);
        return container != null ? new SettingsSidebar(container,
                activity.findViewById(R.id.settingsSearch),
                activity.findViewById(R.id.settingsSearchEmpty),
                activity.findViewById(R.id.settingsSections),
                activity.findViewById(R.id.settingsListPane),
                activity.findViewById(R.id.settingsDetailPane),
                activity.findViewById(R.id.settingsBack),
                activity.findViewById(R.id.settingsTitle)) : null;
    }

    /**
     * Steps back inside the drill-down: out of a search, then from a category to the list.
     * Returns false when there is nowhere to step back to, so the screen should close.
     */
    boolean handleBack() {
        if (!drillDown) {
            return false;
        }
        if (searchField != null && searchField.length() > 0) {
            searchField.setText("");
            return true;
        }
        if (activeItem != null) {
            showList();
            return true;
        }
        return false;
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
            if (boundList != fragment.getListView()) {
                boundList = fragment.getListView();
                boundList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        markCurrentSection();
                    }
                });
            }
        }

        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference pref = screen.getPreference(i);
            if (!(pref instanceof PreferenceCategory) || !pref.isVisible()) {
                continue;
            }

            PreferenceCategory category = (PreferenceCategory) pref;
            // The options pane has room for everything, so skip the "show more" rows
            expandAll(category);

            View item;
            if (drillDown) {
                item = inflateCategoryRow(inflater, category);
            } else {
                TextView row = (TextView) inflater.inflate(R.layout.settings_category_item, container, false);
                row.setText(category.getTitle());
                row.setCompoundDrawablesRelativeWithIntrinsicBounds(iconFor(category.getKey()), 0, 0, 0);
                item = row;
            }
            item.setTag(category);
            item.setOnClickListener(this::select);
            container.addView(item);
            categories.add(category);
        }

        if (drillDown) {
            // Start on the list of categories
            showList();
        } else if (container.getChildCount() > 0) {
            View first = container.getChildAt(0);
            select(first);
            first.requestFocus();
        }
    }

    // Phone row: coloured icon, the category's name, and the sections inside it
    private View inflateCategoryRow(LayoutInflater inflater, PreferenceCategory category) {
        Context context = container.getContext();
        View row = inflater.inflate(R.layout.settings_category_row, container, false);
        ((TextView) row.findViewById(R.id.settingsRowTitle)).setText(category.getTitle());

        List<PreferenceCategory> sections = SettingsSections.sectionsOf(category);
        StringBuilder names = new StringBuilder();
        for (PreferenceCategory section : sections) {
            if (names.length() > 0) {
                names.append(" · ");
            }
            names.append(section.getTitle());
        }
        TextView summary = row.findViewById(R.id.settingsRowSummary);
        summary.setText(names);
        summary.setVisibility(names.length() > 0 ? View.VISIBLE : View.GONE);

        // The icon takes the colour of the category's first section
        int color = R.color.section_blue;
        if (!sections.isEmpty()) {
            int[] icon = SettingsSections.iconOf(sections.get(0).getKey());
            if (icon != null) {
                color = icon[1];
            }
        }
        ((ImageView) row.findViewById(R.id.settingsRowIcon)).setImageDrawable(
                SettingsSections.tinted(context, iconFor(category.getKey()), color));
        return row;
    }

    // Phone: the list of categories fills the screen
    private void showList() {
        activeItem = null;
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setActivated(false);
        }
        showSections(null);
        listPane.setVisibility(View.VISIBLE);
        detailPane.setVisibility(View.GONE);
        if (backButton != null) {
            backButton.setVisibility(View.GONE);
        }
        if (titleView != null) {
            titleView.setText(R.string.settings);
        }
        if (searchEmpty != null) {
            searchEmpty.setVisibility(View.GONE);
        }
        hideKeyboard();
    }

    // Phone: the options of one category, or the search results, fill the screen
    private void showDetail(CharSequence title) {
        listPane.setVisibility(View.GONE);
        detailPane.setVisibility(View.VISIBLE);
        if (backButton != null) {
            backButton.setVisibility(View.VISIBLE);
        }
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    private void hideKeyboard() {
        if (searchField != null && searchField.hasFocus()) {
            searchField.clearFocus();
            InputMethodManager imm = (InputMethodManager)
                    searchField.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
            }
        }
    }

    private static void expandAll(PreferenceGroup group) {
        group.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference child = group.getPreference(i);
            if (child instanceof PreferenceGroup) {
                expandAll((PreferenceGroup) child);
            }
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

    /** Position of the category on show in the category list, or -1 if there is none. */
    int selectedIndex() {
        return activeItem == null ? -1 : container.indexOfChild(activeItem);
    }

    void selectIndex(int index) {
        if (index >= 0 && index < container.getChildCount()) {
            select(container.getChildAt(index));
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
        showSections((PreferenceCategory) item.getTag());
        if (drillDown) {
            showDetail(((PreferenceCategory) item.getTag()).getTitle());
            hideKeyboard();
        }

        if (fragment != null && fragment.getListView() != null) {
            fragment.getListView().scrollToPosition(0);
        }
    }

    // Fills the shortcut column with the sections of the category on show
    private void showSections(PreferenceCategory category) {
        if (jumpContainer == null) {
            return;
        }
        jumpContainer.removeAllViews();
        jumpSections.clear();
        if (category != null) {
            jumpSections.addAll(SettingsSections.sectionsOf(category));
        }

        boolean show = jumpSections.size() >= 2;
        ((View) jumpContainer.getParent()).setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            jumpSections.clear();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(jumpContainer.getContext());
        boolean chips = jumpContainer.getOrientation() == LinearLayout.HORIZONTAL;
        for (PreferenceCategory section : jumpSections) {
            int[] icon = SettingsSections.iconOf(section.getKey());
            View item;
            if (chips) {
                TextView chip = (TextView) inflater.inflate(R.layout.settings_section_chip, jumpContainer, false);
                chip.setText(section.getTitle());
                if (icon != null) {
                    chip.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            SettingsSections.tinted(chip.getContext(), icon[0], icon[1]), null, null, null);
                }
                item = chip;
            } else {
                ImageView image = (ImageView) inflater.inflate(R.layout.settings_section_jump_item, jumpContainer, false);
                if (icon != null) {
                    image.setImageDrawable(SettingsSections.tinted(image.getContext(), icon[0], icon[1]));
                }
                item = image;
            }
            item.setContentDescription(item.getContext().getString(R.string.section_jump_to, section.getTitle()));
            item.setOnClickListener(v -> scrollTo(section));
            jumpContainer.addView(item);
        }
        jumpContainer.getChildAt(0).setActivated(true);
    }

    private void scrollTo(PreferenceCategory section) {
        RecyclerView list = fragment != null ? fragment.getListView() : null;
        if (list == null || !(list.getAdapter() instanceof PreferenceGroup.PreferencePositionCallback)) {
            return;
        }
        int position = ((PreferenceGroup.PreferencePositionCallback) list.getAdapter())
                .getPreferenceAdapterPosition(section);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (list.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(position, 0);
        } else {
            list.scrollToPosition(position);
        }
        list.post(this::markCurrentSection);
    }

    // Highlights the shortcut of the section at the top of the options
    private void markCurrentSection() {
        RecyclerView list = fragment != null ? fragment.getListView() : null;
        if (jumpContainer == null || jumpSections.isEmpty() || list == null ||
                !(list.getLayoutManager() instanceof LinearLayoutManager) ||
                !(list.getAdapter() instanceof PreferenceGroup.PreferencePositionCallback)) {
            return;
        }
        LinearLayoutManager layout = (LinearLayoutManager) list.getLayoutManager();
        PreferenceGroup.PreferencePositionCallback positions =
                (PreferenceGroup.PreferencePositionCallback) list.getAdapter();
        int top = layout.findFirstVisibleItemPosition();
        // At the very end of the list the last section may never reach the top
        boolean atEnd = !list.canScrollVertically(1);

        int current = 0;
        for (int i = 0; i < jumpSections.size(); i++) {
            int position = positions.getPreferenceAdapterPosition(jumpSections.get(i));
            if (position == RecyclerView.NO_POSITION) {
                continue;
            }
            if (position <= top || (atEnd && position <= layout.findLastVisibleItemPosition()
                    && i == jumpSections.size() - 1)) {
                current = i;
            }
        }
        for (int i = 0; i < jumpContainer.getChildCount(); i++) {
            jumpContainer.getChildAt(i).setActivated(i == current);
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
            } else if (drillDown) {
                showList();
            } else if (container.getChildCount() > 0) {
                select(container.getChildAt(0));
            }
            return;
        }

        showSections(null);
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
        if (drillDown) {
            showDetail(container.getContext().getString(R.string.settings_search_results));
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
