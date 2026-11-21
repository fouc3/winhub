package com.winhub.hernandez.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.winhub.hernandez.R
import com.winhub.hernandez.container.Shortcut
import com.winhub.hernandez.contents.AdrenotoolsManager
import com.winhub.hernandez.contents.WrapperManager
import com.winhub.hernandez.contents.WrapperSettingsDictionary
import com.winhub.hernandez.core.MaliPanvk
import com.winhub.hernandez.core.DefaultVersion
import com.winhub.hernandez.core.FileUtils
import com.winhub.hernandez.core.GPUInformation
import com.winhub.hernandez.core.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.winhub.hernandez.util.I18n

// ── Graphics driver configuration (GraphicsDriverConfigDialog, ContainerDetailScreen.kt) ───────────
// The dialog keeps the whole "graphicsDriverConfig" string in its own state and rebuilds it on OK. Here
// every change re-parses the stored string the dialog's way, changes one field and serializes it back in
// the dialog's exact key order, so what lands in the shortcut extra is what the dialog's OK would write.

private val P2_GFX_CURATED = setOf(
    "vulkanVersion", "version", "blacklistedExtensions", "maxDeviceMemory", "presentMode", "syncFrame",
    "disablePresentWait", "resourceType", "bcnEmulation", "bcnEmulationType", "bcnEmulationCache",
    "bcnEmulationAstc", "bcnLayerAuto", "bcnTranscodeEtc2", "bcnTranscodeAstc", "bcnImageView", "bcnDebugLog",
    "bcnCompatSparse", "compatUseGamenative", "gpuName", "fdDevFeatures", "turnipGmem", "turnipTokens",
)

