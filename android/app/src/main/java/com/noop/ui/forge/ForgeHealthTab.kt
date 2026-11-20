package com.noop.ui.forge

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noop.R
import com.noop.analytics.VitalBands
import com.noop.ui.AppViewModel
import com.noop.ui.NoopPrefs
import com.noop.ui.ProfileAvatar
import com.noop.ui.Sparkline
import com.noop.ui.UnitPrefs
import com.noop.ui.Vital
import com.noop.ui.latestVitals
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fork (DX23876), Forge parity 2026-11: the Health tab of the new iOS design (`ForgeHealthView`). Last night's
 * vitals against the wearer's own range first, then heart and stress, body and energy, and the long-term views
 * (Trends moved here from its own tab, as on iOS).
 *
 * The vitals come from [latestVitals], the same resolver the Health monitor draws its tiles from and the Kotlin
 * twin of iOS's `BodyVitalSigns.readings`, so the two screens always agree on what is in range.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForgeHealthTab(padding: PaddingValues, width: ForgeWidth, viewModel: AppViewModel) {
    val context = LocalContext.current
    val colors = Forge.colors
    val days by viewModel.recentDays.collectAsStateWithLifecycle()
    val spo2CandidateByDay by viewModel.spo2CandidateByDay.collectAsStateWithLifecycle()
    val hrvOverCountByDay by viewModel.hrvOverCountByDay.collectAsStateWithLifecycle()
    var settingsOpen by remember { mutableStateOf(false) }
    // Read again when Settings closes, so a unit or SpO₂ choice made there shows without a restart.
    val tempUnit = remember(settingsOpen) { UnitPrefs.temperature(context) }
    val spo2Toggle = remember(settingsOpen) { NoopPrefs.spo2CandidateDisplay(context) }
    val vitals = remember(days, spo2CandidateByDay, hrvOverCountByDay, tempUnit, spo2Toggle) {
        latestVitals(days, tempUnit, spo2CandidateByDay, spo2Toggle, hrvOverCountByDay = hrvOverCountByDay)
            // The raw optical reading is an experimental instrument; an empty one is noise on this tab.
            .filter { !(it.key == "spo2raw" && it.value == null) }
            .sortedBy { listOf("hrv", "rhr", "spo2", "resp", "skin", "spo2raw").indexOf(it.key).let { i -> if (i < 0) 99 else i } }
    }
    val scored = vitals.filter { it.banding.band != VitalBands.Band.NO_DATA && it.key != "spo2raw" }
    val inRange = scored.count { it.banding.band == VitalBands.Band.IN_RANGE }

    val more = rememberMoreNavigator()
    var trendsOpen by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(ForgeDesign.openTrends) {
        if (ForgeDesign.openTrends) { ForgeDesign.openTrends = false; trendsOpen = true }
    }
    fun open(dest: MoreDest) = more.push(MoreRoute.Dest(dest))
    var editingTiles by remember { mutableStateOf(false) }
    var chosenTiles by remember { mutableStateOf(VitalTilePrefs.chosen(context)) }
    // The tile the picker swaps ("" adds one), null while the picker is closed.
    var tilePick by remember { mutableStateOf<String?>(null) }
    // A tile opens its own measure, as iOS's `TabRoute.metricSourced`: the vital's own page (`VitalDetailScreen`, the
    // Android side of iOS's `MetricDetailView`), else Explore on that measure, or the measure's own screen. Steps
    // follow the chosen steps source there too.
    var vitalKey by rememberSaveable { mutableStateOf<String?>(null) }
    fun openMeasure(key: String) = when (key) {
        "ageless" -> open(MoreDest.AgeLess); "weight" -> open(MoreDest.Body); "stress" -> open(MoreDest.Stress)
        in VITAL_PAGES -> vitalKey = VITAL_PAGES.getValue(key)
        else -> {
            com.noop.ui.ExploreLaunch.pendingKey = when (key) {
                "spo2raw" -> "spo2"; "skin" -> "skin_temp"; "vo2max" -> "vo2max_est"; else -> key
            }
            open(MoreDest.Explore)
        }
    }
    var tileColumns by remember { mutableStateOf(VitalTilePrefs.columns(context, if (width == ForgeWidth.Compact) 2 else 3)) }
    var weeklyOpen by remember { mutableStateOf(false) }
    var cycleOpen by remember { mutableStateOf(false) }
    var cycleLogFirst by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf<FocusArea?>(null) }
    val selectTab = LocalForgeTabSelect.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)
            .padding(horizontal = ForgeMetrics.screenPadding),
        verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3),
    ) {
        val openMore = LocalForgeOpenMore.current
        val openSettings = LocalForgeOpenSettings.current ?: openMore
        TabHeader(stringResource(R.string.nav_health), onProfile = { openSettings?.invoke() ?: run { settingsOpen = true } },
                  onSearch = rememberOpenSearch())

        // The tab's answer: how many of last night's vitals sat in the wearer's own range, then each by name.
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
            Text(stringResource(R.string.forge_health_vitals_last_night), style = Forge.type.subhead,
                 color = Forge.sceneInk.secondary)
            Text(if (scored.isEmpty()) stringResource(R.string.forge_health_no_readings)
                 else stringResource(R.string.forge_health_in_range, inRange, scored.size),
                 style = Forge.type.title2.copy(fontWeight = FontWeight.Bold), color = Forge.sceneInk.text)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                scored.forEach { v -> RangeChip(v) }
            }
        }
        // The first vital outside the range, told in a card of its own under the chips, as iOS's `offRangeCard`.
        // Coach chips inside the off-range card, as iOS's  draws them when the coach is on.
        val coachOn = NoopPrefs.coachEnabled(context) && com.noop.ai.AiKeyStore.readConsent(context)
        scored.firstOrNull { it.banding.band == VitalBands.Band.OUT_OF_RANGE }?.let { v ->
            OffRangeCard(v, coachOn) { prompt ->
                com.noop.ui.CoachHandoff.pendingPrompt = prompt
                selectTab(ForgeTab.Coach)
            }
        }

        // Vitals with "Edit", as iOS: the tiles the wearer keeps, in their order, two or three to a row.
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) { Heading(stringResource(R.string.forge_health_vitals)) }
            if (!editingTiles) Text(stringResource(R.string.forge_hv_edit), style = Forge.type.subhead, color = colors.accent,
                                    modifier = Modifier.clickable(role = Role.Button) { editingTiles = true }.padding(4.dp))
        }
        val columns = tileColumns.coerceIn(2, 3)
        if (editingTiles) VitalsEditHeader(columns, onColumns = { tileColumns = it; VitalTilePrefs.setColumns(context, it) },
                                           onDone = { editingTiles = false })
        // The wearer's tiles in their order (iOS `ForgeVitalTileStore`): until edited, the night's vitals as before.
        val placed = (chosenTiles ?: vitals.map { it.key }).take(VitalTilePrefs.MAX)
        fun save(list: List<String>) { chosenTiles = list.distinct().take(VitalTilePrefs.MAX); VitalTilePrefs.setChosen(context, chosenTiles!!) }
        // Reordering by drag while editing, as iOS's `.draggable` tiles: hold a tile, carry it over another, and the two
        // trade places at once. Bounds are measured in the window, unclipped, as on Today.
        val tileBounds = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
        var carried by remember { mutableStateOf<String?>(null) }
        var carryOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        fun swapUnderCarried() {
            val key = carried ?: return
            val from = tileBounds[key] ?: return
            val centre = from.center + carryOffset
            val target = tileBounds.entries.firstOrNull { it.key != key && it.value.contains(centre) }?.key ?: return
            val list = placed.toMutableList()
            val a = list.indexOf(key); val b = list.indexOf(target)
            if (a < 0 || b < 0) return
            list[a] = target; list[b] = key
            val to = tileBounds[target] ?: return
            carryOffset -= (to.topLeft - from.topLeft)
            save(list)
        }
        placed.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth().zIndex(if (carried in row) 1f else 0f), horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
                row.forEachIndexed { col, key ->
                    val index = rowIndex * columns + col
                    androidx.compose.runtime.key(key) {
                    val lifted = carried == key
                    Box(Modifier.weight(1f)
                            .onGloballyPositioned { tileBounds[key] = androidx.compose.ui.geometry.Rect(it.positionInWindow(), it.size.toSize()) }
                            .zIndex(if (lifted) 1f else 0f)
                            .graphicsLayer { if (lifted) { translationX = carryOffset.x; translationY = carryOffset.y; scaleX = 1.04f; scaleY = 1.04f } }
                            .then(if (!editingTiles) Modifier else Modifier.pointerInput(key, placed) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { carried = key; carryOffset = androidx.compose.ui.geometry.Offset.Zero },
                                    onDragEnd = { carried = null; carryOffset = androidx.compose.ui.geometry.Offset.Zero },
                                    onDragCancel = { carried = null; carryOffset = androidx.compose.ui.geometry.Offset.Zero },
                                    onDrag = { change, amount -> change.consume(); carryOffset += amount; swapUnderCarried() })
                            })) {
                        val vital = vitals.firstOrNull { it.key == key }
                        val tap = { if (editingTiles) tilePick = key else openMeasure(key) }
                        if (vital != null) VitalTile(vital, Modifier.fillMaxWidth(), columns > 2) { tap() }
                        else MeasureTile(viewModel, key, Modifier.fillMaxWidth(), columns > 2) { tap() }
                        if (editingTiles) {
                            RemoveBadge(Modifier.align(Alignment.TopEnd)) { save(placed - key) }
                            Row(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                                if (index > 0) MoveButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.forge_vt_earlier)) {
                                    save(placed.toMutableList().apply { add(index - 1, removeAt(index)) })
                                }
                                if (index < placed.lastIndex) MoveButton(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.forge_vt_later)) {
                                    save(placed.toMutableList().apply { add(index + 1, removeAt(index)) })
                                }
                            }
                        }
                    }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (editingTiles && placed.size < VitalTilePrefs.MAX) {
            Row(Modifier.fillMaxWidth().background(colors.surface, androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button) { tilePick = "" }.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.forge_hv_add), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.accent)
            }
        }
        tilePick?.let { replacing ->
            VitalTilePicker(replacing.ifEmpty { null }, placed, vitals, onPick = { picked ->
                save(if (replacing.isEmpty()) placed + picked else placed.map { if (it == replacing) picked else it })
                tilePick = null
            }, onDismiss = { tilePick = null })
        }

        // The cycle at a glance, only while cycle tracking is on and a period is logged (iOS `ForgeCycleSummaryCard`).
        val cycleOn by viewModel.cycleTrackingEnabled.collectAsStateWithLifecycle()
        val periodStarts by viewModel.periodStarts.collectAsStateWithLifecycle()
        if (cycleOn && periodStarts.isNotEmpty()) {
            if (ForgeDesign.preview) ForgeCycleSummaryCard(viewModel, onOpen = { cycleOpen = true }, onLog = { cycleLogFirst = true; cycleOpen = true })
            else TodayCycleForge(viewModel)
        }

        // Today's food log, one tap from the page, as iOS's `FoodHealthCard`.
        FoodTodayCard(onOpen = { open(MoreDest.Food) }, onAdd = { FoodArrival.requestAdd(); open(MoreDest.Food) })

        // The focus areas as six tiles, as iOS: each lists its pages; Sleep opens the Sleep tab.
        Heading(stringResource(R.string.forge_hf_focus))
        HealthFocusGrid { area -> focus = area }

        Heading(stringResource(R.string.forge_health_long_term))
        val longTerm = listOf(MoreDest.AgeLess, MoreDest.RhythmAge).filter { it.shown(context) }
        if (longTerm.isNotEmpty()) DestCard(longTerm) { open(it) }
        HealthWeekInRings(days) { trendsOpen = true }
        ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
            // The week read back, as iOS's first row of the group (`ForgeRoute.weeklyReport`).
            HubRow(Icons.Rounded.CalendarMonth, colors.accent, stringResource(R.string.forge_hv_weekly),
                   stringResource(R.string.forge_hv_weekly_sub), compact = false, onClick = { weeklyOpen = true })
            HubDivider()
            HubRow(Icons.Rounded.ShowChart, colors.charge, stringResource(R.string.nav_trends),
                   stringResource(R.string.forge_health_trends_sub), compact = false, onClick = { trendsOpen = true })
            HubDivider()
            // What works sits between them, as iOS's Long term group lists it.
            if (MoreDest.Insights.shown(context)) {
                HubRow(Icons.Rounded.Lightbulb, ForgeTokens.Color.systemYellow.resolve(), stringResource(R.string.forge_health_what_works),
                       stringResource(R.string.forge_health_what_works_sub), compact = false, onClick = { open(MoreDest.Insights) })
                HubDivider()
            }
            HubRow(Icons.Rounded.GridView, colors.accent, stringResource(R.string.forge_health_all_metrics),
                   stringResource(R.string.forge_health_all_metrics_sub), compact = false, onClick = { open(MoreDest.Explore) })
        }
        Spacer(Modifier.height(ForgeMetrics.space4))
    }

    focus?.let { area ->
        ForgeOverlayPage(onDismiss = { focus = null }) {
            ForgeSceneBackdrop(ForgeBackdrop.Canvas) {
                androidx.activity.compose.BackHandler { focus = null }
                FocusAreaPage(area, viewModel, onBack = { focus = null }, onOpen = { focus = null; open(it) },
                              onOpenVital = { vitalKey = it },
                              onAction = { a -> focus = null; when (a) { "sleep" -> selectTab(ForgeTab.Sleep); "trends" -> trendsOpen = true
                                  "cycle" -> cycleOpen = true } })
            }
        }
    }
    if (weeklyOpen) {
        ForgeOverlayPage(onDismiss = { weeklyOpen = false }) {
            ForgeSceneBackdrop(ForgeBackdrop.Canvas) {
                androidx.activity.compose.BackHandler { weeklyOpen = false }
                ForgeWeeklyReportPage(viewModel, onBack = { weeklyOpen = false },
                    onAsk = if (com.noop.ui.BottomBarStyleStore.coachEnabled) { prompt ->
                        com.noop.ui.CoachHandoff.pendingPrompt = prompt; weeklyOpen = false; selectTab(ForgeTab.Coach)
                    } else null)
            }
        }
    }
    // The cycle, from a focus link or the cycle card: the new design's cycle page, the classic tracker otherwise.
    if (cycleOpen && ForgeDesign.preview) {
        ForgeCycleOverlay(viewModel, logFirst = cycleLogFirst, onDismiss = { cycleOpen = false; cycleLogFirst = false })
    } else if (cycleOpen) {
        val v5 by viewModel.v5Signals.collectAsStateWithLifecycle()
        val starts by viewModel.periodStarts.collectAsStateWithLifecycle()
        v5?.cycle?.let { cycle ->
            com.noop.ui.CycleTrackerDialog(result = cycle, starts = starts, onLog = viewModel::logPeriodStart,
                onDelete = viewModel::deletePeriodStart, onDeleteAll = viewModel::deleteAllPeriodStarts,
                onDismiss = { cycleOpen = false })
        } ?: run { cycleOpen = false }
    }
    MoreOverlay(more, viewModel, onOpenSettings = { settingsOpen = true })
    vitalKey?.let { key ->
        // "Ask coach" while the coach is on and may read card figures, as iOS's `CoachCardButton` hides itself
        // otherwise; the page's figures go to the chat, which opens in its tab.
        val coachOn = com.noop.ui.NoopPrefs.coachEnabled(context) && com.noop.ai.AiKeyStore.readConsent(context)
        val purposesOn by produceState(false, coachOn) {
            value = coachOn && withContext(Dispatchers.IO) { com.noop.core.CoreCoach.cardAllowed() }
        }
        val ask: ((String, String) -> Unit)? = if (!purposesOn) null else { title, summary ->
            com.noop.ui.CoachHandoff.pendingCard = title to summary; vitalKey = null; selectTab(ForgeTab.Coach)
        }
        ForgeOverlayPage(onDismiss = { vitalKey = null }) {
            ForgeSceneBackdrop(ForgeBackdrop.Sky) {
                Hosted(null, onBack = { vitalKey = null }) { com.noop.ui.VitalDetailScreen(viewModel, key, ask) }
            }
        }
    }
    if (trendsOpen) {
        // On a backdrop of its own, clear of the system bars, with a way back: drawn bare, the Health tab and the
        // tab bar showed through it.
        ForgeOverlayPage(onDismiss = { trendsOpen = false }) {
            ForgeSceneBackdrop(ForgeBackdrop.Sky) {
                ForgeTrendsScreen(WindowInsets.systemBars.asPaddingValues(), width, viewModel, onBack = { trendsOpen = false })
            }
        }
    }
    if (settingsOpen) {
        ForgeOverlayPage(onDismiss = { settingsOpen = false }) { ForgeSettings(viewModel, onClose = { settingsOpen = false }) }
    }
}

