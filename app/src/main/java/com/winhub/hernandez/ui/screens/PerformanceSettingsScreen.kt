package com.winhub.hernandez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.winhub.hernandez.ui.components.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HelpOutline
import com.winhub.hernandez.ui.components.Button
import androidx.compose.material3.Icon
import com.winhub.hernandez.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.winhub.hernandez.ui.components.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winhub.hernandez.perf.PerfGpuTurbo
import com.winhub.hernandez.perf.PerfRootApplier
import com.winhub.hernandez.perf.PerformanceSettings
import com.winhub.hernandez.perf.RootManager
import kotlinx.coroutines.launch
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * App Settings → Performance menu. Binds the GLOBAL DEFAULTS (non-root three + root six) to the same
 * [PerformanceSettings] flows the in-game drawer reads, so a change here is reflected live in the
 * other surface (two-way sync via one store).
 *
 * Root tier: a real grant gate (scroll-to-bottom + accept disclaimer -> [RootManager.requestGrant]),
 * then the root toggles applied live through [PerfRootApplier] (snapshot-before-write; reverted on
 * exit/background/crash). The two dangerous toggles (thermal disable, fan max) stay disabled until the
 * safety harness is proven. The temperature watchdog is device-wide; turning it OFF requires its own
 * hard disclaimer.
 */
