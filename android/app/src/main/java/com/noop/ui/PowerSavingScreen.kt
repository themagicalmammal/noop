package com.noop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noop.R
import com.noop.ui.forge.Forge
import com.noop.ui.forge.ForgeCard
import com.noop.ui.forge.ForgeMetrics
import com.noop.ui.forge.PageTitle
import com.noop.ui.forge.RowDivider
import com.noop.ui.forge.SettingsCard
import com.noop.ui.forge.SettingsColumn
import kotlin.math.roundToInt

/**
 * Power saving + Continuous HRV (#477, 2026-10 Settings reorganisation). The two pull in opposite
 * directions (Continuous HRV is the biggest steady drain on the strap, "Pause HRV capture" is the
 * biggest saving), so they belong on one page. Twin of the Apple `PowerSavingView`.
 *
 * The controls, their prefs and their wiring are UNCHANGED: [AppViewModel.applyPowerSaving] still reads
 * every value from [NoopPrefs], so moving the surface cannot alter behaviour. The master gates the
 * sub-options — the threshold slider, "Pause HRV capture" and "Low refresh" only appear (and only apply)
 * while Power saving is on.
 */
@Composable
fun PowerSavingScreen(vm: AppViewModel) {
    SettingsColumn {
        PageTitle(stringResource(R.string.power_saving), { })
        PowerSavingContent(vm)
    }
}

/** The power-saving content without its own scroll wrapper, for use inside a host SettingsColumn. */
@Composable
internal fun PowerSavingContent(vm: AppViewModel) {
    val context = LocalContext.current
    var powerSaving by remember { mutableStateOf(NoopPrefs.powerSaving(context)) }
    var powerSavingBatteryPct by remember { mutableStateOf(NoopPrefs.powerSavingBatteryPct(context)) }
    var pauseHrvOnPowerSave by remember { mutableStateOf(NoopPrefs.pauseHrvOnPowerSave(context)) }
    var lowRefresh by remember { mutableStateOf(NoopPrefs.lowRefresh(context)) }
    var continuousHrv by remember { mutableStateOf(NoopPrefs.continuousHrv(context)) }
    var continuousHrvOvernight by remember { mutableStateOf(NoopPrefs.continuousHrvOvernight(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
        // Continuous HRV — beside power saving since they pull in opposite directions (#477).
        SettingsCard(
            icon = Icons.Filled.MonitorHeart,
            title = stringResource(R.string.l10n_settings_screen_continuous_hrv_capture_1f0805d8),
            blurb = stringResource(R.string.l10n_settings_screen_keeps_the_detailed_beat_to_beat_87b78edd),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space4),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.l10n_settings_screen_continuous_hrv_capture_1f0805d8), style = Forge.type.subhead, color = Forge.colors.text)
                }
                ToggleCheckbox(
                    checked = continuousHrv,
                    onCheckedChange = {
                        continuousHrv = it
                        vm.setContinuousHrv(it)
                    },
                )
            }
            if (continuousHrv) {
                RowDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space4),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.l10n_settings_screen_overnight_only_05747985), style = Forge.type.subhead, color = Forge.colors.text)
                        Text(
                            stringResource(R.string.l10n_settings_screen_runs_the_continuous_hrv_stream_only_3fed47c5),
                            style = Forge.type.footnote,
                            color = Forge.colors.textTertiary,
                        )
                    }
                    ToggleCheckbox(
                        checked = continuousHrvOvernight,
                        onCheckedChange = {
                            continuousHrvOvernight = it
                            vm.setContinuousHrvOvernight(it)
                        },
                    )
                }
            }
        }
        // #477 Power saving. Two BENIGN battery levers only: the offload-cadence stretch (%-gated) and
        // the HRV-capture pause (Battery-Saver-gated). The riskier connection-priority idle throttle is
        // deliberately not surfaced here — it stays dormant pending on-strap validation (#478).
        SettingsCard(
            icon = Icons.Filled.BatteryStd,
            title = stringResource(R.string.power_saving),
            blurb = stringResource(R.string.power_saving_blurb),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space4),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.power_saving_mode), style = Forge.type.subhead, color = Forge.colors.text)
                    Text(
                        stringResource(R.string.power_saving_mode_desc),
                        style = Forge.type.footnote,
                        color = Forge.colors.textTertiary,
                    )
                }
                ToggleCheckbox(
                    checked = powerSaving,
                    onCheckedChange = {
                        powerSaving = it
                        vm.setPowerSaving(it)
                    },
                )
            }
            if (powerSaving) {
                RowDivider()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.power_saving_kick_in), style = Forge.type.subhead, color = Forge.colors.text)
                        Text(stringResource(R.string.power_saving_pct, powerSavingBatteryPct), style = Forge.type.subhead, color = Forge.colors.accent)
                    }
                    Slider(
                        value = powerSavingBatteryPct.toFloat(),
                        // 10–35% snapping to 5% steps (10/15/20/25/30/35). steps = the 4 stops BETWEEN ends.
                        // 35 buys one more step of strap life than the old 30 ceiling: the levers engage ~5%
                        // earlier in the discharge, at the cost of a slightly longer stretch of quieter syncing.
                        onValueChange = { powerSavingBatteryPct = it.roundToInt() },
                        onValueChangeFinished = { vm.setPowerSavingBatteryPct(powerSavingBatteryPct) },
                        valueRange = 10f..35f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = Forge.colors.accent,
                            activeTrackColor = Forge.colors.accent,
                            inactiveTrackColor = Forge.colors.inset,
                        ),
                    )
                }
                RowDivider()
                // HRV pause: a sub-option of power saving, ON by default when the master is on.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space4),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.power_saving_hrv_pause), style = Forge.type.subhead, color = Forge.colors.text)
                        Text(
                            stringResource(R.string.power_saving_hrv_pause_desc),
                            style = Forge.type.footnote,
                            color = Forge.colors.textTertiary,
                        )
                    }
                    ToggleCheckbox(
                        checked = pauseHrvOnPowerSave,
                        onCheckedChange = {
                            pauseHrvOnPowerSave = it
                            vm.setPauseHrvOnPowerSave(it)
                        },
                    )
                }
                RowDivider()
                // Low refresh: a sub-option that applies at ANY charge, not just below the threshold.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space4),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.power_saving_low_refresh), style = Forge.type.subhead, color = Forge.colors.text)
                        Text(
                            stringResource(R.string.power_saving_low_refresh_desc),
                            style = Forge.type.footnote,
                            color = Forge.colors.textTertiary,
                        )
                    }
                    ToggleCheckbox(
                        checked = lowRefresh,
                        onCheckedChange = {
                            lowRefresh = it
                            vm.setLowRefresh(it)
                        },
                    )
                }
            }
        }
    }
}
