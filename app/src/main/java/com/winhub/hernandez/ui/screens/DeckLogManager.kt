@file:OptIn(ExperimentalLayoutApi::class)

package com.winhub.hernandez.ui.screens

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.preference.PreferenceManager
import com.winhub.hernandez.SettingsFragment
import com.winhub.hernandez.core.ControllerLog
import com.winhub.hernandez.core.ExitReasonReporter
import com.winhub.hernandez.core.FileUtils
import com.winhub.hernandez.core.LogInventory
import com.winhub.hernandez.core.LogLocation
import com.winhub.hernandez.core.LogcatCapture
import com.winhub.hernandez.store.StoreDownloadTier
import com.winhub.hernandez.store.blsteam.BlSteamEngineFlag
import com.winhub.hernandez.store.blsteam.BlStoreEngineFlag
import com.winhub.hernandez.util.InAppFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

private const val TAG = "DeckLogManager"

private enum class LogTab(val labelRes: Int) {
    CAPTURE(R.string.ui_capture), STORAGE(R.string.ui_storage),
    GAMES(R.string.ui_game_logs), TOOLS(R.string.ui_tools),
    ;
    val label: String get() = I18n.s(labelRes)
}

private enum class LogActionKind { PRIMARY, NEUTRAL, DANGER }

/**
 * Deck-style Log Manager: a header with a storage meter, four sections (Capture, Storage, Game
 * logs, Tools) and Deck rows throughout. Same preferences, same [LogInventory] and same dialogs as
 * the classic [LogManagerScreen], which Settings still uses outside Deck mode. Every change is
 * written straight to the preference, as the classic screen does.
 */
