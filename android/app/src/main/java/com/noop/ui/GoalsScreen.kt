package com.noop.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.noop.analytics.PeriodGoal
import com.noop.analytics.ActivityStreak
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GoalsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(GoalsStore.load(context)) }
    var sessions by remember { mutableStateOf<List<PeriodGoal.Session>>(emptyList()) }
    var lowRecoveryDays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var showAdd by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<PeriodGoal?>(null) }
    var editTarget by remember { mutableStateOf("") }
    var customMetric by remember { mutableStateOf(PeriodGoal.Metric.DISTANCE) }
    var customPeriod by remember { mutableStateOf(PeriodGoal.Period.WEEK) }
    var customTarget by remember { mutableStateOf("") }
    var customSport by remember { mutableStateOf("") }
    var periodMenu by remember { mutableStateOf(false) }
    var metricMenu by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }

    LaunchedEffect(viewModel.activeStrapId) {
        val now = Instant.now()
        val from = now.minusSeconds(400L * 86_400L).epochSecond
        val rows = runCatching { viewModel.repo.workoutsAcrossSources(from, now.epochSecond + 86_400L, 10_000) }
            .getOrDefault(emptyList())
        sessions = rows.mapNotNull { row ->
            val duration = row.durationS ?: return@mapNotNull null
            if (duration <= 0.0 || row.deviceId.endsWith("-noop")) return@mapNotNull null
            PeriodGoal.Session(
                start = Instant.ofEpochSecond(row.startTs),
                durationS = duration,
                distanceM = row.distanceM ?: 0.0,
                climbM = 0.0,
                sport = row.sport,
            )
        }
        val dayRows = runCatching { viewModel.repo.daysMerged(viewModel.activeStrapId) }.getOrDefault(emptyList())
        lowRecoveryDays = dayRows.mapNotNull { day ->
            if ((day.recovery ?: 100.0) >= 34.0) return@mapNotNull null
            runCatching { LocalDate.parse(day.day, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        }.toSet()
    }

    fun persist(next: GoalsSnapshot) {
        snapshot = next
        GoalsStore.save(context, next)
    }

    LazyScreenScaffold(
        title = uiString(com.noop.R.string.nav_goals),
        subtitle = uiString(com.noop.R.string.goals_subtitle),
    ) {
        item {
            NoopCard(tint = if (snapshot.enabled) Palette.accent else null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(uiString(com.noop.R.string.goals_toggle_title), style = NoopType.headline, color = Palette.textPrimary)
                        Text(uiString(com.noop.R.string.goals_toggle_detail), style = NoopType.footnote, color = Palette.textSecondary)
                    }
                    Switch(
                        checked = snapshot.enabled,
                        onCheckedChange = {
                            persist(snapshot.copy(
                                enabled = it,
                                streaksEnabled = snapshot.streaksEnabled && it,
                                streakStartEpochDay = if (it) snapshot.streakStartEpochDay else null,
                            ))
                        },
                    )
                }
            }
        }
        if (snapshot.enabled) {
            item {
                NoopCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(uiString(com.noop.R.string.goals_streak_title), style = NoopType.headline, color = Palette.textPrimary)
                                Text(uiString(com.noop.R.string.goals_streak_detail), style = NoopType.footnote, color = Palette.textSecondary)
                            }
                            Switch(
                                checked = snapshot.streaksEnabled,
                                onCheckedChange = { enabled ->
                                    val today = LocalDate.now(zone)
                                    persist(snapshot.copy(
                                        streaksEnabled = enabled,
                                        streakStartEpochDay = if (enabled) today.toEpochDay() else null,
                                    ))
                                },
                            )
                        }
                        if (snapshot.streaksEnabled) {
                            val today = LocalDate.now(zone)
                            val start = snapshot.streakStartEpochDay?.let(LocalDate::ofEpochDay) ?: today
                            val pausedDays = snapshot.pauses.flatMap { pause ->
                                val pauseStart = LocalDate.ofEpochDay(pause.startEpochDay)
                                val pauseEnd = LocalDate.ofEpochDay(pause.endEpochDay).coerceAtMost(today)
                                if (pauseEnd.isBefore(pauseStart)) emptyList()
                                else generateSequence(pauseStart) { day -> if (day < pauseEnd) day.plusDays(1) else null }
                                    .take(90).toList()
                            }.filter { !it.isBefore(start) && !it.isAfter(today) }.toSet()
                            val streakInput = ActivityStreak.Input(
                                trained = sessions.asSequence()
                                    .filter { !it.start.atZone(zone).toLocalDate().isBefore(start) }
                                    .map { it.start.atZone(zone).toLocalDate() }
                                    .toSet(),
                                lowRecovery = lowRecoveryDays,
                                paused = pausedDays,
                                restPerWeek = snapshot.restPerWeek,
                                freezesPerMonth = 1,
                            )
                            val reading = ActivityStreak.reading(streakInput, today)
                            Text(
                                uiString(com.noop.R.string.goals_streak_summary, reading.current, reading.longest),
                                style = NoopType.body,
                                color = Palette.accent,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    uiString(com.noop.R.string.goals_rest_days_week, snapshot.restPerWeek),
                                    style = NoopType.footnote,
                                    color = Palette.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { persist(snapshot.copy(restPerWeek = (snapshot.restPerWeek - 1).coerceAtLeast(0))) },
                                    enabled = snapshot.restPerWeek > 0,
                                ) { Icon(Icons.Filled.Remove, contentDescription = uiString(com.noop.R.string.goals_rest_days_decrease), tint = Palette.textSecondary) }
                                IconButton(
                                    onClick = { persist(snapshot.copy(restPerWeek = (snapshot.restPerWeek + 1).coerceAtMost(3))) },
                                    enabled = snapshot.restPerWeek < 3,
                                ) { Icon(Icons.Filled.Add, contentDescription = uiString(com.noop.R.string.goals_rest_days_increase), tint = Palette.textSecondary) }
                            }
                            val activePause = snapshot.pauses.firstOrNull {
                                it.startEpochDay <= today.toEpochDay() && it.endEpochDay >= today.toEpochDay()
                            }
                            if (activePause != null) {
                                val through = LocalDate.ofEpochDay(activePause.endEpochDay)
                                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                Text(uiString(com.noop.R.string.goals_pause_active, through), style = NoopType.footnote, color = Palette.textSecondary)
                            } else {
                                Text(uiString(com.noop.R.string.goals_pause_detail), style = NoopType.footnote, color = Palette.textSecondary)
                            }
                            NoopButton(
                                text = uiString(if (activePause == null) com.noop.R.string.goals_pause_streak else com.noop.R.string.goals_resume_streak),
                                kind = NoopButtonKind.Secondary,
                                onClick = {
                                    if (activePause != null) {
                                        persist(snapshot.copy(pauses = snapshot.pauses.filterNot { it == activePause }))
                                    } else {
                                        val latest = today.plusDays(7)
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val end = LocalDate.of(year, month + 1, dayOfMonth).coerceAtMost(today.plusDays(89))
                                                val kept = snapshot.pauses.filter { it.endEpochDay < today.toEpochDay() }
                                                persist(snapshot.copy(pauses = kept + GoalPause(today.toEpochDay(), end.toEpochDay())))
                                            },
                                            latest.year,
                                            latest.monthValue - 1,
                                            latest.dayOfMonth,
                                        ).apply {
                                            datePicker.minDate = today.atStartOfDay(zone).toInstant().toEpochMilli()
                                            datePicker.maxDate = today.plusDays(89).atStartOfDay(zone).toInstant().toEpochMilli()
                                        }.show()
                                    }
                                },
                            )
                        }
                    }
                }
            }
            if (snapshot.goals.isEmpty()) {
                item {
                    NoopCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(uiString(com.noop.R.string.goals_empty_title), style = NoopType.title2, color = Palette.textPrimary)
                            Text(uiString(com.noop.R.string.goals_empty_detail), style = NoopType.subhead, color = Palette.textSecondary)
                            NoopButton(
                                text = uiString(com.noop.R.string.goals_add),
                                leadingIcon = Icons.Filled.Add,
                                onClick = { showAdd = true },
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(uiString(com.noop.R.string.goals_section_title), style = NoopType.title2, color = Palette.textPrimary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAdd = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(uiString(com.noop.R.string.goals_add))
                        }
                    }
                }
                items(snapshot.goals, key = { it.id }) { goal ->
                    val progress = goal.progress(sessions, Instant.now(), zone)
                    NoopCard(modifier = Modifier.clickable {
                        editGoal = goal
                        editTarget = when (goal.metric) {
                            PeriodGoal.Metric.DISTANCE -> format(goal.target / 1000.0)
                            PeriodGoal.Metric.TIME -> format(goal.target / 3600.0)
                            else -> format(goal.target)
                        }
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goalTitle(goal), style = NoopType.headline, color = Palette.textPrimary)
                                    Text(goalPeriodLabel(goal.period), style = NoopType.footnote, color = Palette.textTertiary)
                                }
                                IconButton(onClick = { persist(snapshot.copy(goals = snapshot.goals.filterNot { it.id == goal.id })) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = uiString(com.noop.R.string.goals_remove), tint = Palette.textTertiary)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress.fraction.toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = Palette.accent,
                                trackColor = Palette.surfaceInset,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(goalValue(goal, progress.done), style = NoopType.body, color = Palette.textPrimary)
                                Spacer(Modifier.weight(1f))
                                Text(goalValue(goal, goal.target), style = NoopType.footnote, color = Palette.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        val now = Instant.now()
        val templates = GoalsStore.starterGoals(sessions, now, zone)
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(uiString(com.noop.R.string.goals_choose_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { goal ->
                        TextButton(
                            onClick = {
                                persist(snapshot.copy(goals = snapshot.goals + goal))
                                showAdd = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(goalTitle(goal), color = Palette.textPrimary) }
                    }
                    TextButton(
                        onClick = {
                            showAdd = false
                            customMetric = PeriodGoal.Metric.DISTANCE
                            customPeriod = PeriodGoal.Period.WEEK
                            customTarget = ""
                            customSport = ""
                            showCustom = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(uiString(com.noop.R.string.goals_custom)) }
                }
            },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) } },
        )
    }

    if (showCustom) {
        val multiplier = when (customMetric) {
            PeriodGoal.Metric.DISTANCE -> 1000.0
            PeriodGoal.Metric.TIME -> 3600.0
            else -> 1.0
        }
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text(uiString(com.noop.R.string.goals_custom)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        periodMenu = true
                    }) {
                        Text(goalPeriodLabel(customPeriod))
                        DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                            PeriodGoal.Period.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(goalPeriodLabel(option)) },
                                    onClick = { customPeriod = option; periodMenu = false },
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        metricMenu = true
                    }) {
                        Text(goalMetricLabel(customMetric))
                        DropdownMenu(expanded = metricMenu, onDismissRequest = { metricMenu = false }) {
                            PeriodGoal.Metric.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(goalMetricLabel(option)) },
                                    onClick = { customMetric = option; metricMenu = false },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customTarget,
                        onValueChange = { customTarget = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text(uiString(com.noop.R.string.goals_target_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customSport,
                        onValueChange = { customSport = it },
                        label = { Text(uiString(com.noop.R.string.goals_sport_optional)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = customTarget.replace(',', '.').toDoubleOrNull()
                    if (amount != null && amount > 0.0) {
                        val now = Instant.now()
                        val sport = customSport.trim().ifBlank { null }
                        val goal = PeriodGoal(
                            id = java.util.UUID.randomUUID().toString(),
                            period = customPeriod,
                            metric = customMetric,
                            sport = sport,
                            target = amount * multiplier,
                            createdAt = now,
                        )
                        persist(snapshot.copy(goals = snapshot.goals + goal))
                        showCustom = false
                    }
                }, enabled = (customTarget.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0) {
                    Text(uiString(com.noop.R.string.l10n_wheel_picker_done_e9b450d1))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustom = false }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) }
            },
        )
    }

    editGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { editGoal = null },
            title = { Text(goalTitle(goal)) },
            text = {
                val progress = goal.progress(sessions, Instant.now(), zone)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(uiString(com.noop.R.string.goals_edit_progress, goalValue(goal, progress.done), goalValue(goal, goal.target)))
                    OutlinedTextField(
                        value = editTarget,
                        onValueChange = { editTarget = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text(uiString(com.noop.R.string.goals_target_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val entered = editTarget.replace(',', '.').toDoubleOrNull()
                    if (entered != null && entered > 0.0) {
                        val multiplier = when (goal.metric) {
                            PeriodGoal.Metric.DISTANCE -> 1000.0
                            PeriodGoal.Metric.TIME -> 3600.0
                            else -> 1.0
                        }
                        persist(snapshot.copy(goals = snapshot.goals.map { if (it.id == goal.id) it.copy(target = entered * multiplier) else it }))
                    }
                    editGoal = null
                }, enabled = (editTarget.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0) {
                    Text(uiString(com.noop.R.string.l10n_wheel_picker_done_e9b450d1))
                }
            },
            dismissButton = {
                TextButton(onClick = { editGoal = null }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) }
            },
        )
    }
}

