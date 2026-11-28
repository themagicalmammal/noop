package com.noop.ui.forge

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    viewModel: com.noop.ui.AppViewModel,
    onOpenPage: (ForgeSettingsPage) -> Unit,
    onOpenSub: (ForgeSettingsSub) -> Unit,
) {
    val colors = Forge.colors
    val live by viewModel.live.collectAsStateWithLifecycle()
    val activeName by viewModel.activeDeviceName.collectAsStateWithLifecycle()

    // Device row: shows the active strap's connection state + name, opens Devices
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onOpenSub(ForgeSettingsSub.Devices) })
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
                activeName ?: stringResource(R.string.forge_device_row_title),
                style = Forge.type.title2,
                color = colors.text
            )
            Text(
                if (live.connected) {
                    val bat = live.batteryPct?.let { " · Battery ${it.toInt()}%" } ?: ""
                    stringResource(R.string.forge_strap_connected) + bat
                } else {
                    stringResource(R.string.forge_strap_disconnected)
                },
                style = Forge.type.caption,
                color = colors.textSecondary
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        HubRow(
            Icons.Rounded.SettingsInputAntenna,
            ForgeTokens.Color.systemGreen.resolve(),
            stringResource(R.string.forge_strap_hub_row_title),
            stringResource(R.string.forge_strap_hub_row_subtitle),
            onClick = { onOpenPage(ForgeSettingsPage.Strap) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.Battery5Bar,
            ForgeTokens.Color.systemGreen.resolve(),
            stringResource(R.string.forge_battery_row_title),
            stringResource(R.string.forge_battery_row_subtitle),
            onClick = { onOpenPage(ForgeSettingsPage.BatteryRecording) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.TouchApp,
            ForgeTokens.Color.systemOrange.resolve(),
            stringResource(R.string.forge_actions_row_title),
            stringResource(R.string.forge_actions_row_subtitle),
            onClick = { onOpenPage(ForgeSettingsPage.StrapActions) }
        )
    }

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        HubRow(
            Icons.Rounded.DeviceHub,
            ForgeTokens.Color.systemIndigo.resolve(),
            stringResource(R.string.forge_devices_row_title),
            stringResource(R.string.forge_devices_row_subtitle),
            onClick = { onOpenSub(ForgeSettingsSub.Devices) }
        )
    }
}

/**
 * Forge-native What's New page: the in-app changelog. Mirrors Swift's WhatsNewSheet.
 * Uses LazyColumn for performance (the changelog has 200+ releases).
 */
@Composable
internal fun ForgeWhatsNewSection(onBack: () -> Unit) {
    val colors = Forge.colors
    SettingsColumn {
        PageTitle(stringResource(R.string.l10n_whats_new_sheet_noop_appchangelog_current_version_05dae27c, com.noop.ui.AppChangelog.CURRENT_VERSION), onBack)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(ForgeMetrics.gap),
        ) {
            // Expectations card.
            item {
                ForgeCard(Modifier.fillMaxWidth(), fill = colors.heroFill) {
                    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
                        Text("What to expect",
                             style = Forge.type.body.copy(fontWeight = FontWeight.SemiBold),
                             color = colors.text)
                        com.noop.ui.AppChangelog.expectations.forEach { e ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space3),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    e.icon,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.padding(top = 2.dp).size(22.dp),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space1),
                                ) {
                                    Text(e.title, style = Forge.type.headline, color = colors.text)
                                    Text(e.body, style = Forge.type.subhead, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
            // Release cards.
            itemsIndexed(com.noop.ui.AppChangelog.releases) { index, release ->
                ForgeReleaseCard(release, isLatest = index == 0)
                if (index < com.noop.ui.AppChangelog.releases.lastIndex) ForgeDivider()
            }
        }
    }
}

@Composable
private fun ForgeReleaseCard(release: com.noop.ui.AppChangelog.Release, isLatest: Boolean = false) {
    val colors = Forge.colors
    ForgeCard(Modifier.fillMaxWidth(), fill = if (isLatest) colors.heroFill else null) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space2),
            ) {
                Text(
                    "v${release.version}",
                    style = Forge.type.captionNumber,
                    color = colors.accent,
                )
                Text(
                    release.title,
                    style = Forge.type.headline,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(release.date, style = Forge.type.caption, color = colors.textTertiary)
            }
            release.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space2),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.3f)),
                    )
                    Text(item, style = Forge.type.subhead, color = colors.textSecondary)
                }
            }
        }
    }
}

