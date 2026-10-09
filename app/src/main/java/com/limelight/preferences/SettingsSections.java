package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.limelight.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Splits each settings category into titled sections (Picture quality, Latency, ...),
 * each with its own icon and colour.
 *
 * A section is an empty category placed between the options, and the options are only
 * reordered, never moved to another group. Their keys, their category and the
 * dependencies between them stay exactly as preferences.xml declares them.
 */
final class SettingsSections {
    static final class Section {
        final String key;
        @StringRes final int title;
        @StringRes final int summary;
        @DrawableRes final int icon;
        @ColorRes final int color;
        final String[] keys;

        Section(String key, int title, int summary, int icon, int color, String... keys) {
            this.key = key;
            this.title = title;
            this.summary = summary;
            this.icon = icon;
            this.color = color;
            this.keys = keys;
        }
    }

    static final class Category {
        final String key;
        final Section[] sections;

        Category(String key, Section... sections) {
            this.key = key;
            this.sections = sections;
        }
    }

    static final Category[] CATEGORIES = {
            new Category("category_video_settings",
                    new Section("section_video_quality", R.string.section_video_quality, R.string.section_video_quality_summary,
                            R.drawable.ic_section_picture, R.color.section_blue,
                            "list_resolution", "list_fps", "seekbar_bitrate_kbps", "seekbar_metered_bitrate_kbps",
                            "checkbox_enable_hdr"),
                    new Section("section_video_latency", R.string.section_video_latency, R.string.section_video_latency_summary,
                            R.drawable.ic_section_latency, R.color.section_amber,
                            "frame_pacing", "checkbox_game_mode", "pref_low_latency_frame_balance",
                            "checkbox_forceTightThresholds", "checkbox_ultra_low_latency"),
                    new Section("section_video_screen", R.string.section_video_screen, R.string.section_video_screen_summary,
                            R.drawable.ic_section_screen, R.color.section_green,
                            "list_video_scale_mode", "checkbox_enable_view_top_center", "checkbox_enforce_display_mode",
                            "checkbox_use_virtual_display", "seekbar_resolution_scale_factor",
                            "checkbox_enable_fullexdisplay", "checkbox_auto_orientation",
                            "checkbox_auto_invert_video_resolution"),
                    new Section("section_video_3d", R.string.section_video_3d, R.string.section_video_3d_summary,
                            R.drawable.ic_section_3d, R.color.section_pink,
                            "render_mode_list", "parallax_depth", "convergence_ratio", "balance_shift"),
                    new Section("section_video_custom", R.string.section_video_custom, R.string.section_video_custom_summary,
                            R.drawable.ic_section_custom, R.color.section_violet,
                            "edit_diy_w_h", "edit_diy_bitrate", "custom_refresh_rate")),
            new Category("category_audio_settings",
                    new Section("section_audio_sound", R.string.section_audio_sound, R.string.section_audio_sound_summary,
                            R.drawable.ic_settings_audio, R.color.section_blue,
                            "list_audio_config", "checkbox_enable_audiofx"),
                    new Section("section_audio_passthrough", R.string.section_audio_passthrough, R.string.section_audio_passthrough_summary,
                            R.drawable.ic_section_surround, R.color.section_orange,
                            "list_passthrough_format", "list_passthrough_buffer")),
            new Category("category_gamepad_settings",
                    new Section("section_pad_buttons", R.string.section_pad_buttons, R.string.section_pad_buttons_summary,
                            R.drawable.ic_settings_gamepad, R.color.section_blue,
                            "seekbar_deadzone", "checkbox_flip_face_buttons", "checkbox_mouse_emulation",
                            "analog_scrolling", "checkbox_gamepad_touchpad_as_mouse"),
                    new Section("section_pad_detection", R.string.section_pad_detection, R.string.section_pad_detection_summary,
                            R.drawable.ic_section_usb, R.color.section_green,
                            "checkbox_multi_controller", "checkbox_usb_driver", "checkbox_usb_bind_all",
                            "checkbox_enable_joyconfix", "checkbox_gamepad_enable_battery_report"),
                    new Section("section_pad_motion", R.string.section_pad_motion, R.string.section_pad_motion_summary,
                            R.drawable.ic_game_rotate, R.color.section_violet,
                            "checkbox_gamepad_motion_sensors", "checkbox_gamepad_motion_fallback",
                            "checkbox_force_device_motion"),
                    new Section("section_pad_rumble", R.string.section_pad_rumble, R.string.section_pad_rumble_summary,
                            R.drawable.ic_section_rumble, R.color.section_amber,
                            "checkbox_enable_rumble", "checkbox_vibrate_fallback", "checkbox_enable_device_rumble",
                            "seekbar_vibrate_fallback_strength")),
            new Category("category_input_settings",
                    new Section("section_input_mouse", R.string.section_input_mouse, R.string.section_input_mouse_summary,
                            R.drawable.ic_settings_mouse, R.color.section_blue,
                            "mouse_mode_list", "checkbox_remember_mouse_mode", "checkbox_mouse_local_cursor",
                            "checkbox_multi_touch_gestures", "checkbox_mouse_nav_buttons", "checkbox_absolute_mouse_mode"),
                    new Section("section_input_trackpad", R.string.section_input_trackpad, R.string.section_input_trackpad_summary,
                            R.drawable.ic_settings_touch, R.color.section_green,
                            "seekbar_trackpad_sensitivity_x", "seekbar_trackpad_sensitivity_y",
                            "checkbox_trackpad_drag_drop_vibration", "seekbar_trackpad_drag_drop_threshold",
                            "checkbox_trackpad_swap_axis"),
                    new Section("section_input_keyboard", R.string.section_input_keyboard, R.string.section_input_keyboard_summary,
                            R.drawable.ic_keyboard_setting, R.color.section_violet,
                            "checkbox_force_qwerty", "checkbox_back_as_meta", "checkbox_right_alt_as_meta",
                            "checkbox_back_as_guide", "checkbox_ignore_synth_events", "checkbox_enable_commit_text")),
            new Category("category_perf_monitor_settings",
                    new Section("section_stats_overlay", R.string.section_stats_overlay, R.string.section_stats_overlay_summary,
                            R.drawable.ic_settings_stats, R.color.section_blue,
                            "checkbox_enable_perf_overlay", "checkbox_enable_perf_overlay_lite",
                            "checkbox_enable_perf_overlay_lite_dialog", "checkbox_enable_perf_overlay_bottom"),
                    new Section("section_stats_logging", R.string.section_stats_logging, R.string.section_stats_logging_summary,
                            R.drawable.ic_section_log, R.color.section_green,
                            "checkbox_enable_perf_logging", "share_performance_logs", "option_view_shared_pref_logs")),
            new Category("category_host_settings",
                    new Section("section_host_streaming", R.string.section_host_streaming, R.string.section_host_streaming_summary,
                            R.drawable.ic_settings_host, R.color.section_blue,
                            "checkbox_enable_sops", "checkbox_host_audio")),
            new Category("category_ui_settings",
                    new Section("section_ui_general", R.string.section_ui_general, R.string.section_ui_general_summary,
                            R.drawable.ic_settings_appearance, R.color.section_blue,
                            "list_theme_mode", "list_languages", "checkbox_resume_without_confirm", "checkbox_enable_quit_dialog",
                            "checkbox_enable_floating_button", "checkbox_full_screen", "checkbox_disable_warnings",
                            "checkbox_enable_post_stream_toast", "checkbox_small_icon_mode", "checkbox_enable_pip"),
                    new Section("section_ui_keyboard", R.string.section_ui_keyboard, R.string.section_ui_keyboard_summary,
                            R.drawable.ic_fullscreen_keyboard, R.color.section_violet,
                            "seekbar_keyboard_axi_opacity", "onscreen_keyboard_autofit", "seekbar_onscreen_keyboard_height",
                            "seekbar_onscreen_keyboard_width", "list_onscreen_keyboard_align_mode",
                            "checkbox_enable_sticky_modifier_key_virtual_keyboard"),
                    new Section("section_ui_zoom", R.string.section_ui_zoom, R.string.section_ui_zoom_summary,
                            R.drawable.ic_game_zoom, R.color.section_green,
                            "checkbox_show_overlay_zoom_toggle_button", "checkbox_remember_zoom_pan")),
            new Category("category_onscreen_controls",
                    new Section("section_touch_gamepad", R.string.section_touch_gamepad, R.string.section_touch_gamepad_summary,
                            R.drawable.ic_settings_gamepad, R.color.section_blue,
                            "checkbox_show_onscreen_controls", "checkbox_hide_osc_when_has_gamepad",
                            "checkbox_onscreen_style_official", "checkbox_vibrate_osc", "checkbox_only_show_L3R3",
                            "checkbox_show_guide_button", "seekbar_osc_opacity", "checkbox_enable_analog_stick_new",
                            "seekbar_osc_free_analog_stick_opacity", "option_reset_osc_preference")),
            new Category("category_advanced_settings",
                    new Section("section_adv_decoding", R.string.section_adv_decoding, R.string.section_adv_decoding_summary,
                            R.drawable.ic_settings_video, R.color.section_blue,
                            "video_format", "checkbox_unlock_fps", "checkbox_reduce_refresh_rate", "checkbox_full_range"),
                    new Section("section_adv_network", R.string.section_adv_network, R.string.section_adv_network_summary,
                            R.drawable.ic_section_network, R.color.section_green,
                            "checkbox_prevent_packet_loss")),
    };