private fun goalTitle(goal: PeriodGoal): String {
    val sport = goal.sport?.let { " · $it" }.orEmpty()
    val period = goalPeriodLabel(goal.period)
    val key = when (goal.metric) {
        PeriodGoal.Metric.DISTANCE -> com.noop.R.string.goals_format_distance
        PeriodGoal.Metric.TIME -> com.noop.R.string.goals_format_time
        PeriodGoal.Metric.CLIMB -> com.noop.R.string.goals_format_climb
        PeriodGoal.Metric.SESSIONS -> com.noop.R.string.goals_format_sessions
    }
    val amount = when (goal.metric) {
        PeriodGoal.Metric.DISTANCE -> format(goal.target / 1000.0)
        PeriodGoal.Metric.TIME -> format(goal.target / 3600.0)
        else -> format(goal.target)
    }
    return uiString(key, amount, sport, period)
}

private fun goalPeriodLabel(period: PeriodGoal.Period): String = when (period) {
    PeriodGoal.Period.WEEK -> uiString(com.noop.R.string.goals_week)
    PeriodGoal.Period.MONTH -> uiString(com.noop.R.string.goals_month)
    PeriodGoal.Period.YEAR -> uiString(com.noop.R.string.goals_year)
}

private fun goalMetricLabel(metric: PeriodGoal.Metric): String = when (metric) {
    PeriodGoal.Metric.DISTANCE -> uiString(com.noop.R.string.goals_metric_distance)
    PeriodGoal.Metric.TIME -> uiString(com.noop.R.string.goals_metric_time)
    PeriodGoal.Metric.CLIMB -> uiString(com.noop.R.string.goals_metric_climb)
    PeriodGoal.Metric.SESSIONS -> uiString(com.noop.R.string.goals_metric_sessions)
}

private fun goalValue(goal: PeriodGoal, value: Double): String = when (goal.metric) {
    PeriodGoal.Metric.DISTANCE -> "${format(value / 1000.0)} km"
    PeriodGoal.Metric.TIME -> "${format(value / 3600.0)} h"
    PeriodGoal.Metric.CLIMB -> "${format(value)} m"
    PeriodGoal.Metric.SESSIONS -> "${format(value)}"
}

private fun format(value: Double): String = DecimalFormat("#,##0.#").format(value)
