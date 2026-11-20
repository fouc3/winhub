package com.winhub.hernandez.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.winhub.hernandez.perf.PerfRootApplier
import com.winhub.hernandez.perf.PerformanceSettings
import com.winhub.hernandez.ui.rememberLiveReadouts
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * Deck-native replacement for [com.winhub.hernandez.ui.PerformanceDashboardDialogPerGame]. Same state
 * surface (per-game overrides + root presets + profiles + reset), re-dressed with Deck widgets so the
 * Deck shortcut editor never falls back to classic Material for its popups.
 */
@Composable
internal fun DeckPerformanceDialog(
    sustained: Boolean, onSustained: (Boolean) -> Unit,
    priority: Boolean, onPriority: (Boolean) -> Unit,
    bigCores: Boolean, onBigCores: (Boolean) -> Unit,
    rootValue: (String) -> Boolean, onRoot: (String, Boolean) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val readouts = rememberLiveReadouts()
    var profile by remember { mutableStateOf<String?>(null) }
    var pendingDanger by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun globalOf(key: String): Boolean = when (key) {
        "sustainedPerfMode" -> PerformanceSettings.sustainedPerfMode.value
        "perfPriorityBoost" -> PerformanceSettings.perfPriorityBoost.value
        "preferBigCores" -> PerformanceSettings.preferBigCores.value
        else -> PerformanceSettings.rootDefaultValue(key)
    }

    fun applyProfile(p: String) {
        profile = p
        when (p) {
            "Off", "Battery" -> {
                onSustained(false); onPriority(false); onBigCores(false)
                onRoot(PerfRootApplier.KEY_GPU_CLOCK_LOCK, false)
                onRoot(PerfRootApplier.KEY_CPU_GOVERNOR, false)
                onRoot(PerfRootApplier.KEY_CPU_FREQ_LOCK, false)
                onRoot(PerfRootApplier.KEY_CORES_ONLINE, false)
            }
            "Balanced" -> {
                onSustained(true); onPriority(false); onBigCores(false)
                onRoot(PerfRootApplier.KEY_GPU_CLOCK_LOCK, false)
            }
            else -> {
                onSustained(true); onPriority(true); onBigCores(true)
                onRoot(PerfRootApplier.KEY_GPU_CLOCK_LOCK, true)
                onRoot(PerfRootApplier.KEY_CPU_GOVERNOR, true)
                onRoot(PerfRootApplier.KEY_CPU_FREQ_LOCK, true)
                onRoot(PerfRootApplier.KEY_CORES_ONLINE, true)
            }
        }
    }

    DeckConfigDialog(title = I18n.s(R.string.performance), onDismiss = onDismiss) {
        DeckConfigNote(I18n.s(R.string.ui_per_game_preset_saved_on_ok_honored_at_launch))

        DeckConfigPanel("Device") {
            DeckRow("GPU clock", readouts["gpuMhz"]?.let { "$it MHz" } ?: "—") {}
            DeckRow("SoC temp", readouts["socTemp"]?.let { "$it °C" } ?: "—") {}
            DeckRow("Governor", readouts["governor"] ?: "—") {}
        }

        DeckConfigChipGrid(
            label = I18n.s(R.string.ui_power_profile),
            chips = listOf("Off", I18n.s(R.string.ui_battery), I18n.s(R.string.ui_balanced), I18n.s(R.string.performance), I18n.s(R.string.ui_turbo))
                .map { Triple(it, profile == it, { applyProfile(it) }) },
        )

        DeckGroup("Quick controls · no root") {
            DeckRow(I18n.s(R.string.ui_sustained_performance_mode), "Holds a stable clock to avoid thermal throttling.") {
                DeckToggle(sustained, onSustained)
            }
            DeckRow(I18n.s(R.string.ui_thread_priority_boost), "Raises emulator thread priority so the scheduler favours the game.") {
                DeckToggle(priority, onPriority)
            }
            DeckRow(I18n.s(R.string.ui_prefer_big_cores), "Routes emulator threads onto the big / prime cluster.") {
                DeckToggle(bigCores, onBigCores)
            }
            DeckRow(I18n.s(R.string.ui_lock_gpu_to_max_clock), "Adreno: no-root KGSL turbo; sysfs pin with root.") {
                val k = PerfRootApplier.KEY_GPU_CLOCK_LOCK
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
        }

        DeckGroup("Root & danger") {
            DeckConfigNote(I18n.s(R.string.ui_saved_for_this_game_applied_at_launch_when_root_is_av))
            DeckRow(I18n.s(R.string.ui_cpu_governor_performance), "Forces every cluster to the performance governor.") {
                val k = PerfRootApplier.KEY_CPU_GOVERNOR
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
            DeckRow(I18n.s(R.string.ui_lock_cpu_frequency_to_max), "scaling_min_freq = scaling_max_freq on every cluster.") {
                val k = PerfRootApplier.KEY_CPU_FREQ_LOCK
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
            DeckRow(I18n.s(R.string.ui_keep_all_cores_online), "Prevents the kernel from hot-plugging cores offline.") {
                val k = PerfRootApplier.KEY_CORES_ONLINE
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
            DeckRow(I18n.s(R.string.ui_disable_thermal_throttling), "Runtime watchdog still guards.") {
                val k = PerfRootApplier.KEY_THERMAL_DISABLE
                DeckToggle(rootValue(k)) { on ->
                    if (on) pendingDanger = "Disable thermal throttling" to { onRoot(k, true) } else onRoot(k, false)
                }
            }
            DeckRow(I18n.s(R.string.ui_fan_to_maximum), "Forces active cooling to full at launch.") {
                val k = PerfRootApplier.KEY_FAN_MAX
                DeckToggle(rootValue(k)) { on ->
                    if (on) pendingDanger = "Fan to maximum" to { onRoot(k, true) } else onRoot(k, false)
                }
            }
        }

        val anyOverride = sustained != globalOf("sustainedPerfMode") ||
            priority != globalOf("perfPriorityBoost") ||
            bigCores != globalOf("preferBigCores") ||
            PerfRootApplier.ROOT_KEYS.any { rootValue(it) != globalOf(it) }
        if (anyOverride) {
            DeckButton(text = I18n.s(R.string.ui_reset_all_to_global), primary = false, onClick = onResetAll)
        }
    }

    pendingDanger?.let { pd ->
        PerfDisclaimerDialog(
            title = pd.first,
            body = PerfDisclaimerCopy.ROOT_RISK,
            confirmLabel = "Enable",
            onConfirm = { pd.second(); pendingDanger = null },
            onDismiss = { pendingDanger = null },
        )
    }
}
