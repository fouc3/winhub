package com.winhub.hernandez.ui.dialogs

import androidx.compose.foundation.background
import com.winhub.hernandez.ui.components.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.winhub.hernandez.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winhub.hernandez.ui.XServerDialogState
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

@Composable
fun CastDialog(state: XServerDialogState) {
    val devices by state.castDevices.collectAsState()
    val scanning by state.castScanning.collectAsState()
    val status by state.castStatus.collectAsState()
    val targetName by state.castTargetName.collectAsState()
    val detail by state.castStatusDetail.collectAsState()

    var showHelp by remember { mutableStateOf(false) }
    // Which device row is expanded (tapped). Selecting a device only shows its info + a Connect button;
    // it does NOT auto-connect — the user connects explicitly.
    var selected by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { state.dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title row: name + "?" help toggle + Refresh (Refresh hidden on the help screen).
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showHelp) "Casting — options & tips" else I18n.s(R.string.ui_cast_to_a_tv_wireless),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (!showHelp) {
                        if (scanning) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        // Round "?" help button.
                        Box(Modifier.size(28.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showHelp = true }, contentAlignment = Alignment.Center) {
                            Text("?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.size(4.dp))
                        TextButton(onClick = { state.onCastRefresh?.run() }) { Text(I18n.s(R.string.social_hub_refresh)) }
                    }
                }

                if (showHelp) {
                    CastHelp()
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showHelp = false }) { Text("Back") }
                    }
                    return@Column
                }

                Text(
                    text = "Google TV / Chromecast devices on your Wi-Fi. Nothing to install on the TV. " +
                        I18n.s(R.string.ui_tap_the_for_how_casting_works_and_the_trade_offs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (devices.isEmpty()) {
                    Text(
                        text = if (scanning) "Searching…" else I18n.s(R.string.ui_no_devices_found_make_sure_your_tv_is_on_the) +
                            I18n.s(R.string.ui_same_wi_fi_then_tap_refresh),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        devices.forEach { d ->
                            // "Active" = this device is the one we're connecting to / connected to.
                            val isActive = d.name == targetName && status != XServerDialogState.CastStatus.IDLE
                            val expanded = d.name == selected || isActive
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selected = d.name },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer
                                                     else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(d.name, fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Text(d.type, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                                    if (expanded) {
                                        if (d.host.isNotBlank())
                                            Text(d.host, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                        Spacer(Modifier.height(6.dp))

                                        if (isActive) {
                                            // Connected / connecting: show live status + Disconnect.
                                            val label = when (status) {
                                                XServerDialogState.CastStatus.CONNECTING -> I18n.s(R.string.ui_connecting)
                                                XServerDialogState.CastStatus.CONNECTED  -> "Connected"
                                                XServerDialogState.CastStatus.FAILED     -> I18n.s(R.string.ui_failed_to_connect)
                                                else -> ""
                                            }
                                            val color = when (status) {
                                                XServerDialogState.CastStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                                                XServerDialogState.CastStatus.FAILED    -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (status == XServerDialogState.CastStatus.CONNECTING)
                                                    CircularProgressIndicator(Modifier.size(14.dp).padding(end = 6.dp), strokeWidth = 2.dp)
                                                Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            }
                                            if (detail.isNotBlank())
                                                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                            TextButton(onClick = { state.onCastDisconnect?.run() },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                                Text(I18n.s(R.string.ui_disconnect))
                                            }
                                        } else {
                                            // Selected but not connected: explicit Connect button.
                                            TextButton(onClick = { state.onCastConnect?.accept(d) },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                                Text(I18n.s(R.string.xmb_fr_connect))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { state.dismiss() }) { Text(I18n.s(R.string.wrapper_manager_close)) }
                }
            }
        }
    }
}

// The "?" help: explains the ways to get the game onto a TV and the pros/cons of each.
@Composable
private fun CastHelp() {
    Column(modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
        HelpBlock(
            title = I18n.s(R.string.ui_cast_no_app_on_the_tv_this_screen_experimental),
            body = I18n.s(R.string.ui_pick_a_google_tv_or_chromecast_from_the_list_nothing),
            pros = listOf(I18n.s(R.string.ui_nothing_to_set_up_on_the_tv), I18n.s(R.string.ui_uses_devices_you_already_have)),
            cons = listOf(
                I18n.s(R.string.ui_a_few_seconds_of_lag_video_is_buffered_good_for_slower_g),
                I18n.s(R.string.ui_video_only_for_now_the_game_s_sound_stays_on_this_device),
                "Google TV / Chromecast only — Roku isn't listed here"
            )
        )
        HelpBlock(
            title = I18n.s(R.string.ui_cast_with_a_receiver_app_coming_later),
            body = I18n.s(R.string.ui_install_our_small_app_on_the_google_tv_once_then_cast),
            pros = listOf(I18n.s(R.string.ui_crisp_low_lag_good_for_any_game), I18n.s(R.string.ui_phone_stays_free_blank_phone_game_on_tv)),
            cons = listOf(I18n.s(R.string.ui_needs_a_one_time_install_on_the_google_tv), I18n.s(R.string.ui_won_t_work_on_roku_closed_device))
        )
        HelpBlock(
            title = I18n.s(R.string.ui_wired_cable_lowest_lag_today),
            body = I18n.s(R.string.ui_usb_c_hdmi_cable_moves_the_game_to_the_tv_with_almost),
            pros = listOf(I18n.s(R.string.ui_best_quality_and_responsiveness), I18n.s(R.string.ui_no_wi_fi_needed)),
            cons = listOf(I18n.s(R.string.ui_needs_a_cable_and_an_hdmi_port))
        )
        HelpBlock(
            title = I18n.s(R.string.ui_roku_tvs),
            body = I18n.s(R.string.ui_roku_can_t_run_our_app_and_can_t_take_our_stream_use_and) +
                I18n.s(R.string.ui_mirroring_in_settings_with_the_roku_s_screen_mirroring_t),
            pros = listOf(I18n.s(R.string.ui_no_app_needed)),
            cons = listOf(I18n.s(R.string.ui_uses_android_s_system_screen_not_this_in_app_list), I18n.s(R.string.ui_depends_on_your_phone_s_miracast))
        )
    }
}

@Composable
private fun HelpBlock(title: String, body: String, pros: List<String>, cons: List<String>) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
        pros.forEach { Text("✓  $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) }
        cons.forEach { Text("✗  $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
    }
}
