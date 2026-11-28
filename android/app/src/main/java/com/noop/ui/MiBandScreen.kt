package com.noop.ui
import com.noop.ui.forge.ForgeSegmentedValues

import com.noop.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.noop.data.MetricSeriesRow
import java.util.Locale
import kotlin.math.roundToInt

// MARK: - Mi Band (per-source page)
//
// Faithful port of the iOS XiaomiBandView. Shows imported Mi Band data under the
// "xiaomi-band" device ID: a range control, StatTiles for the key metrics, and
// ChartCard sections grouped by Heart, Activity and Sleep.

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
fun MiBandScreen(vm: AppViewModel) {
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

    LazyScreenScaffold(title = uiString(R.string.forge_ds_mi_band_title), subtitle = subtitle) {
        when {
            !loaded -> item { LoadingCard() }
            !data.hasAnyData -> item { EmptyState() }
            else -> {
                item { RangeControl(data = data, range = range, onSelect = { range = it }) }
                item { TileGrid(data = data, range = range) }
                item { HeartSection(data = data, range = range) }
                item { ActivitySection(data = data, range = range) }
                item { SleepSection(data = data, range = range) }
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
private fun RangeControl(data: MiData, range: MiRange, onSelect: (MiRange) -> Unit) {
    val stepsRows = resolve(data.raw("steps"), range).rows
    val anyWidened = data.series.any { (_, s) -> s.isNotEmpty() && resolve(s, range).fellBack }
    val n = stepsRows.size
    val unit = if (n == 1) "day" else "days"
    val base = "$n $unit · ${range.windowName}"
    val caption = if (anyWidened) "$base · some sparse series widened" else base

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ForgeSegmentedValues(
                items = MiRange.entries.toList(),
                selection = range,
                label = { it.label },
                onSelect = onSelect,
            )
            Spacer(Modifier.weight(1f))
            Overline(range.caption, color = Palette.textTertiary)
        }
        Text(
            caption,
            style = NoopType.footnote,
            color = if (anyWidened) Palette.statusWarning else Palette.textTertiary,
        )
    }
}

@Composable
private fun LoadingCard() {
    NoopCard(tint = Palette.metricCyan) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConnectionDot(tone = StrandTone.Accent, pulsing = true)
            Text(
                "Reading your Mi Band history…",
                style = NoopType.subhead,
                color = Palette.textSecondary,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    DataPendingNote(
        title = "Nothing imported yet",
        body = "Import your Mi Fitness export in Data Sources to see Mi Band history here.",
    )
}

@Composable
private fun TileGrid(data: MiData, range: MiRange) {
    Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
        TileRow {
            MetricTile(Modifier.weight(1f), data, range, "steps", "Steps", Palette.metricCyan) { intString(it) }
            MetricTile(Modifier.weight(1f), data, range, "rhr", "Resting HR", Palette.metricRose, "bpm") {
                "${it.roundToInt()}"
            }
        }
        TileRow {
            MetricTile(Modifier.weight(1f), data, range, "spo2", "SpO₂", Palette.metricCyan, "%") {
                String.format(Locale.US, "%.1f", it)
            }
            MetricTile(Modifier.weight(1f), data, range, "stress", "Stress", Palette.metricAmber) {
                "${it.roundToInt()}"
            }
        }
        TileRow {
            MetricTile(Modifier.weight(1f), data, range, "distance_m", "Distance", Palette.accent, "m") {
                intString(it)
            }
            MetricTile(Modifier.weight(1f), data, range, "energy_kcal", "Energy", Palette.metricAmber, "kcal") {
                "${it.roundToInt()}"
            }
        }
        TileRow {
            MetricTile(Modifier.weight(1f), data, range, "sleep_score", "Sleep Score", Palette.metricPurple) {
                "${it.roundToInt()}"
            }
            MetricTile(Modifier.weight(1f), data, range, "vitality", "Vitality", Palette.metricCyan) {
                "${it.roundToInt()}"
            }
        }
    }
}

@Composable
private fun TileRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Metrics.gap)) { content() }
}

