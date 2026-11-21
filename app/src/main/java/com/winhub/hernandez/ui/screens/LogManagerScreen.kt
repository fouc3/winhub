package com.winhub.hernandez.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.winhub.hernandez.ui.components.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AssistChip
import com.winhub.hernandez.ui.components.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.winhub.hernandez.ui.components.TextButton
import androidx.compose.material3.Icon
import com.winhub.hernandez.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.winhub.hernandez.ui.components.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.winhub.hernandez.core.FileUtils
import com.winhub.hernandez.util.InAppFilePicker
import com.winhub.hernandez.core.ExitReasonReporter
import com.winhub.hernandez.core.LogInventory
import com.winhub.hernandez.core.LogLocation
import com.winhub.hernandez.core.LogcatCapture
import com.winhub.hernandez.ui.theme.DangerRed
import kotlinx.coroutines.launch
import java.io.File
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * App Settings → Log Manager. One place for everything logging: where logs go, which ones are
 * produced, how many past runs are kept, and what is on disk right now per game.
 *
 * Mirrors the Performance screen deliberately — same card layout, same always-live "?" help button
 * on every toggle. Here the help copy leads with the PERFORMANCE COST, because two of these
 * genuinely slow games down and users need to know that before leaving one on.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LogManagerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var perGame by remember { mutableStateOf(prefs.getBoolean(LogLocation.PREF_PER_GAME, true)) }
    var keepLast by remember {
        mutableStateOf(prefs.getInt(LogLocation.PREF_KEEP_LAST, LogLocation.DEFAULT_KEEP_LAST))
    }
    var wineDebug by remember { mutableStateOf(prefs.getBoolean("enable_wine_debug", false)) }
    var box64Logs by remember { mutableStateOf(prefs.getBoolean("enable_box64_logs", false)) }
    var dxvkLogs by remember { mutableStateOf(prefs.getBoolean("enable_dxvk_logs", true)) }
    var controllerLogs by remember {
        mutableStateOf(prefs.getBoolean(com.winhub.hernandez.core.ControllerLog.PREF_KEY, false))
    }
    var steamLite by remember { mutableStateOf(prefs.getBoolean("enable_steamlite_logs", false)) }
    var logcat by remember { mutableStateOf(prefs.getBoolean("enable_logcat", true)) }
    var crashReports by remember { mutableStateOf(prefs.getBoolean("enable_crash_reports", true)) }
    var exitAutosave by remember { mutableStateOf(prefs.getBoolean(ExitReasonReporter.PREF_AUTOSAVE, false)) }
    var rustSteamEngine by remember {
        mutableStateOf(prefs.getBoolean(com.winhub.hernandez.store.blsteam.BlSteamEngineFlag.PREF_KEY,
            com.winhub.hernandez.store.blsteam.BlSteamEngineFlag.DEFAULT))
    }
    var rustEpicEngine by remember {
        mutableStateOf(prefs.getBoolean(com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.PREF_KEY_EPIC,
            com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.DEFAULT))
    }
    var rustGogEngine by remember {
        mutableStateOf(prefs.getBoolean(com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.PREF_KEY_GOG,
            com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.DEFAULT))
    }
    var rustAmazonEngine by remember {
        mutableStateOf(prefs.getBoolean(com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.PREF_KEY_AMAZON,
            com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.DEFAULT))
    }

    // Location + channels moved here from the old Settings › Logs section, which this screen
    // replaces. They used to be saved by the Settings "Save" FAB; here every change is written
    // immediately, matching the Performance screen and removing the risk of two screens holding
    // the same preference and one overwriting the other on save.
    var locationMode by remember {
        mutableStateOf(prefs.getString(LogLocation.PREF_MODE, LogLocation.MODE_DOCUMENTS) ?: LogLocation.MODE_DOCUMENTS)
    }
    var customPath by remember {
        mutableStateOf(prefs.getString(LogLocation.PREF_CUSTOM_PATH, "") ?: "")
    }
    var showLocationMenu by remember { mutableStateOf(false) }
    var channels by remember {
        mutableStateOf(
            (prefs.getString("wine_debug_channels", com.winhub.hernandez.SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS)
                ?: com.winhub.hernandez.SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS)
                .split(",").filter { it.isNotBlank() }
        )
    }

    // Bumped after any destructive/refreshing action to force a re-scan of the filesystem.
    // Declared before saveMode() below, which increments it — a Kotlin local function can only
    // capture locals declared above it.
    var refreshTick by remember { mutableStateOf(0) }

    fun saveMode(mode: String) {
        locationMode = mode
        prefs.edit().putString(LogLocation.PREF_MODE, mode).apply()
        refreshTick++
    }
    fun saveChannels(list: List<String>) {
        channels = list
        prefs.edit().putString("wine_debug_channels", list.joinToString(",")).apply()
    }

    val dirLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            InAppFilePicker.pickedPath(result.data)?.let { path ->
                customPath = path
                prefs.edit().putString(LogLocation.PREF_CUSTOM_PATH, path).apply()
                saveMode(LogLocation.MODE_CUSTOM)
            }
        }
    }

    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    var exitReport by remember { mutableStateOf<String?>(null) }
    var showExplainAll by remember { mutableStateOf(false) }
    var showKeepMenu by remember { mutableStateOf(false) }
    // Folder the embedded File Manager is showing, or null when it is closed.
    var browseDir by remember { mutableStateOf<File?>(null) }
    // Group open in the log viewer, and the two destructive confirmations.
    var viewing by remember { mutableStateOf<LogInventory.Entry?>(null) }
    var confirmDelete by remember { mutableStateOf<LogInventory.Entry?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }
    val entries = remember(refreshTick, perGame) { LogInventory.scan(context) }

    fun putBool(key: String, v: Boolean) = prefs.edit().putBoolean(key, v).apply()
    var storeDlTier by remember { mutableStateOf(com.winhub.hernandez.store.StoreDownloadTier.get(context)) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    I18n.s(R.string.ui_log_manager),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // ── Where logs go ────────────────────────────────────────────
            SectionLabel(I18n.s(R.string.ui_where_logs_go))
            LogCard {
                PickRow(
                    label = when (locationMode) {
                        LogLocation.MODE_DOWNLOAD -> "Download"
                        LogLocation.MODE_CUSTOM -> if (customPath.isNotEmpty()) "Custom folder" else I18n.s(R.string.ui_choose_folder)
                        // MODE_DOCUMENTS (default) and the retired MODE_APP_DATA both resolve to Documents.
                        else -> "Documents"
                    },
                    sub = LogLocation.resolveLogDir(context)?.absolutePath ?: "—",
                    action = "Change",
                    onClick = { showLocationMenu = true }
                )
                Box {
                    // Same card, leading icons and dividers as the File Manager's drive menu —
                    // one menu style across the app, from the shared MenuStyle helpers.
                    DropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false },
                        modifier = Modifier.outlinedMenuCard(),
                    ) {
                        MenuRow("Download", Icons.Default.Download) {
                            saveMode(LogLocation.MODE_DOWNLOAD); showLocationMenu = false
                        }
                        MenuItemDivider()
                        MenuRow(I18n.s(R.string.ui_documents), Icons.Default.Description) {
                            saveMode(LogLocation.MODE_DOCUMENTS); showLocationMenu = false
                        }
                        MenuItemDivider()
                        MenuRow(I18n.s(R.string.ui_choose_folder), Icons.Default.FolderOpen) {
                            showLocationMenu = false
                            dirLauncher.launch(InAppFilePicker.buildDirIntent(context, I18n.s(R.string.ui_select_log_folder)))
                        }
                    }
                }
                LogToggle(I18n.s(R.string.ui_folder_for_each_game), perGame,
                    hint = I18n.s(R.string.ui_each_game_keeps_its_own_folder),
                    onInfo = { info = "Folder for each game" to LogCopy.PER_GAME }) {
                    perGame = it; putBool(LogLocation.PREF_PER_GAME, it); refreshTick++
                }
            }

            // ── What to record ───────────────────────────────────────────
            SectionLabel(I18n.s(R.string.ui_what_to_record))
            LogCard {
                LogToggle(I18n.s(R.string.ui_wine_debug), wineDebug,
                    hint = I18n.s(R.string.ui_slows_games_down_for_diagnosing_a_problem),
                    onInfo = { info = "Wine debug" to LogCopy.WINE }) {
                    wineDebug = it; putBool("enable_wine_debug", it)
                }
                if (wineDebug) {
                    WineChannelGroup(
                        selected = channels,
                        onChange = { saveChannels(it) },
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )
                }
                LogToggle("Box64 / FEXCore", box64Logs,
                    hint = I18n.s(R.string.ui_slows_games_down_at_higher_verbosity),
                    onInfo = { info = "Box64 / FEXCore" to LogCopy.BOX64 }) {
                    box64Logs = it; putBool("enable_box64_logs", it)
                }
                LogToggle("DXVK & VKD3D", dxvkLogs,
                    hint = I18n.s(R.string.ui_safe_to_leave_on_mostly_written_at_startup),
                    onInfo = { info = "DXVK & VKD3D" to LogCopy.DXVK }) {
                    dxvkLogs = it; putBool("enable_dxvk_logs", it)
                }
                LogToggle(I18n.s(R.string.ui_controller_input), controllerLogs,
                    hint = I18n.s(R.string.ui_for_controls_that_don_t_work_written_when_the_game_ex),
                    onInfo = { info = "Controller input" to LogCopy.CONTROLLER }) {
                    controllerLogs = it; putBool(com.winhub.hernandez.core.ControllerLog.PREF_KEY, it)
                }
                LogToggle(I18n.s(R.string.ui_steamlite_steam_client), steamLite,
                    hint = I18n.s(R.string.ui_only_when_a_game_launches_through_steamlite),
                    onInfo = { info = "SteamLite (Steam client)" to LogCopy.STEAMLITE }) {
                    steamLite = it; putBool("enable_steamlite_logs", it)
                }
                LogToggle(I18n.s(R.string.ui_android_logcat), logcat,
                    hint = I18n.s(R.string.ui_winhub_own_output_only),
                    onInfo = { info = "Android logcat" to LogCopy.LOGCAT }) {
                    logcat = it; putBool("enable_logcat", it)
                }
                LogToggle(I18n.s(R.string.ui_crash_reports), crashReports,
                    hint = I18n.s(R.string.ui_nothing_runs_until_something_crashes),
                    onInfo = { info = "Crash reports" to LogCopy.CRASH }) {
                    crashReports = it; putBool("enable_crash_reports", it)
                }
                // Exit reasons: reads Android's own record of why the process last died — the only way
                // to see a NATIVE crash (which dies in another process and never reaches logcat or the
                // Java crash reporter) on an unrooted device. Toggle only auto-saves on launch; the
                // system keeps the record either way, so the manual button below works even when off.
                LogToggle(
                    if (ExitReasonReporter.isSupported()) "Exit reasons (auto-save on launch)"
                    else I18n.s(R.string.ui_exit_reasons_needs_android_11),
                    exitAutosave,
                    hint = I18n.s(R.string.ui_records_native_crashes_without_root),
                    enabled = ExitReasonReporter.isSupported(),
                    onInfo = { info = I18n.s(R.string.ui_exit_reasons) to LogCopy.EXIT_REASONS }) {
                    exitAutosave = it; putBool(ExitReasonReporter.PREF_AUTOSAVE, it)
                }
                // Developer switch for the native Rust Steam engine (docs/STEAM_RUST_ENGINE_PLAN.md).
                // Read once per process by SteamRepository.initialize(), so it takes effect on the
                // next app start. OFF = JavaSteam as today; ON = the full Steam stack (auth, session,
                // library, downloads, cloud, achievements, social, presence) runs on libblsteam.so and
                // the engine's steam_engine.txt is folded into the SteamLite log bundle (Phase 3b-4).
                LogToggle(I18n.s(R.string.ui_native_steam_engine_restart_required), rustSteamEngine,
                    hint = I18n.s(R.string.ui_on_by_default_the_whole_steam_session_sign_in_library),
                    onInfo = { info = "Rust Steam engine" to LogCopy.RUST_ENGINE }) {
                    rustSteamEngine = it
                    putBool(com.winhub.hernandez.store.blsteam.BlSteamEngineFlag.PREF_KEY, it)
                }
                // Per-store switches for the native download engines (docs/RUST_STORE_ENGINES.md).
                // Read at download start by each store's Java manager, so a flip applies to the next
                // download — no restart. OFF = that manager's existing Java fetch loop.
                LogToggle(I18n.s(R.string.ui_rust_engine_epic_downloads), rustEpicEngine,
                    hint = I18n.s(R.string.ui_on_by_default_epic_file_chunks_are_fetched_by_the_nat),
                    onInfo = { info = "Rust store engines" to LogCopy.RUST_STORE_ENGINES }) {
                    rustEpicEngine = it
                    putBool(com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.PREF_KEY_EPIC, it)
                }
                LogToggle(I18n.s(R.string.ui_rust_engine_gog_downloads), rustGogEngine,
                    hint = I18n.s(R.string.ui_on_by_default_gog_depot_chunks_are_fetched_by_the_nat),
                    onInfo = { info = "Rust store engines" to LogCopy.RUST_STORE_ENGINES }) {
                    rustGogEngine = it
                    putBool(com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.PREF_KEY_GOG, it)
                }
                LogToggle(I18n.s(R.string.ui_rust_engine_amazon_downloads), rustAmazonEngine,
                    hint = I18n.s(R.string.ui_on_by_default_amazon_games_files_are_fetched_by_the_n),
                    onInfo = { info = "Rust store engines" to LogCopy.RUST_STORE_ENGINES }) {
                    rustAmazonEngine = it
                    putBool(com.winhub.hernandez.store.blsteam.BlStoreEngineFlag.PREF_KEY_AMAZON, it)
                }
                // One app-wide speed tier for the three store engines (Steam keeps its per-download
                // picker). Tap cycles Slow → Medium → Fast → Blazing; it is only the ceiling the
                // adaptive window may ramp to, so a weak link still settles below it. Next download.
                Spacer(Modifier.height(4.dp))
                PickRow(
                    I18n.s(R.string.ui_store_download_speed),
                    "Ceiling for the Rust Epic / GOG / Amazon engines. Tap to change; applies to the next download.",
                    action = com.winhub.hernandez.store.StoreDownloadTier.label(storeDlTier),
                    onInfo = { info = I18n.s(R.string.ui_rust_store_engines) to LogCopy.RUST_STORE_ENGINES },
                ) {
                    storeDlTier = com.winhub.hernandez.store.StoreDownloadTier.next(storeDlTier)
                    com.winhub.hernandez.store.StoreDownloadTier.set(context, storeDlTier)
                }

                // Outlined rather than a filled button: the design keeps solid accent for switches
                // only, and this is an occasional action, not the point of the screen.
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    CardAction(
                        I18n.s(R.string.ui_capture_logcat_now),
                        modifier = Modifier.weight(1f).alpha(if (logcat) 1f else 0.4f)
                    ) {
                        if (!logcat) return@CardAction
                        // Runtime.exec + up to 10000 lines + a redaction pass + a file write: far too
                        // much for the UI thread (its own docs say so). Off to IO, refresh after.
                        scope.launch {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                LogcatCapture.captureToFile(context, LogcatCapture.DEFAULT_LINES)
                            }
                            refreshTick++
                        }
                    }
                    InfoDot { info = I18n.s(R.string.ui_capture_logcat_now) to LogCopy.CAPTURE_NOW }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    CardAction(
                        I18n.s(R.string.ui_read_last_crash),
                        modifier = Modifier.weight(1f).alpha(if (ExitReasonReporter.isSupported()) 1f else 0.4f)
                    ) {
                        if (!ExitReasonReporter.isSupported()) return@CardAction
                        // Reads a system stream + writes a file — off the main thread. We show the text
                        // AND save it to the exit-reasons folder so it can be shared like any other log.
                        scope.launch {
                            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                ExitReasonReporter.captureToFile(context)
                                ExitReasonReporter.capture(context)
                            }
                            exitReport = text
                            refreshTick++
                        }
                    }
                    InfoDot { info = I18n.s(R.string.ui_exit_reasons) to LogCopy.EXIT_REASONS }
                }
            }

            // ── Housekeeping ─────────────────────────────────────────────
            SectionLabel(I18n.s(R.string.ui_housekeeping))
            LogCard {
                Box {
                    PickRow(
                        label = I18n.s(R.string.ui_keep_last),
                        sub = if (keepLast == 0) "No history — each run replaces the last"
                              else "$keepLast launch${if (keepLast == 1) "" else "es"} per game",
                        action = "▾",
                        onInfo = { info = I18n.s(R.string.ui_keep_last_launches) to LogCopy.KEEP_LAST },
                        onClick = { showKeepMenu = true }
                    )
                    DropdownMenu(
                        expanded = showKeepMenu,
                        onDismissRequest = { showKeepMenu = false },
                        modifier = Modifier.outlinedMenuCard(),
                    ) {
                        listOf(0, 1, 3, 5, 10, 20, 50).forEachIndexed { i, n ->
                            if (i > 0) MenuItemDivider()
                            MenuRow(
                                if (n == 0) "No history" else "$n launch${if (n == 1) "" else "es"}",
                                if (n == 0) Icons.Default.HistoryToggleOff else Icons.Default.History
                            ) {
                                keepLast = n
                                prefs.edit().putInt(LogLocation.PREF_KEEP_LAST, n).apply()
                                showKeepMenu = false
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(I18n.s(R.string.ui_total_size), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text(
                            LogInventory.humanBytes(LogInventory.totalBytes(entries)) +
                                " across ${entries.size} " + if (entries.size == 1) "folder" else "folders",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                        )
                    }
                    Text(I18n.s(R.string.browse), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { browseDir = LogLocation.resolveLogDir(context) }
                            .padding(horizontal = 8.dp, vertical = 6.dp))
                    Text(I18n.s(R.string.ui_clear_all), color = DangerRed, fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { if (entries.isNotEmpty()) confirmClearAll = true }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .alpha(if (entries.isEmpty()) 0.4f else 1f))
                }
            }

            // ── Logs by game ─────────────────────────────────────────────
            SectionLabel(I18n.s(R.string.ui_logs_by_game))
            if (entries.isEmpty()) {
                LogCard {
                    Text(I18n.s(R.string.ui_no_logs_yet), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                entries.forEach { e ->
                    GameLogCard(
                        entry = e,
                        onView = { viewing = e },
                        onShare = { shareLogGroup(context, e) },
                        onDelete = { confirmDelete = e }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                I18n.s(R.string.ui_explain_the_log_types),
                color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                modifier = Modifier
                    .clickable { showExplainAll = true }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
            Text(
                // Third copy of this claim, and the one that overstated it most — "safe to share"
                // full stop. Same correction as LogReport and LogViewerScreen: name what is actually
                // stripped, and say plainly that paths are kept, because a path is often the thing
                // that makes a log worth having.
                I18n.s(R.string.ui_e_mail_addresses_auth_tokens_and_your_steam_account_name) +
                    "written. File paths are kept so they stay useful for debugging — glance over " +
                    I18n.s(R.string.ui_them_before_sharing_if_a_folder_name_identifies_you_logc) +
                    I18n.s(R.string.ui_winhub_s_own_output),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // The File Manager, opened at a log folder. This host block is what "Browse" needs to do
    // anything at all — without it browseDir is written and never read, which is exactly how the
    // button ended up dead. It also re-scans on close, since the File Manager can delete or move.
    browseDir?.let { dir ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { browseDir = null; refreshTick++ },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // A Dialog window is transparent and the File Manager draws no background of its own —
            // without this Surface it renders on top of whatever is behind the dialog.
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                FileManagerScreen(initialDir = dir)
            }
        }
    }

    viewing?.let { entry ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewing = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LogViewerScreen(entry = entry, onClose = { viewing = null })
            }
        }
    }

    exitReport?.let { report ->
        OutlinedAlertDialog(
            onDismissRequest = { exitReport = null },
            title = { Text("Last crash / exit reasons") },
            text = {
                Column(modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "Saved with your logs under the \"" + ExitReasonReporter.FOLDER +
                            "\" folder. Exit #0 is the most recent; a NATIVE_CRASH entry includes the " +
                            I18n.s(R.string.ui_signal_and_the_top_backtrace_frames),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        report,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = { TextButton(onClick = { exitReport = null }) { Text(I18n.s(R.string.wrapper_manager_close)) } }
        )
    }

    confirmDelete?.let { entry ->
        val count = remember(entry.dir) { LogInventory.deletableCount(entry) }
        OutlinedAlertDialog(
            onDismissRequest = { confirmDelete = null },
            // The loose bucket is already named "Older logs"/"All logs", so "Delete ${name} logs?"
            // read "Delete Older logs logs?". Only a game name needs the word appending.
            title = {
                Text(
                    when {
                        entry.isAppBucket -> I18n.s(R.string.ui_delete_app_crash_logs)
                        entry.isLooseBucket -> "Delete ${entry.name.replaceFirstChar { it.lowercase() }}?"
                        else -> "Delete ${entry.name} logs?"
                    }
                )
            },
            text = {
                Text(
                    if (count == 0)
                    // Reachable: the loose bucket counts every log-shaped file it finds, but only
                    // the ones we wrote are deletable. A folder holding nothing but a user's own
                    // files lands here, and must not imply we are about to touch them.
                        I18n.s(R.string.ui_nothing_here_was_written_by_winhub_so_there_is_nothing_t) +
                            "The files in this folder are yours and are left alone."
                    else
                        "Deletes $count log file${if (count == 1) "" else "s"}, including any kept " +
                            "history.\n\nOnly files WinHub wrote are removed — anything else in " +
                            I18n.s(R.string.ui_that_folder_is_left_alone)
                )
            },
            confirmButton = {
                if (count > 0) {
                    TextButton(onClick = {
                        val n = LogInventory.deleteGroup(context, entry)
                        confirmDelete = null
                        refreshTick++
                        android.widget.Toast.makeText(
                            context, "Deleted $n file${if (n == 1) "" else "s"}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) { Text("Delete", color = DangerRed) }
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(if (count == 0) "OK" else "Cancel")
                }
            }
        )
    }

    if (confirmClearAll) {
        val count = remember(refreshTick) { entries.sumOf { LogInventory.deletableCount(it) } }
        OutlinedAlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text(I18n.s(R.string.ui_clear_all_logs)) },
            text = {
                Text(
                    "Deletes $count log file${if (count == 1) "" else "s"} across ${entries.size} " +
                        "folder${if (entries.size == 1) "" else "s"}, including kept history.\n\n" +
                        "Only files WinHub wrote are removed. Other files in your log folder — " +
                        I18n.s(R.string.ui_including_anything_you_put_there_yourself_are_left_alone)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    var n = 0
                    entries.forEach { n += LogInventory.deleteGroup(context, it) }
                    confirmClearAll = false
                    refreshTick++
                    android.widget.Toast.makeText(
                        context, "Deleted $n file${if (n == 1) "" else "s"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) { Text(I18n.s(R.string.ui_delete_all), color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") } }
        )
    }

    info?.let { (title, body) -> PerfInfoDialog(title = title, body = body, onDismiss = { info = null }) }
    if (showExplainAll) {
        PerfInfoDialog(title = I18n.s(R.string.ui_what_each_log_is_for), body = LogCopy.explainAll(),
            onDismiss = { showExplainAll = false })
    }
}

/** Small uppercase section heading that sits ABOVE its card, as in the design. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        letterSpacing = 0.08.em,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

/** Untitled card — the heading lives above it now. */
@Composable
private fun LogCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) { content() }
}

/** A row that reads as a value with its detail underneath and an action on the right. */
@Composable
private fun PickRow(
    label: String,
    sub: String,
    action: String,
    onInfo: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2)
        }
        if (onInfo != null) InfoDot(onInfo)
        Text(action, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }
}

/** A drive-menu-style row: leading icon tinted primary, label, whole row clickable. */
@Composable
private fun MenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        },
        onClick = onClick,
    )
}