/** A tab's own header row, as iOS's `ForgeLargeHeader`: the title with today's date, the profile at the end. */
@Composable
internal fun TabHeader(title: String, onProfile: () -> Unit, onSearch: (() -> Unit)? = null) {
    val locale = Locale.getDefault()
    Row(Modifier.fillMaxWidth().padding(top = ForgeMetrics.space4), verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(title) }
                withStyle(Forge.type.subhead.toSpanStyle().copy(color = Forge.sceneInk.secondary)) {
                    append("  ")
                    append(LocalDate.now().format(DateTimeFormatter.ofPattern(
                        DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale)))
                }
            },
            style = Forge.type.title2, color = Forge.sceneInk.text, maxLines = 1, modifier = Modifier.weight(1f),
        )
        // The magnifier before the profile, as iOS's `ForgeSearchButton` in each tab's header.
        if (onSearch != null) {
            Box(Modifier.size(ForgeMetrics.minHitTarget).clickable(role = Role.Button, onClick = onSearch),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.forge_more_search),
                     tint = Forge.sceneInk.text, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(ForgeMetrics.space1))
        }
        ProfileAvatar(size = 32.dp, contentDescription = stringResource(R.string.forge_qa_profile_settings),
                      modifier = Modifier.clickable(role = Role.Button, onClick = onProfile))
    }
}
/** The magnifier action in the new design: opens the dedicated search overlay, as iOS pushes `ForgeRoute.search`. Null in the shipped design. */
@Composable
internal fun rememberOpenSearch(): (() -> Unit)? {
    val openSearch = LocalForgeOpenSearch.current ?: return null
    return openSearch
}

