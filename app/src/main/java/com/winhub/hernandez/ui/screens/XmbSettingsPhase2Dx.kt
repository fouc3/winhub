package com.winhub.hernandez.ui.screens

import android.content.Context
import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.winhub.hernandez.R
import com.winhub.hernandez.container.Shortcut
import com.winhub.hernandez.contentdialog.DXVKConfigDialog
import com.winhub.hernandez.contentdialog.VegasKeyCatalog
import com.winhub.hernandez.contentdialog.VegasKeyKnowledge
import com.winhub.hernandez.contentdialog.VegasTierPresets
import com.winhub.hernandez.contents.ContentProfile
import com.winhub.hernandez.contents.ContentsManager
import com.winhub.hernandez.core.KeyValueSet
import com.winhub.hernandez.core.StringUtils
import com.winhub.hernandez.core.WineInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import com.winhub.hernandez.util.I18n

// ── DX wrapper configuration: DxvkConfigDialog (DXVK / VEGAS) or WineD3DConfigDialog ──────────────
// Both dialogs rebuild "dxwrapperConfig" on OK from the stored string (DXVKConfigDialog / WineD3D-
// ConfigDialog.parseConfig → KeyValueSet.put). Every change here does the same with the values the
// dialog would show, so a single change persists exactly what that dialog's OK would.

internal fun xmbDxConfigMenu(xmb: XmbScope, shortcut: Shortcut): XmbMenu {
    val ctx = xmb.context
    val c = shortcut.container
    val entries = ctx.resources.getStringArray(R.array.dxwrapper_entries).toList()
    val id = shortcut.p2Ex("dxwrapper", c.getDXWrapper())
    val w = StringUtils.parseIdentifier(entries.firstOrNull { StringUtils.parseIdentifier(it) == id } ?: entries.firstOrNull() ?: id)
    return if (w.contains("dxvk") || w.contains("vegas")) p2DxvkMenu(xmb, shortcut, w.contains("vegas")) else p2WineD3DMenu(xmb, shortcut)
}

// ── WineD3D ─────────────────────────────────────────────────────────────────────────────────────────

private data class P2D3dSel(
    val csmt: String, val gpuName: String, val ddra: String, val videoMem: String,
    val ssm: String, val orm: String, val renderer: String,
)

private fun p2WineD3DMenu(xmb: XmbScope, s: Shortcut): XmbMenu {
    val ctx = xmb.context
    val res = ctx.resources
    val c = s.container
    val onOff = listOf("Enabled", "Disabled")
    val ormOptions = listOf("fbo", "backbuffer")
    val rendOptions = listOf("gl", "vulkan", "gdi")
    val ddraEntries = res.getStringArray(R.array.ddrawrapper_entries).toList()
    val videoMemEntries = res.getStringArray(R.array.video_memory_size_entries).toList()
    var gpuNames by mutableStateOf<List<String>>(emptyList())
    xmb.scope.launch {
        gpuNames = withContext(Dispatchers.IO) {
            runCatching { com.winhub.hernandez.contentdialog.WineD3DConfigDialog.loadGpuNames(ctx).toList() }.getOrDefault(emptyList())
        }
    }
    fun raw(): String = s.p2Ex("dxwrapperConfig", c.getDXWrapperConfig())
    fun sel(): P2D3dSel {
        val cfg = com.winhub.hernandez.contentdialog.WineD3DConfigDialog.parseConfig(raw())
        return P2D3dSel(
            csmt = if (cfg.get("csmt") == "3") "Enabled" else "Disabled",
            gpuName = cfg.get("gpuName") ?: "",
            ddra = ddraEntries.firstOrNull { StringUtils.parseIdentifier(it) == cfg.get("ddrawrapper") } ?: ddraEntries.firstOrNull() ?: "",
            videoMem = videoMemEntries.firstOrNull { StringUtils.parseNumber(it) == cfg.get("videoMemorySize") } ?: videoMemEntries.firstOrNull() ?: "",
            ssm = if (cfg.get("strict_shader_math") == "1") "Enabled" else "Disabled",
            orm = (cfg.get("OffscreenRenderingMode") ?: "").ifEmpty { "fbo" },
            renderer = (cfg.get("renderer") ?: "").ifEmpty { "gl" },
        )
    }
    fun write(n: P2D3dSel) {
        val cfg = com.winhub.hernandez.contentdialog.WineD3DConfigDialog.parseConfig(raw())
        cfg.put("csmt", if (n.csmt == "Enabled") "3" else "0")
        cfg.put("strict_shader_math", if (n.ssm == "Enabled") "1" else "0")
        cfg.put("OffscreenRenderingMode", n.orm)
        cfg.put("gpuName", n.gpuName)
        cfg.put("ddrawrapper", StringUtils.parseIdentifier(n.ddra))
        cfg.put("videoMemorySize", StringUtils.parseNumber(n.videoMem))
        cfg.put("renderer", n.renderer)
        p2Put(xmb, s, "dxwrapperConfig", cfg.toString())
    }
    fun withCur(opts: List<String>, cur: String) = if (cur.isNotEmpty() && cur !in opts) opts + cur else opts

    return XmbMenu(I18n.s(R.string.ui_wined3d_ctx_getstring_r_string_configuration), Icons.Filled.Tune) {
        val cur = sel()
        val rows = mutableListOf<XmbRow>()
        rows += XmbRow.Choice("csmt", "CSMT", Icons.Filled.Speed, onOff, cur.csmt) { v -> write(sel().copy(csmt = v)) }
        val names = withCur(gpuNames, cur.gpuName)
        if (names.isNotEmpty()) {
            rows += XmbRow.Choice("gpuName", ctx.getString(R.string.gpu_name), Icons.Filled.DeveloperBoard, p2Unique(names), cur.gpuName) { v ->
                write(sel().copy(gpuName = v))
            }
        } else rows += XmbRow.Info(I18n.s(R.string.ui_gpuloading), ctx.getString(R.string.gpu_name), Icons.Filled.DeveloperBoard, "Loading…")
        if (ddraEntries.isNotEmpty()) {
            rows += XmbRow.Choice("ddraw", I18n.s(R.string.ddraw_wrapper), Icons.Filled.Layers, ddraEntries, cur.ddra) { v -> write(sel().copy(ddra = v)) }
        }
        if (videoMemEntries.isNotEmpty()) {
            rows += XmbRow.Choice("videoMem", ctx.getString(R.string.graphics_driver_max_device_memory), Icons.Filled.Memory,
                videoMemEntries, cur.videoMem) { v -> write(sel().copy(videoMem = v)) }
        }
        rows += XmbRow.Choice("ssm", I18n.s(R.string.strict_shader_math), Icons.Filled.Tune, onOff, cur.ssm) { v -> write(sel().copy(ssm = v)) }
        rows += XmbRow.Choice("orm", I18n.s(R.string.offscreen_rendering_mode), Icons.Filled.Image, withCur(ormOptions, cur.orm), cur.orm) { v -> write(sel().copy(orm = v)) }
        rows += XmbRow.Choice("renderer", "Renderer", Icons.Filled.Layers, withCur(rendOptions, cur.renderer), cur.renderer) { v -> write(sel().copy(renderer = v)) }
        rows
    }
}