@Composable
internal fun DeckLogManagerDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(LogTab.CAPTURE) }
    // Bumped after anything that changes what is on disk, to force a re-scan.
    var refreshTick by remember { mutableStateOf(0) }

    var perGame by remember { mutableStateOf(prefs.getBoolean(LogLocation.PREF_PER_GAME, true)) }
    var keepLast by remember {
        mutableStateOf(prefs.getInt(LogLocation.PREF_KEEP_LAST, LogLocation.DEFAULT_KEEP_LAST))
    }
    var wineDebug by remember { mutableStateOf(prefs.getBoolean("enable_wine_debug", false)) }
    var logcat by remember { mutableStateOf(prefs.getBoolean("enable_logcat", true)) }
    var locationMode by remember {
        mutableStateOf(prefs.getString(LogLocation.PREF_MODE, LogLocation.MODE_DOCUMENTS) ?: LogLocation.MODE_DOCUMENTS)
    }
    var customPath by remember {
        mutableStateOf(prefs.getString(LogLocation.PREF_CUSTOM_PATH, "") ?: "")
    }
    var channels by remember {
        mutableStateOf(
            (prefs.getString("wine_debug_channels", SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS)
                ?: SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS)
                .split(",").filter { it.isNotBlank() }
        )
    }
    var storeDlTier by remember { mutableStateOf(StoreDownloadTier.get(context)) }

    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    var exitReport by remember { mutableStateOf<String?>(null) }
    var showExplainAll by remember { mutableStateOf(false) }
    var browseAllChannels by remember { mutableStateOf(false) }
    var browseDir by remember { mutableStateOf<File?>(null) }
    var viewing by remember { mutableStateOf<LogInventory.Entry?>(null) }
    var confirmDelete by remember { mutableStateOf<LogInventory.Entry?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    val entries = remember(refreshTick, perGame) { LogInventory.scan(context) }
    val logDir = remember(refreshTick, locationMode, customPath) { LogLocation.resolveLogDir(context) }

    // The 521-entry catalogue is an asset read, so it stays off the main thread.
    val allChannels by produceState(emptyList<String>()) {
        value = withContext(Dispatchers.IO) {
            try {
                val arr = org.json.JSONArray(FileUtils.readString(context, "wine_debug_channels.json"))
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) {
                Log.w(TAG, "wine_debug_channels.json unreadable", e)
                emptyList()
            }
        }
    }

    fun saveMode(mode: String) {
        locationMode = mode
        prefs.edit().putString(LogLocation.PREF_MODE, mode).apply()
        refreshTick++
    }
    fun saveChannels(list: List<String>) {
        channels = list
        prefs.edit().putString("wine_debug_channels", list.joinToString(",")).apply()
    }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val dirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            InAppFilePicker.pickedPath(result.data)?.let { path ->
                customPath = path
                prefs.edit().putString(LogLocation.PREF_CUSTOM_PATH, path).apply()
                saveMode(LogLocation.MODE_CUSTOM)
            }
        }
    }

    DeckConfigDialog(
        title = I18n.s(R.string.ui_log_manager),
        subtitle = I18n.s(R.string.ui_saving_to) + (logDir?.absolutePath ?: "—"),
        onDismiss = onClose,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // LB / RB step through the sections, as everywhere else in Deck.
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val all = LogTab.values()
                    when (e.key) {
                        Key.ButtonR1 -> { tab = all[(tab.ordinal + 1) % all.size]; true }
                        Key.ButtonL1 -> { tab = all[(tab.ordinal + all.size - 1) % all.size]; true }
                        else -> false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StorageMeter(entries)
            Spacer(Modifier.height(8.dp))
            LogTabs(tab, entries.size) { tab = it }

            when (tab) {
                LogTab.CAPTURE -> {
                    DeckGroup("Recording") {
                        LogSwitchRow(
                            "Wine debug", I18n.s(R.string.ui_slows_games_down_for_diagnosing_a_problem_2), wineDebug,
                            onInfo = { info = I18n.s(R.string.ui_wine_debug) to LogCopy.WINE },
                        ) { wineDebug = it; prefs.edit().putBoolean("enable_wine_debug", it).apply() }
                        if (wineDebug) {
                            val chips = buildList<Triple<String, Boolean, () -> Unit>> {
                                (channels + COMMON_WINE_CHANNELS).distinct().forEach { ch ->
                                    val on = ch in channels
                                    add(Triple(ch, on, { saveChannels(if (on) channels - ch else channels + ch) }))
                                }
                                if (allChannels.isNotEmpty()) {
                                    add(Triple("Browse all ${allChannels.size}…", false, { browseAllChannels = true }))
                                }
                            }
                            DeckConfigChipGrid("Channels · ${channels.size} selected", chips)
                        }
                        PrefSwitchRow(prefs, "enable_box64_logs", false, "Box64 / FEXCore",
                            I18n.s(R.string.ui_slows_games_down_at_higher_verbosity_2),
                            onInfo = { info = "Box64 / FEXCore" to LogCopy.BOX64 })
                        PrefSwitchRow(prefs, "enable_dxvk_logs", true, "DXVK & VKD3D",
                            I18n.s(R.string.ui_safe_to_leave_on_mostly_written_at_startup_2),
                            onInfo = { info = "DXVK & VKD3D" to LogCopy.DXVK })
                        PrefSwitchRow(prefs, ControllerLog.PREF_KEY, false, I18n.s(R.string.ui_controller_input),
                            I18n.s(R.string.ui_for_controls_that_don_t_work_written_when_the_game_exits),
                            onInfo = { info = I18n.s(R.string.ui_controller_input) to LogCopy.CONTROLLER })
                        PrefSwitchRow(prefs, "enable_steamlite_logs", false, I18n.s(R.string.ui_steamlite_steam_client),
                            I18n.s(R.string.ui_only_when_a_game_launches_through_steamlite_2),
                            onInfo = { info = I18n.s(R.string.ui_steamlite_steam_client) to LogCopy.STEAMLITE })
                    }
                    DeckGroup("App") {
                        LogSwitchRow(
                            "Android logcat", I18n.s(R.string.ui_winhub_s_own_output_only), logcat,
                            onInfo = { info = I18n.s(R.string.ui_android_logcat) to LogCopy.LOGCAT },
                        ) { logcat = it; prefs.edit().putBoolean("enable_logcat", it).apply() }
                        PrefSwitchRow(prefs, "enable_crash_reports", true, I18n.s(R.string.ui_crash_reports),
                            I18n.s(R.string.ui_nothing_runs_until_something_crashes_2),
                            onInfo = { info = I18n.s(R.string.ui_crash_reports) to LogCopy.CRASH })
                        // Reads Android's own record of why the process died, the only way to see a
                        // native crash without root. The toggle only auto-saves on launch.
                        PrefSwitchRow(prefs, ExitReasonReporter.PREF_AUTOSAVE, false,
                            if (ExitReasonReporter.isSupported()) I18n.s(R.string.ui_exit_reasons_auto_save_on_launch)
                            else I18n.s(R.string.ui_exit_reasons_needs_android_11),
                            I18n.s(R.string.ui_records_native_crashes_without_root_2),
                            enabled = ExitReasonReporter.isSupported(),
                            onInfo = { info = I18n.s(R.string.ui_exit_reasons) to LogCopy.EXIT_REASONS })
                    }
                }

                LogTab.STORAGE -> {
                    DeckGroup(I18n.s(R.string.ui_where_logs_go)) {
                        DeckChoiceRow(
                            label = I18n.s(R.string.ui_log_location),
                            hint = logDir?.absolutePath ?: "—",
                            options = listOf(
                                "documents" to I18n.s(R.string.ui_documents),
                                "download" to "Download",
                                "custom" to if (customPath.isNotEmpty()) "Custom folder" else "Choose folder…",
                            ),
                            selected = when (locationMode) {
                                LogLocation.MODE_DOWNLOAD -> "download"
                                LogLocation.MODE_CUSTOM -> "custom"
                                // MODE_DOCUMENTS and the retired MODE_APP_DATA both resolve to Documents.
                                else -> "documents"
                            },
                            onPick = {
                                when (it) {
                                    "download" -> saveMode(LogLocation.MODE_DOWNLOAD)
                                    "documents" -> saveMode(LogLocation.MODE_DOCUMENTS)
                                    else -> dirLauncher.launch(InAppFilePicker.buildDirIntent(context, I18n.s(R.string.ui_select_log_folder)))
                                }
                            },
                        )
                        LogSwitchRow(
                            "Folder for each game", I18n.s(R.string.ui_each_game_keeps_its_own_folder_2), perGame,
                            onInfo = { info = I18n.s(R.string.ui_folder_for_each_game) to LogCopy.PER_GAME },
                        ) { perGame = it; prefs.edit().putBoolean(LogLocation.PREF_PER_GAME, it).apply(); refreshTick++ }
                    }
                    DeckGroup("Housekeeping") {
                        DeckChoiceRow(
                            label = I18n.s(R.string.ui_keep_last),
                            hint = if (keepLast == 0) "No history. Each run replaces the last."
                            else "$keepLast launch${if (keepLast == 1) "" else "es"} per game",
                            options = listOf(0, 1, 3, 5, 10, 20, 50).map { n ->
                                n.toString() to when (n) {
                                    0 -> I18n.s(R.string.ui_no_history)
                                    1 -> "1 launch"
                                    else -> "$n launches"
                                }
                            },
                            selected = keepLast.toString(),
                            onPick = {
                                keepLast = it.toInt()
                                prefs.edit().putInt(LogLocation.PREF_KEEP_LAST, keepLast).apply()
                            },
                        )
                        DeckRow(
                            I18n.s(R.string.ui_total_size),
                            LogInventory.humanBytes(LogInventory.totalBytes(entries)) +
                                " across ${entries.size} " + if (entries.size == 1) "folder" else "folders",
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LogAction("Browse", LogActionKind.NEUTRAL) { browseDir = logDir }
                                LogAction(
                                    I18n.s(R.string.ui_clear_all), LogActionKind.DANGER,
                                    modifier = Modifier.alpha(if (entries.isEmpty()) 0.4f else 1f),
                                ) { if (entries.isNotEmpty()) confirmClearAll = true }
                            }
                        }
                    }
                    if (entries.isNotEmpty()) {
                        DeckGroup(I18n.s(R.string.ui_biggest_folders)) {
                            entries.sortedByDescending { it.totalBytes }.take(5).forEach { e ->
                                DeckRow(if (e.isAppBucket) "App & crash logs" else e.name) {
                                    Text(
                                        LogInventory.humanBytes(e.totalBytes),
                                        color = DeckPalette.onSurfaceVariant,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                LogTab.GAMES -> {
                    if (entries.isEmpty()) {
                        DeckGroup(I18n.s(R.string.ui_game_logs)) {
                            DeckRow(I18n.s(R.string.ui_no_logs_yet_2), I18n.s(R.string.ui_turn_on_a_log_in_capture_then_launch_a_game)) {}
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            entries.forEach { e ->
                                DeckGameLogCard(
                                    entry = e,
                                    onView = { viewing = e },
                                    onShare = { shareLogGroup(context, e) },
                                    onDelete = { confirmDelete = e },
                                )
                            }
                        }
                    }
                }

                LogTab.TOOLS -> {
                    DeckGroup(I18n.s(R.string.ui_capture_now)) {
                        DeckRow(I18n.s(R.string.ui_capture_logcat_now), I18n.s(R.string.ui_saves_winhub_s_recent_output_to_your_log_folder)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LogInfoDot { info = I18n.s(R.string.ui_capture_logcat_now) to LogCopy.CAPTURE_NOW }
                                LogAction(
                                    "Capture", LogActionKind.NEUTRAL,
                                    modifier = Modifier.alpha(if (logcat) 1f else 0.4f),
                                ) {
                                    if (!logcat) { toast(I18n.s(R.string.ui_turn_on_android_logcat_first)); return@LogAction }
                                    // Runtime.exec, a redaction pass and a file write: not for the UI thread.
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            LogcatCapture.captureToFile(context, LogcatCapture.DEFAULT_LINES)
                                        }
                                        refreshTick++
                                        toast(I18n.s(R.string.ui_logcat_saved_to_your_log_folder))
                                    }
                                }
                            }
                        }
                        DeckRow(I18n.s(R.string.ui_read_last_crash), I18n.s(R.string.ui_why_android_ended_the_app_last_time_saved_with_your_logs)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LogInfoDot { info = I18n.s(R.string.ui_exit_reasons) to LogCopy.EXIT_REASONS }
                                LogAction(
                                    "Read", LogActionKind.NEUTRAL,
                                    modifier = Modifier.alpha(if (ExitReasonReporter.isSupported()) 1f else 0.4f),
                                ) {
                                    if (!ExitReasonReporter.isSupported()) { toast(I18n.s(R.string.ui_needs_android_11_or_newer)); return@LogAction }
                                    scope.launch {
                                        val text = withContext(Dispatchers.IO) {
                                            ExitReasonReporter.captureToFile(context)
                                            ExitReasonReporter.capture(context)
                                        }
                                        exitReport = text
                                        refreshTick++
                                    }
                                }
                            }
                        }
                    }
                    DeckGroup(I18n.s(R.string.ui_native_engines)) {
                        PrefSwitchRow(prefs, BlSteamEngineFlag.PREF_KEY, BlSteamEngineFlag.DEFAULT,
                            I18n.s(R.string.ui_native_steam_engine_restart_required),
                            "On: the whole Steam session runs natively. Off: legacy Java engine.",
                            onInfo = { info = I18n.s(R.string.ui_rust_steam_engine) to LogCopy.RUST_ENGINE })
                        PrefSwitchRow(prefs, BlStoreEngineFlag.PREF_KEY_EPIC, BlStoreEngineFlag.DEFAULT,
                            "Rust engine: Epic downloads", I18n.s(R.string.ui_applies_to_the_next_download),
                            onInfo = { info = I18n.s(R.string.ui_rust_store_engines) to LogCopy.RUST_STORE_ENGINES })
                        PrefSwitchRow(prefs, BlStoreEngineFlag.PREF_KEY_GOG, BlStoreEngineFlag.DEFAULT,
                            "Rust engine: GOG downloads", I18n.s(R.string.ui_applies_to_the_next_download),
                            onInfo = { info = I18n.s(R.string.ui_rust_store_engines) to LogCopy.RUST_STORE_ENGINES })
                        PrefSwitchRow(prefs, BlStoreEngineFlag.PREF_KEY_AMAZON, BlStoreEngineFlag.DEFAULT,
                            "Rust engine: Amazon downloads", I18n.s(R.string.ui_applies_to_the_next_download),
                            onInfo = { info = I18n.s(R.string.ui_rust_store_engines) to LogCopy.RUST_STORE_ENGINES })
                        // One app-wide ceiling for the three store engines; tap cycles the tier.
                        DeckRow(I18n.s(R.string.ui_store_download_speed), I18n.s(R.string.ui_ceiling_for_the_epic_gog_and_amazon_engines)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LogInfoDot { info = I18n.s(R.string.ui_rust_store_engines) to LogCopy.RUST_STORE_ENGINES }
                                LogAction(StoreDownloadTier.label(storeDlTier), LogActionKind.NEUTRAL) {
                                    storeDlTier = StoreDownloadTier.next(storeDlTier)
                                    StoreDownloadTier.set(context, storeDlTier)
                                }
                            }
                        }
                    }
                    DeckGroup("Help") {
                        DeckRow(I18n.s(R.string.ui_explain_the_log_types), I18n.s(R.string.ui_what_each_log_is_for_and_what_it_costs)) {
                            LogAction("Open", LogActionKind.NEUTRAL) { showExplainAll = true }
                        }
                    }
                    Text(
                        // Names what is actually stripped, and says plainly that paths are kept.
                        I18n.s(R.string.ui_e_mail_addresses_auth_tokens_and_your_steam_account_name) +
                            "written. File paths are kept so they stay useful for debugging, so glance over " +
                            I18n.s(R.string.ui_them_before_sharing_if_a_folder_name_identifies_you_logc) +
                            I18n.s(R.string.ui_winhub_s_own_output),
                        color = DeckPalette.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    )
                }
            }

            DeckHintBar(
                hints = listOf("A" to "Select", "B" to "Back", "LB RB" to I18n.s(R.string.ui_section)),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    // The File Manager opened at the log folder; re-scan on close since it can delete or move.
    browseDir?.let { dir ->
        Dialog(
            onDismissRequest = { browseDir = null; refreshTick++ },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = DeckPalette.background) {
                FileManagerScreen(initialDir = dir)
            }
        }
    }

    viewing?.let { entry ->
        Dialog(
            onDismissRequest = { viewing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = DeckPalette.background) {
                LogViewerScreen(entry = entry, onClose = { viewing = null }, deck = true)
            }
        }
    }

    exitReport?.let { report ->
        DeckConfigDialog(title = "Last crash / exit reasons", onDismiss = { exitReport = null }) {
            DeckConfigNote(
                "Saved with your logs under the \"" + ExitReasonReporter.FOLDER +
                    "\" folder. Exit #0 is the most recent; a NATIVE_CRASH entry includes the " +
                    I18n.s(R.string.ui_signal_and_the_top_backtrace_frames)
            )
            Text(
                report,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = DeckPalette.onBackground,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                LogAction("Close", LogActionKind.PRIMARY) { exitReport = null }
            }
        }
    }

    confirmDelete?.let { entry ->
        val count = remember(entry.dir) { LogInventory.deletableCount(entry) }
        DeckConfigDialog(
            title = when {
                entry.isAppBucket -> I18n.s(R.string.ui_delete_app_crash_logs)
                entry.isLooseBucket -> "Delete ${entry.name.replaceFirstChar { it.lowercase() }}?"
                else -> "Delete ${entry.name} logs?"
            },
            onDismiss = { confirmDelete = null },
            compact = true,
        ) {
            Text(
                if (count == 0)
                    // The loose bucket counts every log-shaped file, but only ours are deletable.
                    I18n.s(R.string.ui_nothing_here_was_written_by_winhub_so_there_is_nothing_t) +
                        "The files in this folder are yours and are left alone."
                else
                    "Deletes $count log file${if (count == 1) "" else "s"}, including any kept " +
                        "history.\n\nOnly files WinHub wrote are removed. Anything else in that " +
                        I18n.s(R.string.ui_folder_is_left_alone),
                color = DeckPalette.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogAction(if (count == 0) "OK" else "Cancel", LogActionKind.NEUTRAL) { confirmDelete = null }
                if (count > 0) {
                    LogAction("Delete", LogActionKind.DANGER) {
                        confirmDelete = null
                        scope.launch {
                            val n = withContext(Dispatchers.IO) { LogInventory.deleteGroup(context, entry) }
                            refreshTick++
                            toast("Deleted $n file${if (n == 1) "" else "s"}")
                        }
                    }
                }
            }
        }
    }

    if (confirmClearAll) {
        val count = remember(refreshTick) { entries.sumOf { LogInventory.deletableCount(it) } }
        DeckConfigDialog(title = I18n.s(R.string.ui_clear_all_logs), onDismiss = { confirmClearAll = false }, compact = true) {
            Text(
                "Deletes $count log file${if (count == 1) "" else "s"} across ${entries.size} " +
                    "folder${if (entries.size == 1) "" else "s"}, including kept history.\n\n" +
                    "Only files WinHub wrote are removed. Other files in your log folder, " +
                    I18n.s(R.string.ui_including_anything_you_put_there_yourself_are_left_alone_2),
                color = DeckPalette.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogAction("Cancel", LogActionKind.NEUTRAL) { confirmClearAll = false }
                LogAction(I18n.s(R.string.ui_delete_all), LogActionKind.DANGER) {
                    confirmClearAll = false
                    val targets = entries
                    scope.launch {
                        val n = withContext(Dispatchers.IO) { targets.sumOf { LogInventory.deleteGroup(context, it) } }
                        refreshTick++
                        toast("Deleted $n file${if (n == 1) "" else "s"}")
                    }
                }
            }
        }
    }

    if (browseAllChannels) {
        AllChannelsDialog(
            all = allChannels,
            selected = channels,
            onChange = { saveChannels(it) },
            onDismiss = { browseAllChannels = false },
        )
    }
    info?.let { (title, body) -> PerfInfoDialog(title = title, body = body, onDismiss = { info = null }) }
    if (showExplainAll) {
        PerfInfoDialog(title = I18n.s(R.string.ui_what_each_log_is_for), body = LogCopy.explainAll(), onDismiss = { showExplainAll = false })
    }
}

/** Storage used, with a bar split into the three biggest folders and the rest. */
@Composable
private fun StorageMeter(entries: List<LogInventory.Entry>) {
    val total = LogInventory.totalBytes(entries)
    val sorted = entries.sortedByDescending { it.totalBytes }
    val top = (0..2).map { sorted.getOrNull(it)?.totalBytes ?: 0L }
    val parts = top + (total - top.sum()).coerceAtLeast(0L)
    val colors = listOf(
        DeckPalette.primary,
        DeckPalette.primary.copy(alpha = 0.6f),
        DeckPalette.secondary.copy(alpha = 0.5f),
        DeckPalette.line2,
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(I18n.s(R.string.ui_storage_used), color = DeckPalette.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                LogInventory.humanBytes(total) + " · ${entries.size} " + if (entries.size == 1) "folder" else "folders",
                color = DeckPalette.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DeckPalette.surfaceVariant),
        ) {
            if (total > 0) {
                parts.forEachIndexed { i, bytes ->
                    if (bytes > 0) Box(Modifier.weight(bytes.toFloat()).fillMaxHeight().background(colors[i]))
                }
            }
        }
    }
}