/** A section's title above its card, as iOS's `ForgeSectionHeading`: bold, sentence case. */
@Composable
private fun Heading(text: String) {
    Text(text, style = Forge.type.title2, color = Forge.sceneInk.text,
         modifier = Modifier.padding(top = ForgeMetrics.space3))
}

@Composable
private fun RangeChip(v: Vital) {
    val colors = Forge.colors
    val ok = v.banding.band == VitalBands.Band.IN_RANGE
    Row(
        Modifier.heightIn(min = 30.dp)
            .background(if (ok) colors.surface.copy(alpha = 0.75f) else colors.effort.copy(alpha = 0.16f), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Error, contentDescription = null,
             tint = if (ok) colors.charge else colors.effort, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(v.label, style = Forge.type.footnote.copy(fontWeight = FontWeight.SemiBold),
             color = if (ok) colors.text else colors.effort)
    }
}

/** A vital outside the wearer's range: which, last night's figure with the yardstick's word, and its course. */
@Composable
private fun OffRangeCard(v: Vital, coachOn: Boolean, onAsk: (String) -> Unit) {
    val colors = Forge.colors
    val state = stringResource(if (v.banding.basis == VitalBands.Basis.PERSONAL) R.string.forge_h_off_baseline
                               else R.string.forge_h_off_typical)
    ForgeCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.forge_h_off_label), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold),
                 color = colors.accent)
        }
        Spacer(Modifier.height(ForgeMetrics.space2))
        Text(stringResource(R.string.forge_h_off_title, v.label), style = Forge.type.title2.copy(fontWeight = FontWeight.Bold),
             color = colors.text)
        v.value?.let { value ->
            val figure = v.format(value)
            val sentence = stringResource(R.string.forge_h_off_last_night, figure, v.unit, state)
            // The figure and its unit bold, wherever the language puts them in the sentence.
            val bold = "$figure ${v.unit}".trim()
            val at = sentence.indexOf(bold)
            Spacer(Modifier.height(ForgeMetrics.space1))
            Text(buildAnnotatedString {
                append(sentence)
                if (at >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.text), at, at + bold.length)
            }, style = Forge.type.body, color = colors.textSecondary)
        }
        if (v.sparkline.size > 1) {
            Spacer(Modifier.height(ForgeMetrics.space3))
            Sparkline(v.sparkline, color = colors.effort)
        }
        // The coach's quick questions, as iOS's offRangeCard chips: cause, then whether to rest.
        if (coachOn) {
            Spacer(Modifier.height(ForgeMetrics.space3))
            val causeTitle = stringResource(R.string.forge_h_off_cause)
            val restTitle = stringResource(R.string.forge_h_off_rest)
            val causePrompt = stringResource(R.string.forge_h_off_cause_prompt, v.label)
            val restPrompt = stringResource(R.string.forge_h_off_rest_prompt, v.label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeChip(causeTitle) { onAsk(causePrompt) }
                ForgeChip(restTitle) { onAsk(restPrompt) }
            }
        }
    }
}