// ── DXVK / VEGAS ────────────────────────────────────────────────────────────────────────────────────

private data class P2DxSel(
    val dxvk: String, val vkd3d: String, val framerate: String, val async: Boolean, val asyncCache: Boolean,
    val aniso: Int, val lod: Int, val level: String, val ddra: String, val d7vk: String, val configFile: String,
)

// Index-aligned with DXVKConfigDialog.ANISOTROPY_VALUES / LOD_BIAS_VALUES (the dialog's labels).
private val P2_ANISO_LABELS = listOf(I18n.s(R.string.ui_game_default), "2x", "4x", "8x", "16x")
private val P2_LOD_LABELS = listOf(
    "Game default", I18n.s(R.string.ui_auto_match_scaling_mode), "Sharper (-0.25)", "Sharper (-0.5)", "Sharper (-0.75)", "Sharpest (-1.0)",
)

private fun p2DxvkMenu(xmb: XmbScope, s: Shortcut, isVegas: Boolean): XmbMenu {
    val ctx = xmb.context
    val res = ctx.resources
    val c = s.container
    val framerates = res.getStringArray(R.array.dxvk_framerate_entries).toList()
    val ddraEntries = res.getStringArray(R.array.ddrawrapper_entries).toList()
    val levels = DXVKConfigDialog.VKD3D_FEATURE_LEVEL.toList()
    // Mali "Wrapper + compat + bcn" relaxes the DXVK 2.x-with-VKD3D filter (#137), as in the editor.
    val relaxDxvkFilter = s.p2Ex("graphicsDriver", c.graphicsDriver) == "wrapper-compat-bcn"

    var loaded by mutableStateOf(false)
    var allDxvk by mutableStateOf<List<String>>(emptyList())
    var vkd3dList by mutableStateOf<List<String>>(emptyList())
    var d7vkList by mutableStateOf(listOf(DXVKConfigDialog.D7VK_BUNDLED))
    var busy by mutableStateOf(false)

    fun raw(): String = s.p2Ex("dxwrapperConfig", c.getDXWrapperConfig())

    /** What the dialog shows for the stored config (its init coercions). */
    fun selOf(cfg: KeyValueSet): P2DxSel {
        val storedVer = cfg.get("version") ?: ""
        val fr = cfg.get("framerate") ?: ""
        val ddraId = cfg.get("ddrawrapper") ?: ""
        val d7 = cfg.get("d7vkVersion") ?: ""
        val lvl = cfg.get("vkd3dLevel") ?: ""
        return P2DxSel(
            dxvk = allDxvk.firstOrNull { it == storedVer } ?: allDxvk.firstOrNull() ?: storedVer,
            vkd3d = (cfg.get("vkd3dVersion") ?: "").ifEmpty { "None" },
            framerate = framerates.firstOrNull { StringUtils.parseNumber(it) == fr } ?: framerates.firstOrNull() ?: "",
            async = cfg.get("async") == "1",
            asyncCache = cfg.get("asyncCache") == "1",
            aniso = DXVKConfigDialog.ANISOTROPY_VALUES.indexOf(cfg.get("anisotropy")).coerceAtLeast(0),
            lod = DXVKConfigDialog.LOD_BIAS_VALUES.indexOf(cfg.get("lodBias")).coerceAtLeast(0),
            level = levels.firstOrNull { it == lvl } ?: levels.firstOrNull() ?: "",
            ddra = ddraEntries.firstOrNull { StringUtils.parseIdentifier(it) == ddraId } ?: ddraEntries.firstOrNull() ?: "",
            d7vk = d7vkList.firstOrNull { it == d7 } ?: DXVKConfigDialog.D7VK_BUNDLED,
            configFile = cfg.get("dxvkConfigFile") ?: "",
        )
    }
    fun sel(): P2DxSel = selOf(DXVKConfigDialog.parseConfig(raw()))

    /** The dialog's OK: every key from the (coerced) selection, onto the stored KeyValueSet. */
    fun write(n: P2DxSel) {
        val type = DXVKConfigDialog.getDXVKType(n.dxvk)
        val cfg = DXVKConfigDialog.parseConfig(raw())
        cfg.put("version", n.dxvk)
        cfg.put("framerate", StringUtils.parseNumber(n.framerate))
        cfg.put("async", if (n.async && type != DXVKConfigDialog.DXVK_TYPE_NONE) "1" else "0")
        cfg.put("asyncCache", if (n.asyncCache && type == DXVKConfigDialog.DXVK_TYPE_GPLASYNC) "1" else "0")
        cfg.put("anisotropy", DXVKConfigDialog.ANISOTROPY_VALUES[n.aniso.coerceIn(0, DXVKConfigDialog.ANISOTROPY_VALUES.size - 1)])
        cfg.put("lodBias", DXVKConfigDialog.LOD_BIAS_VALUES[n.lod.coerceIn(0, DXVKConfigDialog.LOD_BIAS_VALUES.size - 1)])
        cfg.put("vkd3dVersion", n.vkd3d)
        cfg.put("vkd3dLevel", n.level)
        cfg.put("ddrawrapper", StringUtils.parseIdentifier(n.ddra))
        cfg.put("d7vkVersion", n.d7vk)
        cfg.put("dxvkConfigFile", n.configFile)
        p2Put(xmb, s, "dxwrapperConfig", cfg.toString())
    }

    val vegas = P2Vegas(xmb, pointer = { sel().configFile }, setPointer = { p -> write(sel().copy(configFile = p)) }, version = { sel().dxvk })

    fun load() {
        xmb.scope.launch {
            val lists: Triple<List<String>, List<String>, List<String>> = withContext(Dispatchers.IO) {
                val cm = ContentsManager(ctx)
                cm.syncContents()
                val arm = runCatching { WineInfo.fromIdentifier(ctx, cm, c.wineVersion).isArm64EC() }.getOrDefault(false)
                val versions: List<String> = runCatching {
                    (if (isVegas) DXVKConfigDialog.loadVegasVersionList(ctx, cm) else DXVKConfigDialog.loadDxvkVersionList(ctx, cm, arm)).toList()
                }.getOrDefault(emptyList())
                val vk: List<String> = runCatching { DXVKConfigDialog.loadVkd3dVersionList(ctx, cm).toList() }.getOrDefault(listOf("None"))
                val d7: List<String> = runCatching { DXVKConfigDialog.loadD7vkVersionList(ctx, cm).toList() }.getOrDefault(listOf(DXVKConfigDialog.D7VK_BUNDLED))
                if (isVegas) vegas.loadIo(cm)
                Triple(versions, vk, d7)
            }
            allDxvk = lists.first.distinct()
            vkd3dList = lists.second.distinct()
            d7vkList = lists.third.distinct()
            loaded = true
            if (isVegas) vegas.reloadText()
        }
    }
    load()

    fun deleteVegasBuild(ver: String) {
        busy = true
        xmb.scope.launch {
            val msg = withContext(Dispatchers.IO) {
                runCatching {
                    val cm = ContentsManager(ctx)
                    cm.syncContents()
                    // Exact "vegas-<ver>" or a hash-suffixed variant (asset renames), as the dialog matches.
                    val profile = (cm.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VEGAS) ?: emptyList<ContentProfile>())
                        .firstOrNull { p -> val n = p.verName ?: ""; n == "vegas-$ver" || n.removePrefix("vegas-") == ver || n == ver }
                    if (profile != null) { cm.removeContent(profile); cm.syncContents(); I18n.s(R.string.ui_vegas_version_deleted) }
                    else I18n.s(R.string.ui_no_installed_vegas_version_to_delete)
                }.getOrElse { I18n.s(R.string.ui_error_failed_to_delete_it_message) }
            }
            busy = false
            xmb.toast(msg)
            val versions: List<String> = withContext(Dispatchers.IO) {
                runCatching { val cm = ContentsManager(ctx); cm.syncContents(); DXVKConfigDialog.loadVegasVersionList(ctx, cm).toList() }.getOrDefault(allDxvk)
            }
            allDxvk = versions.distinct()
            // The editor then selects the first remaining build; persist that so the game can launch it.
            if (sel().dxvk != (DXVKConfigDialog.parseConfig(raw()).get("version") ?: "")) write(sel())
        }
    }

    val title = if (isVegas) I18n.s(R.string.ui_vegas_ctx_getstring_r_string_configuration) else I18n.s(R.string.ui_dxvk_ctx_getstring_r_string_configuration)
    return XmbMenu(title, Icons.Filled.Tune) body@{
        val rows = mutableListOf<XmbRow>()
        if (!loaded) {
            rows += XmbRow.Info("loading", I18n.s(R.string.ui_loading_versions), Icons.Filled.Info)
            return@body rows
        }
        val cur = sel()
        val filtered = if (cur.vkd3d != "None" && !relaxDxvkFilter) {
            // VKD3D-Proton needs DXVK 2.x's DXGI (#113): 1.x is hidden while VKD3D is on.
            allDxvk.filter { v -> val major = DXVKConfigDialog.tryGetMajor(v); major == null || major >= 2 }
        } else allDxvk
        val dxvkOpts = if (cur.dxvk.isNotEmpty() && cur.dxvk !in filtered) filtered + cur.dxvk else filtered
        val dxvkDisabled = if (cur.dxvk.isNotEmpty() && cur.dxvk !in filtered) setOf(cur.dxvk) else emptySet()
        val type = DXVKConfigDialog.getDXVKType(cur.dxvk)

        rows += XmbRow.Info(I18n.s(R.string.ui_lognote), ctx.getString(R.string.vegas_config_loglevel_envvar_note), Icons.Filled.Warning)
        if (isVegas) {
            rows += XmbRow.Header("hVegas", I18n.s(R.string.ui_vegas_version_3))
            if (filtered.isEmpty()) {
                rows += XmbRow.Info(I18n.s(R.string.ui_novegas), I18n.s(R.string.ui_no_vegas_build_installed_download_one_from_the_container), Icons.Filled.Warning)
            } else {
                rows += XmbRow.Choice("vegasVersion", I18n.s(R.string.ui_vegas_version_3), Icons.Filled.Layers, dxvkOpts, cur.dxvk, disabledOptions = dxvkDisabled) { v ->
                    write(sel().copy(dxvk = v))
                }
                rows += XmbRow.Action("vegasDelete", I18n.s(R.string.ui_delete_this_vegas_build), Icons.Filled.Delete, danger = true,
                    subtitle = cur.dxvk, disabledReason = if (busy) I18n.s(R.string.ui_working) else null) {
                    val ver = sel().dxvk
                    xmb.confirm(XmbConfirm(I18n.s(R.string.ui_delete_vegas_build), I18n.s(R.string.ui_remove_the_installed_vegas_ver_from_this_device), "Delete", danger = true)) {
                        deleteVegasBuild(ver)
                    }
                }
            }
        }
        rows += XmbRow.Choice("vkd3d", ctx.getString(R.string.vkd3d_version), Icons.Filled.Layers,
            if (cur.vkd3d in vkd3dList) vkd3dList else vkd3dList + cur.vkd3d, cur.vkd3d) { v -> write(sel().copy(vkd3d = v)) }
        rows += XmbRow.Link("vkd3dDownload", I18n.s(R.string.ui_download_vkd3d_versions), Icons.Filled.CloudDownload) {
            p2ContentDownloadMenu(xmb, I18n.s(R.string.ui_vkd3d_downloads), ContentProfile.ContentType.CONTENT_TYPE_VKD3D) { load() }
        }
        if (!isVegas) {
            if (dxvkOpts.isNotEmpty()) {
                rows += XmbRow.Choice("dxvk", ctx.getString(R.string.dxvk_version), Icons.Filled.Layers, dxvkOpts, cur.dxvk,
                    subtitle = if (dxvkDisabled.isNotEmpty()) I18n.s(R.string.ui_dxvk_1_x_can_t_back_vkd3d_pick_a_2_x_build) else null,
                    disabledOptions = dxvkDisabled) { v -> write(sel().copy(dxvk = v)) }
            }
            rows += XmbRow.Link("dxvkDownload", I18n.s(R.string.ui_download_dxvk_versions), Icons.Filled.CloudDownload) {
                p2ContentDownloadMenu(xmb, I18n.s(R.string.ui_dxvk_downloads), ContentProfile.ContentType.CONTENT_TYPE_DXVK) { load() }
            }
        }
        if (type != DXVKConfigDialog.DXVK_TYPE_NONE) {
            rows += XmbRow.Toggle("async", "Async", Icons.Filled.Speed, cur.async) { on -> write(sel().copy(async = on)) }
        }
        if (type == DXVKConfigDialog.DXVK_TYPE_GPLASYNC) {
            rows += XmbRow.Toggle("asyncCache", I18n.s(R.string.ui_async_cache), Icons.Filled.Speed, cur.asyncCache) { on -> write(sel().copy(asyncCache = on)) }
        }
        if (framerates.isNotEmpty()) {
            rows += XmbRow.Choice("framerate", ctx.getString(R.string.frame_rate), Icons.Filled.Speed, framerates, cur.framerate) { v ->
                write(sel().copy(framerate = v))
            }
        }

        rows += XmbRow.Header("hTexture", I18n.s(R.string.ui_texture_filtering_2))
        val texNote = I18n.s(R.string.ui_directx_9_11_games_only_applies_the_next_time_t_2)
        rows += XmbRow.Choice("aniso", I18n.s(R.string.ui_anisotropic_filtering_2), Icons.Filled.Image, P2_ANISO_LABELS,
            P2_ANISO_LABELS.getOrElse(cur.aniso) { P2_ANISO_LABELS[0] }, subtitle = texNote) { v ->
            write(sel().copy(aniso = P2_ANISO_LABELS.indexOf(v).coerceAtLeast(0)))
        }
        rows += XmbRow.Choice("lodBias", I18n.s(R.string.ui_texture_sharpness_2), Icons.Filled.Image, P2_LOD_LABELS,
            P2_LOD_LABELS.getOrElse(cur.lod) { P2_LOD_LABELS[0] }, subtitle = texNote) { v ->
            write(sel().copy(lod = P2_LOD_LABELS.indexOf(v).coerceAtLeast(0)))
        }

        rows += XmbRow.Header("hApi", I18n.s(R.string.ui_api_feature_level_2))
        if (levels.isNotEmpty()) {
            rows += XmbRow.Choice("level", I18n.s(R.string.ui_vkd3d_feature_level), Icons.Filled.Layers, levels, cur.level) { v -> write(sel().copy(level = v)) }
        }
        if (ddraEntries.isNotEmpty()) {
            rows += XmbRow.Choice("ddraw", I18n.s(R.string.ddraw_wrapper), Icons.Filled.Layers, ddraEntries, cur.ddra) { v -> write(sel().copy(ddra = v)) }
        }
        if (StringUtils.parseIdentifier(cur.ddra) == "d7vk") {
            rows += XmbRow.Choice("d7vk", I18n.s(R.string.ui_d7vk_version), Icons.Filled.Layers,
                if (cur.d7vk in d7vkList) d7vkList else d7vkList + cur.d7vk, cur.d7vk) { v -> write(sel().copy(d7vk = v)) }
            rows += XmbRow.Link("d7vkDownload", I18n.s(R.string.ui_download_d7vk_versions), Icons.Filled.CloudDownload) {
                p2ContentDownloadMenu(xmb, I18n.s(R.string.ui_d7vk_downloads), ContentProfile.ContentType.CONTENT_TYPE_D7VK) { load() }
            }
        }
        if (isVegas) vegas.rows(rows)
        rows
    }
}