/** The dialog's state, seeded with its defaults from the stored string. */
private class P2GfxCfg(raw: String) {
    private val cfg: Map<String, String> = raw.split(";").associate { elem ->
        val parts = elem.split("=")
        parts[0] to if (parts.size > 1) parts[1] else ""
    }
    var version = cfg["version"] ?: ""
    var vulkanVersion = cfg["vulkanVersion"] ?: "1.4"
    var gpuName = cfg["gpuName"] ?: "Device"
    var presentMode = cfg["presentMode"] ?: "mailbox"
    var resourceType = cfg["resourceType"] ?: "auto"
    var bcnEmulation = cfg["bcnEmulation"] ?: "auto"
    var bcnEmulationType = cfg["bcnEmulationType"] ?: "software"
    var bcnEmulationCache = cfg["bcnEmulationCache"] ?: "0"
    var bcnEmulationAstc = cfg["bcnEmulationAstc"] == "1"
    var syncFrame = cfg["syncFrame"] == "1"
    var disablePresentWait = cfg["disablePresentWait"] == "1"
    var fdDevFeatures = cfg["fdDevFeatures"] == "1"
    var turnipGmem = cfg["turnipGmem"] ?: "auto"
    private val tokens = (cfg["turnipTokens"] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    var forceCb = "forcecb" in tokens
    var noCb = "nocb" in tokens
    var sysmem = "sysmem" in tokens
    var deckEmu = "deck_emu" in tokens
    var bcnLayerAuto = cfg["bcnLayerAuto"]?.let { it == "1" } ?: true
    var bcnTranscodeEtc2 = cfg["bcnTranscodeEtc2"] == "1"
    var bcnTranscodeAstc = cfg["bcnTranscodeAstc"] == "1"
    var bcnImageView = cfg["bcnImageView"]?.let { it == "1" } ?: true
    var bcnDebugLog = cfg["bcnDebugLog"] == "1"
    var bcnCompatSparse = cfg["bcnCompatSparse"] == "1"
    var compatUseGamenative = cfg["compatUseGamenative"] == "1"
    /** Stored number (the dialog writes StringUtils.parseNumber of the picked entry). */
    var maxDeviceMemory = cfg["maxDeviceMemory"] ?: "0"
    var blacklisted: Set<String> = (cfg["blacklistedExtensions"] ?: "").split(",").filter { it.isNotEmpty() }.toSet()
    /** Keys no curated control owns — the auto-detected wrapper settings, in stored order. */
    val extras = LinkedHashMap<String, String>().apply {
        raw.split(";").forEach { elem ->
            val parts = elem.split("=")
            val k = parts[0]
            if (k.isNotEmpty() && k !in P2_GFX_CURATED) put(k, if (parts.size > 1) parts[1] else "")
        }
    }

    private fun b(v: Boolean) = if (v) "1" else "0"

    fun serialize(): String {
        val config = "vulkanVersion=$vulkanVersion;" +
            "version=$version;" +
            "blacklistedExtensions=${blacklisted.joinToString(",")};" +
            "maxDeviceMemory=$maxDeviceMemory;" +
            "presentMode=$presentMode;" +
            "syncFrame=${b(syncFrame)};" +
            "disablePresentWait=${b(disablePresentWait)};" +
            "resourceType=$resourceType;" +
            "bcnEmulation=$bcnEmulation;" +
            "bcnEmulationType=$bcnEmulationType;" +
            "bcnEmulationCache=$bcnEmulationCache;" +
            "bcnEmulationAstc=${b(bcnEmulationAstc)};" +
            "bcnLayerAuto=${b(bcnLayerAuto)};" +
            "bcnTranscodeEtc2=${b(bcnTranscodeEtc2)};" +
            "bcnTranscodeAstc=${b(bcnTranscodeAstc)};" +
            "bcnImageView=${b(bcnImageView)};" +
            "bcnDebugLog=${b(bcnDebugLog)};" +
            "bcnCompatSparse=${b(bcnCompatSparse)};" +
            "compatUseGamenative=${b(compatUseGamenative)};" +
            "gpuName=$gpuName" +
            ";fdDevFeatures=${b(fdDevFeatures)}" +
            ";turnipGmem=$turnipGmem" +
            ";turnipTokens=" + buildList {
                if (forceCb) add("forcecb")
                if (noCb) add("nocb")
                // sysmem is not persisted while GMEM = Force On overrides it (the greyed checkbox).
                if (sysmem && turnipGmem != "on") add("sysmem")
                if (deckEmu) add("deck_emu")
            }.joinToString(",")
        // Detected wrapper settings under their raw env key; sanitised so they can't break the format.
        val extraPart = extras.entries.joinToString("") { (k, v) -> ";$k=${v.replace(";", "").replace("=", "")}" }
        return config + extraPart
    }
}

internal fun xmbDriverConfigMenu(xmb: XmbScope, shortcut: Shortcut): XmbMenu {
    val ctx = xmb.context
    val res = ctx.resources
    val c = shortcut.container
    val jobs = P2Jobs(xmb)

    val vulkanVersions = res.getStringArray(R.array.vulkan_version_entries).toList()
    val presentModes = res.getStringArray(R.array.present_mode_entries).toList()
    val resourceTypes = res.getStringArray(R.array.resource_type_entries).toList()
    val bcnEmulations = res.getStringArray(R.array.bcn_emulation_entries).toList()
    val bcnTypes = res.getStringArray(R.array.bcn_emulation_type_entries).toList()
    val bcnCaches = res.getStringArray(R.array.bcn_emulation_cache_entries).toList()
    val memEntries = res.getStringArray(R.array.device_memory_entries).toList()
    fun memLabel(num: String): String = memEntries.firstOrNull { StringUtils.parseNumber(it) == num } ?: memEntries.firstOrNull() ?: num

    // Async facts (the dialog's LaunchedEffects).
    var ready by mutableStateOf(false)
    var driver by mutableStateOf("")
    var hasIcd by mutableStateOf(false)
    var hasBcnLayer by mutableStateOf(false)
    var isImported by mutableStateOf(false)
    var isQualcomm by mutableStateOf(false)
    var gpuModel by mutableStateOf("")
    var showAllDrivers by mutableStateOf(false)
    var wrapperVersions by mutableStateOf<List<String>>(emptyList())
    var driverVersions by mutableStateOf<List<String>>(emptyList())
    var gpuNames by mutableStateOf(listOf("Device"))
    var detectedKeys by mutableStateOf<List<String>>(emptyList())
    var allExtensions by mutableStateOf<List<String>>(emptyList())
    var isCustomDriver by mutableStateOf(false)
    var driverFellBack by mutableStateOf(false)
    var probeJob: Job? = null

    fun rawCfg(): String = shortcut.p2Ex("graphicsDriverConfig", c.getGraphicsDriverConfig())

    /** The dialog's coercion: an empty or unavailable version becomes the default wrapper driver. */
    fun effectiveVersion(v: String): String {
        if (driverVersions.isEmpty()) return v
        if (v.isNotEmpty() && driverVersions.any { it.equals(v, ignoreCase = true) }) return v
        return wrapperVersions.firstOrNull { it.equals(DefaultVersion.WRAPPER_ADRENO, ignoreCase = true) }
            ?: wrapperVersions.firstOrNull { it.equals(DefaultVersion.WRAPPER, ignoreCase = true) }
            ?: wrapperVersions.firstOrNull() ?: v
    }

    fun probe(version: String) {
        probeJob?.cancel()
        if (version.isEmpty()) { allExtensions = emptyList(); driverFellBack = false; isCustomDriver = false; return }
        probeJob = jobs.scope.launch {
            // Proprietary Qualcomm blobs are never probed in-process (see the dialog); Mesa wrappers are.
            val unsafe = withContext(Dispatchers.IO) {
                graphicsProbeMutex.withLock {
                    runCatching {
                        val mgr = AdrenotoolsManager(ctx)
                        val installedDrivers = mgr.enumarateInstalledDrivers()
                        if (installedDrivers.none { it.equals(version, ignoreCase = true) }) false
                        else !(mgr.getLibraryName(version) ?: "").startsWith("libvulkan", ignoreCase = true)
                    }.getOrDefault(true)
                }
            }
            if (unsafe) {
                allExtensions = emptyList(); driverFellBack = false; isCustomDriver = true
                return@launch
            }
            val exts = withContext(Dispatchers.IO) {
                graphicsProbeMutex.withLock {
                    runCatching { GPUInformation.enumerateExtensions(version, ctx)?.toList() ?: emptyList() }.getOrDefault(emptyList())
                }
            }
            allExtensions = exts.distinct()
            driverFellBack = runCatching { GPUInformation.driverLoadedFellBack() }.getOrDefault(false)
            isCustomDriver = exts.isEmpty()
        }
    }

    fun loadDrivers() {
        jobs.scope.launch {
            val installedDrivers = withContext(Dispatchers.IO) {
                runCatching { AdrenotoolsManager(ctx).enumarateInstalledDrivers().toList() }.getOrDefault(emptyList())
            }
            val arr = res.getStringArray(R.array.wrapper_graphics_driver_version_entries).toList()
            val wrappers = if (showAllDrivers) arr else withContext(Dispatchers.IO) {
                graphicsProbeMutex.withLock {
                    arr.filter { v -> runCatching { GPUInformation.isDriverSupported(v, ctx) }.getOrDefault(false) }
                }
            }
            wrapperVersions = wrappers
            driverVersions = (wrappers + installedDrivers).distinct()
            ready = true
            probe(effectiveVersion(P2GfxCfg(rawCfg()).version))
        }
    }

    // Driver identity + capabilities, GPU facts, GPU names, detected wrapper keys — then the driver list.
    jobs.scope.launch {
        withContext(Dispatchers.IO) {
            val entries = runCatching { WrapperManager.driverEntries(ctx, res.getStringArray(R.array.graphics_driver_entries)).toList() }
                .getOrDefault(emptyList())
            val id = shortcut.p2Ex("graphicsDriver", c.graphicsDriver)
            val drv = StringUtils.parseIdentifier(entries.firstOrNull { StringUtils.parseIdentifier(it) == id } ?: entries.firstOrNull() ?: id)
            val wm = WrapperManager(ctx)
            val caps = runCatching { wm.capsFor(drv) }.getOrNull()
            val imported = runCatching { wm.isImported(drv) }.getOrDefault(false)
            val gpu: Pair<Boolean, String> = graphicsProbeMutex.withLock {
                Pair(
                    runCatching { GPUInformation.getVendorID(null, null) == 0x5143 }.getOrDefault(false),
                    runCatching { GPUInformation.extractModelName(GPUInformation.getRenderer(null, ctx)) ?: "" }.getOrDefault(""),
                )
            }
            val names = mutableListOf("Device")
            runCatching {
                val arr = JSONArray(FileUtils.readString(ctx, "gpu_cards.json"))
                for (i in 0 until arr.length()) names.add(arr.getJSONObject(i).getString("name"))
            }
            val keys: List<String> = if (!imported) emptyList() else runCatching {
                val hidden = wm.hiddenKeys(drv)
                wm.detectedEnvKeys(drv).filter {
                    it !in WrapperManager.HANDLED_ENV_KEYS && !WrapperManager.isDebugEnvKey(it) &&
                        !WrapperManager.isDriverInternalEnvKey(it) && it !in hidden
                }
            }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) {
                driver = drv
                hasIcd = caps?.hasIcd ?: false
                hasBcnLayer = caps?.hasBcnLayer ?: false
                isImported = imported
                isQualcomm = gpu.first
                gpuModel = gpu.second
                gpuNames = names.distinct()
                detectedKeys = keys
            }
        }
        loadDrivers()
    }