/** The icon for a measure, as iOS's . */
/** One vital tile, as iOS's vitalTile: label, figure with unit, trend line and a state badge. */
@Composable
private fun VitalTile(v: Vital, modifier: Modifier, compact: Boolean, onClick: () -> Unit) {
    val badge = if (compact) null else vitalBadge(v)
    ForgeMetricTile(
        label = if (compact) v.label else vitalPlainLabel(v.key, v.label),
        value = v.value?.let(v.format),
        unit = v.unit,
        tint = v.metricColor,
        trend = v.sparkline,
        badge = badge,
        minHeight = if (compact) 96.dp else 132.dp,
        onClick = onClick,
        modifier = modifier,
    )
}

/** The vital's state badge, as iOS's badge(r): the state word and its tone. */
@Composable
private fun vitalBadge(v: Vital): Pair<String, ForgeBadgeTone>? {
    return when {
        v.key == "spo2raw" -> stringResource(R.string.forge_h_badge_uncalibrated) to ForgeBadgeTone.Neutral
        v.banding.band == VitalBands.Band.NO_DATA -> stringResource(R.string.forge_tl_no_data) to ForgeBadgeTone.Neutral
        v.isEstimate -> stringResource(R.string.forge_h_badge_estimated) to ForgeBadgeTone.Neutral
        else -> {
            val personal = v.banding.basis == VitalBands.Basis.PERSONAL
            val inRange = v.banding.band == VitalBands.Band.IN_RANGE
            when {
                inRange -> stringResource(if (personal) R.string.forge_h_badge_in_range else R.string.forge_h_badge_typical) to ForgeBadgeTone.Good
                else -> stringResource(if (personal) R.string.forge_h_off_baseline else R.string.forge_h_off_typical) to ForgeBadgeTone.Attention
            }
        }
    }
}

