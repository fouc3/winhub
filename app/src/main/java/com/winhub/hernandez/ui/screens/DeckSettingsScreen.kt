package com.winhub.hernandez.ui.screens

import com.winhub.hernandez.ui.deck.XmbRailPick
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.winhub.hernandez.ui.components.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.winhub.hernandez.communityconfigs.GameDoctor
import com.winhub.hernandez.R
import com.winhub.hernandez.contents.ContentsManager
import com.winhub.hernandez.core.AppOrientation
import com.winhub.hernandez.core.UpdateManager
import com.winhub.hernandez.box64.Box64Preset
import com.winhub.hernandez.box64.Box64PresetManager
import com.winhub.hernandez.fexcore.FEXCorePreset
import com.winhub.hernandez.fexcore.FEXCorePresetManager
import com.winhub.hernandez.store.SteamPrefs
import com.winhub.hernandez.store.SteamRegion
import com.winhub.hernandez.ui.deck.StoreStyle
import com.winhub.hernandez.ui.theme.AppIcon
import com.winhub.hernandez.ui.theme.AppThemeState
import com.winhub.hernandez.ui.theme.CHARCOAL_PRESET_INDEX
import com.winhub.hernandez.ui.theme.DEFAULT_PRESET_INDEX
import com.winhub.hernandez.ui.theme.ThemePreset
import com.winhub.hernandez.ui.theme.selectableThemePresets
import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import com.winhub.hernandez.MainActivity
import com.winhub.hernandez.retro.RetroLibrary
import com.winhub.hernandez.util.InAppFilePicker
import com.winhub.hernandez.contentdialog.ContentDialog
import com.winhub.hernandez.core.AppUtils
import com.winhub.hernandez.core.PresetScope
import com.winhub.hernandez.core.PreloaderDialog
import com.winhub.hernandez.midi.MidiManager
import com.winhub.hernandez.util.I18n

/**
 * Deck Mode's settings page — a 1:1 mirror of [SettingsScreen]: the same preference keys, the same
 * defaults, the same choice lists (incl. dynamic Box64/FEXCore presets), and the same save semantics
 * (immediate-write vs. batched FAB commit), rendered in DroidDeck's group/row/chip language.
 */
