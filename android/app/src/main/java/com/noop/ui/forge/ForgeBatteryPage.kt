package com.noop.ui.forge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noop.R
import com.noop.ui.AppViewModel
import kotlin.math.roundToInt

/**
 * The strap's battery page, opened from the Today header's battery pill. Shows the charge level,
 * voltage, charging state and firmware, as iOS's strap detail presents them.
 */
@Composable
fun ForgeBatteryPage(viewModel: AppViewModel, onBack: () -> Unit) {
    val live = viewModel.live.collectAsState().value
    val colors = Forge.colors
    val charging = live.charging

    ForgeSceneBackdrop(ForgeBackdrop.Canvas) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(horizontal = ForgeMetrics.screenPadding)) {
                PageTitle(stringResource(R.string.forge_battery_title), onBack)
            }
            Spacer(Modifier.height(ForgeMetrics.space4))
            Column(Modifier.fillMaxWidth().padding(horizontal = ForgeMetrics.screenPadding)) {
                ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius) {
                    BatteryRow(stringResource(R.string.forge_battery_charge),
                        live.batteryPct?.let { "${it.roundToInt()}%" } ?: "—")
                    DividerRow(colors)
                    BatteryRow(stringResource(R.string.forge_battery_voltage),
                        live.batteryMv?.let { "${it} mV" } ?: "—")
                    DividerRow(colors)
                    BatteryRow(stringResource(R.string.forge_battery_status),
                        if (charging == true) stringResource(R.string.forge_battery_charging)
                        else if (charging == false) stringResource(R.string.forge_battery_not_charging)
                        else "—")
                    DividerRow(colors)
                    BatteryRow(stringResource(R.string.forge_battery_firmware),
                        live.strapFirmware ?: "—")
                }
            }
        }
    }
}

@Composable
private fun BatteryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = ForgeMetrics.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Forge.type.subhead, color = Forge.sceneInk.text)
        Text(value, style = Forge.type.subhead, color = Forge.sceneInk.secondary)
    }
}

@Composable
private fun DividerRow(colors: ForgeColors) {
    androidx.compose.material3.HorizontalDivider(
        Modifier.padding(top = ForgeMetrics.space2), color = colors.divider)
}