    /** Re-read, normalise like the dialog's init (version coercion, memory entry, detected keys), change, save. */
    fun write(change: (P2GfxCfg) -> Unit) {
        val cfg = P2GfxCfg(rawCfg())
        val eff = effectiveVersion(cfg.version)
        if (eff != cfg.version) { cfg.version = eff; cfg.blacklisted = emptySet() }
        cfg.maxDeviceMemory = StringUtils.parseNumber(memLabel(cfg.maxDeviceMemory))
        for (k in detectedKeys) {
            val def = WrapperSettingsDictionary.defFor(k)
            cfg.extras[k] = when (def.type) {
                WrapperSettingsDictionary.Type.TOGGLE -> if (cfg.extras[k] == "1") "1" else "0"
                else -> cfg.extras[k] ?: ""
            }
        }
        change(cfg)
        p2Put(xmb, shortcut, "graphicsDriverConfig", cfg.serialize())
    }

    fun uniq(opts: List<String>, cur: String): List<String> = if (cur.isNotEmpty() && cur !in opts) opts + cur else opts

    return XmbMenu(ctx.getString(R.string.graphics_driver_configuration), Icons.Filled.Tune, onClose = { jobs.cancel() }) body@{
        val rows = mutableListOf<XmbRow>()
        if (!ready) {
            rows += XmbRow.Info("loading", I18n.s(R.string.ui_checking_which_drivers_this_gpu_supports), Icons.Filled.Info)
            return@body rows
        }
        val cfg = P2GfxCfg(rawCfg())
        val version = effectiveVersion(cfg.version)
        val isIntegratedBcn = (hasIcd && hasBcnLayer) || (isImported && hasIcd)
        val showBcnLayerSettings = hasBcnLayer && (isImported || !hasIcd)
        val isBcnLayer = driver == "wrapper-bcn_layer" || driver == "wrapper-compat-bcn"
        val isCompatDriver = driver == "wrapper-compat-bcn"

        rows += XmbRow.Choice("vulkan", ctx.getString(R.string.graphics_driver_vulkan_version), Icons.Filled.Layers,
            uniq(vulkanVersions, cfg.vulkanVersion), cfg.vulkanVersion) { v -> write { it.vulkanVersion = v } }
        if (driverVersions.isNotEmpty()) {
            rows += XmbRow.Choice("version", ctx.getString(R.string.graphics_driver_version), Icons.Filled.Memory,
                uniq(driverVersions, version), version,
                subtitle = if (driverFellBack) I18n.s(R.string.ui_this_driver_couldn_t_load_on_this_gpu_the_system_driv) else null) { v ->
                write { if (v != it.version) { it.version = v; it.blacklisted = emptySet() } }
                probe(v)
            }
        } else {
            rows += XmbRow.Info(I18n.s(R.string.ui_nodrivers), I18n.s(R.string.ui_no_compatible_drivers_found_turn_on_show_incompatible_dr), Icons.Filled.Warning)
        }
        rows += XmbRow.Toggle("showAll", ctx.getString(R.string.graphics_driver_show_incompatible), Icons.Filled.Visibility, showAllDrivers,
            subtitle = I18n.s(R.string.ui_lists_drivers_this_gpu_may_not_run)) { on -> showAllDrivers = on; ready = false; loadDrivers() }
        if (isCustomDriver) {
            rows += XmbRow.Info(I18n.s(R.string.ui_customdriver), I18n.s(R.string.ui_custom_qualcomm_adreno_driver_its_extensions_load_when_a), Icons.Filled.Info)
        } else if (allExtensions.isNotEmpty()) {
            val enabled = allExtensions.count { it !in cfg.blacklisted }
            rows += XmbRow.Link("extensions", ctx.getString(R.string.graphics_driver_available_extensions), Icons.Filled.Extension,
                value = "$enabled/${allExtensions.size}", subtitle = I18n.s(R.string.ui_hide_extensions_from_games)) { p2ExtensionsMenu(xmb, { allExtensions }, { rawCfg() }, { ch -> write(ch) }) }
        }
        rows += XmbRow.Choice("gpuName", ctx.getString(R.string.gpu_name), Icons.Filled.DeveloperBoard,
            uniq(gpuNames, cfg.gpuName), cfg.gpuName) { v -> write { it.gpuName = v } }
        if (memEntries.isNotEmpty()) {
            rows += XmbRow.Choice("memory", ctx.getString(R.string.graphics_driver_max_device_memory), Icons.Filled.Memory,
                memEntries, memLabel(cfg.maxDeviceMemory)) { v -> write { it.maxDeviceMemory = StringUtils.parseNumber(v) } }
        }
        rows += XmbRow.Choice("presentMode", ctx.getString(R.string.graphics_driver_present_modes), Icons.Filled.Speed,
            uniq(presentModes, cfg.presentMode), cfg.presentMode) { v -> write { it.presentMode = v } }
        rows += XmbRow.Choice("resourceType", ctx.getString(R.string.graphics_driver_resource_type), Icons.Filled.Memory,
            uniq(resourceTypes, cfg.resourceType), cfg.resourceType) { v -> write { it.resourceType = v } }

        rows += XmbRow.Header("hBcn", I18n.s(R.string.ui_bcn_textures))
        rows += XmbRow.Info("gpuNote", (if (gpuModel.isNotEmpty()) "GPU: $gpuModel — " else "") +
            I18n.s(R.string.ui_bcn_compat_layers_apply_to_mali_and_other_non_qualcom), Icons.Filled.Info)
        rows += XmbRow.Choice("bcnEmulation", ctx.getString(R.string.graphics_driver_bcn_emulation), Icons.Filled.Layers,
            uniq(bcnEmulations, cfg.bcnEmulation), cfg.bcnEmulation) { v -> write { it.bcnEmulation = v } }
        rows += XmbRow.Choice("bcnType", ctx.getString(R.string.graphics_driver_bcn_emulation_type), Icons.Filled.Layers,
            uniq(bcnTypes, cfg.bcnEmulationType), cfg.bcnEmulationType) { v -> write { it.bcnEmulationType = v } }
        rows += XmbRow.Choice("bcnCache", ctx.getString(R.string.graphics_driver_bcn_emulation_cache), Icons.Filled.Layers,
            uniq(bcnCaches, cfg.bcnEmulationCache), cfg.bcnEmulationCache) { v -> write { it.bcnEmulationCache = v } }
        val adrenoNote = if (isQualcomm) I18n.s(R.string.ui_no_effect_on_adreno_native_bcn) else null
        if (isIntegratedBcn) {
            rows += XmbRow.Toggle("bcnAstc", ctx.getString(R.string.graphics_driver_bcn_emulation_astc), Icons.Filled.Layers,
                cfg.bcnEmulationAstc, subtitle = adrenoNote) { on -> write { it.bcnEmulationAstc = on } }
        }
        if (!isBcnLayer && cfg.bcnEmulationType == "compute" && (cfg.bcnEmulation == "auto" || cfg.bcnEmulation == "full")) {
            rows += XmbRow.Toggle("computeAstc", ctx.getString(R.string.bcn_layer_transcode_astc), Icons.Filled.Layers,
                cfg.bcnTranscodeAstc, subtitle = adrenoNote) { on -> write { it.bcnTranscodeAstc = on } }
        }
        if (showBcnLayerSettings) {
            rows += XmbRow.Link("bcnLayer", ctx.getString(R.string.bcn_layer_section), Icons.Filled.Layers,
                subtitle = ctx.getString(R.string.bcn_layer_section_hint)) {
                p2BcnLayerMenu(xmb, { rawCfg() }, { ch -> write(ch) }, { isQualcomm }, isCompatDriver)
            }
        }

        rows += XmbRow.Header("hPresent", "Presentation")
        rows += XmbRow.Toggle("syncFrame", ctx.getString(R.string.graphics_driver_sync_frame), Icons.Filled.Speed, cfg.syncFrame) { on ->
            write { it.syncFrame = on }
        }
        rows += XmbRow.Toggle("presentWait", ctx.getString(R.string.graphics_driver_disable_present_wait), Icons.Filled.Speed, cfg.disablePresentWait) { on ->
            write { it.disablePresentWait = on }
        }
        rows += XmbRow.Toggle("fdDev", I18n.s(R.string.ui_oneui_hyperos_fix), Icons.Filled.Tune, cfg.fdDevFeatures) { on -> write { it.fdDevFeatures = on } }

        rows += XmbRow.Header("hTurnip", "Turnip")
        val gmemLabels = listOf("Auto (Adreno 710/720/722)", I18n.s(R.string.ui_force_on), I18n.s(R.string.ui_force_off))
        val gmemValues = listOf("auto", "on", "off")
        rows += XmbRow.Choice("gmem", I18n.s(R.string.ui_gmem_tiled_rendering), Icons.Filled.Memory, gmemLabels,
            gmemLabels[gmemValues.indexOf(cfg.turnipGmem).coerceAtLeast(0)],
            subtitle = I18n.s(R.string.ui_auto_uses_tu_debug_gmem_only_on_adreno_710_720_722)) { v ->
            write { it.turnipGmem = gmemValues[gmemLabels.indexOf(v).coerceAtLeast(0)] }
        }
        rows += XmbRow.Link("turnipTokens", I18n.s(R.string.ui_advanced_turnip_tu_debug), Icons.Filled.Science,
            subtitle = I18n.s(R.string.ui_expert_only_debug_tokens_off_by_default)) { p2TurnipMenu(xmb, { rawCfg() }, { ch -> write(ch) }) }

        if (isImported) {
            rows += XmbRow.Link("detected", I18n.s(R.string.ui_detected_settings_advanced), Icons.Filled.Science,
                value = if (detectedKeys.isEmpty()) "None" else "${detectedKeys.size}",
                subtitle = I18n.s(R.string.ui_from_scanning_this_wrapper_passed_to_it_as_is)) { p2DetectedMenu(xmb, { detectedKeys }, { rawCfg() }, { ch -> write(ch) }) }
        }
        rows
    }
}