/** The full label for a vital, as iOS's plainLabel: the shared labels are abbreviated for dense screens. */
@Composable
private fun vitalPlainLabel(key: String, defaultLabel: String): String {
    return when (key) {
        "rhr" -> stringResource(R.string.forge_h_vital_rhr_full)
        "resp" -> stringResource(R.string.forge_h_vital_resp_full)
        "spo2", "spo2raw" -> stringResource(R.string.forge_h_vital_spo2_full)
        "skin" -> stringResource(R.string.forge_h_vital_skin_full)
        else -> defaultLabel
    }
}

/** The wearer's vital tiles in order and the tiles per row, as iOS's `ForgeVitalTileStore` keeps them (same keys). */
internal object VitalTilePrefs {
    private const val CHOSEN = "forge.health.vitalTiles"
    private const val COLUMNS = "forge.health.vitalColumns"
    const val MAX = 9
    /** Every tile the grid can show, as iOS's `ForgeVitalTile`: the nightly vitals, then the measures beside them. */
    val vitalKeys = listOf("hrv", "rhr", "spo2", "resp", "skin", "spo2raw")
    val measureKeys = listOf("ageless", "vo2max", "weight", "stress", "steps")
    /** Null until the wearer edits the tiles. */
    fun chosen(context: android.content.Context): List<String>? =
        NoopPrefs.of(context).getString(CHOSEN, null)?.split(",")?.filter { it in vitalKeys || it in measureKeys }
    fun setChosen(context: android.content.Context, keys: List<String>) =
        NoopPrefs.of(context).edit().putString(CHOSEN, keys.joinToString(",")).apply()
    fun columns(context: android.content.Context, fallback: Int): Int = NoopPrefs.of(context).getInt(COLUMNS, fallback)
    fun setColumns(context: android.content.Context, n: Int) = NoopPrefs.of(context).edit().putInt(COLUMNS, n).apply()
}

