package com.noop.ui.forge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noop.R
import com.noop.data.MetricSeriesRow
import com.noop.ui.AppViewModel
import java.util.Locale
import kotlin.math.roundToInt

private const val MI_DEVICE = "xiaomi-band"

private enum class MiRange(val days: Int?, val label: String, val caption: String, val windowName: String) {
    Week(7, "W", "7 DAYS", "week"),
    Month(30, "M", "30 DAYS", "month"),
    Quarter(90, "3M", "90 DAYS", "3 months"),
    Half(180, "6M", "180 DAYS", "6 months"),
    Year(365, "1Y", "365 DAYS", "year"),
    All(null, "ALL", "ALL TIME", "all history");

    val widening: List<MiRange>
        get() = entries.dropWhile { it != this }
}

private data class MiPoint(val day: String, val value: Double)

private fun List<MiPoint>.windowFor(range: MiRange): List<MiPoint> {
    val n = range.days ?: return this
    if (isEmpty()) return emptyList()
    return takeLast(n)
}

private class MiData(
    val series: Map<String, List<MiPoint>>,
) {
    fun raw(key: String): List<MiPoint> = series[key] ?: emptyList()
    val hasAnyData: Boolean
        get() = series.values.any { it.isNotEmpty() }
}

private data class MiResolved(
    val requested: MiRange,
    val effective: MiRange,
    val rows: List<MiPoint>,
) {
    val fellBack: Boolean get() = effective != requested
}

private fun resolve(series: List<MiPoint>, requested: MiRange): MiResolved {
    if (series.isEmpty()) return MiResolved(requested, requested, emptyList())
    val eff = requested.widening.firstOrNull { series.windowFor(it).isNotEmpty() } ?: MiRange.All
    return MiResolved(requested, eff, series.windowFor(eff))
}

private val SERIES_KEYS = listOf(
    "steps", "distance_m", "energy_kcal", "intensity_min",
    "rhr", "avg_hr", "max_hr", "spo2",
    "sleep_total_min", "sleep_deep_min", "sleep_rem_min", "sleep_light_min",
    "sleep_score", "stress", "vitality",
)

@Composable
internal fun ForgeMiBandSection(vm: AppViewModel, onBack: () -> Unit) {
    val colors = Forge.colors
    var loaded by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf(MiData(emptyMap())) }
    var range by remember { mutableStateOf(MiRange.Quarter) }

    LaunchedEffect(Unit) {
        val series = linkedMapOf<String, List<MiPoint>>()
        for (key in SERIES_KEYS) {
            val pts = runCatching {
                vm.repo.metricSeries(MI_DEVICE, key, "0000-00-00", "9999-99-99")
            }.getOrDefault(emptyList<MetricSeriesRow>())
                .sortedBy { it.day }
                .map { MiPoint(it.day, it.value) }
            series[key] = pts
        }
        data = MiData(series)
        loaded = true
    }

    val subtitle = spanSubtitle(loaded, data, range)

    SettingsColumn {
        PageTitle(stringResource(R.string.forge_ds_mi_band_title), onBack)
        Text(subtitle, style = Forge.type.subhead, color = colors.textSecondary)

        when {
            !loaded -> ForgeLoadingCard()
            !data.hasAnyData -> ForgeEmptyState()
            else -> {
                ForgeRangeControl(data = data, range = range, onSelect = { range = it })
                ForgeTileGrid(data = data, range = range)
                ForgeChartSection("Heart", range) {
                    ForgeMetricChartCard(data, range, "rhr", "Resting heart rate", colors.heart) { "${it.roundToInt()} bpm" }
                    ForgeMetricChartCard(data, range, "avg_hr", "Average heart rate", colors.heart) { "${it.roundToInt()} bpm" }
                    ForgeMetricChartCard(data, range, "max_hr", "Max heart rate", colors.heart) { "${it.roundToInt()} bpm" }
                    ForgeMetricChartCard(data, range, "spo2", "Blood oxygen", colors.charge) { String.format(Locale.US, "%.1f%%", it) }
                }
                ForgeChartSection("Activity", range) {
                    ForgeMetricChartCard(data, range, "steps", "Steps", colors.charge) { intString(it) }
                    ForgeMetricChartCard(data, range, "distance_m", "Distance", colors.accent) { intString(it) + " m" }
                    ForgeMetricChartCard(data, range, "energy_kcal", "Energy", colors.effort) { "${it.roundToInt()} kcal" }
                    ForgeMetricChartCard(data, range, "intensity_min", "Intensity minutes", colors.effort) { "${it.roundToInt()} min" }
                }
                ForgeChartSection("Sleep", range) {
                    ForgeMetricChartCard(data, range, "sleep_total_min", "Total sleep", colors.rest) { durationString(it) }
                    ForgeMetricChartCard(data, range, "sleep_score", "Sleep score", colors.rest) { "${it.roundToInt()}" }
                    ForgeMetricChartCard(data, range, "stress", "Stress", colors.effort) { "${it.roundToInt()}" }
                    ForgeMetricChartCard(data, range, "vitality", "Vitality", colors.charge) { "${it.roundToInt()}" }
                }
            }
        }
    }
}

