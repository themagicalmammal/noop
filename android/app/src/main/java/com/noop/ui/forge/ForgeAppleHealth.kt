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
import com.noop.data.AppleDaily
import com.noop.data.MetricSeriesRow
import com.noop.ui.AppViewModel
import com.noop.ui.UnitFormatter
import com.noop.ui.UnitPrefs
import com.noop.ui.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

private const val APPLE_DEVICE = "apple-health"

private enum class AppleRange(val days: Int?, val label: String, val caption: String, val windowName: String) {
    Week(7, "W", "7 DAYS", "week"),
    Month(30, "M", "30 DAYS", "month"),
    Quarter(90, "3M", "90 DAYS", "3 months"),
    Half(180, "6M", "180 DAYS", "6 months"),
    Year(365, "1Y", "365 DAYS", "year"),
    All(null, "ALL", "ALL TIME", "all history");

    val widening: List<AppleRange>
        get() = entries.dropWhile { it != this }
}

private data class HealthPoint(val day: String, val value: Double)

private fun List<HealthPoint>.windowFor(range: AppleRange): List<HealthPoint> {
    val n = range.days ?: return this
    if (isEmpty()) return emptyList()
    return takeLast(n)
}

private class AppleData(
    val rows: List<AppleDaily>,
    val series: Map<String, List<HealthPoint>>,
) {
    fun raw(key: String): List<HealthPoint> = series[key] ?: emptyList()

    val hasAnyData: Boolean
        get() = rows.isNotEmpty() || series.values.any { it.isNotEmpty() }
}

private data class Resolved(
    val requested: AppleRange,
    val effective: AppleRange,
    val rows: List<HealthPoint>,
) {
    val fellBack: Boolean get() = effective != requested
}

private fun resolve(series: List<HealthPoint>, requested: AppleRange): Resolved {
    if (series.isEmpty()) return Resolved(requested, requested, emptyList())
    val eff = requested.widening.firstOrNull { series.windowFor(it).isNotEmpty() } ?: AppleRange.All
    return Resolved(requested, eff, series.windowFor(eff))
}

@Composable
internal fun ForgeAppleHealthSection(vm: AppViewModel, onBack: () -> Unit) {
    val colors = Forge.colors
    var loaded by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf(AppleData(emptyList(), emptyMap())) }
    var range by remember { mutableStateOf(AppleRange.Quarter) }

    LaunchedEffect(Unit) {
        val rows: List<AppleDaily> = runCatching {
            vm.repo.appleDaily(APPLE_DEVICE, "0000-00-00", "9999-99-99")
        }.getOrDefault(emptyList()).sortedBy { it.day }

        val series = linkedMapOf<String, List<HealthPoint>>()
        series["steps"] = rows.mapNotNull { r -> r.steps?.let { HealthPoint(r.day, it.toDouble()) } }
        series["active_kcal"] = rows.mapNotNull { r -> r.activeKcal?.let { HealthPoint(r.day, it) } }
        series["vo2max"] = rows.mapNotNull { r -> r.vo2max?.let { HealthPoint(r.day, it) } }
        series["weight"] = rows.mapNotNull { r -> r.weightKg?.let { HealthPoint(r.day, it) } }

        val seriesKeys = listOf(
            "resting_hr", "hrv", "spo2", "resp_rate", "asleep_min",
            "body_fat", "lean_mass", "bmi",
        )
        for (key in seriesKeys) {
            val pts = runCatching {
                vm.repo.metricSeries(APPLE_DEVICE, key, "0000-00-00", "9999-99-99")
            }.getOrDefault(emptyList<MetricSeriesRow>())
                .sortedBy { it.day }
                .map { HealthPoint(it.day, it.value) }
            series[key] = pts
        }

        data = AppleData(rows, series)
        loaded = true
    }

    val subtitle = spanSubtitle(loaded, data, range)

    SettingsColumn {
        PageTitle(stringResource(R.string.l10n_apple_health_screen_apple_health_b19b87da), onBack)
        Text(subtitle, style = Forge.type.subhead, color = colors.textSecondary)

        when {
            !loaded -> ForgeAppleLoadingCard()
            !data.hasAnyData -> ForgeAppleEmptyState()
            else -> {
                ForgeAppleRangeControl(data = data, range = range, onSelect = { range = it })
                ForgeAppleTileGrid(data = data, range = range)
                ForgeAppleChartSection("Heart & Vitals", "Cardiac", range) {
                    ForgeAppleMetricChartCard(data, range, "resting_hr", "Resting heart rate", colors.heart) { "${it.roundToInt()} bpm" }
                    ForgeAppleMetricChartCard(data, range, "hrv", "Heart rate variability", colors.rest) { "${it.roundToInt()} ms" }
                    ForgeAppleMetricChartCard(data, range, "spo2", "Blood oxygen", colors.charge) { String.format(Locale.US, "%.1f%%", it) }
                    ForgeAppleMetricChartCard(data, range, "resp_rate", "Respiratory rate", colors.accent) { String.format(Locale.US, "%.1f rpm", it) }
                }
                ForgeAppleChartSection("Activity & Energy", "Movement", range) {
                    ForgeAppleMetricChartCard(data, range, "steps", "Steps", colors.charge) { intString(it) }
                    ForgeAppleMetricChartCard(data, range, "active_kcal", "Active energy", colors.effort) { "${intString(it)} kcal" }
                }
                ForgeAppleChartSection("Body Composition", "Slow threads", range) {
                    val unitSystem = UnitPrefs.system(LocalContext.current)
                    ForgeAppleMetricChartCard(data, range, "weight", "Weight", colors.accent) { UnitFormatter.massFromKilograms(it, unitSystem) }
                    ForgeAppleMetricChartCard(data, range, "body_fat", "Body fat", colors.effort) { String.format(Locale.US, "%.1f%%", it) }
                    ForgeAppleMetricChartCard(data, range, "lean_mass", "Lean body mass", colors.accent) { UnitFormatter.massFromKilograms(it, unitSystem) }
                    ForgeAppleMetricChartCard(data, range, "bmi", "BMI", colors.rest) { String.format(Locale.US, "%.1f", it) }
                }
                ForgeAppleChartSection("Sleep", "Rest", range) {
                    ForgeAppleMetricChartCard(data, range, "asleep_min", "Asleep", colors.rest) { durationString(it) }
                }
            }
        }
    }
}

