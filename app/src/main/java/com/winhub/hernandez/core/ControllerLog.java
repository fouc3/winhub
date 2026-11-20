package com.winhub.hernandez.core;

import com.winhub.hernandez.R
import com.winhub.hernandez.util.I18n

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.winhub.hernandez.inputcontrols.HermesWriter;
import com.winhub.hernandez.winhandler.WinHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Controller input" log (Settings › Log Manager, off by default): one {@code controller.txt} per
 * launch of a Wine game, next to the game's other logs, written when the game exits. It is what a
 * user sends when the controls don't work, for the HERMES input chain (DirectInput, Wine's XInput
 * and Direct XInput).
 *
 * <p>It opens with a CHECKLIST per player slot - each step of the chain marked PASS / FAIL / WARN /
 * SKIP / INFO from what the app and the guest actually counted - so a reader sees where input
 * stopped: the app sent nothing, Wine never opened the pad, Wine opened it but read nothing, the game
 * never loaded Direct XInput, rumble never arrived. Then the evidence in the same order: the launch's
 * controller setup, the controllers and their slots, the session's controller events (in-game changes,
 * reconnects, Direct XInput mode changes), HERMES's counters per slot, and the guest's controller
 * lines (Direct XInput DLL and the evdev layer).
 *
 * <p>Nothing runs while the switch is off: {@link #begin} leaves no recorder and every static entry
 * point returns at its first null check. With it on, the launch also captures the guest's output,
 * keeping only the controller lines ({@link #GUEST_LINES}), and turns on the evdev layer's
 * diagnostics (FAKE_EVDEV_LOG=1); both are written at attach / probe / loss, not per input event.
 * The Linux runtime has its own (fake-input.txt in its session folder) and does not use this.
 */
public final class ControllerLog {
    private static final String TAG = "ControllerLog";

    /** Output file name inside the per-game log dir (see {@link LogRotation}). */
    public static final String OUTPUT_NAME = "controller.txt";
    /** The Log Manager switch. */
    public static final String PREF_KEY = "enable_controller_logs";

    private static final int MAX_EVENTS = 400;
    private static final int MAX_GUEST_LINES = 600;
    /** A guest line of the same shape (digits ignored) is kept this many times, then only counted. */
    private static final int MAX_SAME_SHAPE = 3;

    private static volatile ControllerLog active;

    private final long startMs = System.currentTimeMillis();
    private final Map<String, String> setup = new LinkedHashMap<>();
    private final List<String> events = new ArrayList<>();
    private int eventsDropped;
    private final List<String> guest = new ArrayList<>();
    private int guestDropped;
    private final Map<String, Integer> shapes = new LinkedHashMap<>();
    private final long[] rumble = new long[HermesWriter.MAX_SLOTS];
    private final List<String> padsAtStart = new ArrayList<>();
    private final List<String> padsAtEnd = new ArrayList<>();
    private final Set<Integer> slotsUsed = new LinkedHashSet<>();
    private final Set<String> xinputImages = new LinkedHashSet<>();
    private final Set<Integer> xinputAttached = new LinkedHashSet<>();
    private final Set<Integer> xinputNoController = new LinkedHashSet<>();
    private boolean xinputDirect;
    private boolean xinputWine;
    private boolean dinputVisible;
    private boolean dinputVibration;
    private int reconnectsFailed;

    private ControllerLog() {}

    /** True when the Log Manager switch is on. */
    public static boolean isEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(PREF_KEY, false);
    }

    /** Starts this launch's recorder when the switch is on; clears any earlier one either way. */
    public static boolean begin(SharedPreferences prefs) {
        active = isEnabled(prefs) ? new ControllerLog() : null;
        return active != null;
    }

    public static boolean isActive() {
        return active != null;
    }

    /** One line of the launch's controller setup, under {@code key} (a later value replaces it). */
    public static void setup(String key, String value) {
        ControllerLog log = active;
        if (log == null) return;
        synchronized (log) {
            log.setup.put(key, value);
        }
    }

    /** What the checklist needs to know about the launch's choices. */
    public static void launchChoices(boolean directXInput, boolean wineXInput, boolean dinputVisible,
                                     boolean dinputVibration) {
        ControllerLog log = active;
        if (log == null) return;
        synchronized (log) {
            log.xinputDirect = directXInput;
            log.xinputWine = wineXInput;
            log.dinputVisible = dinputVisible;
            log.dinputVibration = dinputVibration;
        }
    }

    /** One timestamped line in the session's controller events. */
    public static void event(String line) {
        ControllerLog log = active;
        if (log == null) return;
        synchronized (log) {
            if (log.events.size() >= MAX_EVENTS) {
                log.eventsDropped++;
                return;
            }
            log.events.add(log.since() + "  " + line);
        }
    }

    /** A controller took {@code slot} (WinHandler): the slot is reported even if it left before the end. */
    public static void slotTaken(int slot, String line) {
        ControllerLog log = active;
        if (log == null) return;
        synchronized (log) {
            if (slot >= 0 && slot < HermesWriter.MAX_SLOTS) log.slotsUsed.add(slot);
        }
        event(line);
    }

    /** A reconnect ended; a failed one is a WARN on the checklist. */
    public static void reconnect(String slots, boolean ok) {
        ControllerLog log = active;
        if (log == null) return;
        synchronized (log) {
            if (!ok) log.reconnectsFailed++;
        }
        event("reconnect of slots " + slots + (ok ? " confirmed" : " NOT confirmed (the game's reader did not come back)"));
    }

    /** A rumble datagram (HXR1) reached the app for {@code slot}: Direct XInput's or the effects engine's. */
    public static void rumble(int slot) {
        ControllerLog log = active;
        if (log == null || slot < 0 || slot >= HermesWriter.MAX_SLOTS) return;
        boolean first;
        synchronized (log) {
            first = log.rumble[slot]++ == 0;
        }
        if (first) event("slot " + slot + ": first rumble request reached the app");
    }

    /** The controllers and their player slots, at the start ({@code atEnd} false) or the end. */
    public static void pads(List<WinHandler.PlayerSlotInfo> slots, boolean atEnd) {
        ControllerLog log = active;
        if (log == null || slots == null) return;
        synchronized (log) {
            List<String> out = atEnd ? log.padsAtEnd : log.padsAtStart;
            out.clear();
            for (WinHandler.PlayerSlotInfo s : slots) {
                if (s.isOnScreen && s.currentSlot < 0) continue;
                String slot = s.currentSlot >= 0 ? "player " + (s.currentSlot + 1) + " (slot " + s.currentSlot + ")"
                        : s.override == WinHandler.SLOT_IGNORE ? "ignored" : "no slot";
                out.add(slot + ": " + s.displayName + (s.isOnScreen ? " [on-screen controls]"
                        : s.isGameController ? "" : " [not a game controller]")
                        + (s.override >= 0 ? " [pinned to player " + (s.override + 1) + "]" : ""));
                if (s.currentSlot >= 0) log.slotsUsed.add(s.currentSlot);
            }
        }
    }

    // ── Guest output ────────────────────────────────────────────────────────────────────────────

    /** Lines the Direct XInput DLL and the evdev layer write (fakeinput.cpp Logger + hermes's logger). */
    private static final Pattern GUEST_LINE = Pattern.compile(
            "HERMES-XInput: |^\\s*fakeinput|^\\s*(Adding controller|Removing controller|Replacing stale controller"
                    + "|Could not attach|Hooking ioctl EVIOCG(ID|NAME)|Unhandled evdev ioctl|EVIOCSFF|EV_FF )");
    private static final Pattern XI_LOADED = Pattern.compile("HERMES-XInput: .* loaded into (.+?) \\(pid \\d+\\)");
    /** The DLL opened the slot's file - on the game's first XInput call, for all four slots, held or not. */
    private static final Pattern XI_ATTACHED = Pattern.compile("HERMES-XInput: slot (\\d+) attached");
    /** The game asked for a slot no controller holds. */
    private static final Pattern XI_NO_CONTROLLER = Pattern.compile("HERMES-XInput: slot (\\d+): .*no controller on this slot");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    /** For {@code ProcessHelper.addDebugCallback}: keeps the guest's controller lines, drops the rest. */
    public static final Callback<String> GUEST_LINES = ControllerLog::guestLine;

    private static void guestLine(String line) {
        ControllerLog log = active;
        if (log == null || line == null || !GUEST_LINE.matcher(line).find()) return;
        Matcher loaded = XI_LOADED.matcher(line);
        Matcher attached = XI_ATTACHED.matcher(line);
        Matcher noController = XI_NO_CONTROLLER.matcher(line);
        synchronized (log) {
            if (loaded.find()) log.xinputImages.add(baseName(loaded.group(1)));
            if (attached.find()) {
                try { log.xinputAttached.add(Integer.parseInt(attached.group(1))); } catch (NumberFormatException ignored) {}
            }
            if (noController.find()) {
                try { log.xinputNoController.add(Integer.parseInt(noController.group(1))); } catch (NumberFormatException ignored) {}
            }
            String shape = DIGITS.matcher(line).replaceAll("#");
            int seen = log.shapes.containsKey(shape) ? log.shapes.get(shape) : 0;
            log.shapes.put(shape, seen + 1);
            if (seen >= MAX_SAME_SHAPE || log.guest.size() >= MAX_GUEST_LINES) {
                log.guestDropped++;
                return;
            }
            log.guest.add(log.since() + "  " + line.trim());
        }
    }

    private static String baseName(String path) {
        String p = path.trim();
        int cut = Math.max(p.lastIndexOf('\\'), p.lastIndexOf('/'));
        return cut >= 0 ? p.substring(cut + 1) : p;
    }

    private String since() {
        return String.format(Locale.ROOT, "+%7.1fs", (System.currentTimeMillis() - startMs) / 1000.0);
    }

    // ── Writing ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Writes {@value #OUTPUT_NAME} into {@code perGameLogDir} and ends the recorder. Reads the slots'
     * counters through {@link HermesWriter#getSessionStat}, so it works after the session retired
     * the slot files. Never throws.
     */
    public static void finish(Context context, File perGameLogDir, String gameName) {
        ControllerLog log = active;
        active = null;
        if (log == null || perGameLogDir == null) return;
        try {
            String text;
            synchronized (log) {
                text = log.build(context, gameName);
            }
            perGameLogDir.mkdirs();
            File target = new File(perGameLogDir, OUTPUT_NAME);
            if (FileUtils.writeString(target, text)) Log.i(TAG, "wrote " + target.getAbsolutePath());
        } catch (Throwable t) {
            // Never let log collection break a game exiting.
            Log.w(TAG, "write failed", t);
        }
    }

    private static long stat(int slot, int which) {
        return HermesWriter.getSessionStat(slot, which);
    }

    private String build(Context context, String gameName) {
        StringBuilder out = new StringBuilder();
        out.append(LogcatCapture.deviceHeader(context));
        out.append("===== CONTROLLER INPUT LOG =====\n");
        out.append("Game: ").append(gameName != null ? gameName : "?").append('\n');
        out.append(String.format(Locale.ROOT, "Session length: %.0f s\n", (System.currentTimeMillis() - startMs) / 1000.0));

        // Slots to report: every slot a controller held. Every slot file gets one neutral publish when it
        // is prepared and Direct XInput opens all four, so neither of those makes a slot a player; input
        // (button edges, a reader on its event node) or rumble aimed at it does.
        Set<Integer> slots = new java.util.TreeSet<>(slotsUsed);
        for (int s = 0; s < HermesWriter.MAX_SLOTS; s++) {
            if (stat(s, HermesWriter.STAT_EDGES) > 0 || stat(s, HermesWriter.STAT_READER_ATTACHES) > 0
                    || rumble[s] > 0) slots.add(s);
        }

        out.append("\n===== CHECKLIST =====\n");
        List<String> rows = new ArrayList<>();
        if (slots.isEmpty()) {
            rows.add(fail("PAD", I18n.s(R.string.ui_no_controller_held_a_player_slot_and_the_app_sent_no_inp)
                    + I18n.s(R.string.ui_winhub_a_controller_check_controls_players_is_the_pad_co)));
        }
        for (int s : slots) checklistForSlot(rows, s);
        checklistSession(rows);
        int pass = 0, fail = 0, warn = 0;
        for (String r : rows) {
            out.append(r).append('\n');
            if (r.startsWith("[PASS]")) pass++;
            else if (r.startsWith("[FAIL]")) fail++;
            else if (r.startsWith("[WARN]")) warn++;
        }
        out.append(String.format(Locale.ROOT, "-- %d pass, %d fail, %d warn%s\n", pass, fail, warn,
                fail == 0 ? "" : " — read the first FAIL: the steps after it depend on it"));

        out.append("\n===== SETUP AT LAUNCH =====\n");
        if (setup.isEmpty()) out.append("(none recorded)\n");
        for (Map.Entry<String, String> e : setup.entrySet()) {
            out.append(e.getKey()).append(": ").append(LogcatCapture.redact(e.getValue())).append('\n');
        }

        out.append("\n===== CONTROLLERS =====\n");
        out.append("At the start:\n");
        appendList(out, padsAtStart, "  (none)");
        out.append("At the end:\n");
        appendList(out, padsAtEnd, "  (none)");

        out.append("\n===== SESSION EVENTS =====\n");
        appendList(out, events, "(none)");
        if (eventsDropped > 0) out.append("(+").append(eventsDropped).append(" more not kept)\n");

        out.append("\n===== HERMES COUNTERS =====\n");
        for (int s = 0; s < HermesWriter.MAX_SLOTS; s++) appendCounters(out, s);

        out.append("\n===== GUEST CONTROLLER LINES (Direct XInput DLL, evdev layer) =====\n");
        if (guest.isEmpty()) out.append("(none)\n");
        for (String l : guest) out.append(LogcatCapture.redact(l)).append('\n');
        if (guestDropped > 0) {
            out.append("(+").append(guestDropped).append(" repeated or later lines not kept)\n");
        }
        return out.toString();
    }

    private void checklistForSlot(List<String> rows, int s) {
        String p = "P" + (s + 1) + " ";
        long publishes = stat(s, HermesWriter.STAT_PUBLISHES);
        long failed = stat(s, HermesWriter.STAT_FAILED_PUBLISHES);
        long attaches = stat(s, HermesWriter.STAT_READER_ATTACHES);
        long reattaches = stat(s, HermesWriter.STAT_READER_REATTACHES);
        long frames = stat(s, HermesWriter.STAT_READER_FRAMES);
        long removals = stat(s, HermesWriter.STAT_READER_REMOVALS);

        if (publishes > 0) {
            rows.add(pass(p + "APP", "the app sent " + publishes + " controller updates for slot " + s));
        } else {
            rows.add(fail(p + "APP", "the app sent nothing for slot " + s
                    + I18n.s(R.string.ui_no_input_from_this_controller_reached_winhub_or_it_was_m)));
        }
        if (failed > 0) {
            rows.add(warn(p + "APP", failed + " updates could not be written to the slot file"));
        }

        if (attaches > 0) {
            rows.add(pass(p + "WINE", I18n.s(R.string.ui_wine_s_controller_layer_opened_the_pad) + attaches + " time(s)"
                    + (reattaches > 0 ? ", re-opened " + reattaches : "")
                    + (removals > 0 ? ", closed " + removals : "")
                    + I18n.s(R.string.ui_this_path_feeds_directinput_and_wine_s_xinput)));
            if (frames > 0) {
                rows.add(pass(p + "READ", I18n.s(R.string.ui_wine_read) + frames + " input frames from the pad"));
            } else if (publishes > 0) {
                rows.add(fail(p + "READ", I18n.s(R.string.ui_wine_opened_the_pad_but_read_no_input_from_it)));
            } else {
                rows.add(skip(p + "READ", I18n.s(R.string.ui_nothing_was_sent_so_nothing_could_be_read)));
            }
        } else if (xinputDirect && xinputAttached.contains(s)) {
            rows.add(warn(p + "WINE", I18n.s(R.string.ui_wine_s_controller_layer_never_opened_the_pad_direct_xinp)
                    + I18n.s(R.string.ui_so_xinput_games_work_but_directinput_games_will_not_see)));
        } else {
            rows.add(fail(p + "WINE", "Wine's controller layer never opened the pad (no /dev/input/event" + s
                    + I18n.s(R.string.ui_reader_directinput_and_wine_s_xinput_cannot_see_it)));
        }

        if (xinputDirect) {
            if (xinputAttached.contains(s) && xinputNoController.contains(s)) {
                rows.add(warn(p + "XINPUT", I18n.s(R.string.ui_the_game_called_xinput_but_direct_xinput_found_no_contro) + s
                        + I18n.s(R.string.ui_when_the_game_asked_for_it_it_may_have_arrived_later)));
            } else if (xinputAttached.contains(s)) {
                rows.add(pass(p + "XINPUT", I18n.s(R.string.ui_the_game_called_xinput_and_direct_xinput_is_reading_this) + s));
            } else if (!xinputImages.isEmpty()) {
                rows.add(warn(p + "XINPUT", I18n.s(R.string.ui_direct_xinput_was_loaded_but_never_connected_to_slot) + s
                        + I18n.s(R.string.ui_the_game_never_asked_for_this_player_or_it_uses_directin)));
            }
        }

        long ffDatagrams = stat(s, HermesWriter.STAT_READER_FF_DATAGRAMS);
        if (rumble[s] > 0) {
            long viaEngine = Math.min(ffDatagrams, rumble[s]);
            rows.add(pass(p + "RUMBLE", rumble[s] + I18n.s(R.string.ui_rumble_requests_reached_the_app) + viaEngine
                    + I18n.s(R.string.ui_through_wine_directinput_effects_or_wine_s_xinput_and) + (rumble[s] - viaEngine)
                    + I18n.s(R.string.ui_from_direct_xinput)));
        } else {
            rows.add(skip(p + "RUMBLE", I18n.s(R.string.ui_no_rumble_requests_this_session_the_game_asked_for_none)));
        }

        long uploads = stat(s, HermesWriter.STAT_READER_FF_UPLOADS);
        long plays = stat(s, HermesWriter.STAT_READER_FF_PLAYS);
        if (uploads > 0 || plays > 0) {
            rows.add(info(p + "DINPUT FF", uploads + I18n.s(R.string.ui_directinput_effects_uploaded) + plays + " played"
                    + (plays == 0 ? " (uploaded but never started)" : "")));
        } else if (!dinputVibration) {
            rows.add(skip(p + "DINPUT FF", I18n.s(R.string.ui_directinput_vibration_is_off)));
        }

        long hz = stat(s, HermesWriter.STAT_READER_AXIS_HZ);
        long fallbacks = stat(s, HermesWriter.STAT_READER_AUTO_FALLBACKS);
        if (hz > 0) {
            rows.add(info(p + "STICKS", "stick updates last paced at " + hz + "/s ("
                    + axisMode((int) stat(s, HermesWriter.STAT_READER_AXIS_MODE)) + ")"
                    + (fallbacks > 0 ? ", fell back to fixed 60/s " + fallbacks + " time(s)" : "")));
        }
    }

    private void checklistSession(List<String> rows) {
        if (xinputDirect) {
            if (!xinputImages.isEmpty()) {
                rows.add(pass("XINPUT DLL", "Direct XInput loaded into " + String.join(", ", xinputImages)));
            } else {
                rows.add(warn("XINPUT DLL", I18n.s(R.string.ui_direct_xinput_was_on_but_no_process_loaded_it_the_game_u)
                        + I18n.s(R.string.ui_only_ships_its_own_xinput_dll_see_setup_or_loads_xinput)));
            }
        } else if (xinputWine) {
            rows.add(info("XINPUT DLL", I18n.s(R.string.ui_xinput_goes_through_wine_s_own_xinput_direct_xinput_off)));
        }
        if (xinputWine && dinputVisible) {
            rows.add(warn("DINPUT", I18n.s(R.string.ui_xinput_source_is_wine_and_directinput_games_can_see_the)
                    + I18n.s(R.string.ui_xinput_then_ignores_the_controller_use_direct_xinput_or)));
        } else {
            rows.add(info("DINPUT", I18n.s(R.string.ui_directinput_games) + (dinputVisible ? "can" : "cannot") + " see the controller"));
        }
        if (reconnectsFailed > 0) {
            rows.add(warn("LIVE", reconnectsFailed + I18n.s(R.string.ui_in_game_reconnect_s_were_not_confirmed_see_session_event)));
        }
    }

    private static String axisMode(int mode) {
        switch (mode) {
            case 1: return I18n.s(R.string.ui_follows_the_game_s_frame_rate);
            case 2: return "60/s fallback, no frame feed";
            case 3: return "fixed";
            case 4: return "unpaced";
            case 5: return "capped by the auto ceiling";
            default: return "mode " + mode;
        }
    }

    private static void appendCounters(StringBuilder out, int s) {
        if (stat(s, HermesWriter.STAT_PUBLISHES) == 0 && stat(s, HermesWriter.STAT_READER_ATTACHES) == 0) {
            out.append("slot ").append(s).append(": unused\n");
            return;
        }
        String[] names = {
                "publishes", "button edges", "wake calls", "woken", "unplugs", "reader attaches", "reader re-attaches",
                "reader taps", "reader coalesced", "reader resyncs", "reader frames", "reader events", "reader removals",
                "instance", "failed publishes", "has backing", "paced frames", "merged axis", "axis hz", "auto fallbacks",
                "frame interval ns", "game frames", "axis mode", "frame wakes", "helper wakes", "auto max",
                "control gen applied", "identity gen attached", "identity reads", "control gen", "ff uploads",
                "ff plays", "ff datagrams", "ff guard fades"};
        out.append("slot ").append(s).append(':');
        for (int i = 0; i < names.length; i++) {
            if (i % 6 == 0) out.append("\n ");
            out.append(' ').append(names[i]).append('=').append(stat(s, i));
            if (i % 6 != 5 && i != names.length - 1) out.append(',');
        }
        out.append('\n');
    }

    private static void appendList(StringBuilder out, List<String> lines, String empty) {
        if (lines.isEmpty()) out.append(empty).append('\n');
        for (String l : lines) out.append(l).append('\n');
    }

    private static String pass(String step, String what) { return "[PASS] " + step + ": " + what; }
    private static String fail(String step, String what) { return "[FAIL] " + step + ": " + what; }
    private static String warn(String step, String what) { return "[WARN] " + step + ": " + what; }
    private static String skip(String step, String what) { return "[SKIP] " + step + ": " + what; }
    private static String info(String step, String what) { return "[INFO] " + step + ": " + what; }
}