/**
 * The Wayland game driver's settings (the pop-up editors' gear, WaylandDriverSettingsDialog): GPU name
 * spoof, max device memory, present mode (mailbox / fifo) and the OneUI / HyperOS UBWC hint. Each change
 * writes ONLY its own key back into the shortcut's graphicsDriverConfig (withGraphicsDriverKeys), so the
 * X11 driver configuration kept in the same string is untouched.
 */
internal fun xmbWaylandDriverConfigMenu(xmb: XmbScope, shortcut: Shortcut): XmbMenu {
    val ctx = xmb.context
    val c = shortcut.container
    val memEntries = ctx.resources.getStringArray(R.array.device_memory_entries).toList()
    fun memLabel(num: String): String = memEntries.firstOrNull { StringUtils.parseNumber(it) == num } ?: memEntries.firstOrNull() ?: num
    fun rawCfg(): String = shortcut.p2Ex("graphicsDriverConfig", c.getGraphicsDriverConfig())
    fun write(key: String, value: String) =
        p2Put(xmb, shortcut, "graphicsDriverConfig", withGraphicsDriverKeys(rawCfg(), mapOf(key to value)))

    return XmbMenu(I18n.s(R.string.ui_wayland_driver_settings), Icons.Filled.Tune) {
        val raw = rawCfg()
        val cfg = raw.split(";").associate { elem ->
            val parts = elem.split("=")
            parts[0] to if (parts.size > 1) parts[1] else ""
        }
        val gpuName = com.winhub.hernandez.core.GpuSpoof.gpuNameOf(raw)
        val names = com.winhub.hernandez.core.GpuSpoof.names(ctx).let { if (gpuName in it) it else it + gpuName }
        val storedPresent = cfg["presentMode"]?.ifEmpty { null } ?: "mailbox"
        val presentValues = WAYLAND_PRESENT_MODES + (if (storedPresent in WAYLAND_PRESENT_MODES) emptyList() else listOf(storedPresent))
        val presentLabels = presentValues.map { if (it in WAYLAND_PRESENT_MODES) it else I18n.s(R.string.ui_it_x11_only_not_used_here) }
        val rows = mutableListOf<XmbRow>()
        rows += XmbRow.Info("help", I18n.s(R.string.ui_for_games_that_refuse_or_misbehave_on_adreno), Icons.Filled.Info,
            subtitle = I18n.s(R.string.ui_directx_games_dxvk_see_the_gpu_named_here_off_by_defa))
        rows += xmbAdapterRows(xmb, shortcut)
        rows += XmbRow.Choice("gpuName", ctx.getString(R.string.gpu_name) + " (spoof)", Icons.Filled.DeveloperBoard, names, gpuName,
            subtitle = com.winhub.hernandez.core.GpuSpoof.vendorWarning(ctx, gpuName)
                ?: I18n.s(R.string.ui_nvidia_games_may_try_nvapi_dlss_or_reflex_amd_amd_ags)) { v -> write("gpuName", v) }
        if (memEntries.isNotEmpty()) {
            rows += XmbRow.Choice("memory", ctx.getString(R.string.graphics_driver_max_device_memory), Icons.Filled.Memory,
                memEntries, memLabel(cfg["maxDeviceMemory"] ?: "0"),
                subtitle = I18n.s(R.string.ui_what_directx_games_are_told_the_gpu_memory_is)) { v -> write("maxDeviceMemory", StringUtils.parseNumber(v)) }
        }
        rows += XmbRow.Choice("presentMode", ctx.getString(R.string.graphics_driver_present_modes), Icons.Filled.Speed,
            presentLabels, presentLabels[presentValues.indexOf(storedPresent)],
            subtitle = I18n.s(R.string.ui_wayland_offers_mailbox_and_fifo_immediate_needs_teari)) { v ->
            write("presentMode", presentValues[presentLabels.indexOf(v)])
        }
        rows += XmbRow.Toggle("fdDev", I18n.s(R.string.ui_oneui_hyperos_fix_ubwc_flag_hint), Icons.Filled.Tune, cfg["fdDevFeatures"] == "1",
            subtitle = I18n.s(R.string.ui_fd_dev_features_enable_tp_ubwc_flag_hint_1_corrupt_te)) { on ->
            write("fdDevFeatures", if (on) "1" else "0")
        }
        if (MaliPanvk.isMaliGpu()) rows += xmbMaliPanvkRows(xmb, cfg, { rawCfg() }) { values ->
            p2Put(xmb, shortcut, "graphicsDriverConfig", withGraphicsDriverKeys(rawCfg(), values))
        }
        rows
    }
}