private fun spanSubtitle(loaded: Boolean, data: AppleData, range: AppleRange): String {
    if (!loaded) {
        return "Steps, heart, sleep, body composition and VO₂ max - synced from the desktop app."
    }
    val rows = resolve(data.raw("steps"), range).rows
    if (rows.isEmpty()) {
        return "Steps, heart, sleep, body composition and VO₂ max - synced from the desktop app."
    }
    val lo = rows.first().day
    val hi = rows.last().day
    val span = if (lo == hi) lo else "$lo → $hi"
    val unit = if (rows.size == 1) "day" else "days"
    return "${rows.size} $unit · $span"
}

@Composable
private fun ForgeAppleLoadingCard() {
    ForgeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(8.dp).height(8.dp))
            Text(
                stringResource(R.string.l10n_apple_health_screen_reading_your_apple_health_history_1a3bf76d),
                style = Forge.type.subhead,
                color = Forge.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ForgeAppleEmptyState() {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
            Text(stringResource(R.string.l10n_apple_health_screen_nothing_imported_yet_f457cdbe),
                 style = Forge.type.title2, color = Forge.colors.text)
            Text("Nothing imported yet. On an iPhone: Health app, tap your photo, Export " +
                 "All Health Data, then import the .zip here in Data Sources.",
                 style = Forge.type.subhead, color = Forge.colors.textSecondary)
        }
    }
}

@Composable
private fun ForgeAppleRangeControl(data: AppleData, range: AppleRange, onSelect: (AppleRange) -> Unit) {
    val colors = Forge.colors
    val stepsRows = resolve(data.raw("steps"), range).rows
    val anyWidened = data.series.any { (_, s) -> s.isNotEmpty() && resolve(s, range).fellBack }
    val n = stepsRows.size
    val unit = if (n == 1) "day" else "days"
    val base = "$n $unit · ${range.windowName}"
    val caption = if (anyWidened) "$base · some sparse series widened" else base

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppleRange.entries.forEach { r ->
                    val selected = r == range
                    Box(
                        Modifier.width(48.dp).height(32.dp)
                            .clickable { onSelect(r) }
                            .background(
                                if (selected) colors.accent else colors.inset,
                                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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

private enum class Aggregate { Latest, Mean }

@Composable
private fun ForgeAppleTileGrid(data: AppleData, range: AppleRange) {
    val unitSystem = UnitPrefs.system(LocalContext.current)
    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "steps", "Steps", Forge.colors.charge) { intString(it) }
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "resting_hr", "Resting HR", Forge.colors.heart) { "${it.roundToInt()}" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "hrv", "HRV", Forge.colors.rest) { "${it.roundToInt()}" }
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "vo2max", "VO₂ Max", Forge.colors.accent) { String.format(Locale.US, "%.1f", it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "weight", "Weight", Forge.colors.accent) { UnitFormatter.massFromKilograms(it, unitSystem) }
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "body_fat", "Body Fat", Forge.colors.effort) { String.format(Locale.US, "%.1f", it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.gap)) {
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "lean_mass", "Lean Mass", Forge.colors.accent) { UnitFormatter.massFromKilograms(it, unitSystem) }
            ForgeAppleMetricTile(Modifier.weight(1f), data, range, "asleep_min", "Asleep avg", Forge.colors.rest, aggregate = Aggregate.Mean) { durationString(it) }
        }
    }
}

@Composable
private fun ForgeAppleMetricTile(
    modifier: Modifier,
    data: AppleData,
    range: AppleRange,
    key: String,
    label: String,
    accent: Color,
    aggregate: Aggregate = Aggregate.Latest,
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
        aggregate == Aggregate.Latest -> {
            val v = values.last()
            value = fmt(v)
            caption = rows.lastOrNull()?.let { "as of ${it.day}" }
        }
        else -> {
            val m = values.average()
            value = fmt(m)
            caption = "avg · ${values.size}d"
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
private fun ForgeAppleChartSection(title: String, overline: String, range: AppleRange, content: @Composable () -> Unit) {
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
private fun ForgeAppleMetricChartCard(
    data: AppleData,
    range: AppleRange,
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
                    Text(stringResource(R.string.l10n_apple_health_screen_no_readings_recorded_05018b26),
                         style = Forge.type.subhead, color = colors.textTertiary)
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

private fun withUnit(value: String, unit: String): String = if (unit.isEmpty()) value else "$value $unit"

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