    // Categories that SettingsGroups nests inside a bigger one get the same header look
    private static final Object[][] NESTED = {
            {"category_special_key_layout", R.drawable.ic_settings_keys, R.color.section_amber},
            {"category_general_settings", R.drawable.ic_game_clipboard, R.color.section_violet},
            {"category_virtual_trackpad_settings", R.drawable.ic_settings_touch, R.color.section_green},
            {"category_settings_misc", R.drawable.ic_help, R.color.section_orange},
    };

    // The 3D sliders only mean something in a 3D render mode
    private static final String RENDER_MODE_KEY = "render_mode_list";
    private static final String RENDER_MODE_2D = "0";
    private static final String[] RENDER_3D_KEYS = {"parallax_depth", "convergence_ratio", "balance_shift"};

    private SettingsSections() {}

    static void apply(PreferenceScreen screen) {
        Context context = screen.getContext();

        for (Category spec : CATEGORIES) {
            Preference found = screen.findPreference(spec.key);
            if (found instanceof PreferenceGroup) {
                split((PreferenceGroup) found, spec, context);
            }
        }
        for (Object[] nested : NESTED) {
            Preference found = screen.findPreference((String) nested[0]);
            if (found instanceof PreferenceCategory) {
                styleHeader(found, context, (Integer) nested[1], (Integer) nested[2]);
            }
        }

        // Sections replace the "show more" rows; androidx rejects a nested group inside
        // a group that still collapses its children
        expandAll(screen);

        bindRenderMode(screen);
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

    /** The section headers and nested sections of a category, in the order they show. */
    static List<PreferenceCategory> sectionsOf(PreferenceGroup category) {
        List<PreferenceCategory> sections = new ArrayList<>();
        List<Preference> children = new ArrayList<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            children.add(category.getPreference(i));
        }
        Collections.sort(children, (a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        for (Preference child : children) {
            if (child instanceof PreferenceCategory && child.isVisible()) {
                sections.add((PreferenceCategory) child);
            }
        }
        return sections;
    }

    private static void split(PreferenceGroup category, Category spec, Context context) {
        // Direct children in their current order
        List<Preference> children = new ArrayList<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            children.add(category.getPreference(i));
        }

        Set<Preference> placed = new HashSet<>();
        List<Preference> ordered = new ArrayList<>();
        for (Section section : spec.sections) {
            List<Preference> members = new ArrayList<>();
            for (String key : section.keys) {
                Preference pref = category.findPreference(key);
                // Only options sitting directly in this category, and not hidden for this device
                if (pref != null && pref.getParent() == category && !placed.contains(pref)) {
                    members.add(pref);
                }
            }
            if (members.isEmpty()) {
                continue;
            }

            PreferenceCategory header = new PreferenceCategory(context);
            header.setKey(section.key);
            header.setTitle(section.title);
            header.setSummary(section.summary);
            header.setPersistent(false);
            styleHeader(header, context, section.icon, section.color);
            category.addPreference(header);

            ordered.add(header);
            ordered.addAll(members);
            placed.addAll(members);
        }
        if (ordered.isEmpty()) {
            return;
        }

        // Anything the sections don't list stays at the end of the last section,
        // and nested sections (whole categories) come after that
        List<Preference> nestedGroups = new ArrayList<>();
        for (Preference child : children) {
            if (placed.contains(child)) {
                continue;
            }
            if (child instanceof PreferenceGroup) {
                nestedGroups.add(child);
            } else {
                ordered.add(child);
            }
        }
        ordered.addAll(nestedGroups);

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setOrder(i);
        }
    }

