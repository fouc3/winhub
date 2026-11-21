package com.winhub.hernandez.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.winhub.hernandez.container.Shortcut
import com.winhub.hernandez.perf.PerfNodeResolver
import com.winhub.hernandez.perf.PerfRootApplier
import com.winhub.hernandez.perf.PerformanceSettings
import com.winhub.hernandez.perf.RootManager
import com.winhub.hernandez.ui.components.PlayerSlotEditorRow
import com.winhub.hernandez.ui.components.buildPlayerSlotEditorRows
import com.winhub.hernandez.widget.HudMetrics
import com.winhub.hernandez.winhandler.WinHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

// ── Performance (PerformanceDashboardDialogPerGame) ─────────────────────────────────────────────────
// Per-game overrides of the App Settings > Performance defaults, stored override-when-different exactly
// as the editor's save(): perfExtraOrNull writes the key only when it differs from the global default,
// otherwise clears it so the game re-inherits. Toggling a row back to the global value is its Reset.

private val P2_PERF_NO_ROOT = listOf("sustainedPerfMode", "perfPriorityBoost", "preferBigCores")
private val P2_PERF_KEYS = P2_PERF_NO_ROOT + PerfRootApplier.ROOT_KEYS
private val P2_PROFILES = listOf("Off", I18n.s(R.string.ui_battery), I18n.s(R.string.ui_balanced), I18n.s(R.string.performance), I18n.s(R.string.ui_turbo))

private fun p2PerfGlobal(key: String): Boolean = when (key) {
    "sustainedPerfMode" -> PerformanceSettings.sustainedPerfMode.value
    "perfPriorityBoost" -> PerformanceSettings.perfPriorityBoost.value
    "preferBigCores" -> PerformanceSettings.preferBigCores.value
    else -> PerformanceSettings.rootDefaultValue(key)
}

/** Effective seed = the per-game override if present, else the global default (no container level). */
private fun p2PerfValue(s: Shortcut, key: String): Boolean = s.p2Ex(key, if (p2PerfGlobal(key)) "1" else "0") == "1"

private fun p2ReadGovernor(): String? {
    for (core in PerfNodeResolver.cpuCores()) {
        val g = core.governor ?: continue
        val v = runCatching { java.io.File(g).readText().trim() }.getOrNull()
        if (!v.isNullOrEmpty()) return v
    }
    return null
}