/** The channels people actually turn on. The other 500 are behind the search field. */
internal val COMMON_WINE_CHANNELS = listOf(
    "err", "warn", "fixme", "seh", "module", "loaddll", "process", "thread",
    "sync", "file", "reg", "heap", "ntdll", "d3d", "dxgi", "vulkan", "opengl", "relay"
)

/**
 * Wine debug channels, built like the DXVK_HUD group in the env-var editor: a disclosure header,
 * the selection shown as static labels while collapsed, and a FilterChip grid when open.
 *
 * One deliberate difference. DXVK_HUD has ~20 options and TU_DEBUG ~30, so those can show every
 * chip when expanded; Wine has **521**, which is a wall of chips nobody can read and a slow
 * composition besides. So the expanded grid shows the channels that actually get used, and a
 * search field reaches the rest — the same escape hatch the env-var editor's own add-picker uses
 * for its oversized catalog. Anything already selected is always shown, however obscure.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WineChannelGroup(
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var browseAll by remember { mutableStateOf(false) }

    val all = remember {
        try {
            val arr = org.json.JSONArray(FileUtils.readString(context, "wine_debug_channels.json"))
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Selected first so a channel can always be switched back off, then the common set, then
    // whatever the query matches.
    val shown = remember(query, selected, all) {
        if (query.isBlank()) (selected + COMMON_WINE_CHANNELS).distinct()
        else all.filter { it.contains(query.trim(), ignoreCase = true) }.take(60)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
        ) {
            Text(
                "Channels",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (expanded) "Collapse channels" else I18n.s(R.string.ui_expand_channels),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (!expanded) {
            // Static labels, not chips: a tap that silently deselected a channel from a summary
            // view would be a destructive action hidden behind a collapsed control.
            if (selected.isEmpty()) {
                Text(I18n.s(R.string.ui_none_selected), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    selected.forEach { ch ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(ch, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp)
                        }
                    }
                }
            }
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search all ${all.size} channels") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            shown.forEach { ch ->
                val isOn = ch in selected
                FilterChip(
                    selected = isOn,
                    onClick = { onChange(if (isOn) selected - ch else selected + ch) },
                    label = { Text(ch, fontSize = 11.sp) }
                )
            }
            // Last chip in the flow, so it reads as "…and the rest are through here" rather than
            // as another channel. Searching only helps once you know a name to type; this is the
            // way in for someone who doesn't.
            if (all.isNotEmpty()) {
                AssistChip(
                    onClick = { browseAll = true },
                    label = { Text("Browse all ${all.size}…", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(14.dp))
                    },
                )
            }
        }
        if (query.isNotBlank() && shown.isEmpty()) {
            Text("No channel matches \"$query\".",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }

        Text(
            I18n.s(R.string.ui_reset_to_defaults),
            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
            modifier = Modifier
                .clickable {
                    onChange(com.winhub.hernandez.SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS
                        .split(",").filter { it.isNotBlank() })
                    query = ""
                }
                .padding(top = 6.dp, bottom = 2.dp)
        )
    }

    if (browseAll) {
        AllChannelsDialog(
            all = all,
            selected = selected,
            onChange = onChange,
            onDismiss = { browseAll = false },
        )
    }
}

/**
 * Every Wine channel, browsable. The chip grid above deliberately shows only the ~18 people use, and
 * the search field only helps if you already know a name to type — this is the way in for someone
 * who doesn't know what exists.
 *
 * Grouped by family rather than listed alphabetically: 521 names in one column is technically "all
 * of them" and practically unreadable, whereas "Sound" holding 15 entries is something a user can
 * actually shop from. "What's this?" turns on a one-line explanation under every row, which is the
 * difference between a list of names and a reference — it is off by default because with it on each
 * row is three lines tall and scanning gets slower.
 */