/**
 * The grouped Devices and data sources page: everything that feeds NOOP, and which source each of
 * today's values uses. Matches iOS `ForgeDevicesSourcesView`.
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
            stringResource(R.string.forge_ds_devices_title),
            stringResource(R.string.forge_ds_devices_subtitle),
            onClick = { onOpenSub(ForgeSettingsSub.Devices) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.DeviceHub,
            ForgeTokens.Color.systemIndigo.resolve(),
            stringResource(R.string.forge_ds_data_sources_title),
            stringResource(R.string.forge_ds_data_sources_subtitle),
            onClick = { onOpenSub(ForgeSettingsSub.DataSources) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.Favorite,
            ForgeTokens.Color.systemRed.resolve(),
            stringResource(R.string.forge_ds_apple_health_title),
            stringResource(R.string.forge_ds_apple_health_subtitle),
            onClick = { onOpenSub(ForgeSettingsSub.AppleHealth) }
        )
        HubDivider()
        HubRow(
            Icons.Rounded.Watch,
            ForgeTokens.Color.systemBlue.resolve(),
            stringResource(R.string.forge_ds_mi_band_title),
            stringResource(R.string.forge_ds_mi_band_subtitle),
            onClick = { onOpenSub(ForgeSettingsSub.MiBand) }
        )
    }

    ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
        HubRow(
            Icons.Rounded.Layers,
            ForgeTokens.Color.systemPurple.resolve(),
            stringResource(R.string.forge_ds_values_by_source_title),
            stringResource(R.string.forge_ds_values_by_source_subtitle),
            onClick = { onOpenSub(ForgeSettingsSub.ValuesBySource) }
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
    Text(
        stringResource(R.string.forge_steps_auto_note),
        style = Forge.type.caption,
        color = colors.textSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/**
 * Forge-native Limitations page: the plain tri-state capability grid of what NOOP reads live
 * off each strap generation, with a legend. Mirrors Swift's NoopLimitationsView.
 */
private enum class LimitState { FULL, PARTIAL, NONE }

private data class LimitRow(val feature: String, val whoop4: LimitState, val whoop5: LimitState)

private val FORGE_LIMIT_ROWS: List<LimitRow> = listOf(
    LimitRow("Live heart rate", LimitState.FULL, LimitState.FULL),
    LimitRow("HRV (rMSSD)", LimitState.FULL, LimitState.FULL),
    LimitRow("Sleep staging", LimitState.FULL, LimitState.FULL),
    LimitRow("Recovery & strain", LimitState.FULL, LimitState.FULL),
    LimitRow("Respiratory rate", LimitState.PARTIAL, LimitState.PARTIAL),
    LimitRow("Stress (on-device)", LimitState.FULL, LimitState.FULL),
    LimitRow("Workout detection", LimitState.FULL, LimitState.FULL),
    LimitRow("Skin temperature", LimitState.PARTIAL, LimitState.FULL),
    LimitRow("Steps", LimitState.PARTIAL, LimitState.FULL),
    LimitRow("Blood oxygen (SpO₂ %)", LimitState.NONE, LimitState.NONE),
    LimitRow("ECG", LimitState.NONE, LimitState.PARTIAL),
    LimitRow("Blood pressure", LimitState.NONE, LimitState.NONE),
)