/**
 * The gear's "Mali (PanVK)" section (MaliPanvkSection), Arm Mali GPUs only: the mode switch (writing
 * bcnEmulation=none with it on, like the pop-up), the BCn emulation pick with its warning, and the
 * adapter sub-switches + PANVK_* variables under a link. Same keys and rules as MaliPanvkSettingsState.
 */
private fun xmbMaliPanvkRows(
    xmb: XmbScope, cfg: Map<String, String>, raw: () -> String, write: (Map<String, String>) -> Unit,
): List<XmbRow> {
    val ctx = xmb.context
    val storedBcn = cfg["bcnEmulation"]?.ifEmpty { null } ?: "auto"
    val stored = cfg[MaliPanvk.KEY_MODE]?.ifEmpty { null }
    val on = MaliPanvk.modeOn(ctx, cfg, cfg["version"])
    val bcn = if (stored == null && on) "none" else storedBcn
    val rows = mutableListOf<XmbRow>()
    rows += XmbRow.Info(I18n.s(R.string.ui_malihelp), ctx.getString(R.string.mali_panvk_section), Icons.Filled.Info,
        subtitle = I18n.s(R.string.ui_for_fristonerr_panvk_driver_mit_credit_fristonerr_for))
    rows += XmbRow.Toggle("maliMode", ctx.getString(R.string.mali_panvk_mode), Icons.Filled.DeveloperBoard, on,
        subtitle = ctx.getString(R.string.mali_panvk_mode_hint)) { v ->
        write(mapOf(MaliPanvk.KEY_MODE to if (v) "1" else "0", "bcnEmulation" to if (v) "none" else storedBcn))
    }
    if (on) {
        val entries = ctx.resources.getStringArray(R.array.bcn_emulation_entries).toList()
        rows += XmbRow.Choice("maliBcn", ctx.getString(R.string.graphics_driver_bcn_emulation), Icons.Filled.Layers,
            entries, bcn, subtitle = ctx.getString(R.string.mali_panvk_bcn_warning)) { v ->
            write(mapOf(MaliPanvk.KEY_MODE to "1", "bcnEmulation" to v))
        }
    }
    rows += XmbRow.Link("panvkAdvanced", ctx.getString(R.string.mali_panvk_advanced), Icons.Filled.Science) {
        XmbMenu(ctx.getString(R.string.mali_panvk_advanced), Icons.Filled.Science) {
            val c = raw().split(";").associate { elem ->
                val parts = elem.split("=")
                parts[0] to if (parts.size > 1) parts[1] else ""
            }
            val auto = ctx.getString(R.string.mali_panvk_default)
            val noWaits = MaliPanvk.subOn(c, MaliPanvk.KEY_NO_SUBMIT_WAITS)
            val subRows = if (!MaliPanvk.modeOn(ctx, c, c["version"])) emptyList() else listOf(
                XmbRow.Toggle(MaliPanvk.KEY_HIDE_EXTS, ctx.getString(R.string.mali_panvk_hide_exts), Icons.Filled.Extension,
                    MaliPanvk.subOn(c, MaliPanvk.KEY_HIDE_EXTS)) { v -> write(mapOf(MaliPanvk.KEY_HIDE_EXTS to if (v) "1" else "0")) },
                XmbRow.Toggle(MaliPanvk.KEY_NO_SUBMIT_WAITS, ctx.getString(R.string.mali_panvk_no_submit_waits), Icons.Filled.Speed,
                    noWaits, subtitle = ctx.getString(R.string.mali_panvk_sync_hint)) { v ->
                    write(mapOf(MaliPanvk.KEY_NO_SUBMIT_WAITS to if (v) "1" else "0"))
                },
                XmbRow.Toggle(MaliPanvk.KEY_NO_ACQUIRE_SIGNAL, ctx.getString(R.string.mali_panvk_no_acquire_signal), Icons.Filled.Speed,
                    noWaits && MaliPanvk.subOn(c, MaliPanvk.KEY_NO_ACQUIRE_SIGNAL),
                    subtitle = ctx.getString(R.string.mali_panvk_sync_hint),
                    disabledReason = if (noWaits) null else I18n.s(R.string.ui_needs_skip_gpu_submit_waits)) { v ->
                    write(mapOf(MaliPanvk.KEY_NO_ACQUIRE_SIGNAL to if (v) "1" else "0"))
                },
            )
            fun mb(key: String, label: Int, hint: Int, min: Int, max: Int) =
                XmbRow.Text(key, ctx.getString(label), Icons.Filled.Memory, c[key] ?: "",
                    subtitle = ctx.getString(hint), placeholder = auto, numeric = true) { v ->
                    write(mapOf(key to (MaliPanvk.validMb(v, min, max) ?: "")))
                }
            subRows + listOf(
                mb(MaliPanvk.KEY_HEAP, R.string.mali_panvk_heap, R.string.mali_panvk_heap_hint, MaliPanvk.HEAP_MIN, MaliPanvk.HEAP_MAX),
                mb(MaliPanvk.KEY_TILER_HEAP, R.string.mali_panvk_tiler_heap, R.string.mali_panvk_tiler_heap_hint,
                    MaliPanvk.TILER_HEAP_MIN, MaliPanvk.TILER_HEAP_MAX),
                mb(MaliPanvk.KEY_POLY_HEAP, R.string.mali_panvk_poly_heap, R.string.mali_panvk_poly_heap_hint,
                    MaliPanvk.POLY_HEAP_MIN, MaliPanvk.POLY_HEAP_MAX),
                XmbRow.Choice(MaliPanvk.KEY_ATOM_STRIDE, ctx.getString(R.string.mali_panvk_atom_stride), Icons.Filled.Tune,
                    listOf(auto) + MaliPanvk.ATOM_STRIDES,
                    c[MaliPanvk.KEY_ATOM_STRIDE]?.takeIf { it in MaliPanvk.ATOM_STRIDES } ?: auto,
                    subtitle = ctx.getString(R.string.mali_panvk_atom_stride_hint)) { v ->
                    write(mapOf(MaliPanvk.KEY_ATOM_STRIDE to if (v == auto) "" else v))
                },
                XmbRow.Toggle(MaliPanvk.KEY_TRACE, ctx.getString(R.string.mali_panvk_trace), Icons.Filled.Science,
                    c[MaliPanvk.KEY_TRACE] == "1") { v -> write(mapOf(MaliPanvk.KEY_TRACE to if (v) "1" else "")) },
            )
        }
    }
    return rows
}