@Composable
internal fun AllChannelsDialog(
    all: List<String>,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    var selectedOnly by remember { mutableStateOf(false) }

    // Grouping is pure string work over 521 items, so it is cheap — but it must not rerun on every
    // recomposition (every keystroke, every toggle), hence the key on the list identity alone.
    val grouped = remember(all) {
        all.groupBy { WineChannelInfo.categoryOf(it) }
            .toList()
            .sortedBy { (cat, _) ->
                WineChannelInfo.CATEGORY_ORDER.indexOf(cat)
                    .let { if (it < 0) WineChannelInfo.CATEGORY_ORDER.size else it }
            }
            // Within a category: the everyday ones first, then ones we can explain, then the rest
            // alphabetically. Plain alphabetical put `debug_buffer` at the top of "Errors and
            // tracing" and pushed err/warn/fixme/seh below the fold — the exact channels someone
            // opening this list came for. Sorting by "has a description" alone does not fix it,
            // because debug_buffer has one too; COMMON_WINE_CHANNELS is the actual priority order.
            // Deliberately NOT selected-first: rows would jump under the finger as you tick them.
            .map { (cat, names) ->
                cat to names.sortedWith(
                    compareBy<String> {
                        val i = COMMON_WINE_CHANNELS.indexOf(it)
                        if (i < 0) COMMON_WINE_CHANNELS.size else i
                    }
                        .thenByDescending { WineChannelInfo.hasDetail(it) }
                        .thenBy { it }
                )
            }
    }

    val visible = remember(grouped, query, selectedOnly, selected) {
        val q = query.trim()
        grouped.mapNotNull { (cat, names) ->
            val kept = names.filter { ch ->
                (q.isBlank() || ch.contains(q, ignoreCase = true) ||
                    cat.contains(q, ignoreCase = true)) &&
                    (!selectedOnly || ch in selected)
            }
            if (kept.isEmpty()) null else cat to kept
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            // Nearly full-bleed. This device is a landscape handheld, and at 0.9 height with the
            // title, search field and chips each on their own row the list got a single visible
            // row — the one part of the dialog that matters. The header is now one row and the
            // list takes everything left over.
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.95f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(I18n.s(R.string.ui_all_wine_channels), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${all.size} available · ${selected.size} on",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilterChip(
                        selected = showHelp,
                        onClick = { showHelp = !showHelp },
                        label = { Text(I18n.s(R.string.ui_what_this), fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.HelpOutline, null, modifier = Modifier.size(14.dp))
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = selectedOnly,
                        onClick = { selectedOnly = !selectedOnly },
                        label = { Text(I18n.s(R.string.ui_on_only), fontSize = 11.sp) },
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )

                if (visible.isEmpty()) {
                    Text(
                        if (selectedOnly && query.isBlank()) I18n.s(R.string.ui_no_channels_are_on)
                        else "Nothing matches \"$query\".",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        visible.forEach { (category, names) ->
                            item(key = "hdr-$category") {
                                Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                                    Text(
                                        category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (showHelp) {
                                        Text(
                                            WineChannelInfo.categoryBlurb(category),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            items(names, key = { "ch-$it" }) { ch ->
                                val isOn = ch in selected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onChange(if (isOn) selected - ch else selected + ch)
                                        }
                                        .padding(vertical = 3.dp),
                                ) {
                                    Checkbox(
                                        checked = isOn,
                                        onCheckedChange = {
                                            onChange(if (isOn) selected - ch else selected + ch)
                                        },
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                                        Text(
                                            ch,
                                            fontSize = 13.sp,
                                            color = if (isOn) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (showHelp) {
                                            Text(
                                                WineChannelInfo.describe(ch),
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

/**
 * One game's logs: icon, name, what is on disk, then View / Share / Delete as in the design.
 * Tapping the name expands the file list.
 */
@Composable
private fun GameLogCard(
    entry: LogInventory.Entry,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        entry.isAppBucket -> Icons.Default.PhoneAndroid
                        // Loose files in the log root are not a game and must not look like one.
                        entry.isLooseBucket -> Icons.Default.History
                        entry.dir.name.startsWith("Container") -> Icons.Default.Settings
                        else -> Icons.Default.SportsEsports
                    },
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                Text(if (entry.isAppBucket) "App & crash logs" else entry.name,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(
                    buildString {
                        append("${entry.fileCount} file${if (entry.fileCount == 1) "" else "s"}")
                        append(" · ").append(LogInventory.humanBytes(entry.totalBytes))
                        if (entry.archivedRuns > 0) append(" · ${entry.archivedRuns} kept")
                        append(" · ").append(relativeTime(entry.lastModified))
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            LogInventory.filesIn(entry.dir).forEach { f ->
                Text("• ${f.name}  (${LogInventory.humanBytes(f.length())})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp))
            }
        }
        // Divider then a three-up action row, as in the design.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
        ) {
            CardAction(I18n.s(R.string.xmb_all_view), modifier = Modifier.weight(1f), primary = true, onClick = onView)
            CardAction(I18n.s(R.string.share), modifier = Modifier.weight(1f), onClick = onShare)
            CardAction("Delete", modifier = Modifier.weight(1f), danger = true, onClick = onDelete)
        }
    }
}

/** One of the three per-card actions. */
@Composable
private fun CardAction(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val fg = when {
        danger -> DangerRed
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(7.dp))
            .border(
                1.dp,
                if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(7.dp)
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 12.sp)
    }
}

/** The design's one non-theme colour: destructive actions read as red in both light and dark. */

/** "12 min ago" / "yesterday" — a timestamp is not what anyone is looking for in this list. */
internal fun relativeTime(millis: Long): String {
    if (millis <= 0) return "—"
    val mins = (System.currentTimeMillis() - millis) / 60000
    return when {
        mins < 1 -> I18n.s(R.string.ui_just_now)
        mins < 60 -> "$mins min ago"
        mins < 60 * 24 -> "${mins / 60} hr ago"
        mins < 60 * 48 -> "yesterday"
        else -> "${mins / (60 * 24)} days ago"
    }
}

@Composable
private fun LogToggle(
    label: String,
    checked: Boolean,
    hint: String? = null,
    enabled: Boolean = true,
    onInfo: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier.alpha(0.4f))
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            if (hint != null) {
                Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        if (onInfo != null) InfoDot(onInfo)
        Spacer(Modifier.width(4.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** Always-live "?" — a locked or off toggle must still be explainable. */
@Composable
private fun InfoDot(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Outlined.HelpOutline, I18n.s(R.string.ui_what_this),
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

/**
 * Help copy. Every entry leads with the performance cost, because that is the thing a user needs to
 * decide with — two of these are genuinely expensive to leave on.
 */
internal object LogCopy {
    val PER_GAME =
        "No performance cost.\n\n" +
        I18n.s(R.string.ui_gives_each_game_its_own_folder_inside_your_log_location) +
        I18n.s(R.string.ui_game_s_logs_don_t_get_mixed_up_with_another_s_turn_it_of) +
        "folder like older versions did.\n\n" +
        I18n.s(R.string.ui_logs_written_before_you_turned_this_on_are_left_exactly)

    val WINE =
        "⚠️ SLOWS GAMES DOWN. Only turn this on to diagnose a problem, then turn it back off.\n\n" +
        I18n.s(R.string.ui_records_what_wine_itself_is_doing_every_message_is_writt) +
        "Wine produces a lot of them — the \"seh\" channel alone logs an entry every time the game " +
        I18n.s(R.string.ui_raises_an_exception_which_normal_windows_programs_do_con) +
        "such as \"relay\" can make a game many times slower.\n\n" +
        I18n.s(R.string.ui_this_is_the_log_to_enable_when_a_game_won_t_start_crashe)

    val BOX64 =
        "⚠️ SLOWS GAMES DOWN at higher verbosity.\n\n" +
        I18n.s(R.string.ui_records_what_the_x86_translator_box64_or_fexcore_is_doin) +
        I18n.s(R.string.ui_pc_instructions_to_run_on_your_phone_useful_for_a_game_t) +
        "log doesn't explain, or one that misbehaves only under one emulator.\n\n" +
        I18n.s(R.string.ui_leave_off_for_normal_play)

    val DXVK =
        "Little to no performance cost.\n\n" +
        "Records what the graphics translation layers report — DXVK for DirectX 9/10/11, VKD3D for " +
        "DirectX 12. Almost everything they write happens while the game is starting: which GPU and " +
        "driver were picked, which features are missing, why a shader failed.\n\n" +
        I18n.s(R.string.ui_this_is_the_log_to_check_for_black_screens_missing_textu) +
        I18n.s(R.string.ui_graphics_feature_safe_to_leave_on)

    val CONTROLLER =
        "Little to no performance cost.\n\n" +
        I18n.s(R.string.ui_records_how_the_controller_reached_the_game_for_when_the) +
        "XInput games (Direct XInput and Wine's own XInput) and DirectInput games, plus vibration. " +
        I18n.s(R.string.ui_it_notes_the_controller_settings_the_game_launched_with) +
        I18n.s(R.string.ui_player_every_in_game_change_in_controls_pad_and_how_much) +
        "actually read.\n\n" +
        "Everything lands in a single \"controller.txt\" next to that game's other logs, written when " +
        "the game exits. It opens with a checklist (PASS / FAIL / WARN) for each player, so you can see " +
        "where input stopped: the app got nothing from the controller, Wine never opened it, the game " +
        "never read it, or Direct XInput never loaded. Send that file when you report a controller " +
        "problem — play long enough to try the controls, then quit the game normally.\n\n" +
        "Wine games only; the Linux runtime keeps its own controller diagnostics. Nothing is recorded " +
        I18n.s(R.string.ui_while_this_is_off)

    val STEAMLITE =
        "Little to no performance cost.\n\n" +
        I18n.s(R.string.ui_collects_the_real_steam_client_s_own_logs_the_ones_it_wr) +
        "under SteamLite (the online / VAC launch path). That covers your login and network connection, " +
        "which game it launched, downloads, cloud saves and achievements. Steam writes these small files " +
        "as it goes, so leaving this on costs you essentially nothing.\n\n" +
        I18n.s(R.string.ui_it_only_produces_anything_when_a_game_is_actually_launch) +
        I18n.s(R.string.ui_or_a_goldberg_offline_launch_writes_nothing_at_all_so_th) +
        "played online through the real client.\n\n" +
        "Everything is gathered into a single \"steamlite.txt\" next to that game's other logs, and it " +
        I18n.s(R.string.ui_opens_with_a_plain_english_summary_of_what_ran_plus_an_a) +
        "(a VAC-insecure launch, a login taken over by another device, a cloud-save conflict) so you " +
        "don't have to read the raw logs.\n\n" +
        I18n.s(R.string.ui_your_steam_account_name_and_auth_tokens_are_scrubbed_out) +
        I18n.s(R.string.ui_partially_masked_but_a_masked_steam_id_may_still_remain)

    val LOGCAT =
        "No performance cost.\n\n" +
        I18n.s(R.string.ui_android_s_own_system_log_as_it_relates_to_winhub_it_is_c) +
        "tap \"Capture logcat now\", or when the app crashes — not continuously, so switching it on " +
        "does not cost you any frames.\n\n" +
        I18n.s(R.string.ui_it_contains_winhub_s_output_only_android_does_not_let_an) +
        "without root, and we deliberately don't offer a system-wide capture even with root: it " +
        "would sweep up other apps' private data into a file you're likely to share. If you have " +
        I18n.s(R.string.ui_root_and_need_the_full_system_log_use_a_dedicated_logcat)

    val CRASH =
        "No performance cost — nothing runs until something actually crashes.\n\n" +
        I18n.s(R.string.ui_if_winhub_itself_crashes_a_report_is_saved_with_your_dev) +
        I18n.s(R.string.ui_app_version_what_went_wrong_and_the_last_few_hundred_lin) +
        "exactly what's needed to work out a crash after the fact.\n\n" +
        "Reports are kept with your other logs under \"App & crash logs\"."

    val CAPTURE_NOW =
        I18n.s(R.string.ui_takes_a_snapshot_of_winhub_s_recent_android_log_right_no) +
        "logs.\n\n" +
        I18n.s(R.string.ui_useful_when_something_went_wrong_but_the_app_didn_t_cras) +
        I18n.s(R.string.ui_fresh_then_share_it)

    val EXIT_REASONS =
        "Reads Android's own record of why WinHub last shut down or crashed — no root needed.\n\n" +
        "This is the ONLY way to see a NATIVE crash on an unrooted device: those die in a separate " +
        I18n.s(R.string.ui_system_process_so_they_never_show_up_in_the_logcat_captu) +
        I18n.s(R.string.ui_the_app_just_restarts_looking_fine_for_a_native_crash_th) +
        "and the top backtrace frames — the actual cause, not a hint.\n\n" +
        "The system keeps this record across the crash, so \"Read last crash\" works even if you only " +
        I18n.s(R.string.ui_turn_this_on_afterwards_the_toggle_just_controls_whether) +
        I18n.s(R.string.ui_automatically_each_time_the_app_starts_needs_android_11)

    val KEEP_LAST =
        "How many past runs to keep for each game.\n\n" +
        "When a game launches, the previous run's logs are moved into a \"previous\" folder rather " +
        I18n.s(R.string.ui_than_being_overwritten_and_anything_older_than_this_many) +
        "run's files keep their normal names, so the newest log is always the obvious one.\n\n" +
        I18n.s(R.string.ui_set_it_to_0_to_keep_no_history_at_all)

    val RUST_ENGINE =
        "On by default — no performance cost in game.\n\n" +
        I18n.s(R.string.ui_runs_the_whole_steam_side_of_winhub_sign_in_library_down) +
        "achievements, friends) on the app's native engine. Needs an app restart to take effect.\n\n" +
        "Turn it off only if something Steam-related misbehaves: that switches back to the legacy " +
        I18n.s(R.string.ui_java_engine_for_this_release_nothing_is_lost_while_the_n) +
        I18n.s(R.string.ui_is_added_to_the_steamlite_bundle_so_a_problem_can_be_tra)

    val RUST_STORE_ENGINES =
        "On by default — no performance cost in game.\n\n" +
        "Fetches Epic, GOG and Amazon game files with the app's native download engine (the same " +
        I18n.s(R.string.ui_adaptive_multi_connection_fetcher_the_steam_engine_uses) +
        "— what is installed, resume, shortcuts, cloud saves — is unchanged.\n\n" +
        I18n.s(R.string.ui_each_switch_is_read_when_a_download_starts_so_flipping_o) +
        "of that store without a restart. Turn a store off if its downloads misbehave: that store " +
        I18n.s(R.string.ui_goes_back_to_its_java_downloader_nothing_is_lost)

    fun explainAll(): String = buildString {
        append("Costs performance while on\n\n")
        append("• Wine debug\n$WINE\n\n")
        append("• Box64 / FEXCore\n$BOX64\n\n")
        append("\nSafe to leave on\n\n")
        append("• DXVK & VKD3D\n$DXVK\n\n")
        append("• SteamLite (Steam client)\n$STEAMLITE\n\n")
        append("• Android logcat\n$LOGCAT\n\n")
        append("• Crash reports\n$CRASH\n\n")
        append("\nOrganisation\n\n")
        append("• Folder for each game\n$PER_GAME\n\n")
        append("• Keep last runs\n$KEEP_LAST\n\n")
        append("\nDeveloper\n\n")
        append("• Rust Steam engine\n$RUST_ENGINE\n\n")
        append("• Rust store engines (Epic / GOG / Amazon downloads)\n$RUST_STORE_ENGINES\n")
    }
}