@Composable
internal fun ForgeLimitationsSection(onBack: () -> Unit) {
    val colors = Forge.colors
    SettingsColumn {
        PageTitle(stringResource(R.string.nav_noop_limitations), onBack)
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
                Text(stringResource(R.string.forge_limitations_what_noop_reads),
                     style = Forge.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                     color = colors.text)
                // Column header.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.forge_limitations_feature),
                         style = Forge.type.caption, color = colors.textTertiary,
                         modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.forge_limitations_4_0),
                         style = Forge.type.caption, color = colors.textTertiary,
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                         modifier = Modifier.width(48.dp))
                    Text(stringResource(R.string.forge_limitations_5_0_mg),
                         style = Forge.type.caption, color = colors.textTertiary,
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                         modifier = Modifier.width(48.dp))
                }
                FORGE_LIMIT_ROWS.forEachIndexed { idx, row ->
                    if (idx > 0) ForgeDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "${row.feature}: WHOOP 4.0 ${row.whoop4.spoken}, 5.0/MG ${row.whoop5.spoken}"
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.feature, style = Forge.type.body, color = colors.text,
                             modifier = Modifier.weight(1f))
                        SupportCell(row.whoop4, colors)
                        SupportCell(row.whoop5, colors)
                    }
                }
            }
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.forge_limitations_legend),
                     style = Forge.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                     color = colors.text)
                LegendRow(LimitState.FULL, stringResource(R.string.forge_limitations_read_live), colors)
                LegendRow(LimitState.PARTIAL, stringResource(R.string.forge_limitations_on_device), colors)
                LegendRow(LimitState.NONE, stringResource(R.string.forge_limitations_not_strap), colors)
            }
        }
    }
}

@Composable
private fun LegendRow(state: LimitState, label: String, colors: ForgeColors) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
        SupportCell(state, colors)
        Text(label, style = Forge.type.footnote, color = colors.textSecondary,
             modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SupportCell(state: LimitState, colors: ForgeColors) {
    Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        when (state) {
            LimitState.FULL -> androidx.compose.material3.Icon(
                Icons.Filled.Check,
                contentDescription = "yes",
                tint = colors.charge,
                modifier = Modifier.size(18.dp))
            LimitState.PARTIAL -> androidx.compose.material3.Icon(
                Icons.Filled.Remove,
                contentDescription = "partly",
                tint = colors.effort,
                modifier = Modifier.size(18.dp))
            LimitState.NONE -> androidx.compose.material3.Icon(
                Icons.Filled.Close,
                contentDescription = "no",
                tint = colors.missing,
                modifier = Modifier.size(18.dp))
        }
    }
}

/** Spoken support label for the row's accessibility description. */
private val LimitState.spoken: String
    get() = when (this) {
        LimitState.FULL -> "yes"
        LimitState.PARTIAL -> "partly"
        LimitState.NONE -> "no"
    }

/**
 * Forge-native Model Comparison page: what each strap (4.0 vs 5.0/MG) can read,
 * and why. Mirrors Swift's WhoopModelComparisonScreen.
 */
private data class ForgeCapabilityRow(
    val feature: String,
    val whoop4: ForgeSupport,
    val whoop5: ForgeSupport,
    val note: String? = null,
)

private enum class ForgeSupport { YES, NO, PARTIAL }