/** ExtensionPickerDialog: every probed extension, on = offered to games (off = blacklisted). */
private fun p2ExtensionsMenu(
    xmb: XmbScope, exts: () -> List<String>, raw: () -> String, write: ((P2GfxCfg) -> Unit) -> Unit,
): XmbMenu = XmbMenu(xmb.context.getString(R.string.graphics_driver_available_extensions), Icons.Filled.Extension) {
    val cfg = P2GfxCfg(raw())
    val list = exts()
    if (list.isEmpty()) listOf(XmbRow.Info("none", I18n.s(R.string.ui_no_extensions_available_for_this_driver), Icons.Filled.Info))
    else list.map { ext ->
        XmbRow.Toggle("ext:$ext", ext, Icons.Filled.Extension, ext !in cfg.blacklisted) { on ->
            write { it.blacklisted = if (on) it.blacklisted - ext else it.blacklisted + ext }
        }
    }
}

/** The dialog's collapsed "Advanced Turnip (TU_DEBUG)" block. */
private fun p2TurnipMenu(xmb: XmbScope, raw: () -> String, write: ((P2GfxCfg) -> Unit) -> Unit): XmbMenu =
    XmbMenu(I18n.s(R.string.ui_advanced_turnip), Icons.Filled.Science) {
        val cfg = P2GfxCfg(raw())
        val sysmemBlocked = cfg.turnipGmem == "on"
        listOf(
            XmbRow.Info("note", I18n.s(R.string.ui_expert_only_turnip_debug_tokens_unioned_with_the_gmem), Icons.Filled.Warning),
            XmbRow.Toggle("forcecb", I18n.s(R.string.ui_forcecb_force_concurrent_binning), Icons.Filled.Science, cfg.forceCb) { on -> write { it.forceCb = on } },
            XmbRow.Toggle("nocb", I18n.s(R.string.ui_nocb_disable_concurrent_binning), Icons.Filled.Science, cfg.noCb) { on -> write { it.noCb = on } },
            XmbRow.Toggle("sysmem", I18n.s(R.string.ui_sysmem_force_sysmem_bypass_gmem), Icons.Filled.Science, cfg.sysmem && !sysmemBlocked,
                disabledReason = if (sysmemBlocked) I18n.s(R.string.ui_disabled_turnip_gmem_force_on_overrides_sysmem) else null) { on -> write { it.sysmem = on } },
            XmbRow.Toggle("deckEmu", I18n.s(R.string.ui_deck_emu_advertise_as_steamdeck), Icons.Filled.Science, cfg.deckEmu,
                subtitle = I18n.s(R.string.ui_needs_a_banners_turnip_driver_ignored_on_stock_turnip)) { on -> write { it.deckEmu = on } },
        )
    }

/** The dialog's "BCn Layer Settings" block (implicit bcn_layer overlay; compat rows for Wrapper + compat + bcn). */
private fun p2BcnLayerMenu(
    xmb: XmbScope, raw: () -> String, write: ((P2GfxCfg) -> Unit) -> Unit, qualcomm: () -> Boolean, isCompatDriver: Boolean,
): XmbMenu {
    val ctx = xmb.context
    return XmbMenu(ctx.getString(R.string.bcn_layer_section), Icons.Filled.Layers) {
        val cfg = P2GfxCfg(raw())
        val rows = mutableListOf<XmbRow>()
        if (qualcomm()) rows += XmbRow.Info("adreno", I18n.s(R.string.ui_no_effect_on_adreno_native_bcn_these_apply_to_mali_no), Icons.Filled.Info)
        rows += XmbRow.Toggle("forceDecode", ctx.getString(R.string.bcn_layer_force_decode), Icons.Filled.Layers, cfg.bcnLayerAuto,
            subtitle = ctx.getString(R.string.bcn_layer_force_decode_hint)) { on -> write { it.bcnLayerAuto = on } }
        rows += XmbRow.Toggle("etc2", ctx.getString(R.string.bcn_layer_transcode_etc2), Icons.Filled.Layers, cfg.bcnTranscodeEtc2,
            subtitle = ctx.getString(R.string.bcn_layer_transcode_hint)) { on -> write { it.bcnTranscodeEtc2 = on } }
        rows += XmbRow.Toggle("astc", ctx.getString(R.string.bcn_layer_transcode_astc), Icons.Filled.Layers, cfg.bcnTranscodeAstc,
            subtitle = ctx.getString(R.string.bcn_layer_transcode_hint)) { on -> write { it.bcnTranscodeAstc = on } }
        rows += XmbRow.Toggle("imageView", ctx.getString(R.string.bcn_layer_image_view), Icons.Filled.Layers, cfg.bcnImageView,
            subtitle = ctx.getString(R.string.bcn_layer_image_view_hint)) { on -> write { it.bcnImageView = on } }
        rows += XmbRow.Toggle("debugLog", ctx.getString(R.string.bcn_layer_debug_log), Icons.Filled.Layers, cfg.bcnDebugLog,
            subtitle = ctx.getString(R.string.bcn_layer_debug_log_hint)) { on -> write { it.bcnDebugLog = on } }
        if (isCompatDriver) {
            rows += XmbRow.Toggle("gamenative", ctx.getString(R.string.compat_use_gamenative), Icons.Filled.Layers, cfg.compatUseGamenative,
                subtitle = ctx.getString(R.string.compat_use_gamenative_hint)) { on -> write { it.compatUseGamenative = on } }
            rows += XmbRow.Toggle("sparse", ctx.getString(R.string.bcn_compat_sparse), Icons.Filled.Layers, cfg.bcnCompatSparse,
                subtitle = ctx.getString(R.string.bcn_compat_sparse_hint)) { on -> write { it.bcnCompatSparse = on } }
        }
        rows
    }
}