@Composable
fun PerformanceSettingsScreen(onClose: () -> Unit) {
    val sustained by PerformanceSettings.sustainedPerfMode.collectAsState()
    val priority by PerformanceSettings.perfPriorityBoost.collectAsState()
    val bigCores by PerformanceSettings.preferBigCores.collectAsState()
    val rootState by PerformanceSettings.rootState.collectAsState()
    val harnessProven by PerformanceSettings.harnessProven.collectAsState()
    // "Auto deep-clean on launch" (Tier 2) global default — persisted via the same root-default store.
    val autoDeepClean by PerformanceSettings.rootDefaultFlow(PerfRootApplier.KEY_AUTO_DEEP_CLEAN).collectAsState()

    val scope = rememberCoroutineScope()
    var showRootDisclaimer by remember { mutableStateOf(false) }
    // Per-toggle "?" info dialog: (title, body). And the consolidated "Explain toggles" sheet.
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showExplainAll by remember { mutableStateOf(false) }

    val granted = rootState == RootManager.RootState.GRANTED

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Performance",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Text(
                I18n.s(R.string.ui_global_defaults_apply_to_every_game_unless_a_game_sets_i) +
                    "(in the game's settings or from the in-game menu).",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )

            // ── Non-root global defaults (always editable) ──
            PerfCard(title = I18n.s(R.string.ui_global_defaults)) {
                PerfToggle(I18n.s(R.string.ui_sustained_performance_mode), sustained,
                    onInfo = { info = I18n.s(R.string.ui_sustained_performance_mode) to PerfCopy.SUSTAINED }) { PerformanceSettings.setSustainedPerfMode(it) }
                PerfToggle(I18n.s(R.string.ui_thread_priority_boost), priority,
                    onInfo = { info = I18n.s(R.string.ui_thread_priority_boost) to PerfCopy.PRIORITY }) { PerformanceSettings.setPerfPriorityBoost(it) }
                PerfToggle(I18n.s(R.string.ui_prefer_big_cores), bigCores,
                    onInfo = { info = I18n.s(R.string.ui_prefer_big_cores) to PerfCopy.BIG_CORES }) { PerformanceSettings.setPreferBigCores(it) }
                // GPU pin lives here (not in the root card) because it now has a non-root path on
                // Adreno. It still upgrades itself to the stronger sysfs pin when root is granted.
                RootToggle(PerfRootApplier.KEY_GPU_CLOCK_LOCK, I18n.s(R.string.ui_lock_gpu_to_max_clock), granted, harnessProven,
                    onInfo = { info = I18n.s(R.string.ui_lock_gpu_to_max_clock) to PerfCopy.GPU_CLOCK })
                if (!granted && !PerfGpuTurbo.isSupported) {
                    Text(I18n.s(R.string.ui_lock_gpu_to_max_clock_needs_an_adreno_qualcomm_gpu_or),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }

            // ── Root tier ──
            PerfCard(title = I18n.s(R.string.ui_root_performance_controls)) {
                Text(I18n.s(R.string.ui_root_status) + rootStateLabel(rootState),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                when (rootState) {
                    RootManager.RootState.UNAVAILABLE -> {
                        Text("No root manager detected (Magisk / KernelSU / APatch). Root controls stay disabled.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    RootManager.RootState.GRANTED -> { /* toggles below are enabled */ }
                    else -> {
                        // AVAILABLE_NOT_GRANTED or DENIED -> offer (or re-offer) the grant.
                        val label = if (rootState == RootManager.RootState.DENIED) "Grant Root (retry)" else "Grant Root"
                        Button(onClick = { showRootDisclaimer = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(label, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                RootToggle(PerfRootApplier.KEY_CPU_GOVERNOR, I18n.s(R.string.ui_cpu_governor_performance), granted, harnessProven,
                    onInfo = { info = I18n.s(R.string.ui_cpu_governor_performance) to PerfCopy.CPU_GOV })
                RootToggle(PerfRootApplier.KEY_CPU_FREQ_LOCK, I18n.s(R.string.ui_lock_cpu_frequency_to_max), granted, harnessProven,
                    onInfo = { info = I18n.s(R.string.ui_lock_cpu_frequency_to_max) to PerfCopy.CPU_FREQ })
                RootToggle(PerfRootApplier.KEY_CORES_ONLINE, I18n.s(R.string.ui_keep_all_cores_online), granted, harnessProven,
                    onInfo = { info = I18n.s(R.string.ui_keep_all_cores_online) to PerfCopy.CORES_ONLINE })
                RootToggle(PerfRootApplier.KEY_THERMAL_DISABLE, I18n.s(R.string.ui_disable_thermal_throttling), granted, harnessProven,
                    onInfo = { info = I18n.s(R.string.ui_disable_thermal_throttling) to PerfCopy.THERMAL })
                RootToggle(PerfRootApplier.KEY_FAN_MAX, I18n.s(R.string.ui_fan_to_maximum), granted, harnessProven,
                    onInfo = { info = I18n.s(R.string.ui_fan_to_maximum) to PerfCopy.FAN })

                if (granted && !harnessProven) {
                    Text("Thermal / fan controls are locked until safety-revert is verified on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }

                // ── Free memory (dual tier). Section "?" explains both tiers + the auto toggle. ──
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(I18n.s(R.string.ui_free_memory), color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    InfoButton { info = I18n.s(R.string.ui_free_memory) to PerfCopy.FREE_MEM_SECTION }
                }

                // TIER 1 — drop file caches (light; near-invisible RAM by design). Root-gated as before.
                Button(
                    onClick = { PerfRootApplier.freeMemoryNow() },
                    enabled = granted,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(I18n.s(R.string.ui_drop_file_caches), color = MaterialTheme.colorScheme.onPrimary) }
                Text(I18n.s(R.string.ui_frees_cached_files_little_visible_ram_the_system_recl),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                // TIER 2 — deep clean (root-only; the real RAM free via `am kill-all`).
                Button(
                    onClick = { PerfRootApplier.deepCleanMemory() },
                    enabled = granted,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(I18n.s(R.string.ui_deep_clean_free_app_memory), color = MaterialTheme.colorScheme.onPrimary) }
                Text(I18n.s(R.string.ui_force_closes_background_apps_to_free_real_memory_won),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                // AUTO toggle — persists like the other root defaults; fires at game launch.
                PerfToggle(I18n.s(R.string.ui_auto_deep_clean_on_launch), autoDeepClean, enabled = granted,
                    onInfo = { info = I18n.s(R.string.ui_auto_deep_clean_on_launch) to PerfCopy.AUTO_DEEP_CLEAN }) {
                    PerformanceSettings.setRootDefault(PerfRootApplier.KEY_AUTO_DEEP_CLEAN, it)
                }
            }

            // ── Temperature watchdog (device-wide; not root-gated). Shared control block, identical
            // and synced with the in-game surface (both bind the one TempWatchdog singleton). ──
            PerfCard(title = I18n.s(R.string.ui_temperature_watchdog)) {
                Text(
                    I18n.s(R.string.ui_auto_reverts_all_performance_settings_before_the_device) +
                        I18n.s(R.string.ui_your_device_s_own_thermal_trip_points_keep_this_on_unles),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
                WatchdogSection()
            }

            Text(
                I18n.s(R.string.ui_auto_revert_on_game_exit_app_background_and_crash_is_alw),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
            )

            Button(onClick = { showExplainAll = true }, modifier = Modifier.fillMaxWidth()) {
                Text(I18n.s(R.string.ui_explain_toggles), color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    info?.let { (title, body) ->
        PerfInfoDialog(title = title, body = body, onDismiss = { info = null })
    }

    if (showExplainAll) {
        PerfInfoDialog(title = I18n.s(R.string.ui_what_the_toggles_do), body = explainAllBody(), onDismiss = { showExplainAll = false })
    }

    if (showRootDisclaimer) {
        PerfDisclaimerDialog(
            title = I18n.s(R.string.ui_power_user_performance_read_first),
            body = PerfDisclaimerCopy.ROOT_RISK,
            confirmLabel = I18n.s(R.string.ui_grant_root),
            onDismiss = { showRootDisclaimer = false },
            onConfirm = {
                showRootDisclaimer = false
                scope.launch { RootManager.requestGrant() } // fires the su prompt; state updates live
            }
        )
    }
}

/** A PerfRootApplier-owned toggle bound to its global default; applies live via PerfRootApplier. */
@Composable
private fun RootToggle(key: String, label: String, granted: Boolean, harnessProven: Boolean, onInfo: () -> Unit) {
    val checked by PerformanceSettings.rootDefaultFlow(key).collectAsState()
    val gated = PerfRootApplier.isHarnessGated(key) && !harnessProven
    // The GPU pin is usable without root on Adreno (KGSL turbo); everything else needs the grant.
    val usableWithoutRoot = key == PerfRootApplier.KEY_GPU_CLOCK_LOCK && PerfGpuTurbo.isSupported
    val enabled = (granted || usableWithoutRoot) && !gated
    PerfToggle(label, checked, enabled = enabled, onInfo = onInfo) { on ->
        PerformanceSettings.setRootDefault(key, on)
        PerfRootApplier.apply(key, on)
    }
}

private fun rootStateLabel(state: RootManager.RootState): String = when (state) {
    RootManager.RootState.UNKNOWN -> "checking…"
    RootManager.RootState.UNAVAILABLE -> I18n.s(R.string.ui_not_available_on_this_device)
    RootManager.RootState.AVAILABLE_NOT_GRANTED -> I18n.s(R.string.ui_available_not_granted)
    RootManager.RootState.GRANTED -> "granted"
    RootManager.RootState.DENIED -> "denied"
}

@Composable
private fun PerfCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun PerfToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onInfo: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Only the label + switch dim/gate on `enabled`; the "?" stays live so a locked toggle is
        // still explainable.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier.alpha(0.4f))
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        }
        if (onInfo != null) InfoButton(onInfo)
        Spacer(Modifier.width(4.dp))
        Row(modifier = if (enabled) Modifier else Modifier.alpha(0.4f)) {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

/** Small "?" affordance that opens a soft info dialog. Always live (even for disabled toggles). */
@Composable
private fun InfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Outlined.HelpOutline, I18n.s(R.string.ui_what_this),
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

private object PerfCopy {
    val SUSTAINED = I18n.s(R.string.ui_keeps_clock_speeds_steady_instead_of_spiking_and_droppin)
    val PRIORITY = I18n.s(R.string.ui_gives_the_game_s_rendering_higher_priority_than_other_ap)
    val BIG_CORES = I18n.s(R.string.ui_pins_the_game_to_your_fastest_cpu_cores_instead_of_the_e)
    val CPU_GOV = I18n.s(R.string.ui_forces_the_cpu_to_run_fast_instead_of_ramping_on_demand)
    const val CPU_FREQ = "Pins the CPU at max clock so it never slows down. Top performance, highest heat/battery. (Root)"
    val CORES_ONLINE = I18n.s(R.string.ui_stops_cores_being_put_to_sleep_keeping_every_core_availa)
    val GPU_CLOCK =
        I18n.s(R.string.ui_pins_the_gpu_at_top_speed_so_it_stops_dropping_clocks_to) +
        "consistent, but hotter and heavier on battery.\n\n" +
        "No root needed on Adreno (Qualcomm) GPUs — this is the same \"Adreno turbo\" trick Switch " +
        "emulators use: it asks the GPU driver directly to stop scaling clocks down, through a device " +
        "file the app is already allowed to open.\n\n" +
        I18n.s(R.string.ui_with_root_granted_it_switches_to_a_stronger_system_level) +
        I18n.s(R.string.ui_device_s_own_heat_protection_still_applies_and_the_setti) +
        "background the app, or it crashes.\n\n" +
        I18n.s(R.string.ui_on_non_adreno_gpus_mali_xclipse_powervr_this_toggle_need)
    val THERMAL = I18n.s(R.string.ui_removes_your_device_s_built_in_heat_protection_can_overh)
    val FAN = I18n.s(R.string.ui_runs_the_fan_at_full_speed_for_max_cooling_devices_with)
    val FREE_MEM_SECTION =
        "Two ways to free memory, from lightest to strongest.\n\n" +
        "Drop file caches (Tier 1)\n" +
        "Writes to the kernel's drop-caches control to release cached files. This is light and " +
        I18n.s(R.string.ui_near_invisible_by_design_the_system_refills_the_cache_al) +
        "see the free-RAM number move. It does NOT close any apps.\n\n" +
        "Deep clean (Tier 2)\n" +
        I18n.s(R.string.ui_the_real_ram_free_force_closes_background_apps_to_reclai) +
        "\"kill background\" command (am kill-all), which only touches apps the system considers safe " +
        "to kill — it will NOT close your running game, this app, or system/persistent apps. This is " +
        "the same kind of thing a phone's built-in \"RAM cleaner\" / Game Turbo does.\n\n" +
        "Auto deep-clean on launch\n" +
        I18n.s(R.string.ui_when_on_a_deep_clean_runs_automatically_each_time_you_st) +
        "with the most free RAM available.\n\n" +
        I18n.s(R.string.ui_root_is_required_for_deep_clean_and_for_dropping_caches)
    val AUTO_DEEP_CLEAN =
        "Runs a deep clean automatically every time a game/container launches — force-closes safe " +
        I18n.s(R.string.ui_background_apps_via_am_kill_all_never_your_game_or_syste)
}

/** Consolidated explainer body, grouped by root requirement. */
private fun explainAllBody(): String = buildString {
    append("No root needed\n\n")
    append("• Sustained Performance Mode\n${PerfCopy.SUSTAINED}\n\n")
    append("• Thread Priority Boost\n${PerfCopy.PRIORITY}\n\n")
    append("• Prefer Big Cores\n${PerfCopy.BIG_CORES}\n\n")
    append("• Lock GPU to max clock\n${PerfCopy.GPU_CLOCK}\n\n")
    append("\nRequires root\n\n")
    append("• CPU governor → performance\n${PerfCopy.CPU_GOV}\n\n")
    append("• Lock CPU frequency to max\n${PerfCopy.CPU_FREQ}\n\n")
    append("• Keep all cores online\n${PerfCopy.CORES_ONLINE}\n\n")
    append("• Disable thermal throttling\n${PerfCopy.THERMAL}\n\n")
    append("• Fan to maximum\n${PerfCopy.FAN}\n\n")
    append("• Free memory\n${PerfCopy.FREE_MEM_SECTION}\n")
}
