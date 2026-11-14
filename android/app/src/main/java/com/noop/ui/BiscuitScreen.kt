package com.noop.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.noop.R
import com.noop.analytics.ActivityStreak
import com.noop.analytics.BiscuitClip
import com.noop.analytics.BiscuitController
import com.noop.analytics.BiscuitDay
import com.noop.analytics.BiscuitMood
import com.noop.analytics.BiscuitOutfit
import com.noop.analytics.BiscuitReaction
import com.noop.analytics.PeriodGoal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * The Biscuit face screen (parity twin of Aura's `NoopTabBiscuit`).
 *
 * One screen-owned [BiscuitController] drives the art, the poke milestone line and the lab day
 * playback, while the dress loop re-evaluates today's [BiscuitDay] immediately and every 300 s
 * while the screen is resumed (parity `.task` cadence).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiscuitScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val pokes = remember(context) { BiscuitPokes.load(context) }
    val renderer = remember(context) { BiscuitVectorArt.load(context) }
    val controller = remember(renderer) {
        BiscuitController(renderer.scene, mood = BiscuitMood.HAPPY, frugal = false)
    }
    var mood by remember { mutableStateOf(controller.mood) }
    var outfits by remember { mutableStateOf(controller.outfits) }
    var wearsToasted by remember { mutableStateOf(pokes.wearsToasted) }
    var lastRecovery by remember { mutableStateOf<Double?>(null) }
    var line by remember { mutableStateOf<Int?>(null) }
    var playingDay by remember { mutableStateOf(false) }
    var dayStep by remember { mutableStateOf(0) }

    // Lab day playback (parity `NoopCompanionLab.day`): the seven lab steps in a loop.
    LaunchedEffect(playingDay) {
        if (!playingDay) return@LaunchedEffect
        var step = 0
        while (true) {
            val lab = LAB_DAY[step]
            dayStep = step
            mood = lab.mood
            outfits = lab.outfits
            controller.mood = lab.mood
            controller.outfits = lab.outfits
            delay(3_600)
            step = (step + 1) % LAB_DAY.size
        }
    }

    // Dress loop: the first pass runs immediately, then every 300 s while the screen is resumed.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel.activeStrapId) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val inputs = runCatching { dressInputs(context, viewModel) }.getOrNull()
                if (inputs != null) {
                    lastRecovery = inputs.recovery
                    val day = BiscuitDay.current(inputs.recovery, null, null, inputs.streak)
                    val dressedMood = day.mood(canSleep = true)
                    val dressedOutfits = day.outfits(sessionLive = inputs.sessionLive, readingData = false)
                    val coat = pokes.coat(inputs.recovery)
                    val hadCrown = BiscuitOutfit.CROWN in outfits
                    if (mood != dressedMood) {
                        mood = dressedMood
                        controller.mood = dressedMood
                    }
                    if (outfits != dressedOutfits) {
                        outfits = dressedOutfits
                        controller.outfits = dressedOutfits
                    }
                    if (controller.bodyColor != coat) controller.bodyColor = coat
                    if (BiscuitOutfit.CROWN in outfits && !hadCrown) controller.play(BiscuitClip.CELEBRATE)
                }
                delay(300_000)
            }
        }
    }

    // Poke milestones (parity `biscuitReactions`).
    LaunchedEffect(controller) {
        controller.onReaction = { reaction ->
            if (reaction != BiscuitReaction.SQUISH) {
                val said = pokes.poked()
                if (said != null) line = said
            }
        }
    }
    LaunchedEffect(line) {
        if (line == null) return@LaunchedEffect
        delay(5_000)
        pokes.clearLine()
        line = null
    }

    // Coat choice (parity `.task(id: pokes.wearsToasted)`).
    LaunchedEffect(wearsToasted) {
        controller.bodyColor = pokes.coat(lastRecovery)
    }

    LazyScreenScaffold(uiString(R.string.nav_biscuit), uiString(R.string.biscuit_subtitle)) {
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.biscuit_prompt), style = NoopType.footnote, color = Palette.textSecondary)
                    BiscuitArt(controller, modifier = Modifier.fillMaxWidth())
                    Text(
                        line?.let { uiString(it) } ?: if (playingDay) uiString(LAB_DAY[dayStep].label) else mood.title,
                        style = NoopType.footnote,
                        color = Palette.textSecondary,
                    )
                    NoopButton(
                        text = uiString(if (playingDay) R.string.biscuit_stop_day else R.string.biscuit_play_day),
                        kind = NoopButtonKind.Secondary,
                        fullWidth = true,
                        onClick = { playingDay = !playingDay },
                    )
                }
            }
        }
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.biscuit_moods), style = NoopType.headline, color = Palette.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BiscuitMood.entries.forEach { entry ->
                            FilterChip(
                                selected = mood == entry,
                                onClick = {
                                    playingDay = false
                                    mood = entry
                                    controller.mood = entry
                                },
                                label = { Text(entry.title) },
                            )
                        }
                    }
                }
            }
        }
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.biscuit_outfits), style = NoopType.headline, color = Palette.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BiscuitOutfit.entries.forEach { entry ->
                            FilterChip(
                                selected = entry in outfits,
                                onClick = {
                                    playingDay = false
                                    outfits = if (entry in outfits) outfits - entry else outfits + entry
                                },
                                label = { Text(entry.title) },
                            )
                        }
                    }
                }
            }
        }
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.biscuit_actions), style = NoopType.headline, color = Palette.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BiscuitClip.entries.forEach { entry ->
                            FilterChip(
                                selected = false,
                                onClick = { controller.play(entry) },
                                label = { Text(entry.title) },
                            )
                        }
                    }
                }
            }
        }
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.biscuit_coat_title), style = NoopType.footnote, color = Palette.textPrimary)
                    Text(uiString(R.string.biscuit_coat_detail), style = NoopType.caption, color = Palette.textSecondary)
                    Text(uiString(R.string.biscuit_coat_poked, pokes.count), style = NoopType.caption, color = Palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = !wearsToasted,
                            onClick = {
                                pokes.wearsToasted = false
                                wearsToasted = false
                            },
                            label = { Text(uiString(R.string.biscuit_coat_recovery)) },
                        )
                        FilterChip(
                            selected = wearsToasted,
                            onClick = {
                                pokes.wearsToasted = true
                                wearsToasted = true
                            },
                            label = { Text(uiString(R.string.biscuit_coat_toasted)) },
                        )
                    }
                }
            }
        }
    }
}