private val FORGE_CAPABILITIES: List<ForgeCapabilityRow> = listOf(
    ForgeCapabilityRow(
        "Live heart rate",
        ForgeSupport.YES, ForgeSupport.YES,
        "Both stream live HR to NOOP over Bluetooth.",
    ),
    ForgeCapabilityRow(
        "Sleep, recovery & strain history",
        ForgeSupport.YES, ForgeSupport.PARTIAL,
        "The 4.0's history is fully decoded. On a 5/MG, history decoding is experimental: live HR works, " +
            "deeper history is still being mapped.",
    ),
    ForgeCapabilityRow(
        "NOOP re-broadcasts your HR (gym / Zwift / Garmin)",
        ForgeSupport.YES, ForgeSupport.YES,
        "Data Sources → \"Broadcast heart rate\" turns your PHONE into a standard BLE HR sensor using " +
            "whatever HR NOOP is reading, so this works on a 4.0 too. It's local Bluetooth; nothing leaves " +
            "your phone.",
    ),
    ForgeCapabilityRow(
        "Strap broadcasts its own HR",
        ForgeSupport.PARTIAL, ForgeSupport.YES,
        "A 4.0 is asked over its own broadcast command and answers, but nothing can read back whether " +
            "it really advertises, and it has been confirmed on one strap on one firmware. A 5/MG uses " +
            "the whoop_live_hr_in_adv_ind_pkt firmware setting, which NOOP reads back.",
    ),
    ForgeCapabilityRow(
        "Steps",
        ForgeSupport.PARTIAL, ForgeSupport.YES,
        "A 4.0 sends no step count, so NOOP ESTIMATES steps from motion, calibrated to your phone " +
            "(Settings → Profile → Steps estimate). A 5/MG reports a motion counter NOOP reads directly.",
    ),
    ForgeCapabilityRow(
        "Rename the strap's Bluetooth name",
        ForgeSupport.YES, ForgeSupport.NO,
        "Renaming works over the 4.0's firmware command; the 5/MG path isn't supported.",
    ),
    ForgeCapabilityRow(
        "Buzz the strap (alarms, haptics, time)",
        ForgeSupport.YES, ForgeSupport.YES,
    ),
)

@Composable
internal fun ForgeModelComparisonSection(onBack: () -> Unit) {
    val colors = Forge.colors
    SettingsColumn {
        PageTitle(stringResource(R.string.l10n_whoop_model_comparison_screen_4_0_vs_5_0_mg_56099a02), onBack)
        // Intro card.
        ForgeCard(Modifier.fillMaxWidth(), fill = colors.heroFill) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_whoop_model_comparison_screen_both_straps_are_supported_8545557a),
                     style = Forge.type.headline, color = colors.text)
                Text(stringResource(R.string.l10n_whoop_model_comparison_screen_noop_pairs_with_the_whoop_4_599b088c),
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        // Capability table.
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
                Text(stringResource(R.string.l10n_whoop_model_comparison_screen_feature_ad565d9d),
                     style = Forge.type.body.copy(fontWeight = FontWeight.SemiBold),
                     color = colors.text)
                // Column header.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.l10n_whoop_model_comparison_screen_feature_ad565d9d),
                         style = Forge.type.caption, color = colors.textTertiary,
                         modifier = Modifier.weight(1f))
                    Text("4.0", style = Forge.type.caption, color = colors.textTertiary,
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.width(48.dp))
                    Text(stringResource(R.string.l10n_whoop_model_comparison_screen_5_mg_62eaa522),
                         style = Forge.type.caption, color = colors.textTertiary,
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.width(48.dp))
                }
                FORGE_CAPABILITIES.forEachIndexed { idx, cap ->
                    if (idx > 0) ForgeDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space1)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cap.feature, style = Forge.type.body, color = colors.text,
                                 modifier = Modifier.weight(1f))
                            ForgeSupportCell(cap.whoop4, colors)
                            ForgeSupportCell(cap.whoop5, colors)
                        }
                        cap.note?.let {
                            Text(it, style = Forge.type.footnote, color = colors.textTertiary)
                        }
                    }
                }
            }
        }
        // Reassurance card.
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_whoop_model_comparison_screen_on_a_whoop_4_0_32a5968d),
                     style = Forge.type.headline, color = colors.text)
                Text(stringResource(R.string.whoop4_direct_broadcast_explainer),
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ForgeSupportCell(support: ForgeSupport, colors: ForgeColors) {
    Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        when (support) {
            ForgeSupport.YES -> androidx.compose.material3.Icon(
                Icons.Filled.Check,
                contentDescription = "Yes",
                tint = colors.charge,
                modifier = Modifier.size(18.dp))
            ForgeSupport.NO -> androidx.compose.material3.Icon(
                Icons.Filled.Close,
                contentDescription = "No",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp))
            ForgeSupport.PARTIAL -> androidx.compose.material3.Icon(
                Icons.Filled.Remove,
                contentDescription = "Partly",
                tint = colors.effort,
                modifier = Modifier.size(18.dp))
        }
    }
}

