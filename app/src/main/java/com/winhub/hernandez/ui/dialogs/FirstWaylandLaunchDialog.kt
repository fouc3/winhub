package com.winhub.hernandez.ui.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.winhub.hernandez.ui.XServerDialogState
import com.winhub.hernandez.ui.components.TextButton
import com.winhub.hernandez.ui.screens.OutlinedAlertDialog
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * Shown once per container, before its first launch on Wayland. Wine sets the container up the first
 * time it runs, which takes a while with only the wallpaper and taskbar showing; closing the app in
 * that window can hang it. Back / B counts as Cancel.
 */
@Composable
fun FirstWaylandLaunchDialog(state: XServerDialogState) {
    fun choose(launch: Boolean) {
        val callback = state.onFirstWaylandLaunchChoice
        state.onFirstWaylandLaunchChoice = null
        state.dismiss()
        callback?.invoke(launch)
    }

    OutlinedAlertDialog(
        onDismissRequest = { choose(false) },
        title = { Text(I18n.s(R.string.ui_first_launch_of_this_container)) },
        text = {
            Text(
                if (state.firstLaunchRunsGame)
                    I18n.s(R.string.ui_this_is_the_first_time_this_container_runs_on_wayland_so) +
                        I18n.s(R.string.ui_it_opens_the_container_which_can_take_about_30_seconds_w) +
                        "taskbar showing. Then your game starts by itself and the file manager closes. This only " +
                        "happens on the first launch.\n\nPlease be patient and let it work. Don't close the app " +
                        "meanwhile: closing it early can make the app hang."
                else
                    I18n.s(R.string.ui_wine_sets_this_container_up_the_first_time_it_runs_on_wa) +
                        I18n.s(R.string.ui_seconds_and_you_will_only_see_the_wallpaper_and_taskbar) +
                        "once per container.\n\nPlease don't close the app while it works. Closing it early " +
                        I18n.s(R.string.ui_can_make_the_app_hang),
            )
        },
        confirmButton = { TextButton(onClick = { choose(true) }) { Text(I18n.s(R.string.ui_launch)) } },
        dismissButton = { TextButton(onClick = { choose(false) }) { Text(I18n.s(R.string.ui_cancel_2)) } },
    )
}
