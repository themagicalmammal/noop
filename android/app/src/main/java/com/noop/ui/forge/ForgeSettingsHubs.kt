package com.noop.ui.forge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.noop.R
import com.noop.core.CorePrefs
import com.noop.core.CoreRead
import com.noop.ui.HostedPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fork (DX23876), Android parity plan: the Forge Settings hub pages, as iOS has them
 * (`StrandiOS/Forge/ForgeSettingsHubs.swift`): one entry where there used to be several, so no
 * setting moves or disappears; only the index gets shorter.
 */

/**
 * The grouped Strap page: connection, steps and heart-rate sharing; recording and battery;
 * double-tap and buzz. Before, these were three top-level Settings rows.
 */
@Composable
internal fun ForgeStrapHubPage(
    onOpenPage: (ForgeSettingsPage) -> Unit,
    onOpenSub: (ForgeSettingsSub) -> Unit,
) {
    val colors = Forge.colors

    // Device row: shows the active strap's connection state, opens Data Sources
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onOpenSub(ForgeSettingsSub.DataSources) })
            .background(colors.surface, RoundedCornerShape(ForgeMetrics.groupedRadius))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Sensors,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(36.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.forge_device_row_title),
                style = Forge.type.title2,
                color = colors.text
            )
            Text(
                stringResource(R.string.forge_device_connect_hint),
                style = Forge.type.caption,
                color = colors.textSecondary
            )
        }
    }

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        HubRow(
            Icons.Rounded.SettingsInputAntenna,
            ForgeTokens.Color.systemGreen.resolve(),
            stringResource(R.string.l10n_settings_screen_strap_02b88eeb),
            stringResource(R.string.forge_settings_strap_subtitle),
            onClick = { onOpenPage(ForgeSettingsPage.Strap) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.Battery5Bar,
            ForgeTokens.Color.systemGreen.resolve(),
            stringResource(R.string.forge_settings_battery),
            stringResource(R.string.forge_settings_battery_subtitle),
            onClick = { onOpenPage(ForgeSettingsPage.BatteryRecording) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.TouchApp,
            ForgeTokens.Color.systemOrange.resolve(),
            stringResource(R.string.forge_settings_actions),
            stringResource(R.string.forge_settings_actions_subtitle),
            onClick = { onOpenPage(ForgeSettingsPage.StrapActions) }
        )
    }

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        HubRow(
            Icons.Rounded.DeviceHub,
            ForgeTokens.Color.systemIndigo.resolve(),
            stringResource(R.string.forge_set_devices_sources),
            stringResource(R.string.forge_set_devices_sources_detail),
            onClick = { onOpenSub(ForgeSettingsSub.DataSources) }
        )
    }
}

/**
 * The grouped Devices and data sources page: everything that feeds NOOP, and which source each of
 * today's values uses.
 */
@Composable
internal fun ForgeDevicesSourcesPage(
    onOpenSub: (ForgeSettingsSub) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stepsSource: String by remember { mutableStateOf("automatic") }

    LaunchedEffect(Unit) {
        stepsSource = withContext(Dispatchers.IO) {
            try {
                CoreRead.open(context)
                CorePrefs.stepsSource()
            } catch (e: Exception) {
                "automatic"
            }
        }
    }

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        HubRow(
            Icons.Rounded.DeviceHub,
            ForgeTokens.Color.systemIndigo.resolve(),
            stringResource(R.string.nav_data_sources),
            stringResource(R.string.forge_settings_data_sources_detail),
            onClick = { onOpenSub(ForgeSettingsSub.DataSources) }
        )
    }

    // Steps source picker: where daily steps come from
    StepsSourcePicker(initial = stepsSource, onChange = { key ->
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    CoreRead.open(context)
                    CorePrefs.setStepsSource(context, key)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    })
}

/**
 * How NOOP scores: the guide in words and the model working on this phone, as two tabs of one page.
 */
@Composable
internal fun ForgeHowScoresPage(
    viewModel: com.noop.ui.AppViewModel,
    onBack: () -> Unit,
) {
    val colors = Forge.colors
    var part: Int by remember { mutableStateOf(0) }
    val tokens = remember(colors) { hostedTokens(colors) }

    HostedPalette(tokens, colors.dark) {
    // Segmented tabs: Explained / Your model
    Row(
        Modifier.background(colors.inset, RoundedCornerShape(10.dp)).padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val labels = listOf(
            stringResource(R.string.forge_how_scores_explained),
            stringResource(R.string.forge_how_scores_your_model),
        )
        labels.forEachIndexed { index, label ->
            val chosen = index == part
            Text(
                label,
                style = Forge.type.footnote,
                color = if (chosen) colors.text else colors.textSecondary,
                maxLines = 1,
                modifier = Modifier
                    .background(if (chosen) colors.surface else colors.inset, RoundedCornerShape(8.dp))
                    .clickable(role = Role.RadioButton) { part = index }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    Spacer(Modifier.height(ForgeMetrics.space2))

    if (part == 0) {
        // Explained: the Scoring Guide
        com.noop.ui.ScoringGuideScreen(onClose = onBack, embedded = true)
    } else {
        // Your model: the Intelligence screen, wrapped in a bounded box so its LazyColumn
        // does not receive infinite height constraints from the scrollable SettingsColumn.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(800.dp)
        ) {
            com.noop.ui.IntelligenceScreen(viewModel)
        }
    }
    }
}

/** The steps source picker: automatic, strap, or phone. */
@Composable
internal fun StepsSourcePicker(initial: String, onChange: (String) -> Unit) {
    var chosen: String by remember { mutableStateOf(initial) }
    val colors = Forge.colors

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        Text(
            stringResource(R.string.forge_steps_from),
            style = Forge.type.body,
            color = colors.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        listOf(
            "automatic" to stringResource(R.string.forge_steps_auto),
            "strap" to stringResource(R.string.forge_steps_strap),
            "phone" to stringResource(R.string.forge_steps_phone),
        ).forEachIndexed { i, (key, label) ->
            if (i > 0) HubDivider()
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.RadioButton) {
                    if (chosen != key) {
                        chosen = key
                        onChange(key)
                    }
                }.padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = Forge.type.body, color = colors.text, modifier = Modifier.weight(1f))
                if (chosen == key) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