internal fun xmbPerformanceMenu(xmb: XmbScope, shortcut: Shortcut): XmbMenu {
    val jobs = P2Jobs(xmb)
    var profile by mutableStateOf<String?>(null)
    var readouts by mutableStateOf<Map<String, String>>(emptyMap())
    var rootState by mutableStateOf(RootManager.state.value)

    // Current device state for the gauges (the dialog polls HudMetrics every ~1.5 s while open).
    jobs.scope.launch {
        val hud = HudMetrics(xmb.context)
        while (true) {
            val m = withContext(Dispatchers.IO) {
                val out = HashMap<String, String>()
                runCatching {
                    out["gpuMhz"] = hud.getGpuClockMhz()?.let { "$it MHz" } ?: "—"
                    val cpuT = hud.getCpuTempC()
                    val gpuT = hud.getGpuTempC()
                    val soc = if (cpuT != null && gpuT != null) maxOf(cpuT, gpuT) else (cpuT ?: gpuT)
                    out["socTemp"] = soc?.let { "$it °C" } ?: "—"
                    out["governor"] = p2ReadGovernor() ?: "—"
                }
                out
            }
            readouts = m
            rootState = RootManager.state.value
            delay(1500)
        }
    }

    fun write(values: List<Pair<String, Boolean>>) {
        p2PutAll(xmb, shortcut, values.map { (k, v) -> k to perfExtraOrNull(v, p2PerfGlobal(k)) })
    }
    fun setOne(key: String, v: Boolean) = write(listOf(key to v))

    fun applyProfile(p: String) {
        profile = p
        val off = listOf(
            PerfRootApplier.KEY_GPU_CLOCK_LOCK to false, PerfRootApplier.KEY_CPU_GOVERNOR to false,
            PerfRootApplier.KEY_CPU_FREQ_LOCK to false, PerfRootApplier.KEY_CORES_ONLINE to false,
        )
        when (p) {
            // Off neutralises the root tier so Galaxy Performance can own the clocks; Battery matches it.
            "Off", "Battery" -> write(listOf("sustainedPerfMode" to false, "perfPriorityBoost" to false, "preferBigCores" to false) + off)
            "Balanced" -> write(listOf("sustainedPerfMode" to true, "perfPriorityBoost" to false, "preferBigCores" to false,
                PerfRootApplier.KEY_GPU_CLOCK_LOCK to false))
            else -> write(listOf("sustainedPerfMode" to true, "perfPriorityBoost" to true, "preferBigCores" to true,
                PerfRootApplier.KEY_GPU_CLOCK_LOCK to true, PerfRootApplier.KEY_CPU_GOVERNOR to true,
                PerfRootApplier.KEY_CPU_FREQ_LOCK to true, PerfRootApplier.KEY_CORES_ONLINE to true))
        }
    }

    return XmbMenu(I18n.s(R.string.performance), Icons.Filled.Speed, onClose = { jobs.cancel() }) {
        val rows = mutableListOf<XmbRow>()
        val granted = rootState == RootManager.RootState.GRANTED

        fun toggle(key: String, label: String, sub: String, danger: Boolean = false) {
            val v = p2PerfValue(shortcut, key)
            val global = p2PerfGlobal(key)
            rows += XmbRow.Toggle("perf:$key", label, if (danger) Icons.Filled.Warning else Icons.Filled.Bolt, v,
                subtitle = if (v != global) "Overridden for this game · global is ${if (global) "on" else "off"}" else sub) { on ->
                profile = null
                if (on && danger) {
                    xmb.confirm(XmbConfirm(label, "Use at your own risk — writes system clock/thermal/fan files; can overheat or damage the device.", "Enable", danger = true)) {
                        setOne(key, true)
                    }
                } else setOne(key, on)
            }
        }

        rows += XmbRow.Header("hNow", I18n.s(R.string.ui_this_device_now))
        rows += XmbRow.Info(I18n.s(R.string.ui_gpuclock), I18n.s(R.string.ui_gpu_clock), Icons.Filled.Memory, readouts["gpuMhz"] ?: "…")
        rows += XmbRow.Info("socTemp", I18n.s(R.string.ui_soc_temp), Icons.Filled.Speed, readouts["socTemp"] ?: "…")
        rows += XmbRow.Info("governor", "Governor", Icons.Filled.Tune, readouts["governor"] ?: "…")

        rows += XmbRow.Header("hProfile", I18n.s(R.string.ui_power_profile))
        val profOpts = if (profile == null) listOf("Custom") + P2_PROFILES else P2_PROFILES
        rows += XmbRow.Choice("profile", I18n.s(R.string.ui_power_profile), Icons.Filled.Speed, profOpts, profile ?: "Custom",
            subtitle = I18n.s(R.string.ui_sets_the_toggles_below_in_one_go), disabledOptions = setOf("Custom")) { v -> applyProfile(v) }

        rows += XmbRow.Header("hQuick", I18n.s(R.string.ui_quick_controls_no_root))
        toggle("sustainedPerfMode", I18n.s(R.string.ui_sustained_performance_mode), I18n.s(R.string.ui_holds_a_stable_clock_to_avoid_thermal_throttling))
        toggle("perfPriorityBoost", I18n.s(R.string.ui_thread_priority_boost), I18n.s(R.string.ui_raises_emulator_thread_priority_so_the_scheduler_favours))
        toggle("preferBigCores", I18n.s(R.string.ui_prefer_big_cores), "Routes emulator threads onto the big / prime cluster")
        toggle(PerfRootApplier.KEY_GPU_CLOCK_LOCK, I18n.s(R.string.ui_lock_gpu_to_max_clock), I18n.s(R.string.ui_adreno_no_root_kgsl_turbo_sysfs_pin_with_root))

        rows += XmbRow.Header("hRoot", "Root & danger")
        rows += XmbRow.Info(I18n.s(R.string.ui_rootstate), if (granted) "Root granted" else I18n.s(R.string.ui_saved_for_this_game_applied_at_launch_when_root_is_avail),
            if (granted) Icons.Filled.Info else Icons.Filled.Warning)
        if (!granted) {
            rows += XmbRow.Action("grantRoot", I18n.s(R.string.ui_grant_root_2), Icons.Filled.Warning, subtitle = I18n.s(R.string.ui_power_user_controls_read_the_warning_first)) {
                xmb.confirm(XmbConfirm(I18n.s(R.string.ui_power_user_performance), I18n.s(R.string.ui_use_at_your_own_risk_these_controls_write_system_files_a), I18n.s(R.string.ui_grant_root), danger = true)) {
                    xmb.scope.launch {
                        RootManager.requestGrant()
                        rootState = RootManager.state.value
                    }
                }
            }
        }
        toggle(PerfRootApplier.KEY_CPU_GOVERNOR, I18n.s(R.string.ui_cpu_governor_performance), I18n.s(R.string.ui_forces_every_cluster_to_the_performance_governor))
        toggle(PerfRootApplier.KEY_CPU_FREQ_LOCK, I18n.s(R.string.ui_lock_cpu_frequency_to_max), "scaling_min_freq = scaling_max_freq on every cluster")
        toggle(PerfRootApplier.KEY_CORES_ONLINE, I18n.s(R.string.ui_keep_all_cores_online), I18n.s(R.string.ui_prevents_the_kernel_from_hot_plugging_cores_offline))
        toggle(PerfRootApplier.KEY_THERMAL_DISABLE, I18n.s(R.string.ui_disable_thermal_throttling), I18n.s(R.string.ui_runtime_watchdog_still_guards), danger = true)
        toggle(PerfRootApplier.KEY_FAN_MAX, I18n.s(R.string.ui_fan_to_maximum), I18n.s(R.string.ui_forces_active_cooling_to_full_at_launch), danger = true)

        val anyOverride = P2_PERF_KEYS.any { p2PerfValue(shortcut, it) != p2PerfGlobal(it) }
        if (anyOverride) {
            rows += XmbRow.Action("resetAll", I18n.s(R.string.ui_reset_all_to_global), Icons.Filled.RestartAlt, subtitle = I18n.s(R.string.ui_this_game_follows_app_settings_again)) {
                profile = null
                write(P2_PERF_KEYS.map { it to p2PerfGlobal(it) })
            }
        }
        rows
    }
}

