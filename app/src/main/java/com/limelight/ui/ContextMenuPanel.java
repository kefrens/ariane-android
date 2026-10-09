package com.limelight.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.utils.UiClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows an activity's context menu for a host or app in the side panel instead of
 * the platform's floating menu. The activity keeps building the menu in
 * onCreateContextMenu and handling it in onContextItemSelected, as before.
 */
public final class ContextMenuPanel {
    private static final int PANEL_WIDTH_DP = 440;

    private ContextMenuPanel() {}

    /** Opens the options of the given item view of the list. */
    public static void show(Activity activity, AbsListView list, View itemView) {
        int position = list.getPositionForView(itemView);
        if (position == AdapterView.INVALID_POSITION) {
            return;
        }

        long id = list.getAdapter() != null ? list.getAdapter().getItemId(position) : position;
        AdapterView.AdapterContextMenuInfo info =
                new AdapterView.AdapterContextMenuInfo(itemView, position, id);

        PanelMenu menu = new PanelMenu(info);
        activity.onCreateContextMenu(menu, itemView, info);
        if (!menu.hasVisibleItems()) {
            activity.onContextMenuClosed(menu);
            return;
        }

        // A phone has no room for a side panel, so the menu rises from the bottom
        final boolean sheet = UiClass.of(activity) == UiClass.PHONE;
        final Dialog dialog = new Dialog(activity,
                sheet ? R.style.Ariane_BottomSheet : R.style.Ariane_SidePanel);
        LayoutInflater inflater = LayoutInflater.from(dialog.getContext());
        View root = inflater.inflate(R.layout.option_panel, null);

        TextView titleView = root.findViewById(R.id.optionPanelTitle);
        titleView.setText(menu.headerTitle);
        titleView.setVisibility(menu.headerTitle != null ? View.VISIBLE : View.GONE);

        LinearLayout listView = root.findViewById(R.id.optionPanelList);
        if (sheet) {
            float density = activity.getResources().getDisplayMetrics().density;
            root.setBackgroundResource(R.drawable.bottom_sheet_bg);
            root.setPadding((int) (16 * density), (int) (24 * density),
                    (int) (16 * density), (int) (16 * density));
            // Grow with the list instead of filling the screen
            View scroll = (View) listView.getParent();
            LinearLayout.LayoutParams scrollParams = (LinearLayout.LayoutParams) scroll.getLayoutParams();
            scrollParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            scrollParams.weight = 0;
            scroll.setLayoutParams(scrollParams);
        }
        for (final Item item : menu.sortedVisibleItems()) {
            TextView row = (TextView) inflater.inflate(R.layout.settings_category_item, listView, false);
            row.setText(item.title);
            row.setEnabled(item.enabled);
            if (item.checkable) {
                row.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
                        item.checked ? R.drawable.ic_check : 0, 0);
            }
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (item.clickListener == null || !item.clickListener.onMenuItemClick(item)) {
                    activity.onContextItemSelected(item);
                }
            });
            listView.addView(row);
        }

        // Like the platform menu, the activity hears that the menu closed after any choice
        dialog.setOnDismissListener(d -> activity.onContextMenuClosed(menu));
        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            float density = activity.getResources().getDisplayMetrics().density;
            if (sheet) {
                window.setGravity(Gravity.BOTTOM);
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                window.setGravity(Gravity.END);
                window.setLayout((int) (PANEL_WIDTH_DP * density), ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }

        dialog.show();

        View first = listView.getChildAt(0);
        if (first != null) {
            first.requestFocus();
        }
    }

    private static final class PanelMenu implements ContextMenu {
        private final ContextMenuInfo info;
        private final List<Item> items = new ArrayList<>();
        private CharSequence headerTitle;

        PanelMenu(ContextMenuInfo info) {
            this.info = info;
        }

        List<Item> sortedVisibleItems() {
            List<Item> visible = new ArrayList<>();
            for (Item item : items) {
                if (item.visible) {
                    visible.add(item);
                }
            }
            // Stable, so items with the same order keep the order they were added in
            Collections.sort(visible, (a, b) -> Integer.compare(a.order, b.order));
            return visible;
        }

        @Override public ContextMenu setHeaderTitle(int titleRes) { return setHeaderTitle(activityString(titleRes)); }
        @Override public ContextMenu setHeaderTitle(CharSequence title) { headerTitle = title; return this; }
        @Override public ContextMenu setHeaderIcon(int iconRes) { return this; }
        @Override public ContextMenu setHeaderIcon(Drawable icon) { return this; }
        @Override public ContextMenu setHeaderView(View view) { return this; }
        @Override public void clearHeader() { headerTitle = null; }

        private CharSequence activityString(int res) {
            return info instanceof AdapterView.AdapterContextMenuInfo ?
                    ((AdapterView.AdapterContextMenuInfo) info).targetView.getContext().getText(res) : null;
        }

        @Override public MenuItem add(CharSequence title) { return add(0, 0, 0, title); }
        @Override public MenuItem add(int titleRes) { return add(0, 0, 0, activityString(titleRes)); }
        @Override public MenuItem add(int groupId, int itemId, int order, int titleRes) {
            return add(groupId, itemId, order, activityString(titleRes));
        }
        @Override public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
            Item item = new Item(groupId, itemId, order, title, info);
            items.add(item);
            return item;
        }

        @Override public SubMenu addSubMenu(CharSequence title) { throw new UnsupportedOperationException(); }
        @Override public SubMenu addSubMenu(int titleRes) { throw new UnsupportedOperationException(); }
        @Override public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) { throw new UnsupportedOperationException(); }
        @Override public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) { throw new UnsupportedOperationException(); }
        @Override public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller,
                                              Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) { return 0; }

        @Override public void removeItem(int id) {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).itemId == id) {
                    items.remove(i);
                }
            }
        }
        @Override public void removeGroup(int groupId) {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).groupId == groupId) {
                    items.remove(i);
                }
            }
        }
        @Override public void clear() { items.clear(); }
        @Override public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
            for (Item item : items) { if (item.groupId == group) item.checkable = checkable; }
        }
        @Override public void setGroupVisible(int group, boolean visible) {
            for (Item item : items) { if (item.groupId == group) item.visible = visible; }
        }
        @Override public void setGroupEnabled(int group, boolean enabled) {
            for (Item item : items) { if (item.groupId == group) item.enabled = enabled; }
        }
        @Override public boolean hasVisibleItems() {
            for (Item item : items) { if (item.visible) return true; }
            return false;
        }
        @Override public MenuItem findItem(int id) {
            for (Item item : items) { if (item.itemId == id) return item; }
            return null;
        }
        @Override public int size() { return items.size(); }
        @Override public MenuItem getItem(int index) { return items.get(index); }
        @Override public void close() {}
        @Override public boolean performShortcut(int keyCode, KeyEvent event, int flags) { return false; }
        @Override public boolean isShortcutKey(int keyCode, KeyEvent event) { return false; }
        @Override public boolean performIdentifierAction(int id, int flags) { return false; }
        @Override public void setQwertyMode(boolean isQwerty) {}
    }

    private static final class Item implements MenuItem {
        final int groupId;
        final int itemId;
        final int order;
        final ContextMenu.ContextMenuInfo info;
        CharSequence title;
        CharSequence titleCondensed;
        Drawable icon;
        Intent intent;
        char numericShortcut;
        char alphabeticShortcut;
        boolean checkable;
        boolean checked;
        boolean visible = true;
        boolean enabled = true;
        OnMenuItemClickListener clickListener;

        Item(int groupId, int itemId, int order, CharSequence title, ContextMenu.ContextMenuInfo info) {
            this.groupId = groupId;
            this.itemId = itemId;
            this.order = order;
            this.title = title;
            this.info = info;
        }

        @Override public int getItemId() { return itemId; }
        @Override public int getGroupId() { return groupId; }
        @Override public int getOrder() { return order; }
        @Override public MenuItem setTitle(CharSequence title) { this.title = title; return this; }
        @Override public MenuItem setTitle(int title) { return this; }
        @Override public CharSequence getTitle() { return title; }
        @Override public MenuItem setTitleCondensed(CharSequence title) { titleCondensed = title; return this; }
        @Override public CharSequence getTitleCondensed() { return titleCondensed != null ? titleCondensed : title; }
        @Override public MenuItem setIcon(Drawable icon) { this.icon = icon; return this; }
        @Override public MenuItem setIcon(int iconRes) { return this; }
        @Override public Drawable getIcon() { return icon; }
        @Override public MenuItem setIntent(Intent intent) { this.intent = intent; return this; }
        @Override public Intent getIntent() { return intent; }
        @Override public MenuItem setShortcut(char numericChar, char alphaChar) {
            numericShortcut = numericChar;
            alphabeticShortcut = alphaChar;
            return this;
        }
        @Override public MenuItem setNumericShortcut(char numericChar) { numericShortcut = numericChar; return this; }
        @Override public char getNumericShortcut() { return numericShortcut; }
        @Override public MenuItem setAlphabeticShortcut(char alphaChar) { alphabeticShortcut = alphaChar; return this; }
        @Override public char getAlphabeticShortcut() { return alphabeticShortcut; }
        @Override public MenuItem setCheckable(boolean checkable) { this.checkable = checkable; return this; }
        @Override public boolean isCheckable() { return checkable; }
        @Override public MenuItem setChecked(boolean checked) { this.checked = checked; return this; }
        @Override public boolean isChecked() { return checked; }
        @Override public MenuItem setVisible(boolean visible) { this.visible = visible; return this; }
        @Override public boolean isVisible() { return visible; }
        @Override public MenuItem setEnabled(boolean enabled) { this.enabled = enabled; return this; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public boolean hasSubMenu() { return false; }
        @Override public SubMenu getSubMenu() { return null; }
        @Override public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener listener) { clickListener = listener; return this; }
        @Override public ContextMenu.ContextMenuInfo getMenuInfo() { return info; }
        @Override public void setShowAsAction(int actionEnum) {}
        @Override public MenuItem setShowAsActionFlags(int actionEnum) { return this; }
        @Override public MenuItem setActionView(View view) { return this; }
        @Override public MenuItem setActionView(int resId) { return this; }
        @Override public View getActionView() { return null; }
        @Override public MenuItem setActionProvider(ActionProvider actionProvider) { return this; }
        @Override public ActionProvider getActionProvider() { return null; }
        @Override public boolean expandActionView() { return false; }
        @Override public boolean collapseActionView() { return false; }
        @Override public boolean isActionViewExpanded() { return false; }
        @Override public MenuItem setOnActionExpandListener(OnActionExpandListener listener) { return this; }
    }
}