@Composable
private fun measureTitle(key: String): String = when (key) {
    "ageless" -> "AgeLess"; "vo2max" -> stringResource(R.string.forge_vt_vo2); "weight" -> stringResource(R.string.forge_vt_weight)
    "stress" -> stringResource(R.string.forge_vt_stress); else -> stringResource(R.string.forge_vt_steps)
}

/** A measure beside the vitals, its latest value and two months as a line (iOS `ForgeHealthMeasureTile`). */
@Composable
private fun MeasureTile(viewModel: AppViewModel, key: String, modifier: Modifier, compact: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val revision by com.noop.core.CoreShadow.dataRevision.collectAsState()
    val values by androidx.compose.runtime.produceState(emptyList<Double>(), key, revision) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (key == "ageless") runCatching { com.noop.core.CoreAgeLess.snapshot(context)?.weeks?.map { it.age } }.getOrNull().orEmpty()
            else {
                val (k, src) = when (key) { "vo2max" -> "vo2max_est" to "my-whoop"; "weight" -> "weight" to "noop-weight"
                                            "stress" -> "stress" to "my-whoop"; else -> "steps" to "my-whoop" }
                val today = LocalDate.now()
                runCatching { viewModel.repo.resolvedSeries(k, src, today.minusDays(60).toString(), today.toString(), strapDeviceId = viewModel.activeStrapId).points.map { it.value } }
                    .getOrDefault(emptyList())
            }
        }
    }
    val last = values.lastOrNull()
    val text = last?.let { v -> when (key) {
        "stress" -> stringResource(when { v < 1.0 -> R.string.forge_vt_low; v < 2.0 -> R.string.forge_vt_medium; else -> R.string.forge_vt_high })
        "steps" -> java.text.NumberFormat.getIntegerInstance().format(Math.round(v))
        "ageless" -> Math.round(v).toString()
        else -> "%.1f".format(java.util.Locale.getDefault(), v)
    } }
    val unit = when (key) { "weight" -> "kg"; "ageless" -> stringResource(R.string.forge_vt_years); else -> null }
    ForgeMetricTile(
        label = measureTitle(key),
        value = text,
        unit = unit,
        tint = measureTint(key),
        trend = values.takeIf { it.size > 1 }?.takeLast(60),
        minHeight = if (compact) 96.dp else 132.dp,
        onClick = onClick,
        modifier = modifier,
    )
}

