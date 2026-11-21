package com.winhub.hernandez.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.winhub.hernandez.ui.components.Button
import com.winhub.hernandez.ui.components.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.winhub.hernandez.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winhub.hernandez.util.I18n
import com.winhub.hernandez.R

/** Verbatim risk copy for the hard-gate disclaimers (kept in one place so both surfaces match). */
object PerfDisclaimerCopy {
    val ROOT_RISK =
        "⚠️ USE AT YOUR OWN RISK — Power-user controls write directly to your device's system files " +
        "(CPU/GPU clocks, thermal, fan). Misuse can cause overheating, instability, rapid battery " +
        I18n.s(R.string.ui_drain_data_loss_or_permanent_hardware_damage_and_may_voi) +
        I18n.s(R.string.ui_provides_these_tools_with_no_warranty_of_any_kind_you_ac) +
        "outcome."

    val WATCHDOG_OFF =
        "⚠️ DISABLING THERMAL SAFETY — The temperature watchdog is the only automatic protection that " +
        I18n.s(R.string.ui_rolls_your_settings_back_before_the_device_gets_dangerou) +
        I18n.s(R.string.ui_reach_damaging_temperatures_with_no_software_interventio) +
        I18n.s(R.string.ui_own_emergency_trip_remains_do_not_leave_this_off_unatten) +
        I18n.s(R.string.ui_restarts_until_you_turn_it_back_on_yourself_winhub_will)
}

/**
 * Informational dialog — a single "Got it" acknowledgement, NOT a hard gate. Used to explain what a
 * watchdog preset does and the actual °C it resolves to on this device.
 */
@Composable
fun PerfInfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).padding(8.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                // Bounded + scrollable so long explainers (watchdog, the consolidated toggle list) fit.
                Text(
                    body, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp,
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss) { Text(I18n.s(R.string.ui_got_it)) }
                }
            }
        }
    }
}

/**
 * Hard-gate disclaimer: a scrollable risk statement whose confirm button stays disabled until the
 * user has BOTH scrolled to the bottom AND ticked "I understand and accept all risk". Reused for the
 * root grant gate and the watchdog-disable gate.
 */
@Composable
fun PerfDisclaimerDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scroll = rememberScrollState()
    var accepted by remember { mutableStateOf(false) }
    // Content fitting without a scrollbar (maxValue 0) counts as "seen in full"; otherwise require the
    // scroll to actually reach the bottom.
    val reachedBottom by remember {
        derivedStateOf { scroll.maxValue == 0 || scroll.value >= scroll.maxValue - 8 }
    }
    val canConfirm = reachedBottom && accepted

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).padding(8.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)

                Text(
                    body,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(scroll)
                )

                if (!reachedBottom) {
                    Text(I18n.s(R.string.ui_scroll_to_the_end_to_continue),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it }, enabled = reachedBottom)
                    Spacer(Modifier.width(6.dp))
                    Text(I18n.s(R.string.ui_i_understand_and_accept_all_risk),
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (canConfirm) { onConfirm(); } }, enabled = canConfirm) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}