/** One step of the lab day playback: the mood, its outfits and the status label. */
private data class LabStep(val mood: BiscuitMood, val outfits: Set<BiscuitOutfit>, @StringRes val label: Int)

/** The seven lab steps in order (parity `NoopCompanionLab.day`). */
private val LAB_DAY = listOf(
    LabStep(BiscuitMood.HAPPY, emptySet(), R.string.biscuit_day_step_1),
    LabStep(BiscuitMood.CURIOUS, setOf(BiscuitOutfit.GLASSES), R.string.biscuit_day_step_2),
    LabStep(BiscuitMood.FOCUSED, setOf(BiscuitOutfit.COACH), R.string.biscuit_day_step_3),
    LabStep(BiscuitMood.PROUD, emptySet(), R.string.biscuit_day_step_4),
    LabStep(BiscuitMood.SLEEPY, setOf(BiscuitOutfit.NIGHTCAP), R.string.biscuit_day_step_5),
    LabStep(BiscuitMood.ASLEEP, setOf(BiscuitOutfit.NIGHTCAP), R.string.biscuit_day_step_6),
    LabStep(BiscuitMood.RADIANT, setOf(BiscuitOutfit.CROWN), R.string.biscuit_day_step_7),
)

/** The inputs the dress loop reads: today's recovery, live-session flag and streak. */
private data class DressInputs(val recovery: Double?, val sessionLive: Boolean, val streak: ActivityStreak.Reading?)

/**
 * Read the inputs the dress loop needs (same session and reading rules as the Goals screen).
 * Android stores no HRV low band, so the dip is never judged here.
 */
private suspend fun dressInputs(context: Context, viewModel: AppViewModel): DressInputs {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val now = Instant.now()
    val rows = viewModel.repo.workoutsAcrossSources(
        now.minusSeconds(400L * 86_400L).epochSecond,
        now.epochSecond + 86_400L,
        10_000,
    )
    val sessions = rows.mapNotNull { row ->
        val duration = row.durationS ?: (row.endTs - row.startTs).toDouble()
        if (duration <= 0.0) return@mapNotNull null
        if (WorkoutEditing.classify(row.source) == WorkoutSource.DETECTED && duration < 20 * 60) return@mapNotNull null
        val climb = Regex("(\\d+) m climbed", RegexOption.IGNORE_CASE).find(row.notes.orEmpty())
            ?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        PeriodGoal.Session(
            start = Instant.ofEpochSecond(row.startTs),
            durationS = duration,
            distanceM = row.distanceM ?: 0.0,
            climbM = climb,
            sport = if (row.sport.contains("strength", ignoreCase = true) || row.sport.contains("lift", ignoreCase = true))
                "Strength" else WorkoutEditing.displaySport(row.sport),
        )
    }
    val dayRows = viewModel.repo.daysMerged(viewModel.activeStrapId)
    val lowRecoveryDays = dayRows.mapNotNull { day ->
        if ((day.recovery ?: 100.0) >= 34.0) return@mapNotNull null
        runCatching { LocalDate.parse(day.day, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }.toSet()
    val snapshot = GoalsStore.load(context)
    val streak = if (!snapshot.enabled || !snapshot.streaksEnabled) {
        null
    } else {
        val start = snapshot.streakStartEpochDay?.let(LocalDate::ofEpochDay) ?: today
        val pausedDays = snapshot.pauses.flatMap { pause ->
            val pauseStart = LocalDate.ofEpochDay(pause.startEpochDay)
            val pauseEnd = LocalDate.ofEpochDay(pause.endEpochDay).coerceAtMost(today)
            if (pauseEnd.isBefore(pauseStart)) emptyList()
            else generateSequence(pauseStart) { day -> if (day < pauseEnd) day.plusDays(1) else null }
                .take(90).toList()
        }.filter { !it.isBefore(start) && !it.isAfter(today) }.toSet()
        val trainedDays = sessions.asSequence()
            .filter { !it.start.atZone(zone).toLocalDate().isBefore(start) }
            .map { it.start.atZone(zone).toLocalDate() }
            .toSet()
        ActivityStreak.reading(
            ActivityStreak.Input(
                trained = trainedDays,
                lowRecovery = lowRecoveryDays,
                paused = pausedDays,
                restPerWeek = snapshot.restPerWeek,
                freezesPerMonth = 1,
                earnedFreezes = snapshot.freezeBankEpochDays.map(LocalDate::ofEpochDay),
            ),
            today,
        )
    }
    return DressInputs(
        recovery = dayRows.lastOrNull { it.recovery != null }?.recovery,
        sessionLive = rows.any { it.endTs > now.epochSecond },
        streak = streak,
    )
}
