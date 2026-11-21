package com.winhub.hernandez.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.winhub.hernandez.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winhub.hernandez.ui.screens.OutlinedAlertDialog
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/**
 * Confirm sheet for a storage move — says exactly where the game is going, what it costs, and what
 * the user is trading away, then hands off to [MoveGameStorage.execute].
 *
 * The two warnings are the ones that actually bite:
 *  • **Speed** — the card is FUSE-backed, so a game that streams assets can stall on it. This is the
 *    same trade the "Install to SD card" download toggle warns about, and the reason "Copy game to
 *    Drive C" exists. Only shown for the internal → card direction.
 *  • **EA titles** — an EA/Denuvo game can treat a new install path as a new machine and demand a
 *    re-activation, which spends a limited activation allowance. Shown whenever [EaSupport] fingerprints
 *    the folder, in either direction.
 */
@Composable
internal fun MoveStorageDialog(
    plan: MoveGameStorage.Plan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val toSd = plan.to.place == MoveGameStorage.Place.SD
    val fromLabel = if (plan.from == MoveGameStorage.Place.INTERNAL) "Internal storage" else "SD card"
    OutlinedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (toSd) "Move to SD card" else I18n.s(R.string.ui_move_to_internal_storage)) },
        text = {
            Column {
                Text(
                    text = "${plan.gameName}\n$fromLabel  →  ${plan.to.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Size: ${SteamSdInstall.fmtBytes(plan.sizeBytes)}   ·   " +
                        "Free there: ${SteamSdInstall.fmtBytes(plan.to.freeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!plan.fitsOnTarget) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = I18n.s(R.string.ui_there_isn_t_enough_free_space_for_this_game),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(10.dp))
                // What the user is really asking: "will my game still work afterwards?" Say it up
                // front rather than making them find out.
                Text(
                    text = I18n.s(R.string.ui_your_saves_cloud_saves_and_achievements_are_kept_they_do) +
                        I18n.s(R.string.ui_the_game_folder_the_game_s_shortcut) +
                        (if (plan.shortcuts.size == 1) " is" else "s (${plan.shortcuts.size}) are") +
                        " updated to the new location, so launching works exactly as before.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (plan.shortcuts.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = I18n.s(R.string.ui_this_game_isn_t_in_a_container_yet_so_there_no_shortc),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (toSd) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = I18n.s(R.string.ui_the_sd_card_is_slower_than_internal_storage_some_games_s_2) +
                            I18n.s(R.string.ui_loading_from_a_card_if_that_happens_move_it_back_or_use) +
                            "Drive C from the shortcut editor.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (plan.isEaTitle) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = I18n.s(R.string.ui_this_is_an_ea_game_ea_s_activation_can_treat_a_new_insta) +
                            I18n.s(R.string.ui_as_a_new_computer_and_ask_you_to_sign_in_and_activate_ag) +
                            I18n.s(R.string.ui_one_of_your_limited_activations),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = I18n.s(R.string.ui_the_original_copy_is_only_deleted_after_the_new_one_is_c) +
                        I18n.s(R.string.ui_failed_move_leaves_the_game_exactly_where_it_is),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = plan.fitsOnTarget) {
                Text(if (toSd) "Move to SD card" else I18n.s(R.string.ui_move_to_internal))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Blocking progress for a running move. Not dismissable — the only way out is Cancel, and that is
 * only offered while the copy is still running: once the shortcuts and the database pointer are
 * being rewritten the move is committing, and stopping there is what would leave them disagreeing.
 */
@Composable
internal fun MoveProgressDialog(
    gameName: String,
    progress: MoveGameStorage.Progress,
    onCancel: () -> Unit,
) {
    val copying = progress.phase == MoveGameStorage.Phase.COPYING
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = I18n.s(R.string.ui_moving_name, gameName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))
                if (copying) {
                    LinearProgressIndicator(
                        progress = { progress.pct / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when (progress.phase) {
                        MoveGameStorage.Phase.COPYING ->
                            "${progress.pct}%  ·  ${SteamSdInstall.fmtBytes(progress.copiedBytes)} of " +
                                SteamSdInstall.fmtBytes(progress.totalBytes)
                        MoveGameStorage.Phase.VERIFYING -> I18n.s(R.string.ui_checking_the_copy)
                        MoveGameStorage.Phase.REPOINTING -> I18n.s(R.string.ui_updating_shortcuts)
                        MoveGameStorage.Phase.CLEANING -> I18n.s(R.string.ui_freeing_the_old_copy)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (copying && progress.currentFile.isNotBlank()) {
                    Text(
                        text = progress.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCancel, enabled = copying) { Text("Cancel") }
                }
            }
        }
    }
}
