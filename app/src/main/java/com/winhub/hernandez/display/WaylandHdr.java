package com.winhub.hernandez.display;

import com.winhub.hernandez.R;
import com.winhub.hernandez.util.I18n;

import com.winhub.hernandez.R;
import com.winhub.hernandez.util.I18n;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import com.winhub.hernandez.container.Container;
import com.winhub.hernandez.container.Shortcut;

import java.util.Locale;

/**
 * The "HDR output" setting of the Wayland backend — one place for everything the container editor,
 * the game-shortcut editor, XMB game settings and the launch path must agree on.
 *
 * <p><b>What it does.</b> On a screen that reports HDR10, a game that supports HDR gets it: the Wayland
 * compositor offers games HDR10 (wp_color_manager_v1 + 10-bit buffers) and puts their frames on their
 * own display layer tagged BT.2020 PQ (see {@code waylandcomp/src/banner_color.h}), and the session
 * exports {@code DXVK_HDR=1} so DXVK tells the game the display is HDR. The game still needs HDR
 * switched on in its own settings. Not the drawer's "HDR" screen effect (an SDR bloom filter).
 *
 * <p><b>Where it is stored.</b> {@link #EXTRA} in the container's extraData ("1" = on, absent = off) and
 * in a game shortcut's extras ("1" on, "0" off, absent / "" = the container's). The shortcut wins, the
 * same resolve the launch path uses ({@link #effective}). {@code BANNER_WAYLAND_HDR} in the container's
 * or shortcut's environment variables overrides both (1 / 0 / force) — kept for testing.
 *
 * <p><b>When it can be on.</b> Only where the screen the game runs on lists HDR10
 * ({@link #unavailableReason}); the editors grey the row out with the reason everywhere else, and the
 * launch path turns nothing on there even if the stored value says on.
 *
 * <p>Every method is safe to call from any thread and never throws.
 */
public final class WaylandHdr {
    private WaylandHdr() {}

    /** The extra's key on the container and on a shortcut. */
    public static final String EXTRA = "waylandHdr";

    public static final String TITLE = I18n.s(R.string.ui_hdr_output_hdr10);

    /** The setting's help text (every editor shows the same words). */
    public static final String HELP_TEXT =
            "Applies from the next launch: whether the game is offered HDR is decided when it starts " +
            "(DXVK_HDR and the display's HDR offer). Games that support HDR then show it on this screen: " +
            I18n.s(R.string.ui_their_frames_go_to_the_display_in_10_bit_with_the_hdr10) +
            "the game's own settings too. In game, the drawer's Graphics tab has a live \"HDR output\" " +
            I18n.s(R.string.ui_switch_between_real_hdr_and_the_same_picture_tone_mapped) +
            "the game and windowed games stay HDR (the whole picture is composed in HDR); with frame " +
            I18n.s(R.string.ui_generation_it_depends_on_the_screen_offering_an_hdr_swap) +
            "tone-mapped to SDR. Windows is told this screen's peak brightness as Android reports it; " +
            "Android gives no live brightness in nits, so the session log records the live HDR/SDR headroom " +
            "instead. Not the same as the \"HDR\" screen effect.";

    /** One line for XMB, whose subtitles are a single line. */
    public static final String HELP_SHORT =
            I18n.s(R.string.ui_offer_real_hdr10_from_the_next_launch_switch_hdr_on_in_t);

    /** The screen a game started from an editor will run on: the device's built-in display. */
    public static Display targetDisplay(Context context) {
        try {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            return dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** {@code null} when {@code display} can show HDR10, else the reason it cannot (for a greyed row). */
    public static String unavailableReason(Display display) {
        DisplayHdrInfo d = DisplayHdrInfo.read(display);
        if (d.supportsHdr10) return null;
        if ("unknown".equals(d.formats)) return "This screen does not report its HDR capability, so HDR output stays off.";
        if ("none".equals(d.formats)) return "This screen does not support HDR (it reports no HDR types), so HDR output stays off.";
        return I18n.s(R.string.ui_this_screen_does_not_support_hdr10_it_reports) + d.formats + I18n.s(R.string.ui_so_hdr_output_stays_off);
    }

    /** {@link #unavailableReason(Display)} for the device's built-in screen. */
    public static String unavailableReason(Context context) {
        return unavailableReason(targetDisplay(context));
    }

    /** The shortcut's own choice: "1" on, "0" off, "" = use the container's. */
    public static String shortcutChoice(Shortcut shortcut) {
        if (shortcut == null) return "";
        String v = shortcut.getExtra(EXTRA, "");
        return "1".equals(v) || "0".equals(v) ? v : "";
    }

    /** The stored setting for a game: the shortcut's own choice, else the container's. */
    public static boolean effective(Shortcut shortcut, Container container) {
        String s = shortcutChoice(shortcut);
        if (!s.isEmpty()) return s.equals("1");
        return container != null && container.isWaylandHdr();
    }

    /** Decimal nits for an env var: "1351", "0.05", "0" — no exponent, no trailing zeros; null for < 0. */
    public static String nits(float v) {
        if (v < 0f || Float.isNaN(v) || Float.isInfinite(v)) return null;
        String s = String.format(Locale.US, "%.4f", v);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