/** #132 Layer 1: one control per env key scanned from an imported wrapper, stored under the raw key. */
private fun p2DetectedMenu(
    xmb: XmbScope, keys: () -> List<String>, raw: () -> String, write: ((P2GfxCfg) -> Unit) -> Unit,
): XmbMenu = XmbMenu(I18n.s(R.string.ui_detected_settings), Icons.Filled.Science) {
    val cfg = P2GfxCfg(raw())
    val rows = mutableListOf<XmbRow>()
    rows += XmbRow.Info("note", I18n.s(R.string.ui_values_are_passed_to_the_wrapper_as_is_unknown_ones_a), Icons.Filled.Info)
    val list = keys()
    if (list.isEmpty()) rows += XmbRow.Info("none", I18n.s(R.string.ui_no_extra_settings_detected), Icons.Filled.Info)
    list.forEach { key ->
        val def = WrapperSettingsDictionary.defFor(key)
        val current = cfg.extras[key] ?: ""
        val hint = def.hint.ifBlank { key }
        when (def.type) {
            WrapperSettingsDictionary.Type.TOGGLE -> rows += XmbRow.Toggle("d:$key", def.label, Icons.Filled.Tune, current == "1", subtitle = hint) { on ->
                write { it.extras[key] = if (on) "1" else "0" }
            }
            WrapperSettingsDictionary.Type.DROPDOWN -> {
                val selected = current.ifEmpty { def.default }.ifEmpty { def.choices.firstOrNull() ?: "" }
                val opts = p2Unique(if (selected.isNotEmpty() && selected !in def.choices) def.choices + selected else def.choices)
                if (opts.isEmpty()) {
                    rows += XmbRow.Text("d:$key", def.label, Icons.Filled.Tune, current, subtitle = hint) { v -> write { it.extras[key] = v } }
                } else {
                    rows += XmbRow.Choice("d:$key", def.label, Icons.Filled.Tune, opts, selected, subtitle = hint) { v -> write { it.extras[key] = v } }
                }
            }
            WrapperSettingsDictionary.Type.SLIDER -> {
                val max = if (def.max > def.min) def.max else def.min + 1f
                val fv = (current.toFloatOrNull() ?: def.default.toFloatOrNull() ?: def.min).coerceIn(def.min, max)
                rows += XmbRow.Slider("d:$key", def.label, Icons.Filled.Tune, fv, def.min, max, if (def.step > 0f) def.step else 1f,
                    format = { it.toInt().toString() }, subtitle = hint) { v -> write { it.extras[key] = v.toInt().toString() } }
            }
            WrapperSettingsDictionary.Type.TEXT -> rows += XmbRow.Text("d:$key", def.label, Icons.Filled.Tune, current, subtitle = hint) { v ->
                write { it.extras[key] = v }
            }
        }
    }
    rows
}

/**
 * The Wayland adapter settings in the XMB (the same ones the pop-up and Deck editors have): the applied
 * summary, the profile with its help, the single switches, and the adapter report with a Re-check. Per
 * game: every adapter key can be left on "Inherit" (blank = the container's choice), and a game without
 * its own driver config starts from a copy of the container's with the adapter keys blanked.
 */
