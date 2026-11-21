package com.winhub.hernandez.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.winhub.hernandez.container.Shortcut
import com.winhub.hernandez.core.GameModCache
import com.winhub.hernandez.core.GameMods
import com.winhub.hernandez.core.WinePath
import java.io.File
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * Game settings > Mods: which mods (core.GameMods) this game's EXE can take, each one's per-game switch
 * (off by default) and Special K's DLL name ("" = picked from the EXE). Switching a mod on downloads it
 * from its release if this device does not have it yet.
 */
internal class GameModsSetting(
    val mods: List<GameMods.Mod>, val machine: Int, val imports: Set<String>,
    initialOn: Map<String, Boolean>, initialSkDll: String,
) {
    val on = mutableStateMapOf<String, Boolean>().apply { putAll(initialOn) }
    var skDll by mutableStateOf(initialSkDll)
    /** Per mod id: "" or a short download state ("40 %", "ready", "download failed"). */
    val status = mutableStateMapOf<String, String>()
    private val main = Handler(Looper.getMainLooper())

    fun write(putExtra: (String, String?) -> Unit) {
        for (m in mods) putExtra(m.extra, if (on[m.id] == true) "1" else null)
        if (GameMods.SPECIAL_K in mods) putExtra(GameMods.EXTRA_SPECIALK_DLL, skDll.ifEmpty { null })
    }

    fun download(filesDir: File, mod: GameMods.Mod) {
        val asset = GameMods.asset(mod, machine) ?: return
        if (GameModCache.cached(filesDir, asset) != null) { status[mod.id] = ""; return }
        status[mod.id] = "0 %"
        Thread {
            val done = try {
                GameModCache.get(filesDir, asset) { f -> main.post { status[mod.id] = "${(f * 100).toInt().coerceIn(0, 100)} %" } }
                "ready"
            } catch (e: Exception) {
                I18n.s(R.string.ui_download_failed)
            }
            main.post { status[mod.id] = done }
        }.apply { isDaemon = true }.start()
    }
}

@Composable
internal fun rememberGameModsSetting(shortcut: Shortcut): GameModsSetting = remember(shortcut) {
    val exe = runCatching { WinePath.resolveAndroidPath(shortcut.container, shortcut.path) }.getOrNull()
    val machine = GameMods.peMachine(exe)
    val mods = GameMods.ALL.filter { GameMods.supports(it, shortcut.path, machine) }
    val imports = if (GameMods.SPECIAL_K in mods) GameMods.peImports(exe) else emptySet()
    GameModsSetting(
        mods, machine, imports,
        GameMods.ALL.associate { it.id to (shortcut.getExtra(it.extra) == "1") },
        shortcut.getExtra(GameMods.EXTRA_SPECIALK_DLL) ?: "",
    )
}

/** Deck editor's Mods tab: the mods this game can take, then the programs it starts. */
@Composable
internal fun DeckModsTab(mods: GameModsSetting, programs: StartProgramsState) {
    val filesDir = LocalContext.current.filesDir
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DeckGroup(I18n.s(R.string.ui_game_mods)) {
            if (mods.mods.isEmpty()) {
                DeckRow(
                    label = I18n.s(R.string.ui_no_mods_for_this_game),
                    hint = I18n.s(R.string.ui_reframework_is_for_re_engine_games_and_special_k_for_32) +
                        I18n.s(R.string.ui_this_game_s_exe_can_t_take_either),
                ) {}
            }
            for (mod in mods.mods) {
                val status = mods.status[mod.id].orEmpty()
                DeckRow(
                    label = mod.name,
                    hint = when (status) {
                        "" -> I18n.s(R.string.ui_downloaded_the_first_time_it_is_switched_on_2)
                        "ready" -> I18n.s(R.string.ui_downloaded_and_ready_2)
                        "download failed" -> I18n.s(R.string.ui_download_failed_check_your_connection_and_switch_it_off)
                        else -> "Downloading… $status"
                    },
                ) {
                    DeckToggle(checked = mods.on[mod.id] == true) { checked ->
                        mods.on[mod.id] = checked
                        if (checked) mods.download(filesDir, mod)
                    }
                }
                if (mod === GameMods.SPECIAL_K && mods.on[mod.id] == true) {
                    val auto = GameMods.targetDll(mod, "", mods.imports, mods.machine)
                    DeckChoiceRow(
                        label = I18n.s(R.string.ui_special_k_dll_name),
                        hint = I18n.s(R.string.ui_name_it_loads_as_in_the_game_folder),
                        options = listOf("" to "$auto (auto)") + GameMods.SPECIALK_DLLS.map { it to it },
                        selected = mods.skDll,
                        onPick = { mods.skDll = it },
                    )
                }
            }
        }
        DeckStartProgramsTab(programs)
    }
}
