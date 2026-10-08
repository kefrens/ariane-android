package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.utils.KeyConfigHelper;
import com.limelight.utils.KeyMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provide options for ongoing Game Stream.
 * <p>
 * Shown on back action in game activity, as a panel that slides in from the right.
 * Submenus replace the panel's content, and Back steps back up to the parent menu.
 */
public class GameMenu implements Game.GameMenuCallbacks {

    public static final long KEY_UP_DELAY = 25;
    private static final long TEST_GAME_FOCUS_DELAY = 10;

    public static final String PREF_NAME = "specialPrefs"; // SharedPreferences的名称

    public static final String KEY_NAME = "special_key"; // 要保存的键名称

    private static final int PANEL_WIDTH_DP = 440;

    public static class MenuOption {
        private final String label;
        private final boolean withGameFocus;
        private final Runnable runnable;
        private int iconRes;
        private boolean opensSubmenu;
        private Toggle toggle;
        private String value;

        public MenuOption(String label, boolean withGameFocus, Runnable runnable) {
            this.label = label;
            this.withGameFocus = withGameFocus;
            this.runnable = runnable;
        }

        public MenuOption(String label, Runnable runnable) {
            this(label, false, runnable);
        }

        public MenuOption icon(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        // The option shows another menu in the same panel instead of closing it
        public MenuOption submenu() {
            this.opensSubmenu = true;
            return this;
        }

        // The option is an on/off switch: it stays in the panel and redraws its switch
        public MenuOption toggle(Toggle toggle) {
            this.toggle = toggle;
            return this;
        }

        // The current choice, shown at the end of the row
        public MenuOption value(String value) {
            this.value = value;
            return this;
        }
    }

    public interface Toggle {
        boolean isOn();
    }

    private static class KeyChip {
        final String label;
        final short[] keys;

        KeyChip(String label, short[] keys) {
            this.label = label;
            this.keys = keys;
        }
    }

    private final Game game;
    private final Context dialogScreenContext;

    private Dialog currentDialog;
    private TextView panelTitle;
    private TextView panelSubtitle;
    private LinearLayout panelList;
    private View panelKeysSection;
    private ViewGroup panelKeys;
    private View panelFooter;
    private ScrollView panelScroll;
    private Runnable panelBack;
    private GameInputDevice menuDevice;

    public GameMenu(Game game, Context dialogScreenContext) {
        this.game = game;
        this.dialogScreenContext = dialogScreenContext;
    }

    public GameMenu(Game game) {
        this.game = game;
        this.dialogScreenContext = game;
    }

    private String getString(int id) {
        return game.getResources().getString(id);
    }


    private void sendKeys(short[] keys) {
        game.sendKeys(keys);
    }

    private void runWithGameFocus(Runnable runnable) {
        // Ensure that the Game activity is still active (not finished)
        if (game.isFinishing()) {
            return;
        }
        // Check if the game window has focus again, if not try again after delay
        if (!game.hasWindowFocus() && dialogScreenContext instanceof Game) {
            new Handler().postDelayed(() -> runWithGameFocus(runnable), TEST_GAME_FOCUS_DELAY);
            return;
        }
        // Game Activity has focus, run runnable
        runnable.run();
    }

    private void run(MenuOption option) {
        if (option.runnable == null) {
            return;
        }

        if (option.withGameFocus) {
            runWithGameFocus(option.runnable);
        } else {
            option.runnable.run();
        }
    }