private fun spanSubtitle(loaded: Boolean, data: MiData, range: MiRange): String {
    if (!loaded) {
        return "Steps, heart, sleep and stress - imported from Mi Fitness."
    }
    val rows = resolve(data.raw("steps"), range).rows
    if (rows.isEmpty()) {
        return "Steps, heart, sleep and stress - imported from Mi Fitness."
    }
    val lo = rows.first().day
    val hi = rows.last().day
    val span = if (lo == hi) lo else "$lo → $hi"
    val unit = if (rows.size == 1) "day" else "days"
    return "${rows.size} $unit · $span"
}

@Composable
private fun ForgeLoadingCard() {
    ForgeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(8.dp).height(8.dp)) {
                // Simple pulsing dot
            }
            Text(
                "Reading your Mi Band history…",
                style = Forge.type.subhead,
                color = Forge.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ForgeEmptyState() {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
            Text("Nothing imported yet", style = Forge.type.title2, color = Forge.colors.text)
            Text("Import your Mi Fitness export in Data Sources to see Mi Band history here.",
                 style = Forge.type.subhead, color = Forge.colors.textSecondary)
        }
    }
}

@Composable
private fun ForgeRangeControl(data: MiData, range: MiRange, onSelect: (MiRange) -> Unit) {
    val colors = Forge.colors
    val stepsRows = resolve(data.raw("steps"), range).rows
    val anyWidened = data.series.any { (_, s) -> s.isNotEmpty() && resolve(s, range).fellBack }
    val n = stepsRows.size
    val unit = if (n == 1) "day" else "days"
    val base = "$n $unit · ${range.windowName}"
    val caption = if (anyWidened) "$base · some sparse series widened" else base

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Simple segmented control
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiRange.entries.forEach { r ->
                    val selected = r == range
                    androidx.compose.foundation.layout.Box(
                        Modifier.width(48.dp).height(32.dp)
                            .clickable { onSelect(r) }
                            .background(
                                if (selected) colors.accent else colors.inset,
                                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                    ) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(r.label, style = Forge.type.caption.copy(
                                color = if (selected) colors.onAccent else colors.textSecondary
                            ))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(range.caption, style = Forge.type.footnote, color = colors.textTertiary)
        }
        Text(
            caption,
            style = Forge.type.footnote,
            color = if (anyWidened) Color(0xFFCC8A00) else colors.textTertiary,
        )
    }
}