@Composable
private fun LogTabs(selected: LogTab, gameCount: Int, onSelect: (LogTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
    ) {
        LogTab.values().forEach { t ->
            val sel = t == selected
            val shape = RoundedCornerShape(12.dp)
            val interaction = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(if (sel) DeckPalette.secondary.copy(alpha = 0.18f) else DeckPalette.surface)
                    .border(1.dp, if (sel) DeckPalette.secondary else DeckPalette.line, shape)
                    .deckOuterFocusStroke(interaction = interaction, shape = shape)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(t) }
                    .padding(horizontal = 18.dp),
            ) {
                Text(
                    if (t == LogTab.GAMES && gameCount > 0) "${t.label} · $gameCount" else t.label,
                    color = if (sel) DeckPalette.onBackground else DeckPalette.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A Deck row with an always-live "?" and the switch. A locked toggle must still be explainable. */
@Composable
private fun LogSwitchRow(
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean = true,
    onInfo: () -> Unit,
    onChange: (Boolean) -> Unit,
) {
    DeckRow(label, hint) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogInfoDot(onInfo)
            Box(Modifier.alpha(if (enabled) 1f else 0.4f)) {
                DeckToggle(checked) { if (enabled) onChange(it) }
            }
        }
    }
}

/** [LogSwitchRow] backed directly by one boolean preference. */
@Composable
private fun PrefSwitchRow(
    prefs: SharedPreferences,
    key: String,
    default: Boolean,
    label: String,
    hint: String,
    enabled: Boolean = true,
    onInfo: () -> Unit,
) {
    var on by remember { mutableStateOf(prefs.getBoolean(key, default)) }
    LogSwitchRow(label, hint, on, enabled, onInfo) {
        on = it
        prefs.edit().putBoolean(key, it).apply()
    }
}

@Composable
private fun LogInfoDot(onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .deckSelfFocusStroke(shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            Icons.Outlined.HelpOutline, I18n.s(R.string.ui_what_this),
            tint = DeckPalette.secondary, modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun LogAction(
    text: String,
    kind: LogActionKind,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val primary = kind == LogActionKind.PRIMARY
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (primary) DeckPalette.primary else DeckPalette.surfaceVariant)
            .border(1.dp, if (primary) DeckPalette.primary else DeckPalette.line2, shape)
            .deckOuterFocusStroke(
                interaction = interaction,
                shape = shape,
                focusColor = if (primary) DeckPalette.onPrimary else DeckPalette.primary,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text,
            color = when (kind) {
                LogActionKind.PRIMARY -> DeckPalette.onPrimary
                LogActionKind.DANGER -> DeckPalette.error
                LogActionKind.NEUTRAL -> DeckPalette.onBackground
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** One game's logs: icon, name, what is on disk, then View / Share / Delete. The chevron lists the files. */
@Composable
private fun DeckGameLogCard(
    entry: LogInventory.Entry,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeckPalette.groupShape)
            .background(DeckPalette.surface)
            .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeckPalette.surfaceVariant)
                    .border(1.dp, DeckPalette.line2, RoundedCornerShape(14.dp)),
            ) {
                Icon(
                    when {
                        entry.isAppBucket -> Icons.Default.PhoneAndroid
                        // Loose files in the log root are not a game and must not look like one.
                        entry.isLooseBucket -> Icons.Default.History
                        entry.dir.name.startsWith("Container") -> Icons.Default.Settings
                        else -> Icons.Default.SportsEsports
                    },
                    null, tint = DeckPalette.primary, modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (entry.isAppBucket) "App & crash logs" else entry.name,
                    color = DeckPalette.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append("${entry.fileCount} file${if (entry.fileCount == 1) "" else "s"}")
                        append(" · ").append(LogInventory.humanBytes(entry.totalBytes))
                        if (entry.archivedRuns > 0) append(" · ${entry.archivedRuns} kept")
                        append(" · ").append(relativeTime(entry.lastModified))
                    },
                    color = DeckPalette.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            val shape = RoundedCornerShape(10.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .deckSelfFocusStroke(shape)
                    .clickable { expanded = !expanded },
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide files" else I18n.s(R.string.ui_show_files),
                    tint = DeckPalette.onSurfaceVariant,
                )
            }
        }
        if (expanded) {
            val files = remember(entry.dir) { LogInventory.filesIn(entry.dir) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                files.forEach { f ->
                    Text(
                        "${f.name} · ${LogInventory.humanBytes(f.length())}",
                        color = DeckPalette.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeckPalette.background)
                            .border(1.dp, DeckPalette.line, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            LogAction("View", LogActionKind.PRIMARY, Modifier.weight(1f), onView)
            LogAction("Share", LogActionKind.NEUTRAL, Modifier.weight(1f), onShare)
            LogAction("Delete", LogActionKind.DANGER, Modifier.weight(1f), onDelete)
        }
    }
}