    private boolean hasTouchscreen() {
        return game.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    private void showMenuDialog(String title, MenuOption[] options, Runnable onBack) {
        showMenuDialog(title, getString(R.string.game_panel_back_up), options, onBack, null);
    }

    // keyChips is only given for the top menu, which also gets the Disconnect and Quit buttons
    private void showMenuDialog(String title, String subtitle, MenuOption[] options, Runnable onBack,
                                KeyChip[] keyChips) {
        if (currentDialog == null || !currentDialog.isShowing()) {
            createPanel();
        }

        panelBack = onBack;
        panelTitle.setText(title);
        panelSubtitle.setText(subtitle);

        LayoutInflater inflater = LayoutInflater.from(currentDialog.getContext());
        panelList.removeAllViews();
        for (MenuOption option : options) {
            View item = inflater.inflate(R.layout.game_panel_item, panelList, false);
            bindItem(item, option);
            item.setOnClickListener(v -> {
                if (option.toggle != null) {
                    run(option);
                    bindItem(item, option);
                } else if (option.opensSubmenu) {
                    if (option.runnable != null) {
                        option.runnable.run();
                    }
                } else {
                    hideMenu();
                    run(option);
                }
            });
            panelList.addView(item);
        }

        panelKeys.removeAllViews();
        if (keyChips != null) {
            for (KeyChip chip : keyChips) {
                TextView view = (TextView) inflater.inflate(R.layout.game_panel_chip, panelKeys, false);
                view.setText(chip.label);
                view.setOnClickListener(v -> {
                    if (chip.keys == null) {
                        showSpecialKeysMenu();
                    } else {
                        hideMenu();
                        sendKeys(chip.keys);
                    }
                });
                panelKeys.addView(view);
            }
        }
        panelKeysSection.setVisibility(keyChips != null ? View.VISIBLE : View.GONE);
        panelFooter.setVisibility(keyChips != null ? View.VISIBLE : View.GONE);

        panelScroll.scrollTo(0, 0);
        View first = panelList.getChildAt(0);
        if (first != null) {
            first.requestFocus();
        }
    }

    private void bindItem(View item, MenuOption option) {
        ImageView icon = item.findViewById(R.id.panelItemIcon);
        TextView label = item.findViewById(R.id.panelItemLabel);
        TextView value = item.findViewById(R.id.panelItemValue);
        ImageView end = item.findViewById(R.id.panelItemEnd);

        if (option.iconRes != 0) {
            icon.setImageResource(option.iconRes);
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }
        label.setText(option.label);

        value.setText(option.value);
        value.setVisibility(option.value != null ? View.VISIBLE : View.GONE);

        int endRes = 0;
        if (option.toggle != null) {
            boolean on = option.toggle.isOn();
            endRes = on ? R.drawable.panel_switch_on : R.drawable.panel_switch_off;
            item.setContentDescription(option.label + ", " + getString(on ? R.string.yes : R.string.no));
        } else if (option.opensSubmenu) {
            endRes = R.drawable.ic_chevron_right;
        }
        if (endRes != 0) {
            end.setImageResource(endRes);
            end.setVisibility(View.VISIBLE);
        } else {
            end.setVisibility(View.GONE);
        }
    }

    private void createPanel() {
        final Dialog dialog = new Dialog(dialogScreenContext, R.style.Ariane_SidePanel);
        View root = LayoutInflater.from(dialog.getContext()).inflate(R.layout.game_panel, null);
        panelTitle = root.findViewById(R.id.gamePanelTitle);
        panelSubtitle = root.findViewById(R.id.gamePanelSubtitle);
        panelList = root.findViewById(R.id.gamePanelList);
        panelScroll = root.findViewById(R.id.gamePanelScroll);
        panelKeysSection = root.findViewById(R.id.gamePanelKeysSection);
        panelKeys = root.findViewById(R.id.gamePanelKeys);
        panelFooter = root.findViewById(R.id.gamePanelFooter);
        root.findViewById(R.id.gamePanelDisconnect).setOnClickListener(v -> {
            hideMenu();
            game.disconnect();
        });
        root.findViewById(R.id.gamePanelQuit).setOnClickListener(v -> {
            hideMenu();
            game.quit();
        });
        dialog.setContentView(root);

        // Back (or B on a gamepad) steps up to the parent menu, and closes the top one
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_BACK || panelBack == null) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                panelBack.run();
            }
            return true;
        });
        dialog.setOnCancelListener(d -> hideMenu());

        Window window = dialog.getWindow();
        if (window != null) {
            float density = dialogScreenContext.getResources().getDisplayMetrics().density;
            window.setGravity(Gravity.END);
            window.setLayout((int) (PANEL_WIDTH_DP * density), ViewGroup.LayoutParams.MATCH_PARENT);
        }

        currentDialog = dialog;
        dialog.show();
    }

    private void showSpecialKeysMenu() {
        List<MenuOption> options = new ArrayList<>();

        if(!PreferenceConfiguration.readPreferences(game).disableDefaultExtraKeys){
            options.add(new MenuOption(getString(R.string.game_menu_send_keys_esc),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_ESCAPE})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_f11),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_F11})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_f4),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_F4})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_enter),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_RETURN})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_v),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_V})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_d),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_g),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_G})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_tab),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_shift_tab),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_TAB})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_shift_left),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_LEFT})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_shift_f1),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F1})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_shift_f12),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F12})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_b),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B})));
        }

        // Import custom shortcuts
        SharedPreferences preferences = game.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        String value = preferences.getString(KEY_NAME,"");

        if(!TextUtils.isEmpty(value)){
            try {
                KeyConfigHelper.ShortcutFile shortcutFile = KeyConfigHelper.parseShortcutFile(value);
                if (shortcutFile != null && shortcutFile.data != null && !shortcutFile.data.isEmpty()) {
                    List<KeyConfigHelper.Shortcut> data = shortcutFile.data;
                    for (KeyConfigHelper.Shortcut sc : data) {
                        List<String> keys = sc.keys;
                        short[] keyCodes = new short[keys.size()];

                        for (int i = 0; i < keys.size(); i++) {
                            String code = keys.get(i);
                            int keycode;

                            if (code.startsWith("0x")) {               // literal hex value
                                keycode = Integer.parseInt(code.substring(2), 16);
                            } else if (code.startsWith("VK_")) {       // symbolic constant in KeyMapper
                                Field field = KeyMapper.class.getDeclaredField(code);
                                keycode = field.getInt(null);
                            } else {                                   // unsupported
                                throw new IllegalArgumentException("Unknown key code: " + code);
                            }
                            keyCodes[i] = (short) keycode;
                        }

                        // Whatever MenuOption looks like in your project
                        MenuOption option = new MenuOption(sc.name, () -> sendKeys(keyCodes));
                        options.add(option);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(game,getString(R.string.wrong_import_format),Toast.LENGTH_SHORT).show();
            }
        }
        showMenuDialog(getString(R.string.game_menu_send_keys), options.toArray(new MenuOption[options.size()]),
                () -> showMenu(menuDevice));
    }

    private void showAdvancedMenu(GameInputDevice device) {
        List<MenuOption> options = new ArrayList<>();

        // On-screen controls only make sense with a touchscreen, so a TV doesn't list them
        if (hasTouchscreen()) {
            options.add(new MenuOption(getString(R.string.game_menu_toggle_floating_button), true, game::toggleFloatingButtonVisibility));
            options.add(new MenuOption(getString(R.string.game_menu_toggle_keyboard_model), true, game::toggleKeyboardController));
            if (!game.isOnExternalDisplay()) {
                options.add(new MenuOption(getString(R.string.game_menu_toggle_virtual_model), true, game::toggleVirtualController));
            }
            options.add(new MenuOption(getString(R.string.game_menu_toggle_virtual_keyboard_model), true, game::toggleFullKeyboard));
            options.add(new MenuOption(getString(R.string.game_menu_switch_touch_sensitivity_model), true, game::switchTouchSensitivity));
        }
        options.add(new MenuOption(getString(R.string.game_menu_send_keys), this::showSpecialKeysMenu).submenu());
        if (device != null) {
            options.addAll(device.getGameMenuOptions());
        }
        showMenuDialog(getString(R.string.game_panel_more_options), options.toArray(new MenuOption[options.size()]),
                () -> showMenu(device));
    }

    private void showClipboardMenu(GameInputDevice device) {
        MenuOption[] options = {
                new MenuOption(getString(R.string.game_menu_upload_clipboard), true,
                        () -> game.sendClipboard(true)).icon(R.drawable.ic_game_clipboard),
                new MenuOption(getString(R.string.game_menu_fetch_clipboard), true,
                        () -> game.getClipboard(0)).icon(R.drawable.ic_game_clipboard),
        };
        showMenuDialog(getString(R.string.game_panel_clipboard), options, () -> showMenu(device));
    }

    private void showServerCmd(ArrayList<String> serverCmds, GameInputDevice device) {
        List<MenuOption> options = new ArrayList<>();

        AtomicInteger index = new AtomicInteger(0);
        for (String str : serverCmds) {
            final int finalI = index.getAndIncrement();
            options.add(new MenuOption("> " + str, true, () -> game.sendExecServerCmd(finalI)));
        };

        showMenuDialog(getString(R.string.game_menu_server_cmd), options.toArray(new MenuOption[options.size()]),
                () -> showMenu(device));
    }

    public void showMenu(GameInputDevice device) {
        menuDevice = device;
        boolean touch = hasTouchscreen();
        List<MenuOption> options = new ArrayList<>();

        options.add(new MenuOption(getString(R.string.game_panel_stats), game::toggleHUD)
                .icon(R.drawable.ic_settings_stats).toggle(game::isPerfOverlayEnabled));

        options.add(new MenuOption(getString(R.string.game_panel_keyboard), true,
                game::toggleKeyboard).icon(R.drawable.ic_settings_keys));

        if (touch && game.allowChangeMouseMode) {
            options.add(new MenuOption(getString(R.string.game_panel_mouse_mode), true,
                    () -> game.selectMouseMode(dialogScreenContext))
                    .icon(R.drawable.ic_settings_mouse).value(game.getMouseModeLabel()));
        }

        options.add(new MenuOption(getString(R.string.game_panel_clipboard), () -> showClipboardMenu(device))
                .icon(R.drawable.ic_game_clipboard).value(getString(R.string.game_panel_clipboard_value)).submenu());

        options.add(new MenuOption(getString(R.string.game_menu_server_cmd),
                () -> {
                    ArrayList<String> serverCmds = game.getServerCmds();
                    if (serverCmds.isEmpty()) {
                        hideMenu();
                        int themeResId = game.getApplicationInfo().theme;
                        Context themedContext = new ContextThemeWrapper(dialogScreenContext, themeResId);
                        new AlertDialog.Builder(themedContext)
                                .setTitle(R.string.game_dialog_title_server_cmd_empty)
                                .setMessage(R.string.game_dialog_message_server_cmd_empty)
                                .show();
                    } else {
                        this.showServerCmd(serverCmds, device);
                    }
                }).icon(R.drawable.ic_game_command).submenu());

        // Pan and zoom and rotation are for touchscreens, so a TV doesn't list them
        if (touch) {
            options.add(new MenuOption(getString(game.isZoomModeEnabled() ? R.string.game_menu_disable_zoom_mode : R.string.game_menu_enable_zoom_mode), true,
                    game::toggleZoomMode).icon(R.drawable.ic_game_zoom));

            if (dialogScreenContext == game) {
                options.add(new MenuOption(getString(R.string.game_menu_rotate_screen), true,
                        game::rotateScreen).icon(R.drawable.ic_game_rotate));
            }
        }

        options.add(new MenuOption(getString(R.string.game_panel_more_options),
                () -> showAdvancedMenu(device)).icon(R.drawable.ic_settings_advanced).submenu());

        List<KeyChip> chips = new ArrayList<>();
        if (!PreferenceConfiguration.readPreferences(game).disableDefaultExtraKeys) {
            chips.add(new KeyChip("Esc", new short[]{KeyboardTranslator.VK_ESCAPE}));
            chips.add(new KeyChip("Win", new short[]{KeyboardTranslator.VK_LWIN}));
            chips.add(new KeyChip("Alt+F4", new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_F4}));
            chips.add(new KeyChip("Alt+Enter", new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_RETURN}));
            chips.add(new KeyChip("Win+G", new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_G}));
            chips.add(new KeyChip("Win+D", new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D}));
            chips.add(new KeyChip(getString(R.string.game_panel_task_manager), new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE}));
        }
        // No keys: the chip opens the full list, with any custom shortcuts
        chips.add(new KeyChip(getString(R.string.game_panel_more) + " \u25B8", null));

        String title = game.getAppName() != null ? game.getAppName() : getString(R.string.quick_menu_title);
        showMenuDialog(title, mainSubtitle(), options.toArray(new MenuOption[options.size()]), null,
                chips.toArray(new KeyChip[0]));
    }

    // "GAMING-PC · 42 min · Back closes", leaving out what isn't known
    private String mainSubtitle() {
        List<String> parts = new ArrayList<>();
        String host = game.getPcName();
        long started = game.getStreamStartedAtMs();
        if (host != null && started > 0) {
            long minutes = (SystemClock.elapsedRealtime() - started) / 60000;
            parts.add(game.getString(R.string.game_panel_subtitle_host_time, host, (int) minutes));
        } else if (host != null) {
            parts.add(host);
        }
        parts.add(getString(R.string.game_panel_back_closes));
        return TextUtils.join(" \u00B7 ", parts);
    }

    @Override
    public void hideMenu() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        currentDialog = null;
    }

    @Override
    public boolean isMenuOpen() {
        return currentDialog != null && currentDialog.isShowing();
    }
}