    private static void styleHeader(Preference header, Context context, @DrawableRes int icon, @ColorRes int color) {
        header.setLayoutResource(R.layout.settings_section_header);
        header.setIconSpaceReserved(true);
        header.setIcon(tinted(context, icon, color));
    }

    /** Icon and colour of a section header, or null for any other key. */
    static int[] iconOf(String sectionKey) {
        for (Category spec : CATEGORIES) {
            for (Section section : spec.sections) {
                if (section.key.equals(sectionKey)) {
                    return new int[]{section.icon, section.color};
                }
            }
        }
        for (Object[] nested : NESTED) {
            if (nested[0].equals(sectionKey)) {
                return new int[]{(Integer) nested[1], (Integer) nested[2]};
            }
        }
        return null;
    }

    static Drawable tinted(Context context, @DrawableRes int icon, @ColorRes int color) {
        Drawable drawable = AppCompatResources.getDrawable(context, icon);
        if (drawable == null) {
            return null;
        }
        drawable = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, color));
        return drawable;
    }

    private static void bindRenderMode(PreferenceScreen screen) {
        Preference renderMode = screen.findPreference(RENDER_MODE_KEY);
        if (renderMode == null) {
            return;
        }
        SharedPreferences prefs = renderMode.getSharedPreferences();
        String current = prefs != null ? prefs.getString(RENDER_MODE_KEY, RENDER_MODE_2D) : RENDER_MODE_2D;
        show3dSliders(screen, !RENDER_MODE_2D.equals(current));

        Preference.OnPreferenceChangeListener previous = renderMode.getOnPreferenceChangeListener();
        renderMode.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean accepted = previous == null || previous.onPreferenceChange(preference, newValue);
            if (accepted) {
                show3dSliders(screen, !RENDER_MODE_2D.equals(String.valueOf(newValue)));
            }
            return accepted;
        });
    }

    private static void show3dSliders(PreferenceScreen screen, boolean visible) {
        for (String key : RENDER_3D_KEYS) {
            Preference slider = screen.findPreference(key);
            if (slider != null) {
                slider.setVisible(visible);
            }
        }
    }
}