// ── VEGAS config file (tier + config source + per-key editor) ───────────────────────────────────────
// Option B, as in the shortcut editor's dialog (no container root): the stored dxvkConfigFile IS the
// live file. A stock baseline stays read-only until the first edit, which writes the user's own copy to
// filesDir/vegas-defaults/configs/<tag>.user.conf and moves the pointer to it (persisted at once, like
// the container editor's onLivePointerChanged). The first edit after a selection keeps a ".bak" copy.

private class P2Vegas(
    val xmb: XmbScope,
    val pointer: () -> String,
    val setPointer: (String) -> Unit,
    val version: () -> String,
) {
    val ctx: Context = xmb.context
    var ready by mutableStateOf(false)
    var stock by mutableStateOf<List<DXVKConfigDialog.StockSource>>(emptyList())
    var customBase by mutableStateOf<List<String>>(emptyList())
    var vocab by mutableStateOf<Map<String, List<String>>>(emptyMap())
    var baseline by mutableStateOf<Map<String, List<VegasKeyKnowledge.EditRow>>>(emptyMap())
    var text by mutableStateOf("")
    var missing by mutableStateOf(false)
    var pendingKey by mutableStateOf<String?>(null)
    var knowledge: VegasKeyKnowledge? = null
    var catalog: VegasKeyCatalog? = null
    var gpuModel: String? = null
    var detectedTier: Int? = null

    private val sidecarDir: File get() = File(ctx.filesDir, "vegas-defaults/configs")

    fun sidecarFor(src: DXVKConfigDialog.StockSource): File {
        val base = (src.tag?.takeIf { it.isNotBlank() } ?: (src.verName ?: "").removePrefix("vegas-").substringBefore(" ·"))
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(sidecarDir, "$base.user.conf")
    }

    /** The stock baseline a stored pointer belongs to — its parked file or the user's copy of it. */
    fun stockOf(path: String): DXVKConfigDialog.StockSource? =
        if (path.isEmpty()) null else stock.firstOrNull { it.file.absolutePath == path || sidecarFor(it).absolutePath == path }

    fun liveFile(): File? {
        val p = pointer()
        if (p.isEmpty()) return null
        val st = stockOf(p) ?: return File(p)
        val sc = sidecarFor(st)
        return if (sc.isFile) sc else st.file
    }

    /** Called on the IO thread by the menu's loader. */
    suspend fun loadIo(cm: ContentsManager) {
        val src: List<DXVKConfigDialog.StockSource> = runCatching { DXVKConfigDialog.loadVegasStockSources(ctx, cm).toList() }.getOrDefault(emptyList())
        val cfgsrc: List<String> = runCatching { DXVKConfigDialog.loadVegasConfigSourceList(ctx).toList() }.getOrDefault(emptyList())
        val prefs = ctx.getSharedPreferences("vegas_config_ui", Context.MODE_PRIVATE)
        // Autonomy tails persisted by the dialog's live checks (same prefs), so classification matches it.
        val k: VegasKeyKnowledge? = runCatching { DXVKConfigDialog.loadVegasKeyKnowledge(ctx) }.getOrNull()
        val saved: List<String> = prefs.getString("released_tail", null)?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        if (k != null && saved.isNotEmpty()) runCatching { k.mergeReleasedTail(saved) }
        val cat: VegasKeyCatalog? = runCatching { DXVKConfigDialog.loadVegasKeyCatalog(ctx) }.getOrNull()
        val tail: List<String> = prefs.getString("catalog_tail", null)?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        if (cat != null && tail.isNotEmpty()) runCatching { cat.mergeTailTags(tail) }
        val base = HashMap<String, List<VegasKeyKnowledge.EditRow>>()
        val values = HashMap<String, LinkedHashSet<String>>()
        for (st in src) {
            val t = runCatching { st.file.readText() }.getOrNull() ?: continue
            val rows: List<VegasKeyKnowledge.EditRow> = (k?.editRows(t, st.verName) ?: VegasKeyKnowledge.editRowsUnclassified(t)).toList()
            base[st.file.absolutePath] = rows
            for (r in rows) if (r.enabled && r.value.isNotEmpty()) values.getOrPut(r.key) { LinkedHashSet() }.add(r.value)
        }
        val model = graphicsProbeMutex.withLock { runCatching { VegasTierPresets.readGpuModel(ctx) }.getOrNull() }
        withContext(Dispatchers.Main) {
            knowledge = k
            catalog = cat
            gpuModel = model
            detectedTier = VegasTierPresets.classifyModel(model)
            stock = src
            customBase = cfgsrc.filter { it != "None" }
            baseline = base
            vocab = values.mapValues { it.value.toList() }
            ready = true
        }
    }

    fun reloadText() {
        val f = liveFile()
        xmb.scope.launch {
            val r: Pair<String, Boolean> = withContext(Dispatchers.IO) {
                if (f == null) "" to false
                else if (!f.isFile) "" to true
                else (runCatching { f.readText() }.getOrDefault("") to false)
            }
            text = r.first
            missing = r.second
        }
    }

    fun currentRows(): List<VegasKeyKnowledge.EditRow> =
        (knowledge?.editRows(text, version()) ?: VegasKeyKnowledge.editRowsUnclassified(text)).toList()

    private fun baselineRows(): List<VegasKeyKnowledge.EditRow> =
        stockOf(pointer())?.let { baseline[it.file.absolutePath] } ?: emptyList()

    fun isBooleanKey(key: String): Boolean {
        val v = vocab[key] ?: return false
        return v.isNotEmpty() && v.all { it == "0" || it == "1" }
    }

    /** §6a.6: a key from the other VEGAS line's schema can never apply to this stock build. */
    private fun blocked(key: String): Boolean {
        val tag = stockOf(pointer())?.tag ?: return false
        val cat = catalog ?: return false
        if (!cat.isWrongFamily(key, tag)) return false
        xmb.toast(I18n.s(R.string.ui_key_belongs_to_another_vegas_schema_build_tag_would_i))
        return true
    }

    /** The dialog's commitConfigWrite: back up once, write (stock → the user's own copy), move the pointer. */
    private fun commit(transform: (String) -> String?) {
        val target = liveFile() ?: return
        val st = stockOf(pointer())
        val sidecar = st?.let { sidecarFor(it) }
        val sidecarExists = sidecar?.isFile == true
        val toSidecar = st != null && !sidecarExists && sidecar != null
        xmb.scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (!target.isFile) return@runCatching false
                    val next = transform(target.readText()) ?: return@runCatching false
                    val autoBak = File(target.absolutePath + ".bak")
                    if (!autoBak.isFile) runCatching { target.copyTo(autoBak) }
                    val out = if (toSidecar && sidecar != null) sidecar else target
                    out.parentFile?.mkdirs()
                    out.writeText(next)
                    true
                }.getOrDefault(false)
            }
            if (ok) {
                val finalPath = if (toSidecar && sidecar != null) sidecar.absolutePath else target.absolutePath
                if (finalPath != pointer()) setPointer(finalPath) else { xmb.saved(); xmb.refresh() }
                reloadText()
            } else xmb.toast(I18n.s(R.string.ui_failed_to_update_config_file))
        }
    }

    fun applyToggle(key: String, value: String, enable: Boolean) {
        if (blocked(key)) return
        commit { t -> VegasKeyKnowledge.toggleLine(t, key, enable) ?: if (enable) VegasKeyKnowledge.setLine(t, key, value) else t }
    }

    fun applyValue(key: String, value: String) {
        if (blocked(key)) return
        commit { t -> VegasKeyKnowledge.setLine(t, key, value) }
    }

    fun applyDelete(key: String) {
        if (blocked(key)) return
        commit { t -> VegasKeyKnowledge.removeLine(t, key) }
    }

    fun rows(rows: MutableList<XmbRow>) {
        if (!ready) {
            rows += XmbRow.Info(I18n.s(R.string.ui_vegasloading), I18n.s(R.string.ui_reading_vegas_configs), Icons.Filled.Info)
            return
        }
        val live = liveFile()
        val cfgRows = if (live != null && !missing) currentRows() else emptyList()

        // Performance tier (FAQ #11) → vegas.forceTier in the live file.
        rows += XmbRow.Header("hTier", I18n.s(R.string.ui_performance_tier_2))
        val applied = cfgRows.firstOrNull { it.key == "vegas.forceTier" }?.value?.toIntOrNull()
        val autoLabel = "Auto" + (detectedTier?.let { " · T$it" } ?: "")
        val tierLabels = listOf(autoLabel) + VegasTierPresets.TIERS.map { it.label }
        val tierSel = if (applied == null || applied == 0) autoLabel else VegasTierPresets.TIERS.firstOrNull { it.number == applied }?.label ?: autoLabel
        val model = gpuModel
        rows += XmbRow.Choice("tier", I18n.s(R.string.ui_performance_tier_2), Icons.Filled.Speed, tierLabels, tierSel,
            subtitle = when {
                model == null -> I18n.s(R.string.ui_gpu_model_unreadable_tier_is_manual)
                detectedTier != null -> I18n.s(R.string.ui_model_auto_tier_detectedtier)
                else -> I18n.s(R.string.ui_gpu_model_no_tier_suggestion)
            },
            disabledReason = if (live == null || missing) I18n.s(R.string.ui_pick_a_config_file_first_defaults_has_no_file_to_writ) else null) { v ->
            if (v == autoLabel) { if (applied != null && applied != 0) applyValue("vegas.forceTier", "0") }
            else VegasTierPresets.TIERS.firstOrNull { it.label == v }?.let { applyValue("vegas.forceTier", it.number.toString()) }
        }
        applied?.takeIf { it != 0 }?.let { VegasTierPresets.PARAMS[it] }?.let { p ->
            rows.add(XmbRow.Info(I18n.s(R.string.ui_tierparams),
                I18n.s(R.string.ui_draw_threshold_p_drawthreshold_d3d9_p_drawthres_2),
                Icons.Filled.Info))
        }
        if (model != null && !model.contains("adreno", ignoreCase = true)) {
            rows += XmbRow.Info("mali", I18n.s(R.string.ui_mali_pair_the_wrapper_compat_bcn_driver_with_the_rela), Icons.Filled.Warning)
        }

        // Config source: defaults / a stock config / a custom file.
        rows += XmbRow.Header("hConfig", I18n.s(R.string.ui_config_file))
        val stored = pointer()
        val storedStock = stockOf(stored)
        val customs = customBase + (if (stored.isNotEmpty() && storedStock == null && stored !in customBase) listOf(stored) else emptyList())
        val kinds = ArrayList<Pair<String, String?>>() // label, pointer target (null = stock resolved at pick)
        kinds += "Use defaults (no config file)" to ""
        stock.forEach { kinds += I18n.s(R.string.ui_stock_it_displaylabel) to null }
        customs.forEach { kinds += it to it }
        val labels = p2Unique(kinds.map { it.first })
        val selIdx = when {
            stored.isEmpty() -> 0
            storedStock != null -> 1 + stock.indexOf(storedStock)
            else -> 1 + stock.size + customs.indexOf(stored).coerceAtLeast(0)
        }.coerceIn(0, labels.size - 1)
        rows += XmbRow.Choice("configSource", I18n.s(R.string.ui_config_source), Icons.Filled.Description, labels, labels[selIdx],
            subtitle = live?.absolutePath ?: I18n.s(R.string.ui_vegas_built_in_defaults)) { v ->
            val i = labels.indexOf(v)
            if (i >= 0) {
                val target = when {
                    i == 0 -> ""
                    i <= stock.size -> stock[i - 1].let { st -> sidecarFor(st).takeIf { it.isFile }?.absolutePath ?: st.file.absolutePath }
                    else -> kinds[i].second ?: ""
                }
                setPointer(target)
                reloadText()
            }
        }
        rows += XmbRow.Link("configBrowse", I18n.s(R.string.ui_browse_for_a_config_file), Icons.Filled.FolderOpen, subtitle = I18n.s(R.string.ui_any_conf_the_built_in_dxvk_config_file)) {
            val ext = Environment.getExternalStorageDirectory()
            val roots = listOf("Download" to File(ext, "Download"), I18n.s(R.string.ui_internal_storage_2) to ext).filter { it.second.isDirectory }
            xmbFileBrowserMenu(xmb, I18n.s(R.string.ui_select_config_file), ext, roots, { true }) { f ->
                // The browser has already closed its columns: we're back on the DX config column.
                if (f.absolutePath !in customBase) customBase = customBase + f.absolutePath
                setPointer(f.absolutePath)
                reloadText()
            }
        }
        if (live != null) {
            if (missing) {
                rows += XmbRow.Info("missing", I18n.s(R.string.ui_not_found_live_absolutepath), Icons.Filled.Warning)
                rows += XmbRow.Action("useDefaults", I18n.s(R.string.ui_use_defaults_instead), Icons.Filled.RestartAlt) { setPointer(""); reloadText() }
            } else {
                rows += XmbRow.Link("configKeys", I18n.s(R.string.ui_config_keys), Icons.Filled.Tune, value = I18n.s(R.string.ui_cfgrows_size_keys),
                    subtitle = if (storedStock != null && !sidecarFor(storedStock).isFile) I18n.s(R.string.ui_read_only_until_the_first_edit_changes_create_y_2)
                               else I18n.s(R.string.ui_toggle_change_add_or_remove_keys)) { keysMenu() }
            }
        }
    }

    fun keysMenu(): XmbMenu = XmbMenu(I18n.s(R.string.ui_config_keys), Icons.Filled.Tune) {
        val rows = mutableListOf<XmbRow>()
        val live = liveFile()
        if (live == null || missing) {
            rows += XmbRow.Info("none", I18n.s(R.string.ui_no_config_file_selected), Icons.Filled.Info)
        } else {
            val st = stockOf(pointer())
            val cfgRows = currentRows()
            if (st != null) {
                rows += XmbRow.Info(I18n.s(R.string.ui_stocknote),
                    if (sidecarFor(st).isFile) I18n.s(R.string.ui_your_edits_live_in_your_own_copy_the_stock_version_st)
                    else I18n.s(R.string.ui_read_only_until_the_first_edit_changes_create_your_ow), Icons.Filled.Info)
            }
            val pk = pendingKey
            rows += XmbRow.Text("addKey", I18n.s(R.string.ui_add_key), Icons.Filled.Add, pk ?: "",
                subtitle = if (pk != null) I18n.s(R.string.ui_now_set_its_value_below) else I18n.s(R.string.ui_key_name_first_then_its_value),
                placeholder = I18n.s(R.string.ui_e_g_dxvk_maxframelatency)) { k ->
                val key = k.trim()
                if (!VegasKeyKnowledge.isValidConfigKey(key)) xmb.toast(I18n.s(R.string.ui_not_a_valid_config_key_use_a_dotted_name_or_env_style))
                else pendingKey = key
            }
            if (pk != null) {
                rows += XmbRow.Text("addValue", I18n.s(R.string.ui_value_for_pk), Icons.Filled.Add, "", placeholder = I18n.s(R.string.ui_value)) { v ->
                    if (v.isNotBlank()) { applyValue(pk, v.trim()); pendingKey = null }
                }
            }
            rows += XmbRow.Header("hKeys", I18n.s(R.string.ui_keys_cfgrows_size))
            val k = knowledge
            cfgRows.forEach { r ->
                val badge = k?.badgeFor(r.key, version()) ?: "unclassified"
                rows += XmbRow.Link("k:${r.key}", r.key, Icons.Filled.Tune,
                    value = if (r.enabled) r.value else I18n.s(R.string.ui_off_r_value), subtitle = badge) { keyMenu(r.key) }
            }
            if (st != null) {
                val active = cfgRows.map { it.key }.toSet()
                val pending = baselineRows().filter { it.key !in active }
                if (pending.isNotEmpty()) rows += XmbRow.Header("hPending", I18n.s(R.string.ui_new_in_this_baseline))
                pending.forEach { r ->
                    val gated = k != null && k.isGated(r.key, version())
                    rows += XmbRow.Action("pend:${r.key}", r.key, Icons.Filled.Add, value = r.value,
                        subtitle = I18n.s(R.string.ui_missing_adds_r_key_r_value),
                        disabledReason = if (gated) I18n.s(R.string.ui_needs_a_newer_build) else null) { applyToggle(r.key, r.value, true) }
                }
            }
        }
        rows
    }

    fun keyMenu(key: String): XmbMenu = XmbMenu(key, Icons.Filled.Tune) {
        val rows = mutableListOf<XmbRow>()
        val r = currentRows().firstOrNull { it.key == key }
        if (r == null) {
            rows += XmbRow.Info("gone", I18n.s(R.string.ui_this_key_is_no_longer_in_the_file), Icons.Filled.Info)
        } else {
            val gated = knowledge?.isGated(key, version()) == true
            val gatedReason = if (gated) I18n.s(R.string.ui_ineffective_on_this_build) else null
            rows += XmbRow.Toggle("enabled", "Enabled", Icons.Filled.Tune, r.enabled, subtitle = I18n.s(R.string.ui_off_comments_the_line_out)) { on ->
                applyToggle(key, r.value, on)
            }
            if (isBooleanKey(key)) {
                rows += XmbRow.Info("value", "Value", Icons.Filled.Tune, r.value)
            } else {
                val opts = LinkedHashSet<String>()
                if (r.value.isNotEmpty()) opts.add(r.value)
                vocab[key].orEmpty().forEach { opts.add(it) }
                if (opts.isNotEmpty()) {
                    rows += XmbRow.Choice("value", "Value", Icons.Filled.Tune, opts.toList(), r.value,
                        subtitle = I18n.s(R.string.ui_values_used_in_stock_configs), disabledReason = gatedReason) { v -> applyValue(key, v) }
                }
                val base = baselineRows().firstOrNull { it.key == key }
                if (base != null && (base.value != r.value || base.enabled != r.enabled)) {
                    rows += XmbRow.Action("resetStock", I18n.s(R.string.ui_reset_to_stock_base_value), Icons.Filled.RestartAlt,
                        disabledReason = gatedReason) { applyValue(key, base.value) }
                }
                rows += XmbRow.Text("custom", I18n.s(R.string.ui_custom_value), Icons.Filled.Edit, r.value, disabledReason = gatedReason) { v ->
                    if (v.isNotBlank()) applyValue(key, v.trim())
                }
            }
            rows += XmbRow.Action("delete", I18n.s(R.string.ui_remove_key_2), Icons.Filled.Delete, danger = true, disabledReason = gatedReason) {
                xmb.confirm(XmbConfirm(I18n.s(R.string.ui_remove_key_2), I18n.s(R.string.ui_remove_key_add_it_back_with_add_key), "Remove", danger = true)) {
                    applyDelete(key)
                    xmb.pop()
                }
            }
        }
        rows
    }
}
