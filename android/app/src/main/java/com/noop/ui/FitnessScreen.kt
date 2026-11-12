package com.noop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noop.R
import com.noop.data.WorkoutRow
import java.util.Calendar

@Composable
fun FitnessScreen(
    viewModel: AppViewModel,
    onOpenWorkouts: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenImports: () -> Unit,
) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.activeStrapId) { viewModel.loadWorkouts() }

    val nowSeconds = System.currentTimeMillis() / 1000L
    val weekStart = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 1000L
    val week = workouts.filter { it.startTs >= weekStart && it.startTs <= nowSeconds }
    val minutes = week.sumOf { it.durationS ?: 0.0 }.toInt() / 60

    LazyScreenScaffold(
        title = uiString(R.string.nav_fitness),
        subtitle = uiString(R.string.fitness_subtitle),
    ) {
        item {
            NoopCard(tint = Palette.accent, modifier = Modifier.clickable(onClick = onOpenWorkouts)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(uiString(R.string.fitness_this_week), style = NoopType.headline, color = Palette.textPrimary)
                        Text(uiString(R.string.fitness_week_summary, week.size, minutes), style = NoopType.body, color = Palette.textSecondary)
                    }
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Palette.accent)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FitnessActionCard(
                    modifier = Modifier.weight(1f), icon = Icons.Filled.Add,
                    title = uiString(R.string.fitness_workouts), detail = uiString(R.string.fitness_open_log),
                    onClick = onOpenWorkouts,
                )
                FitnessActionCard(
                    modifier = Modifier.weight(1f), icon = Icons.Filled.Flag,
                    title = uiString(R.string.nav_goals), detail = uiString(R.string.fitness_training_targets),
                    onClick = onOpenGoals,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(uiString(R.string.fitness_recent), style = NoopType.title2, color = Palette.textPrimary, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onOpenWorkouts) { Text(uiString(R.string.fitness_all)) }
            }
        }
        if (workouts.isEmpty()) {
            item {
                NoopCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(uiString(R.string.fitness_empty), style = NoopType.body, color = Palette.textSecondary)
                        NoopButton(
                            text = uiString(R.string.fitness_import_history),
                            leadingIcon = Icons.Filled.ImportExport,
                            kind = NoopButtonKind.Secondary,
                            onClick = onOpenImports,
                        )
                    }
                }
            }
        } else {
            items(workouts.take(5), key = { "${it.deviceId}:${it.startTs}" }) { workout ->
                RecentWorkoutCard(workout, onClick = onOpenWorkouts)
            }
        }
        item {
            NoopCard(modifier = Modifier.clickable(onClick = onOpenImports)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ImportExport, contentDescription = null, tint = Palette.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiString(R.string.fitness_bring_history), style = NoopType.headline, color = Palette.textPrimary)
                        Text(uiString(R.string.fitness_import_detail), style = NoopType.footnote, color = Palette.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FitnessActionCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    NoopCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Palette.accent)
            Text(title, style = NoopType.headline, color = Palette.textPrimary)
            Text(detail, style = NoopType.footnote, color = Palette.textSecondary)
        }
    }
}

@Composable
private fun RecentWorkoutCard(workout: WorkoutRow, onClick: () -> Unit) {
    val date = android.text.format.DateUtils.getRelativeTimeSpanString(
        workout.startTs * 1000L, System.currentTimeMillis(), android.text.format.DateUtils.DAY_IN_MILLIS,
    ).toString()
    NoopCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = Palette.textTertiary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(workout.sport, style = NoopType.body, color = Palette.textPrimary)
                Text(date, style = NoopType.footnote, color = Palette.textTertiary)
            }
            workout.durationS?.let {
                Text(uiString(R.string.fitness_duration_minutes, (it / 60.0).toInt()), style = NoopType.footnote, color = Palette.textSecondary)
            }
        }
    }
}