@Composable
private fun MetricTile(
    modifier: Modifier,
    data: MiData,
    range: MiRange,
    key: String,
    label: String,
    accent: Color,
    unit: String = "",
    fmt: (Double) -> String,
) {
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
            value = if (unit.isEmpty()) fmt(v) else "$v $unit"
            caption = rows.lastOrNull()?.let { "as of ${it.day}" }
        }
    }

    StatTile(
        modifier = modifier,
        label = label,
        value = value,
        caption = caption,
        accent = if (empty) Palette.textTertiary else accent,
    )
}

@Composable
private fun HeartSection(data: MiData, range: MiRange) {
    ChartSection("Heart", range) {
        MetricChartCard(data, range, "rhr", "Resting heart rate", Palette.metricRose) { "${it.roundToInt()} bpm" }
        MetricChartCard(data, range, "avg_hr", "Average heart rate", Palette.metricRose) { "${it.roundToInt()} bpm" }
        MetricChartCard(data, range, "max_hr", "Max heart rate", Palette.metricRose) { "${it.roundToInt()} bpm" }
        MetricChartCard(data, range, "spo2", "Blood oxygen", Palette.metricCyan) { String.format(Locale.US, "%.1f%%", it) }
    }
}

@Composable
private fun ActivitySection(data: MiData, range: MiRange) {
    ChartSection("Activity", range) {
        MetricChartCard(data, range, "steps", "Steps", Palette.metricCyan) { intString(it) }
        MetricChartCard(data, range, "distance_m", "Distance", Palette.accent) { intString(it) + " m" }
        MetricChartCard(data, range, "energy_kcal", "Energy", Palette.metricAmber) { "${it.roundToInt()} kcal" }
        MetricChartCard(data, range, "intensity_min", "Intensity minutes", Palette.metricAmber) { "${it.roundToInt()} min" }
    }
}

@Composable
private fun SleepSection(data: MiData, range: MiRange) {
    ChartSection("Sleep", range) {
        MetricChartCard(data, range, "sleep_total_min", "Total sleep", Palette.metricPurple) { durationString(it) }
        MetricChartCard(data, range, "sleep_score", "Sleep score", Palette.metricPurple) { "${it.roundToInt()}" }
        MetricChartCard(data, range, "stress", "Stress", Palette.metricAmber) { "${it.roundToInt()}" }
        MetricChartCard(data, range, "vitality", "Vitality", Palette.metricCyan) { "${it.roundToInt()}" }
    }
}

@Composable
private fun ChartSection(
    title: String,
    range: MiRange,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
        SectionHeader(title, overline = title, trailing = range.caption)
        content()
    }
}

@Composable
private fun MetricChartCard(
    data: MiData,
    range: MiRange,
    key: String,
    title: String,
    accent: Color,
    fmt: (Double) -> String,
) {
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

    NoopCard(tint = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Overline(title)
                    Text(
                        subtitle,
                        style = NoopType.footnote,
                        color = if (resolved.fellBack) Palette.statusWarning else Palette.textTertiary,
                    )
                }
                Text(trailing, style = NoopType.number(18f), color = if (n > 0) accent else Palette.textTertiary)
            }

            when {
                n >= 2 -> LineChart(
                    values = values,
                    modifier = Modifier.height(Metrics.chartHeight),
                    color = accent,
                    fill = true,
                )
                n == 1 -> SinglePoint(values.first(), accent, fmt)
                else -> EmptyChart()
            }

            ChartFooterRow(
                items = if (n > 0) {
                    listOf(
                        "Avg" to fmt(values.average()),
                        "Min" to fmt(values.min()),
                        "Max" to fmt(values.max()),
                        "Points" to "$n",
                    )
                } else {
                    listOf("Avg" to "—", "Min" to "—", "Max" to "—", "Points" to "0")
                },
            )
        }
    }
}

@Composable
private fun SinglePoint(value: Double, accent: Color, fmt: (Double) -> String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(Metrics.chartHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Overline("Latest reading")
            Text(fmt(value), style = NoopType.number(34f), color = accent)
        }
    }
}

@Composable
private fun EmptyChart() {
    Box(
        modifier = Modifier.fillMaxWidth().height(Metrics.chartHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text("No readings recorded", style = NoopType.subhead, color = Palette.textTertiary)
    }
}

@Composable
private fun ChartFooterRow(items: List<Pair<String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(Metrics.sectionGap)) {
        items.forEach { (label, value) ->
            Column {
                Overline(label, color = Palette.textTertiary)
                Text(value, style = NoopType.captionNumber, color = Palette.textSecondary)
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