// ── Player slots (PlayerSlotsEditor, per-game) ──────────────────────────────────────────────────────
// "controllerSlotOverrides" holds the same JSON the container editor and the in-game Players tab use,
// only ever parsed/built by WinHandler. Empty = inherit the container's Player Slots.

private val P2_SLOT_LABELS = listOf("Auto", I18n.s(R.string.ui_player_1), I18n.s(R.string.ui_player_2), I18n.s(R.string.ui_player_3), I18n.s(R.string.ui_player_4), "Ignore")
private fun p2SlotValue(label: String): Int = when (label) {
    I18n.s(R.string.ui_player_1) -> 0
    I18n.s(R.string.ui_player_2) -> 1
    I18n.s(R.string.ui_player_3) -> 2
    I18n.s(R.string.ui_player_4) -> 3
    "Ignore" -> WinHandler.SLOT_IGNORE
    else -> -1
}
private fun p2SlotLabel(v: Int): String = when {
    v == WinHandler.SLOT_IGNORE -> "Ignore"
    v in 0..3 -> "Player ${v + 1}"
    else -> "Auto"
}

internal fun xmbPlayerSlotsMenu(xmb: XmbScope, shortcut: Shortcut): XmbMenu {
    fun saved(): String = shortcut.p2Ex("controllerSlotOverrides", "")
    fun build(): List<PlayerSlotEditorRow> = runCatching { buildPlayerSlotEditorRows(saved().ifEmpty { "{}" }) }.getOrDefault(emptyList())
    // Enumerating input devices isn't free: rebuilt on open, after each change and on Refresh.
    var slotRows by mutableStateOf(build())

    fun pin(descriptor: String, value: Int) {
        val map = WinHandler.parseSlotOverridesJson(saved().ifEmpty { "{}" }).toMutableMap()
        if (value == -1) map.remove(descriptor) else map[descriptor] = value
        p2Put(xmb, shortcut, "controllerSlotOverrides", WinHandler.buildSlotOverridesJson(map).ifEmpty { null })
        slotRows = build()
    }

    return XmbMenu(I18n.s(R.string.ui_player_slots_2), Icons.Filled.Gamepad) {
        val rows = mutableListOf<XmbRow>()
        val json = saved()
        if (json.isEmpty()) {
            rows += XmbRow.Info("inherit", I18n.s(R.string.ui_inheriting_the_container_s_player_slots_pin_a_control), Icons.Filled.Info)
        } else {
            rows += XmbRow.Action("useDefault", I18n.s(R.string.fullscreen_mode_default), Icons.Filled.RestartAlt, subtitle = I18n.s(R.string.ui_drop_this_game_pins)) {
                p2Put(xmb, shortcut, "controllerSlotOverrides", null)
                slotRows = build()
            }
        }
        rows += XmbRow.Info("about", I18n.s(R.string.ui_pin_a_controller_or_the_on_screen_pad_to_a_player_slot_a), Icons.Filled.Info)
        rows += XmbRow.Action("refresh", I18n.s(R.string.ui_refresh_controllers), Icons.Filled.Refresh, subtitle = I18n.s(R.string.ui_re_scan_connected_pads)) { slotRows = build() }
        if (slotRows.isEmpty()) {
            rows += XmbRow.Info("none", I18n.s(R.string.ui_no_controllers_detected_connect_one_to_assign_it), Icons.Filled.Info)
        }
        val seen = HashSet<String>()
        slotRows.forEach { r ->
            if (!seen.add(r.descriptor)) return@forEach
            val status = when {
                r.override == WinHandler.SLOT_IGNORE -> "Ignored"
                r.override >= 0 -> "Pinned to Player ${r.override + 1}"
                else -> I18n.s(R.string.ui_auto_assigned_by_launch_order)
            }
            val suffix = when {
                r.isOnScreen -> I18n.s(R.string.ui_on_screen_controls)
                !r.connected -> I18n.s(R.string.ui_not_connected_saved_pin)
                else -> ""
            }
            rows += XmbRow.Choice("slot:${r.descriptor}", r.displayName, if (r.isOnScreen) Icons.Filled.TouchApp else Icons.Filled.Gamepad,
                P2_SLOT_LABELS, p2SlotLabel(r.override), subtitle = status + suffix) { v ->
                pin(r.descriptor, p2SlotValue(v))
            }
        }
        rows
    }
}