@Composable
private fun ForgeTileGrid(data: MiData, range: MiRange) {
    val colors = Forge.colors
    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeMetricTile(Modifier.weight(1f), data, range, "steps", "Steps", colors.charge) { intString(it) }
            ForgeMetricTile(Modifier.weight(1f), data, range, "rhr", "Resting HR", colors.heart) { "${it.roundToInt()}" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeMetricTile(Modifier.weight(1f), data, range, "spo2", "SpO₂", colors.charge) { String.format(Locale.US, "%.1f", it) }
            ForgeMetricTile(Modifier.weight(1f), data, range, "stress", "Stress", colors.effort) { "${it.roundToInt()}" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeMetricTile(Modifier.weight(1f), data, range, "distance_m", "Distance", colors.accent) { intString(it) }
            ForgeMetricTile(Modifier.weight(1f), data, range, "energy_kcal", "Energy", colors.effort) { "${it.roundToInt()}" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeMetricTile(Modifier.weight(1f), data, range, "sleep_score", "Sleep Score", colors.rest) { "${it.roundToInt()}" }
            ForgeMetricTile(Modifier.weight(1f), data, range, "vitality", "Vitality", colors.charge) { "${it.roundToInt()}" }
        }
    }
}

@Composable
private fun ForgeMetricTile(
    modifier: Modifier,
    data: MiData,
    range: MiRange,
    key: String,
    label: String,
    accent: Color,
    fmt: (Double) -> String,
) {
    val colors = Forge.colors
    val rows = resolve(data.raw(key), range).rows
    val values = rows.map { it.value }
    val empty = values.isEmpty()

    val value: String
    val caption: String?
    when {
        empty -> {
            value = "—"
            caption = null
        }
        else -> {
            val v = values.last()
            value = fmt(v)
            caption = rows.lastOrNull()?.let { "as of ${it.day}" }
        }
    }

    ForgeCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space1)) {
            Text(label, style = Forge.type.overline, color = colors.textTertiary)
            Text(value, style = Forge.type.figure, color = if (empty) colors.textTertiary else accent)
            caption?.let {
                Text(it, style = Forge.type.footnote, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ForgeChartSection(title: String, range: MiRange, content: @Composable () -> Unit) {
    val colors = Forge.colors
    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = Forge.type.title2, color = colors.text)
            Spacer(Modifier.weight(1f))
            Text(range.caption, style = Forge.type.footnote, color = colors.textTertiary)
        }
        content()
    }
}

@Composable
private fun ForgeMetricChartCard(
    data: MiData,
    range: MiRange,
    key: String,
    title: String,
    accent: Color,
    fmt: (Double) -> String,
) {
    val colors = Forge.colors
    val resolved = resolve(data.raw(key), range)
    val rows = resolved.rows
    val values = rows.map { it.value }
    val n = values.size
    val mean = if (n > 0) values.average() else null
    val trailing = mean?.let { fmt(it) } ?: "—"

    val subtitle = run {
        val unit = if (n == 1) "reading" else "readings"
        if (resolved.fellBack) "$n $unit · sparse - widened to ${resolved.effective.windowName}"
        else "$n $unit · ${range.windowName}"
    }

    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = Forge.type.subhead, color = colors.text)
                    Text(
                        subtitle,
                        style = Forge.type.footnote,
                        color = if (resolved.fellBack) Color(0xFFCC8A00) else colors.textTertiary,
                    )
                }
                Text(trailing, style = Forge.type.figure, color = if (n > 0) accent else colors.textTertiary)
            }

            when {
                n >= 2 -> com.noop.ui.LineChart(
                    values = values,
                    modifier = Modifier.height(ForgeMetrics.chartHeight),
                    color = accent,
                    fill = true,
                )
                n == 1 -> Box(
                    Modifier.fillMaxWidth().height(ForgeMetrics.chartHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Latest reading", style = Forge.type.overline, color = colors.textTertiary)
                        Text(fmt(values.first()), style = Forge.type.heroNumber, color = accent)
                    }
                }
                else -> Box(
                    Modifier.fillMaxWidth().height(ForgeMetrics.chartHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No readings recorded", style = Forge.type.subhead, color = colors.textTertiary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.sectionGap)) {
                val items = if (n > 0) {
                    listOf(
                        "Avg" to fmt(values.average()),
                        "Min" to fmt(values.min()),
                        "Max" to fmt(values.max()),
                        "Points" to "$n",
                    )
                } else {
                    listOf("Avg" to "—", "Min" to "—", "Max" to "—", "Points" to "0")
                }
                items.forEach { (label, value) ->
                    Column {
                        Text(label, style = Forge.type.overline, color = colors.textTertiary)
                        Text(value, style = Forge.type.caption, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

private fun intString(v: Double): String {
    val n = v.roundToInt()
    return if (kotlin.math.abs(n) >= 1000) String.format(Locale.US, "%,d", n) else "$n"
}

private fun durationString(minutes: Double): String {
    val total = minutes.roundToInt()
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