private val ForgeSupport.spoken: String
    get() = when (this) {
        ForgeSupport.YES -> "yes"
        ForgeSupport.NO -> "no"
        ForgeSupport.PARTIAL -> "partly"
    }

/**
 * Forge-native How NOOP Works page: the plain-English primer explaining sleep sorting,
 * scores, recording, and provenance. Mirrors Swift's HowNoopWorks primer.
 */
@Composable
internal fun ForgeHowNoopWorksSection(onBack: () -> Unit) {
    val colors = Forge.colors
    SettingsColumn {
        PageTitle(stringResource(R.string.l10n_how_noop_works_screen_how_noop_works_3396b27a), onBack)
        // Intro card.
        ForgeCard(Modifier.fillMaxWidth(), fill = colors.heroFill) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_how_noop_works_screen_the_one_rule_2d7a1b5c),
                     style = Forge.type.body.copy(fontWeight = FontWeight.SemiBold),
                     color = colors.text)
                Text(stringResource(R.string.l10n_how_noop_works_screen_noop_never_shows_you_a_number_d1db9958),
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        // Primer sections.
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_how_noop_works_screen_how_your_sleep_is_sorted_6a8e82e0),
                     style = Forge.type.headline, color = colors.text)
                Text("NOOP picks your main sleep as your longest real block, and (once it has " +
                    "learned your usual hours) the one nearest your normal sleep time. Everything " +
                    "else that day is a nap. You can always edit bed and wake times.",
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_how_noop_works_screen_how_your_scores_work_21a0e2be),
                     style = Forge.type.headline, color = colors.text)
                Text("Charge, Effort and Rest are scored on your own device from your strap data. " +
                    "Charge needs about four nights of sleep to learn your baseline (that's \"Calibrating\", " +
                    "counted as nights of 4 on the ring), and keeps sharpening over your first couple of weeks. " +
                    "On a WHOOP 5 or MG the strap banks little history, so that count can sit at 0 of 4 until you " +
                    "have worn it across a few nights. That's the strap's sync limit, not a fault. " +
                    "Before there's a number, NOOP shows what it can without faking one.",
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_how_noop_works_screen_how_your_scores_are_computed_1b2d6c30),
                     style = Forge.type.headline, color = colors.text)
                Text("Charge weighs five signals against your own baseline: your overnight HRV matters most, " +
                    "then your resting heart rate, how well you slept, your breathing rate, and how far your skin " +
                    "temperature drifted from normal. Higher HRV and lower resting heart rate lift Charge; a big " +
                    "skin-temperature drift in either direction lowers it. Each signal is measured as how far " +
                    "tonight sits from your personal baseline, never an absolute target. If a signal is missing, " +
                    "it's dropped and the rest are reweighted, so the number always reflects only what was " +
                    "actually measured. The \"What shaped it\" breakdown under the Charge ring shows each signal's " +
                    "point contribution.",
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_how_noop_works_screen_what_recording_means_b896c422),
                     style = Forge.type.headline, color = colors.text)
                Text("When your strap is connected NOOP is saving data live. \"Last synced\" tells " +
                    "you how fresh it is. If it says \"Not recording\", reconnect.",
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.l10n_how_noop_works_screen_where_your_numbers_come_from_e169963a),
                     style = Forge.type.headline, color = colors.text)
                Text("A badge shows whether a number was scored on-device by NOOP, or imported " +
                    "from Whoop or Apple Health.",
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }
        // Footer note.
        Text(stringResource(R.string.l10n_how_noop_works_screen_noop_never_makes_up_a_number_da29aca5),
             style = Forge.type.footnote, color = colors.textTertiary)
    }
}