/** The measure's tint, as iOS's `ForgeMeasureWidget.tint`. */
@Composable
private fun measureTint(key: String): Color {
    return when (key) {
        "stress" -> ForgeTokens.StrandPalette.effortColor.resolve()
        "weight" -> ForgeTokens.StrandPalette.metricAmber.resolve()
        "steps" -> ForgeTokens.StrandPalette.chargeColor.resolve()
        else -> Forge.colors.accent
    }
}

@Composable
private fun MoveButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.padding(2.dp).size(26.dp).background(Forge.colors.textSecondary, CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Forge.colors.surface, modifier = Modifier.size(14.dp))
    }
}

/** The tiles to add or to swap [replacing] for, as iOS's picker: Vitals, then Measures; placed ones are ticked. */
@Composable
private fun VitalTilePicker(replacing: String?, placed: List<String>, vitals: List<Vital>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = Forge.colors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.background(colors.surface, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).padding(20.dp)
                   .verticalScroll(rememberScrollState())) {
            Text(replacing?.let { r -> stringResource(R.string.forge_vt_swap, vitals.firstOrNull { it.key == r }?.label ?: measureTitle(r)) }
                     ?: stringResource(R.string.forge_hv_add), style = Forge.type.headline, color = colors.text)
            listOf(R.string.forge_vt_vitals to VitalTilePrefs.vitalKeys.filter { k -> vitals.any { it.key == k } },
                   R.string.forge_vt_measures to VitalTilePrefs.measureKeys.filter { it != "ageless" || MoreDest.AgeLess.shown(LocalContext.current) })
                .forEach { (header, keys) ->
                    Text(stringResource(header).uppercase(), style = Forge.type.overline, color = colors.textSecondary,
                         modifier = Modifier.padding(top = ForgeMetrics.space4, bottom = ForgeMetrics.space2))
                    keys.forEach { k ->
                        val isPlaced = k in placed
                        Row(Modifier.fillMaxWidth().clickable(enabled = !isPlaced, role = Role.Button) { onPick(k) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(vitals.firstOrNull { it.key == k }?.label ?: measureTitle(k), style = Forge.type.body.copy(fontWeight = FontWeight.SemiBold),
                                 color = colors.text, modifier = Modifier.weight(1f))
                            Icon(if (isPlaced) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircle, contentDescription = null,
                                 tint = if (isPlaced) colors.textTertiary else colors.accent)
                        }
                    }
                }
            Text(stringResource(R.string.forge_hv_done), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.accent,
                 modifier = Modifier.align(Alignment.End).clickable(role = Role.Button, onClick = onDismiss).padding(top = ForgeMetrics.space3))
        }
    }
}

