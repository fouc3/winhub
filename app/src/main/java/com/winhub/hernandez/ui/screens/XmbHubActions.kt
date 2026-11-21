package com.winhub.hernandez.ui.screens

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.preference.PreferenceManager
import com.winhub.hernandez.container.Shortcut
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/** What only Home can do for a game: its own launch-method, settings, pin, container and delete flows. */
internal class HubXmbOps(
    val launchMethod: () -> Unit,
    val addToHome: () -> Unit,
    val containerProperties: () -> Unit,
    val openSettings: () -> Unit,
    val delete: () -> Unit,
)

/**
 * The XMB options column for a Home game. Saves and export live under Settings > General. Launch method, Add to Home Screen, Container properties and
 * Delete run Home's own flows.
 */
internal fun hubXmbActions(
    context: Context,
    shortcut: Shortcut,
    game: HubGame,
    host: XmbGameHost,
    ops: HubXmbOps,
): List<XmbAction> = buildList {
    add(XmbAction(I18n.s(R.string.ui_launch_method), Icons.Filled.Layers) { ops.launchMethod() })
    if (GameSettingsStyle.isClassic(PreferenceManager.getDefaultSharedPreferences(context))) {
        add(XmbAction("Settings", Icons.Filled.Settings, I18n.s(R.string.ui_display_graphics_controller)) { ops.openSettings() })
    } else {
        add(XmbAction("Settings", Icons.Filled.Settings, I18n.s(R.string.ui_display_graphics_controller), menu = { xmbSettingsMenu(it, shortcut, host) }))
    }
    add(XmbAction(I18n.s(R.string.ui_game_doctor), Icons.Filled.Healing, I18n.s(R.string.ui_why_it_won_t_run_and_what_to_try), menu = { xmbGameDoctorMenu(it, shortcut) }))
    add(XmbAction(I18n.s(R.string.ui_game_details), Icons.Filled.Edit, "Name, genres, year, description", menu = { xmbGameDetailsMenu(it, shortcut) }))
    if (isSteamOriginShortcut(shortcut)) {
        add(XmbAction(I18n.s(R.string.ui_cloud_saves), Icons.Filled.CloudSync, I18n.s(R.string.ui_steam_cloud_sync_opens_its_own_screen)) { launchSaveManager(context, steamAppIdOf(shortcut)) })
    }
    add(XmbAction(I18n.s(R.string.ui_search_covers), Icons.Filled.Search, menu = { xmbScrapeCoverMenu(it, shortcut) }))
    add(XmbAction(I18n.s(R.string.ui_clone_to_container), Icons.Filled.ContentCopy, menu = { xmbCloneMenu(it, shortcut, host) }))
    add(XmbAction(I18n.s(R.string.ui_copy_to_drive_c_2), Icons.Filled.DriveFileMove, menu = { xmbCopyToDriveCMenu(it, shortcut) }))
    add(XmbAction(I18n.s(R.string.ui_change_executable_2), Icons.Filled.SwapHoriz, menu = { xmbChangeExeMenu(it, shortcut) }))
    add(XmbAction(I18n.s(R.string.ui_view_logs), Icons.Filled.Description, menu = { xmbLogsMenu(it, shortcut) }))
    add(XmbAction(I18n.s(R.string.add_to_home_screen), Icons.Filled.Home) { ops.addToHome() })
    add(XmbAction("Properties", Icons.Filled.Info, I18n.s(R.string.ui_times_played_playtime), menu = { xmbPropertiesMenu(it, shortcut) }))
    add(XmbAction(I18n.s(R.string.ui_container_properties), Icons.Filled.Folder) { ops.containerProperties() })
    add(XmbAction("Delete", Icons.Filled.Delete, I18n.s(R.string.ui_asks_to_confirm_first), danger = true, menu = { hubDeleteMenu(it, game, ops) }))
}

private fun hubDeleteMenu(xmb: XmbScope, game: HubGame, ops: HubXmbOps): XmbMenu =
    XmbMenu(I18n.s(R.string.ui_delete_game), Icons.Filled.Delete, initialKey = "cancel") {
        listOf(
            XmbRow.Info("msg", "Delete “${game.name}”?", Icons.Filled.Info),
            XmbRow.Action("delete", "Delete", Icons.Filled.Delete, danger = true) {
                ops.delete()
                xmb.popToRoot()
            },
            XmbRow.Action("cancel", "Cancel", Icons.Filled.Close) { xmb.pop() },
        )
    }