private fun xmbAdapterRows(xmb: XmbScope, shortcut: Shortcut): List<XmbRow> {
    val ctx = xmb.context
    val keys = com.winhub.hernandez.core.WaylandAdapterSettings.KEYS
    val containerCfg = shortcut.container.getGraphicsDriverConfig()
    fun own(): String = shortcut.getExtra("graphicsDriverConfig", "")
    fun base(): String = own().ifEmpty { withGraphicsDriverKeys(containerCfg, keys.associateWith { "" }) }
    fun write(key: String, value: String) =
        p2Put(xmb, shortcut, "graphicsDriverConfig", withGraphicsDriverKeys(base(), mapOf(key to value)))
    val ownMap = own().split(";").filter { it.isNotEmpty() }.associate { e -> e.substringBefore("=") to e.substringAfter("=", "") }
    fun v(key: String): String = ownMap[key] ?: ""
    val defaultWord = "Inherit"
    val driverId = com.winhub.hernandez.contentdialog.GraphicsDriverConfigDialog.getVersion(own())?.ifEmpty { null }
        ?: com.winhub.hernandez.contentdialog.GraphicsDriverConfigDialog.getVersion(containerCfg) ?: ""
    val probe = if (driverId.isEmpty()) null else runCatching {
        com.winhub.hernandez.core.WaylandDriverProbe.cached(ctx, driverId)
    }.getOrNull()
    val rows = mutableListOf<XmbRow>()

    // Applied at launch + the profile.
    val effective = com.winhub.hernandez.core.WaylandAdapterSettings.resolve(containerCfg, own().ifEmpty { null }, null, probe)
    rows += XmbRow.Info(I18n.s(R.string.ui_adapplied), I18n.s(R.string.ui_applied_at_launch), Icons.Filled.Info, subtitle = effective.summary("mailbox"))
    val containerProfile = com.winhub.hernandez.core.WaylandAdapterSettings.profileLabel(
        com.winhub.hernandez.contentdialog.GraphicsDriverConfigDialog.parseGraphicsDriverConfig(containerCfg)[
            com.winhub.hernandez.core.WaylandAdapterSettings.KEY_PROFILE] ?: "")
    val inheritLabel = I18n.s(R.string.ui_inherit_the_container_s_containerprofile)
    val profiles = com.winhub.hernandez.core.WaylandAdapterSettings.PROFILES
    val profileLabels = listOf(inheritLabel) + profiles.map { com.winhub.hernandez.core.WaylandAdapterSettings.profileLabel(it) }
    val storedProfile = v(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_PROFILE)
    val selectedProfile = if (storedProfile.isEmpty()) inheritLabel
        else com.winhub.hernandez.core.WaylandAdapterSettings.profileLabel(storedProfile)
    val profileHelp = if (storedProfile.isEmpty()) I18n.s(R.string.ui_use_whatever_the_container_is_set_to)
        else com.winhub.hernandez.core.WaylandAdapterSettings.profileHelp(storedProfile)
    rows += XmbRow.Choice("adProfile", "Profile", Icons.Filled.Tune, profileLabels, selectedProfile,
        subtitle = profileHelp +
            (if (effective.profile != com.winhub.hernandez.core.WaylandAdapterSettings.PROFILE_SMOOTH)
                "  Tip: if frames look uneven or jittery, try Smooth. It paces frames through the compositor " +
                    I18n.s(R.string.ui_vsync_no_zero_copy_which_helps_when_the_gpu_is_close) else "")) { picked ->
        val i = profileLabels.indexOf(picked)
        write(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_PROFILE, if (i <= 0) "" else profiles[i - 1])
    }
    rows += XmbRow.Info(I18n.s(R.string.ui_adenvnote), I18n.s(R.string.ui_env_vars_always_win), Icons.Filled.Info,
        subtitle = I18n.s(R.string.ui_env_vars_you_type_under_env_vars_always_win_over_thes))

    // Advanced: single switches.
    rows += XmbRow.Header("adAdvanced", I18n.s(R.string.ui_advanced_single_switches))
    fun tri(key: String, label: String, help: String) {
        val labels = listOf(defaultWord, "On", "Off")
        rows += XmbRow.Choice("ad_$key", label, Icons.Filled.Tune, labels,
            when (v(key)) { "1" -> "On"; "0" -> "Off"; else -> defaultWord }, subtitle = help) { picked ->
            write(key, when (picked) { "On" -> "1"; "Off" -> "0"; else -> "" })
        }
    }
    val presentKey = com.winhub.hernandez.core.WaylandAdapterSettings.KEY_PRESENT
    val presentStored = ownMap["presentMode"]?.ifEmpty { null } ?: "mailbox"
    run {
        val labels = listOf(defaultWord, "mailbox", "fifo")
        rows += XmbRow.Choice("ad_$presentKey", ctx.getString(R.string.graphics_driver_present_modes), Icons.Filled.Speed, labels,
            v(presentKey).ifEmpty { defaultWord },
            subtitle = "mailbox = newest frame, lowest latency; fifo = vsync, even pacing. Default follows the " +
                I18n.s(R.string.ui_profile_else_the_stored_presentstored_immediate_needs)) { picked ->
            write(presentKey, if (picked == defaultWord) "" else picked)
        }
    }
    tri(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_ASYNC_COPY, I18n.s(R.string.ui_asynchronous_copy),
        I18n.s(R.string.ui_the_compositor_keeps_frames_in_flight_instead_of_waiting) +
            I18n.s(R.string.ui_frames_arrive_out_of_order_or_a_game_stutters_only_on))
    tri(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_ZERO_COPY, I18n.s(R.string.ui_zero_copy_presentation),
        I18n.s(R.string.ui_the_game_s_own_buffer_goes_on_screen_without_a_copy_lowe) +
            I18n.s(R.string.ui_sync_file_fences_in_the_driver_see_the_report_turn_of))
    tri(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_CLIENT_FENCE, I18n.s(R.string.ui_client_render_fences),
        I18n.s(R.string.ui_with_zero_copy_the_game_s_driver_tells_the_compositor_wh) +
            I18n.s(R.string.ui_only_to_compare_off_can_show_half_drawn_frames))
    tri(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_UBWC, I18n.s(R.string.ui_ubwc_compressed_buffers),
        I18n.s(R.string.ui_frames_travel_as_compressed_gpu_buffers_less_bandwidth_t) +
            I18n.s(R.string.ui_garbled_blocks_or_stripes))
    tri(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_MODIFIERS, I18n.s(R.string.ui_drm_format_modifiers),
        I18n.s(R.string.ui_lets_the_game_s_driver_and_the_compositor_agree_on_buffe) +
            I18n.s(R.string.ui_plain_linear_buffers_banner_wsi_no_modifiers_1_try_it))
    tri(com.winhub.hernandez.core.WaylandAdapterSettings.KEY_KGSL_SHIM, I18n.s(R.string.ui_kgsl_timing_shim),
        "Works around a Turnip bug where asking \"is the GPU done?\" waits for the whole frame " +
            I18n.s(R.string.ui_the_report_s_timing_bug_default_lets_the_adapter_dete))
    run {
        val key = com.winhub.hernandez.core.WaylandAdapterSettings.KEY_SUBGROUPS
        val labels = listOf(defaultWord, "Auto", "Off")
        rows += XmbRow.Choice("ad_$key", "Subgroups", Icons.Filled.Tune, labels,
            when (v(key)) { "auto" -> "Auto"; "0" -> "Off"; else -> defaultWord },
            subtitle = I18n.s(R.string.ui_auto_tells_the_game_what_subgroup_operations_your_mesa_d) +
                "other games that need them start). Off hides them, as older adapters did; try it if a game " +
                I18n.s(R.string.ui_shows_glitches_or_crashes_that_it_did_not_before)) { picked ->
            write(key, when (picked) { "Auto" -> "auto"; "Off" -> "0"; else -> "" })
        }
    }

    // Adapter report.
    rows += XmbRow.Header("adReportHeader", I18n.s(R.string.ui_adapter_report))
    fun info(key: String, label: String, value: String) {
        rows += XmbRow.Info(key, label, Icons.Filled.Info, subtitle = value)
    }
    when {
        driverId.isEmpty() || driverId == "System" ->
            info("adReport", I18n.s(R.string.ui_adapter_report), I18n.s(R.string.ui_no_turnip_graphics_driver_is_set_so_there_is_nothing))
        probe == null -> info("adReport", "Adapter report", I18n.s(R.string.ui_not_checked_yet))
        !probe.ok -> info("adReport", I18n.s(R.string.ui_could_not_check), probe.error ?: I18n.s(R.string.ui_unknown_error))
        else -> {
            info("adReport0", "Driver", probe.driverLine())
            info("adReport1", I18n.s(R.string.ui_timing_bug), when (probe.kgslZeroTimeoutBug) {
                true -> I18n.s(R.string.ui_yes_the_timing_shim_will_be_used)
                false -> "no"
                null -> I18n.s(R.string.ui_not_measured)
            })
            info("adReport2", I18n.s(R.string.ui_fences_sync_file), if (probe.syncFdFence) "yes" else "no")
            info("adReport3", I18n.s(R.string.ui_zero_copy_safe), (if (probe.zeroCopySafe()) "yes" else "no") + " — " + probe.zeroCopyReason())
            info("adReport4", I18n.s(R.string.ui_compressed_buffers_modifiers), if (probe.drmModifiers) "yes" else "no")
            info("adReport5", I18n.s(R.string.ui_high_gpu_priority), when (probe.highPriorityAccepted) {
                true -> "accepted"
                false -> "refused" + (if (probe.highPriorityRefusal.isNotEmpty()) " (${probe.highPriorityRefusal})" else "")
                null -> I18n.s(R.string.ui_not_offered_by_the_driver)
            })
            info("adReport6", "Checked", I18n.s(R.string.ui_in_probe_probems_ms) + (if (probe.fromCache) " (saved result)" else ""))
        }
    }
    if (driverId.isNotEmpty() && driverId != "System") {
        rows += XmbRow.Action("adRecheck", "Re-check", Icons.Filled.Science,
            subtitle = I18n.s(R.string.ui_checks_the_graphics_driver_again_about_2_seconds)) {
            xmb.toast(I18n.s(R.string.ui_checking_driver_about_2_seconds))
            xmb.scope.launch {
                withContext(Dispatchers.IO) {
                    com.winhub.hernandez.core.WaylandDriverProbe.forget(ctx, driverId)
                    graphicsProbeMutex.withLock {
                        runCatching { com.winhub.hernandez.core.WaylandDriverProbe.probe(ctx, driverId) }
                    }
                }
                xmb.refresh()
            }
        }
    }
    return rows
}