@Composable
internal fun DeckSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // ── Dynamic preset lists (same source as SettingsScreen: the preset managers). ──
    var box64Presets by remember { mutableStateOf(listOf<Box64Preset>()) }
    var fexPresets by remember { mutableStateOf(listOf<FEXCorePreset>()) }
    LaunchedEffect(Unit) {
        box64Presets = Box64PresetManager.getPresets("box64", context)
        fexPresets = FEXCorePresetManager.getPresets(context)
    }

    // ── Batched (FAB-saved) state — mirrors SettingsScreen's saveSettings() set. ──
    var box64 by remember { mutableStateOf(prefs.getString("box64_preset", "COMPATIBILITY") ?: "COMPATIBILITY") }
    var fex by remember { mutableStateOf(prefs.getString("fexcore_preset", "COMPATIBILITY") ?: "COMPATIBILITY") }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var bigPicture by remember { mutableStateOf(prefs.getBoolean("enable_big_picture_mode", false)) }
    var landing by remember { mutableStateOf(prefs.getString("default_landing_screen", "games") ?: "games") }
    var apiKeyEnabled by remember { mutableStateOf(prefs.getBoolean("enable_custom_api_key", false)) }
    var apiKey by remember { mutableStateOf(prefs.getString("custom_api_key", "") ?: "") }
    var cursorLock by remember { mutableStateOf(prefs.getBoolean("cursor_lock", false)) }
    var xinputOff by remember { mutableStateOf(prefs.getBoolean("xinput_toggle", false)) }
    var dri3 by remember { mutableStateOf(prefs.getBoolean("use_dri3", true)) }
    var xr by remember { mutableStateOf(prefs.getBoolean("use_xr", true)) }
    var cursorSpeed by remember { mutableStateOf(prefs.getFloat("cursor_speed", 1.0f)) }
    var fileProvider by remember { mutableStateOf(prefs.getBoolean("enable_file_provider", true)) }
    var androidBrowser by remember { mutableStateOf(prefs.getBoolean("open_with_android_browser", false)) }
    var shareClipboard by remember { mutableStateOf(prefs.getBoolean("share_android_clipboard", false)) }
    var contentsUrl by remember {
        mutableStateOf(
            prefs.getString("downloadable_contents_url", ContentsManager.REMOTE_PROFILES)
                ?: ContentsManager.REMOTE_PROFILES,
        )
    }
    var winlatorPathUri by remember { mutableStateOf<Uri?>(null) }
    var shortcutsPathUri by remember { mutableStateOf<Uri?>(null) }
    var saved by remember { mutableStateOf(false) }

    // ── Immediate-write state (SettingsScreen writes these on change, not on FAB). ──
    var notifyUpdates by remember { mutableStateOf(UpdateManager.isNotifyEnabled(context)) }
    var includePre by remember { mutableStateOf(UpdateManager.isIncludePrereleases(context)) }
    var steamChat by remember { mutableStateOf(SteamPrefs.isChatNotificationsEnabled(context)) }
    var steamOffline by remember { mutableStateOf(SteamPrefs.isOfflinePresenceEnabled(context)) }
    var steamRegion by remember { mutableStateOf(SteamRegion.mode(context)) }
    var showStores by remember { mutableStateOf(AppThemeState.showStores.value) }
    var showInternal by remember { mutableStateOf(AppThemeState.showInternalStorage.value) }
    var showSd by remember { mutableStateOf(AppThemeState.showSdStorage.value) }
    var orientation by remember { mutableStateOf(AppOrientation.mode(context)) }
    var uiScale by remember { mutableStateOf(AppThemeState.uiScale.value) }
    var fontScale by remember { mutableStateOf(AppThemeState.fontScale.value) }
    var topBarOpacity by remember { mutableStateOf(AppThemeState.topBarOpacity.value) }
    // Container editor category layout ("tabs" | "rail" | "pages") — immediate-write, read by
    // DeckContainerDetailScreen on open.
    var editorLayout by remember { mutableStateOf(prefs.getString("container_editor_layout", "rail")?.takeIf { it != "tabs" } ?: "rail") }
    // This page's own category layout ("tabs" | "rail" | "pages"). Defaults to the left rail — the
    // requested default — and is independent of the container editor's choice.
    val portraitUi = LocalDeckPortrait.current
    var settingsLayout by remember { mutableStateOf(if (portraitUi) (if (prefs.getString(PORTRAIT_TABS_PREF, "strip") != "pages") "portrait" else "pages") else prefs.getString("settings_layout", "rail")?.takeIf { it != "tabs" } ?: "rail") }
    var settingsTab by remember { mutableStateOf(0) }
    var forceMali by remember { mutableStateOf(prefs.getBoolean(com.winhub.hernandez.core.MaliPanvk.PREF_FORCE, false)) }
    // The categories are this page's sub-tabs: L2/R2 step them, like Contents and Storage.
    val subTabs = LocalDeckTabs.current
    LaunchedEffect(subTabs) { subTabs.bind(6) }
    LaunchedEffect(subTabs.index) { if (settingsTab != subTabs.index) settingsTab = subTabs.index }
    var settingsPagesCategoryOpen by remember { mutableStateOf(false) }
    val settingsTabs = remember { listOf(I18n.s(R.string.general), I18n.s(R.string.ui_interface), I18n.s(R.string.ui_appearance), "System", I18n.s(R.string.ui_graphics), I18n.s(R.string.ui_updater)) }

    // ── Theme + config-editor presentation (immediate-write). ──
    val themeChoices = remember { selectableThemePresets }
    val themeIndex by AppThemeState.presetIndex.collectAsState(initial = AppThemeState.presetIndex.value)
    val selectedTheme = themeChoices.getOrNull(themeIndex)?.name ?: themeChoices.first().name
    var configEditorStyle by remember {
        mutableStateOf(prefs.getString("config_editor_style", "popup") ?: "popup")
    }
    // Store pages ("deck" | "xmb"); the store Activities read it when they open.
    var storeStyle by remember { mutableStateOf(if (StoreStyle.isXmb(context)) StoreStyle.XMB else StoreStyle.DECK) }
    // Deck Home's layout ("classic" | "full_xmb"); the shell and DeckHome follow it live.
    var homeStyle by remember { mutableStateOf(DeckHomeStyle.read(prefs)) }
    // The retro library's ROM folder and the consoles it shows.
    var retroRoot by remember { mutableStateOf(RetroLibrary.romRoot(context)) }
    var retroConsoleCount by remember { mutableStateOf(RetroLibrary.enabledSystems(context).size) }
    var showRetroConsoles by remember { mutableStateOf(false) }
    var retroHidden by remember { mutableStateOf(RetroLibrary.excludedPaths(context).size) }
    var appIcon by remember { mutableStateOf(AppIcon.choice(context)) }
    val retroFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            InAppFilePicker.pickedPath(result.data)?.takeIf { it.isNotBlank() }?.let {
                RetroLibrary.setRomRoot(context, it)
                retroRoot = it
            }
        }
    }
    // Interface look ("glass" | "material"); every Deck page follows it live.
    var uiLook by remember { mutableStateOf(GlassLook.read(prefs)) }
    // What a game's Settings opens from the Steam / Full XMB Home ("classic" | "xmb"); read when the game's options are built.
    var gameSettingsStyle by remember { mutableStateOf(GameSettingsStyle.read(prefs)) }
    // Navigation sounds ("steamos" | "phone" | "off"); DeckSound reads it on every sound.
    var navSounds by remember { mutableStateOf(prefs.getString(DeckSound.KEY, DeckSound.STEAMOS) ?: DeckSound.STEAMOS) }

    // ── Sound (MIDI Sound Font) — same as SettingsScreen's Sound section. ──
    var sfNames by remember { mutableStateOf(listOf<String>()) }
    var selectedSF by remember { mutableStateOf(0) }
    fun refreshSF() {
        val names = mutableListOf(MidiManager.DEFAULT_SF2_FILE)
        val files = MidiManager.getSoundFontDir(context).listFiles()
        if (files != null) for (file in files) if (file.name.endsWith(".sf2")) names.add(file.name)
        sfNames = names
    }

    // ── ImageFS backup/restore — same as SettingsScreen's ImageFS section. ──
    val activity = context as? Activity
    val mainActivity = context as? MainActivity

    // ── Music Player: takes effect straight away, no Save needed. ──
    var musicName by remember { mutableStateOf(com.winhub.hernandez.core.MusicPlayer.trackName()) }
    var musicOn by remember { mutableStateOf(com.winhub.hernandez.core.MusicPlayer.isEnabled()) }
    var musicInGame by remember { mutableStateOf(com.winhub.hernandez.core.MusicPlayer.playsInGame()) }
    val musicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data) else null
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Music"
            val appContext = context.applicationContext
            // Copies the file, so it runs off the main thread.
            Thread {
                val ok = com.winhub.hernandez.core.MusicPlayer.importTrack(appContext, uri, name)
                activity?.runOnUiThread {
                    if (ok) {
                        com.winhub.hernandez.core.MusicPlayer.reload()
                        musicName = com.winhub.hernandez.core.MusicPlayer.trackName()
                        musicOn = true
                    } else {
                        AppUtils.showToast(context, I18n.s(R.string.ui_could_not_load_that_file))
                    }
                }
            }.start()
        }
    }
    var showBackupDialog by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    fun beginRestoreFromUri(uri: Uri) {
        pendingRestoreUri = uri
        showRestoreConfirm = true
    }

    // ── Frame Generation — LSFG Native (Lossless Scaling DLL) — same as SettingsScreen. ──
    val lsfgDllFile = remember { File(context.filesDir, "lsfg-vk/Lossless.dll") }
    fun lsfgDllStatusText(): String {
        if (!(lsfgDllFile.isFile && lsfgDllFile.length() > 0)) return I18n.s(R.string.ui_not_set_lsfg_native_will_stay_off)
        val mb = lsfgDllFile.length() / (1024 * 1024)
        return when (prefs.getString("lsfg_dll_source", null)) {
            "store"  -> "Imported from Steam store (Lossless Scaling) — $mb MB"
            "manual" -> "Imported manually — $mb MB"
            else     -> "Imported ($mb MB)"
        }
    }
    var lsfgDllStatus by remember { mutableStateOf(lsfgDllStatusText()) }
    var lsfgShaderStatus by remember { mutableStateOf("") }
    val lsfgShaderBuilding = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    fun buildLsfgShaderCache() {
        if (!(lsfgDllFile.isFile && lsfgDllFile.length() > 0)) { lsfgShaderStatus = ""; return }
        if (!lsfgShaderBuilding.compareAndSet(false, true)) return
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val appCtx = context.applicationContext
        lsfgShaderStatus = I18n.s(R.string.ui_preparing_shaders_for_lsfg_native_one_time_can_take_a)
        Thread {
            val started = System.currentTimeMillis()
            val status = try { com.winhub.hernandez.core.LsfgNative.ensureCache(appCtx, false) } catch (e: Throwable) { -1 }
            val took = (System.currentTimeMillis() - started) / 1000
            val text = when (status) {
                com.winhub.hernandez.core.LsfgNative.STATUS_OK ->
                    if (took >= 2) "Shaders ready for LSFG Native (built in ${took}s)" else "Shaders ready for LSFG Native"
                -1 -> I18n.s(R.string.ui_shader_preparation_failed_it_will_be_retried_when_a_g)
                else -> com.winhub.hernandez.core.LsfgNative.explain(status)
            }
            main.post { lsfgShaderStatus = text; lsfgShaderBuilding.set(false) }
        }.start()
    }
    fun importLosslessDllFromUri(uri: Uri) {
        try {
            lsfgDllFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                lsfgDllFile.outputStream().use { output -> input.copyTo(output) }
            }
            prefs.edit().putString("lsfg_dll_source", "manual").apply()
            lsfgDllStatus = lsfgDllStatusText()
            buildLsfgShaderCache()
        } catch (e: Exception) {
            lsfgDllStatus = I18n.s(R.string.ui_import_failed) + e.message
        }
    }
    fun findStoreLosslessDll(): File? {
        val root = File(context.filesDir, "imagefs/steam_games")
        if (!root.isDirectory) return null
        return root.walkTopDown().maxDepth(6)
            .filter { it.isFile && it.name.equals("Lossless.dll", ignoreCase = true) && it.length() > 0 }
            .maxByOrNull { it.lastModified() }
    }
    fun detectLosslessDllFromStore() {
        val src = findStoreLosslessDll()
        if (src == null) {
            Toast.makeText(context, I18n.s(R.string.ui_no_steam_store_lossless_scaling_found_download_it_fro), Toast.LENGTH_LONG).show()
            return
        }
        try {
            lsfgDllFile.parentFile?.mkdirs()
            src.inputStream().use { input -> lsfgDllFile.outputStream().use { output -> input.copyTo(output) } }
            prefs.edit().putString("lsfg_dll_source", "store").apply()
            lsfgDllStatus = lsfgDllStatusText()
            buildLsfgShaderCache()
            Toast.makeText(context, "Lossless.dll set from Steam store install.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            lsfgDllStatus = I18n.s(R.string.ui_detect_failed) + e.message
            Toast.makeText(context, I18n.s(R.string.ui_detect_failed) + e.message, Toast.LENGTH_LONG).show()
        }
    }

    // ── In-app pickers (primary actions, matching SettingsScreen). ──
    val installSFInAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data) else null
        if (uri != null && activity != null) {
            val dialog = PreloaderDialog(activity)
            dialog.showOnUiThread(R.string.installing_content)
            MidiManager.installSF2File(context, uri, object : MidiManager.OnSoundFontInstalledCallback {
                override fun onSuccess() {
                    dialog.closeOnUiThread()
                    activity.runOnUiThread {
                        ContentDialog.alert(context, R.string.sound_font_installed_success, null)
                        refreshSF()
                    }
                }
                override fun onFailed(reason: Int) {
                    dialog.closeOnUiThread()
                    val resId = when (reason) {
                        MidiManager.ERROR_BADFORMAT -> R.string.sound_font_bad_format
                        MidiManager.ERROR_EXIST -> R.string.sound_font_already_exist
                        else -> R.string.sound_font_installed_failed
                    }
                    activity.runOnUiThread { ContentDialog.alert(context, resId, null) }
                }
            })
        }
    }
    val restoreFileInAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data)?.let { beginRestoreFromUri(it) }
    }
    val importLosslessDllInAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data)?.let { importLosslessDllFromUri(it) }
    }

    // ── Seed the SoundFont list + warm the LSFG shader cache on entry (as SettingsScreen does). ──
    LaunchedEffect(Unit) {
        refreshSF()
        buildLsfgShaderCache()
    }

    // ── Overlay dialogs. ──
    var showLogManager by remember { mutableStateOf(false) }
    var showPerformance by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateNote by remember { mutableStateOf<String?>(null) }
    // Kept so the install button below has the build to download.
    var availableUpdate by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    fun checkNow() {
        checkingUpdate = true
        updateNote = null
        availableUpdate = null
        UpdateManager.check(context) { info ->
            activity?.runOnUiThread {
                checkingUpdate = false
                availableUpdate = info?.takeIf { it.isNewer }
                updateNote = info?.takeIf { it.isNewer }?.let { "V ${it.versionName} available" }
                    ?: I18n.s(R.string.ui_you_re_on_the_latest_build)
            }
        }
    }
    fun selectChannel(preview: Boolean) {
        if (preview == includePre) return
        includePre = preview
        UpdateManager.setIncludePrereleases(context, preview)
        checkNow()
    }

    // SAF launchers for the two path pickers.
    val winlatorPathLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) winlatorPathUri = uri }
    val shortcutsPathLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) shortcutsPathUri = uri }

    fun save() {
        // Same writes as SettingsScreen.saveSettings() — only the keys this page owns, commit().
        val editor = prefs.edit()
            .putString("box64_preset", box64)
            .putString("fexcore_preset", fex)
            .putBoolean("dark_mode", darkMode)
            .putBoolean("enable_big_picture_mode", bigPicture)
            .putString("default_landing_screen", landing)
            .putBoolean("enable_custom_api_key", apiKeyEnabled)
            .putBoolean("cursor_lock", cursorLock)
            .putBoolean("xinput_toggle", xinputOff)
            .putBoolean("use_dri3", dri3)
            .putBoolean("use_xr", xr)
            .putFloat("cursor_speed", cursorSpeed)
            .putBoolean("enable_file_provider", fileProvider)
            .putBoolean("open_with_android_browser", androidBrowser)
            .putBoolean("share_android_clipboard", shareClipboard)
            .putString("downloadable_contents_url", contentsUrl)
        if (apiKeyEnabled) editor.putString("custom_api_key", apiKey)
        else editor.remove("custom_api_key")
        winlatorPathUri?.let {
            editor.putString("winlator_path_uri", it.toString())
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        shortcutsPathUri?.let {
            editor.putString("shortcuts_export_path_uri", it.toString())
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        editor.commit()
        saved = true
    }

    DeckDragFrame(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .deckPageBackground(),
    ) {
        // ── Header ──
        Column(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 10.dp)) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeckPalette.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = I18n.s(R.string.ui_identical_to_the_settings_tab),
                fontSize = 13.sp,
                color = DeckPalette.onSurfaceVariant,
            )
        }

        // ── Category navigation: top tabs / left rail / full-page list ──
        if (settingsLayout == "tabs") {
            DeckStretchyTabs(
                labels = settingsTabs,
                selected = settingsTab,
                onSelect = { settingsTab = it; subTabs.select(it) },
                focusable = false,
                scroll = true,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
            )
        }

        if (settingsLayout == "portrait") {
            DeckPortraitTabs(
                settingsTabs, settingsTab, { settingsTab = it; subTabs.select(it) },
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (settingsLayout == "pages" && !settingsPagesCategoryOpen) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                settingsTabs.forEachIndexed { i, label ->
                    DeckEditorCategoryRow(
                        label = label,
                        selected = i == settingsTab,
                        onClick = { settingsTab = i; subTabs.select(i); settingsPagesCategoryOpen = true },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        } else {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (settingsLayout == "rail") {
                DeckEditorCategoryRail(
                    labels = settingsTabs,
                    selected = settingsTab,
                    onSelect = { settingsTab = it; subTabs.select(it) },
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                )
            }

        // Switching tabs removes the focused control with the old page; once the swap settles, put the
        // pad on the new page's first control (top-left in focus order) instead of a lost focus.
        val tabContentFocus = remember { FocusRequester() }
        var firstTabShown by remember { mutableStateOf(true) }
        LaunchedEffect(settingsTab, settingsPagesCategoryOpen) {
            if (firstTabShown) { firstTabShown = false; return@LaunchedEffect }
            kotlinx.coroutines.delay(80)
            if (XmbRailPick.fresh()) return@LaunchedEffect
            runCatching { tabContentFocus.requestFocus() }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(tabContentFocus)
                .focusGroup()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
        if (settingsLayout == "pages" && settingsPagesCategoryOpen) {
            DeckRow(
                label = I18n.s(R.string.ui_all_categories),
                hint = I18n.s(R.string.ui_back_to_the_category_list),
                onConfigClick = { settingsPagesCategoryOpen = false },
                control = {},
            )
            Spacer(Modifier.height(8.dp))
        }
        when (settingsTab) {
        0 -> {
        // ── Steam ──
        DeckGroup(title = "Steam") {
            DeckRow(label = I18n.s(R.string.ui_steam_chat_notifications)) {
                DeckToggle(steamChat) {
                    steamChat = it
                    SteamPrefs.setChatNotificationsEnabled(context, it)
                }
            }
            DeckRow(label = I18n.s(R.string.ui_show_me_as_in_game_for_offline_launches)) {
                DeckToggle(steamOffline) {
                    steamOffline = it
                    SteamPrefs.setOfflinePresenceEnabled(context, it)
                }
            }
            DeckChoiceRow(
                label = I18n.s(R.string.ui_steam_connection_region),
                options = buildList {
                    add(SteamRegion.AUTO to I18n.s(R.string.ui_auto_nearest_by_ping_2))
                    SteamRegion.CATALOG.forEach { add(it.code to "${it.name}  (${it.code})") }
                },
                selected = steamRegion,
                onPick = {
                    steamRegion = it
                    SteamRegion.setMode(context, it)
                },
            )
        }

        // ── Sound ──
        DeckGroup(title = I18n.s(R.string.sound)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_midi_sound_font),
                options = sfNames.map { it to it },
                selected = sfNames.getOrElse(selectedSF) { MidiManager.DEFAULT_SF2_FILE },
                onPick = { picked -> selectedSF = sfNames.indexOf(picked).coerceAtLeast(0) },
            )
            DeckRow(label = "Install / Remove", hint = I18n.s(R.string.ui_add_a_sf2_or_remove_a_custom_one)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeckButton("Install") {
                        installSFInAppLauncher.launch(
                            InAppFilePicker.buildIntent(context, InAppFilePicker.SF2, I18n.s(R.string.ui_select_soundfont)),
                        )
                    }
                    DeckButton(I18n.s(R.string.remove), primary = false) {
                        if (selectedSF != 0) {
                            ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_sound_font) {
                                if (MidiManager.removeSF2File(context, sfNames[selectedSF])) {
                                    AppUtils.showToast(context, R.string.sound_font_removed_success)
                                    refreshSF()
                                } else AppUtils.showToast(context, R.string.sound_font_removed_failed)
                            }
                        } else AppUtils.showToast(context, R.string.cannot_remove_default_sound_font)
                    }
                }
            }
        }

        // ── Music Player ──
        DeckGroup(title = I18n.s(R.string.ui_music_player)) {
            DeckRow(
                label = I18n.s(R.string.ui_music_file),
                hint = musicName ?: "No file chosen. Pick an MP3 to play while you use WinHub.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeckButton(I18n.s(R.string.ui_choose)) {
                        musicLauncher.launch(
                            InAppFilePicker.buildIntent(
                                context, arrayOf("mp3", "m4a", "ogg", "wav", "flac", "aac"), I18n.s(R.string.ui_choose_music),
                            ),
                        )
                    }
                    if (musicName != null) {
                        DeckButton(I18n.s(R.string.clear), primary = false) {
                            com.winhub.hernandez.core.MusicPlayer.clear(context)
                            musicName = null
                        }
                    }
                }
            }
            if (musicName != null) {
                DeckRow(label = I18n.s(R.string.ui_play_music), hint = I18n.s(R.string.ui_loops_while_winhub_is_open)) {
                    DeckToggle(musicOn) {
                        musicOn = it
                        com.winhub.hernandez.core.MusicPlayer.setEnabled(it)
                    }
                }
                DeckRow(
                    label = I18n.s(R.string.ui_play_in_games),
                    hint = I18n.s(R.string.ui_off_by_default_the_music_plays_under_the_game_own_sou),
                ) {
                    DeckToggle(musicInGame) {
                        musicInGame = it
                        com.winhub.hernandez.core.MusicPlayer.setPlayInGame(it)
                    }
                }
            }
        }

        // ── Mali ──
        DeckGroup(title = "Mali") {
            DeckRow(
                label = I18n.s(R.string.ui_force_mali_mode),
                hint = I18n.s(R.string.ui_use_this_if_the_app_doesn_t_detect_a_non_adreno_gpu),
            ) {
                DeckToggle(forceMali) {
                    forceMali = it
                    prefs.edit().putBoolean(com.winhub.hernandez.core.MaliPanvk.PREF_FORCE, it).apply()
                    com.winhub.hernandez.core.MaliPanvk.setForced(it)
                }
            }
        }

        // ── Settings view ──
        DeckGroup(title = I18n.s(R.string.ui_settings_view)) { DeckViewModeRow() }

        // ── Game Doctor ──
        DeckGroup(title = I18n.s(R.string.ui_game_doctor)) {
            var doctorAuto by remember { mutableStateOf(GameDoctor.isAutoEnabled(context)) }
            DeckRow(
                label = I18n.s(R.string.ui_auto_step_down),
                hint = I18n.s(R.string.ui_when_a_game_crashes_soon_after_launch_lower_its_fex_p),
                control = {
                    DeckToggle(doctorAuto) {
                        doctorAuto = it
                        GameDoctor.setAutoEnabled(context, it)
                    }
                },
            )
        }

        // ── Box64 ──
        DeckGroup(title = "Box64") {
            DeckChoiceRow(
                label = I18n.s(R.string.box64_preset),
                options = box64Presets.map { it.id to it.name },
                selected = box64,
                onPick = { box64 = it; saved = false },
            )
            PresetEditorRow(
                kind = PresetKind.BOX64,
                selectedPresetId = box64,
                scope = PresetScope.GLOBAL,
                container = null,
                shortcut = null,
                onSelect = { box64 = it; saved = false },
                onListChanged = {
                    box64Presets = Box64PresetManager.getPresets("box64", context)
                    // The selected custom preset may be the one just deleted.
                    if (box64Presets.none { it.id == box64 }) { box64 = "COMPATIBILITY"; saved = false }
                },
                onValuesChanged = {},
            )
        }

        // ── FEXCore Config ──
        DeckGroup(title = I18n.s(R.string.ui_fexcore_config)) {
            DeckChoiceRow(
                label = I18n.s(R.string.fexcore_preset),
                options = fexPresets.map { it.id to it.name },
                selected = fex,
                onPick = { fex = it; saved = false },
            )
            PresetEditorRow(
                kind = PresetKind.FEXCORE,
                selectedPresetId = fex,
                scope = PresetScope.GLOBAL,
                container = null,
                shortcut = null,
                onSelect = { fex = it; saved = false },
                onListChanged = {
                    fexPresets = FEXCorePresetManager.getPresets(context)
                    if (fexPresets.none { it.id == fex }) { fex = "COMPATIBILITY"; saved = false }
                },
                onValuesChanged = {},
            )
        }

        }
        1 -> {
        // ── Side Menu ──
        DeckGroup(title = I18n.s(R.string.ui_side_menu)) {
            DeckRow(label = I18n.s(R.string.ui_show_game_stores)) {
                DeckToggle(showStores) {
                    showStores = it
                    AppThemeState.setShowStores(it)
                }
            }
            DeckRow(label = I18n.s(R.string.ui_show_internal_storage)) {
                DeckToggle(showInternal) {
                    showInternal = it
                    AppThemeState.setShowInternalStorage(it)
                }
            }
            DeckRow(label = I18n.s(R.string.ui_show_sd_card_storage)) {
                DeckToggle(showSd) {
                    showSd = it
                    AppThemeState.setShowSdStorage(it)
                }
            }
        }

        // ── App Orientation ──
        DeckGroup(title = I18n.s(R.string.ui_app_orientation)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_app_orientation),
                options = listOf(
                    AppOrientation.AUTO to "Auto",
                    AppOrientation.PORTRAIT to "Portrait",
                    AppOrientation.LANDSCAPE to "Landscape",
                ),
                selected = orientation,
                onPick = {
                    orientation = it
                    AppOrientation.setMode(context, it)
                },
            )
        }

        // ── Interface Scale ──
        DeckGroup(title = I18n.s(R.string.ui_interface_scale)) {
            DeckSliderRow(
                label = I18n.s(R.string.ui_ui_scale),
                value = uiScale,
                range = 0.5f..1.5f,
                format = { "${(it * 100).toInt()}%" },
                onChange = {
                    uiScale = it
                    AppThemeState.setUiScale(it)
                },
            )
            DeckSliderRow(
                label = I18n.s(R.string.ui_font_size),
                value = fontScale,
                range = 0.5f..1.5f,
                format = { "${(it * 100).toInt()}%" },
                onChange = {
                    fontScale = it
                    AppThemeState.setFontScale(it)
                },
            )
        }

        // ── Default Screen on Launch ──
        DeckGroup(title = I18n.s(R.string.ui_default_screen_on_launch)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_default_landing_screen),
                options = listOf(
                    "games" to I18n.s(R.string.ui_game_shortcuts),
                    "containers" to I18n.s(R.string.containers),
                ),
                selected = landing,
                onPick = { landing = it; saved = false },
            )
        }

        // ── Big Picture Mode ──
        DeckGroup(title = I18n.s(R.string.ui_big_picture_mode)) {
            DeckRow(label = I18n.s(R.string.ui_enable_big_picture_mode_on_app_launch)) {
                DeckToggle(bigPicture) { bigPicture = it; saved = false }
            }
        }

        }
        2 -> {
        // ── Theme ──
        DeckGroup(title = I18n.s(R.string.theme)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_app_theme),
                hint = I18n.s(R.string.ui_recolors_the_whole_app_including_popups_and_touch_con),
                options = themeChoices.map { it.name to it.name },
                selected = selectedTheme,
                onPick = { name ->
                    val idx = themeChoices.indexOfFirst { it.name == name }
                    if (idx >= 0) AppThemeState.setPreset(idx)
                },
            )
            DeckRow(
                label = I18n.s(R.string.ui_preview),
                hint = selectedTheme,
                control = {
                    ThemePreviewDots(
                        themeChoices.firstOrNull { it.name == selectedTheme } ?: themeChoices.first(),
                    )
                },
            )
            DeckChoiceRow(
                label = I18n.s(R.string.ui_app_icon),
                hint = I18n.s(R.string.ui_four_circles_of_the_launcher_icon_default_keeps_them),
                options = AppIcon.options,
                selected = appIcon,
                onPick = {
                    appIcon = it
                    AppIcon.setChoice(context, it)
                },
            )
        }

        // ── Orientation ── (only place this can be changed; the app restarts its screen to apply it)
        DeckGroup(title = I18n.s(R.string.orientation)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_app_orientation_2),
                hint = I18n.s(R.string.ui_portrait_is_a_touch_first_layout_with_the_tabs_along),
                options = listOf(
                    AppOrientation.AUTO to "Landscape",
                    AppOrientation.PORTRAIT to "Portrait",
                ),
                selected = if (AppOrientation.mode(context) == AppOrientation.PORTRAIT) AppOrientation.PORTRAIT else AppOrientation.AUTO,
                onPick = { mode ->
                    if (mode != AppOrientation.mode(context)) {
                        AppOrientation.setMode(context, mode)
                        activity?.recreate()
                    }
                },
            )
        }

        // ── Home ── (landscape only: portrait Home is always the cover grid)
        if (!portraitUi) DeckGroup(title = I18n.s(R.string.ui_home)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_interface_look),
                hint = I18n.s(R.string.ui_glass_puts_every_page_on_see_through_glass_and_a_game),
                options = listOf(
                    GlassLook.GLASS to "Glass",
                    GlassLook.MATERIAL to "Material",
                ),
                selected = uiLook,
                onPick = {
                    uiLook = it
                    prefs.edit().putString(GlassLook.KEY, it).apply()
                    // Glass is tuned on Charcoal (neutral surfaces): the default theme moves to it, a chosen one stays.
                    if (it == GlassLook.GLASS && AppThemeState.presetIndex.value == DEFAULT_PRESET_INDEX) {
                        AppThemeState.setPreset(CHARCOAL_PRESET_INDEX)
                    }
                },
            )
            DeckChoiceRow(
                label = I18n.s(R.string.ui_home_style),
                hint = I18n.s(R.string.ui_full_xmb_puts_the_stores_and_social_on_home_as_cards),
                options = listOf(
                    DeckHomeStyle.FULL_XMB to I18n.s(R.string.ui_full_xmb),
                    DeckHomeStyle.STEAM to "Steam",
                ),
                selected = homeStyle,
                onPick = {
                    homeStyle = it
                    prefs.edit().putString(DeckHomeStyle.KEY, it).apply()
                },
            )
            DeckChoiceRow(
                label = I18n.s(R.string.ui_game_settings),
                hint = I18n.s(R.string.ui_what_a_game_settings_opens_from_home_classic_opens_th),
                options = listOf(
                    GameSettingsStyle.CLASSIC to "Classic",
                    GameSettingsStyle.XMB to "XMB",
                ),
                selected = gameSettingsStyle,
                onPick = {
                    gameSettingsStyle = it
                    prefs.edit().putString(GameSettingsStyle.KEY, it).apply()
                },
            )
        }

        // ── Retro library ── (landscape only, like the Home it switches on)
        if (!portraitUi) DeckGroup(title = I18n.s(R.string.retro_title)) {
            DeckRow(
                label = I18n.s(R.string.retro_opt_folder),
                hint = retroRoot ?: I18n.s(R.string.ui_not_chosen_yet_winhub_looks_in_roms_and_games_when_you_o),
            ) {
                DeckButton(I18n.s(R.string.ui_choose), primary = false) {
                    retroFolderLauncher.launch(InAppFilePicker.buildDirIntent(context, I18n.s(R.string.retro_opt_folder), retroRoot))
                }
            }
            DeckRow(
                label = "Consoles",
                hint = if (retroConsoleCount == 0) "None picked. Pick the consoles whose games the retro library shows" else "$retroConsoleCount picked",
            ) {
                DeckButton(I18n.s(R.string.ui_choose), primary = false) { showRetroConsoles = true }
            }
            DeckRow(
                label = I18n.s(R.string.ui_removed_games),
                hint = if (retroHidden == 0) "None removed from the retro library" else "$retroHidden removed from the retro library. Their ROM files are untouched",
            ) {
                DeckButton(I18n.s(R.string.ui_show_again), primary = false, enabled = retroHidden > 0) {
                    RetroLibrary.clearExcluded(context)
                    RetroSession.scan = null
                    retroHidden = 0
                }
            }
            DeckRow(label = I18n.s(R.string.retro_opt_rescan), hint = I18n.s(R.string.ui_looks_for_new_and_missing_rom_files_the_next_time_the)) {
                DeckButton(I18n.s(R.string.ui_rescan), primary = false) {
                    RetroSession.scan = null
                    Toast.makeText(context, I18n.s(R.string.ui_the_retro_library_rescans_when_you_open_it), Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (showRetroConsoles) RetroConsolesDialog {
            showRetroConsoles = false
            retroConsoleCount = RetroLibrary.enabledSystems(context).size
        }

        // ── Sounds ──
        DeckGroup(title = I18n.s(R.string.ui_navigation_sounds)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_sound_set),
                hint = I18n.s(R.string.ui_sounds_the_d_pad_and_buttons_make_in_deck_mode_steamo),
                options = listOf(
                    DeckSound.STEAMOS to "SteamOS",
                    DeckSound.PHONE to "Phone",
                    DeckSound.OFF to "Off",
                ),
                selected = navSounds,
                onPick = {
                    navSounds = it
                    prefs.edit().putString(DeckSound.KEY, it).apply()
                    DeckSound.init(context)
                    DeckSound.play(DeckSound.SELECT)
                },
            )
        }

        // ── Stores ── (the Deck pages inside this app, or the full-screen XMB store pages)
        DeckGroup(title = I18n.s(R.string.xmb_tab_stores)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_store_style),
                hint = I18n.s(R.string.ui_xmb_opens_each_store_as_its_own_full_screen_xmb_page),
                options = listOf(
                    StoreStyle.DECK to "Deck",
                    StoreStyle.XMB to "XMB",
                ),
                selected = storeStyle,
                onPick = {
                    storeStyle = it
                    prefs.edit().putString(StoreStyle.KEY, it).apply()
                },
            )
        }

        // ── Top Bar ──
        DeckGroup(title = I18n.s(R.string.ui_top_bar)) {
            DeckSliderRow(
                label = I18n.s(R.string.ui_top_bar_opacity),
                hint = I18n.s(R.string.ui_background_of_the_tab_bar_across_the_top_of_deck_mode),
                value = topBarOpacity,
                range = 0f..1f,
                format = { "${(it * 100).toInt()}%" },
                onChange = {
                    topBarOpacity = it
                    AppThemeState.setTopBarOpacity(it)
                },
            )
        }

        // ── Editors ──
        DeckGroup(title = I18n.s(R.string.ui_editors)) {
            DeckChoiceRow(
                label = I18n.s(R.string.ui_config_editor_style),
                hint = I18n.s(R.string.ui_how_driver_wrapper_and_renderer_settings_open),
                options = listOf(
                    "popup" to I18n.s(R.string.ui_popup_dialog),
                    "dropdown" to I18n.s(R.string.ui_dropdown_inline),
                ),
                selected = configEditorStyle,
                onPick = {
                    configEditorStyle = it
                    prefs.edit().putString("config_editor_style", it).apply()
                },
            )
            if (portraitUi) DeckChoiceRow(
                label = I18n.s(R.string.ui_portrait_tabs),
                hint = I18n.s(R.string.ui_how_the_container_editor_contents_and_settings_arrang),
                options = listOf(
                    "strip" to I18n.s(R.string.ui_icon_tabs_top),
                    "pages" to I18n.s(R.string.ui_category_list),
                ),
                selected = settingsLayout.let { if (it == "pages") "pages" else "strip" },
                onPick = {
                    settingsLayout = if (it == "pages") "pages" else "portrait"
                    settingsPagesCategoryOpen = false
                    prefs.edit().putString(PORTRAIT_TABS_PREF, it).apply()
                },
            )
            if (!portraitUi) DeckChoiceRow(
                label = I18n.s(R.string.ui_container_editor_layout),
                hint = I18n.s(R.string.ui_how_the_container_editor_arranges_its_categories),
                options = listOf(
                    "rail" to I18n.s(R.string.ui_side_rail),
                    "pages" to I18n.s(R.string.ui_category_pages),
                ),
                selected = editorLayout,
                onPick = {
                    editorLayout = it
                    prefs.edit().putString("container_editor_layout", it).apply()
                },
            )
            if (!portraitUi) DeckChoiceRow(
                label = I18n.s(R.string.ui_settings_layout),
                hint = I18n.s(R.string.ui_how_the_settings_page_save_manager_and_contents_arran),
                options = listOf(
                    "rail" to I18n.s(R.string.ui_side_rail),
                    "pages" to I18n.s(R.string.ui_category_pages),
                ),
                selected = settingsLayout,
                onPick = {
                    settingsLayout = it
                    settingsPagesCategoryOpen = false
                    prefs.edit().putString("settings_layout", it).apply()
                },
            )
        }

        }
        3 -> {
        // ── Path Settings ──
        DeckGroup(title = I18n.s(R.string.ui_path_settings)) {
            DeckRow(
                label = I18n.s(R.string.ui_winlator_path),
                hint = winlatorPathUri?.lastPathSegment ?: "/storage/emulated/0/Winlator",
            ) {
                DeckButton(I18n.s(R.string.ui_choose), primary = false) { winlatorPathLauncher.launch(null) }
            }
            DeckRow(
                label = I18n.s(R.string.ui_shortcut_export_path),
                hint = shortcutsPathUri?.lastPathSegment ?: "/storage/emulated/0/Winlator/Shortcuts",
            ) {
                DeckButton(I18n.s(R.string.ui_choose), primary = false) { shortcutsPathLauncher.launch(null) }
            }
        }

        // ── SteamGrid API ──
        DeckGroup(title = I18n.s(R.string.ui_steamgrid_api)) {
            DeckRow(label = I18n.s(R.string.ui_set_steamgrid_api_key_cover_art)) {
                DeckToggle(apiKeyEnabled) { apiKeyEnabled = it; saved = false }
            }
            if (apiKeyEnabled) {
                DeckTextRow(
                    label = I18n.s(R.string.ui_api_key),
                    value = apiKey,
                    onValueChange = { apiKey = it; saved = false },
                    width = 260.dp,
                )
            }
        }

        // ── XServer ──
        DeckGroup(title = I18n.s(R.string.xserver)) {
            DeckSliderRow(
                label = I18n.s(R.string.cursor_speed),
                value = cursorSpeed,
                range = 0.1f..2.0f,
                format = { "${(it * 100).toInt()}%" },
                onChange = { cursorSpeed = it; saved = false },
            )
            DeckRow(label = I18n.s(R.string.ui_true_mouse_control_deactivate_with_volume_down)) {
                DeckToggle(cursorLock) { cursorLock = it; saved = false }
            }
            DeckRow(label = I18n.s(R.string.ui_disable_xinput_used_for_exclusive_m_kb_support)) {
                DeckToggle(xinputOff) { xinputOff = it; saved = false }
            }
            DeckRow(label = I18n.s(R.string.ui_use_dri3_extension)) {
                DeckToggle(dri3) { dri3 = it; saved = false }
            }
            DeckRow(label = I18n.s(R.string.ui_use_xr)) {
                DeckToggle(xr) { xr = it; saved = false }
            }
        }

        // ── Logs ──
        DeckGroup(title = I18n.s(R.string.logs)) {
            DeckRow(label = I18n.s(R.string.ui_open_log_manager), hint = "Wine / Box64 logging and log location") {
                DeckButton(I18n.s(R.string.xmb_all_games_open), primary = false) { showLogManager = true }
            }
        }

        // ── Experimental ──
        DeckGroup(title = I18n.s(R.string.experimental)) {
            DeckRow(label = I18n.s(R.string.enable_file_provider)) {
                DeckToggle(fileProvider) { fileProvider = it; saved = false }
            }
            DeckRow(label = I18n.s(R.string.ui_open_with_android_browser)) {
                DeckToggle(androidBrowser) { androidBrowser = it; saved = false }
            }
            DeckRow(label = I18n.s(R.string.ui_share_android_clipboard)) {
                DeckToggle(shareClipboard) { shareClipboard = it; saved = false }
            }
            DeckTextRow(
                label = I18n.s(R.string.ui_downloadable_contents_url),
                value = contentsUrl,
                onValueChange = { contentsUrl = it; saved = false },
                width = 320.dp,
            )
        }

        // ── ImageFS ──
        DeckGroup(title = I18n.s(R.string.ui_imagefs)) {
            DeckRow(label = I18n.s(R.string.ui_reinstall_imagefs), hint = I18n.s(R.string.ui_restore_system_files_containers_unchanged)) {
                DeckButton(I18n.s(R.string.ui_reinstall), primary = false) {
                    ContentDialog.confirm(context, R.string.do_you_want_to_reinstall_imagefs) {
                        mainActivity?.reinstallImageFs()
                    }
                }
            }
            DeckRow(label = I18n.s(R.string.ui_backup_data), hint = I18n.s(R.string.ui_archive_the_app_data_directory)) {
                DeckButton(I18n.s(R.string.ui_backup), primary = false) { showBackupDialog = true }
            }
            DeckRow(label = I18n.s(R.string.ui_restore_data), hint = I18n.s(R.string.ui_restore_from_a_backup_restarts_the_app)) {
                DeckButton(I18n.s(R.string.ui_restore), primary = false) {
                    restoreFileInAppLauncher.launch(
                        InAppFilePicker.buildIntent(context, InAppFilePicker.SAVE, I18n.s(R.string.ui_select_backup)),
                    )
                }
            }
        }

        }
        4 -> {
        // ── Frame Generation — LSFG Native (Lossless Scaling) ──
        DeckGroup(title = I18n.s(R.string.ui_frame_generation_lsfg_native)) {
            DeckRow(label = I18n.s(R.string.ui_status), hint = lsfgDllStatus) {
                if (lsfgShaderStatus.isNotEmpty()) {
                    Text(lsfgShaderStatus, color = DeckPalette.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            DeckRow(label = I18n.s(R.string.ui_detect_from_steam_store), hint = I18n.s(R.string.ui_find_lossless_dll_in_a_store_install)) {
                DeckButton(I18n.s(R.string.ui_detect), primary = false) { detectLosslessDllFromStore() }
            }
            DeckRow(label = "Import Lossless.dll", hint = I18n.s(R.string.ui_pick_your_own_copy)) {
                DeckButton(I18n.s(R.string.preset_action_import), primary = false) {
                    importLosslessDllInAppLauncher.launch(
                        InAppFilePicker.buildIntent(context, InAppFilePicker.DLL, "Select Lossless.dll"),
                    )
                }
            }
            if (lsfgDllFile.isFile) {
                DeckRow(label = "Remove Lossless.dll") {
                    DeckButton(I18n.s(R.string.remove), primary = false) {
                        lsfgDllFile.delete()
                        com.winhub.hernandez.core.LsfgNative.cacheFile(context).delete()
                        prefs.edit().remove("lsfg_dll_source").apply()
                        lsfgDllStatus = lsfgDllStatusText()
                        lsfgShaderStatus = ""
                    }
                }
            }
        }

        // ── Performance ──
        DeckGroup(title = I18n.s(R.string.performance)) {
            DeckRow(label = I18n.s(R.string.ui_open_performance_settings)) {
                DeckButton(I18n.s(R.string.xmb_all_games_open), primary = false) { showPerformance = true }
            }
        }
        }
        5 -> {
        // ── Release channel ── (Stable = latest non-prerelease, Preview = newest release incl. betas)
        DeckGroup(title = I18n.s(R.string.ui_release_channel)) {
            DeckRow(
                label = I18n.s(R.string.ui_update_channel),
                hint = if (includePre) "Preview: beta builds, newest first" else I18n.s(R.string.ui_stable_tested_releases_only),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeckButton(I18n.s(R.string.ui_stable), primary = !includePre) { selectChannel(false) }
                    DeckButton(I18n.s(R.string.ui_preview), primary = includePre) { selectChannel(true) }
                }
            }
        }

        // ── Updates ──
        DeckGroup(title = I18n.s(R.string.ui_updates)) {
            DeckRow(label = I18n.s(R.string.ui_notify_me_about_updates)) {
                DeckToggle(notifyUpdates) {
                    notifyUpdates = it
                    UpdateManager.setNotifyEnabled(context, it)
                }
            }
            DeckRow(
                label = if (checkingUpdate) "Checking…" else I18n.s(R.string.ui_check_for_updates_2),
                hint = updateNote ?: "Installed: V ${UpdateManager.installedVersionName()}",
            ) {
                DeckButton(I18n.s(R.string.ui_check), primary = false) { checkNow() }
            }
            availableUpdate?.let { update ->
                val sizeMb = update.downloadSize.takeIf { it > 0 }?.let { " (${it / (1024 * 1024)} MB)" } ?: ""
                DeckRow(
                    label = "Download & install V ${update.versionName}",
                    hint = (update.notes.takeIf { it.isNotBlank() } ?: I18n.s(R.string.ui_downloads_in_the_background_and_opens_the_installer)) + sizeMb,
                ) {
                    DeckButton("Install", primary = true) {
                        activity?.let { UpdateManager.downloadAndInstall(it, update) {} }
                    }
                }
            }
        }

        // ── Layers and components: Proton, driver, FEXCore ──
        DeckComponentStatusGroup()
        }
        }
        }
        }
        }

        // ── Save bar (fixed, outside the tab content) ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeckPalette.primary)
                    .deckDragItem("settings-save")
                    .clickable { save() }
                    .padding(horizontal = 28.dp),
            ) {
                Text(
                    text = I18n.s(R.string.save),
                    color = DeckPalette.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (saved) {
                Text(
                    text = I18n.s(R.string.ui_saved),
                    color = DeckPalette.good,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
    }

    // ── Overlay dialogs (same as SettingsScreen). ──
    if (showLogManager) DeckLogManagerDialog(onClose = { showLogManager = false })
    if (showPerformance) {
        AlertDialog(
            onDismissRequest = { showPerformance = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPerformance = false }) { Text(I18n.s(R.string.wrapper_manager_close)) }
            },
            text = { PerformanceSettingsScreen(onClose = { showPerformance = false }) },
        )
    }
    // ── ImageFS backup/restore dialogs (same as SettingsScreen). ──
    if (isBackingUp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeckPalette.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(I18n.s(R.string.backing_up_data))
            }
        }
    }
    if (showBackupDialog) {
        DeckWarningDialog(
            title = I18n.s(R.string.ui_backup_data),
            message = I18n.s(R.string.ui_do_you_want_to_create_a_backup_of_the_app_data_direct),
            confirmLabel = "Yes",
            onConfirm = {
                showBackupDialog = false
                isBackingUp = true
                val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
                executor.execute {
                    val dataDir = context.filesDir.parentFile
                    val backupFile = File(Environment.getExternalStorageDirectory(), "app_data_backup.tar")
                    try {
                        com.winhub.hernandez.core.TarCompressorUtils.archive(
                            arrayOf(dataDir), backupFile,
                        ) { file -> !file.absolutePath.contains("imagefs/tmp/.sysvshm") }
                        (context as? Activity)?.runOnUiThread {
                            isBackingUp = false
                            AppUtils.showToast(context, "Backup completed: ${backupFile.path}")
                        }
                    } catch (_: Exception) {
                        (context as? Activity)?.runOnUiThread {
                            isBackingUp = false
                            AppUtils.showToast(context, I18n.s(R.string.ui_backup_failed))
                        }
                    }
                }
            },
            onDismiss = { showBackupDialog = false },
        )
    }
    if (showRestoreConfirm) {
        DeckWarningDialog(
            title = I18n.s(R.string.ui_restore_data),
            message = I18n.s(R.string.ui_this_will_restart_the_app_continue),
            confirmLabel = "Restore",
            onConfirm = {
                showRestoreConfirm = false
                pendingRestoreUri?.let { uri ->
                    val intent = Intent(context, com.winhub.hernandez.restore.RestoreActivity::class.java)
                    intent.data = uri
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            },
            onDismiss = { showRestoreConfirm = false; pendingRestoreUri = null },
        )
    }
}

/** Four-dot live preview of the active theme: background, surface, primary and secondary. */
@Composable
private fun ThemePreviewDots(preset: ThemePreset) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(preset.background, preset.surface, preset.primary, preset.secondary).forEach { color ->
            Box(
                Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
                    .border(1.dp, DeckPalette.line2, RoundedCornerShape(5.dp)),
            )
        }
    }
}