/** "Edit Vitals" with Done and the tiles per row, as iOS's editing header. */
@Composable
private fun VitalsEditHeader(columns: Int, onColumns: (Int) -> Unit, onDone: () -> Unit) {
    val colors = Forge.colors
    ForgeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.forge_hv_edit_vitals), style = Forge.type.headline, color = colors.text, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.forge_hv_done), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = androidx.compose.ui.graphics.Color.White,
                 modifier = Modifier.background(colors.accent, CircleShape).clickable(role = Role.Button, onClick = onDone)
                     .padding(horizontal = 16.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(ForgeMetrics.space3))
        Text(stringResource(R.string.forge_hv_per_row), style = Forge.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
        Spacer(Modifier.height(ForgeMetrics.space1))
        Row(Modifier.fillMaxWidth().background(colors.inset, androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).padding(2.dp)) {
            listOf(2, 3).forEach { n ->
                Text(n.toString(), style = Forge.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.text,
                     textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                     modifier = Modifier.weight(1f).background(if (n == columns) colors.surface else androidx.compose.ui.graphics.Color.Transparent,
                                                               androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                         .clickable(role = Role.Tab) { onColumns(n) }.padding(vertical = 6.dp))
            }
        }
    }
}

/** The small cross in a tile's corner while editing. */
@Composable
private fun RemoveBadge(modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.padding(4.dp).size(26.dp).background(Forge.colors.textSecondary, CircleShape)
            .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Close, stringResource(R.string.forge_hv_remove), tint = Forge.colors.surface, modifier = Modifier.size(14.dp))
    }
}

/** Vital tiles whose measure has a page of its own, by tile key, to the page's key. */
private val VITAL_PAGES = mapOf("rhr" to "rhr", "hrv" to "hrv", "resp" to "resp", "skin" to "skin",
                                "spo2" to "spo2", "spo2raw" to "spo2", "vo2max" to "vo2max_est", "steps" to "steps_est")
