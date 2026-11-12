package com.noop.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.noop.analytics.GoalWeekRecap
import com.noop.analytics.WeeklyQuests
import com.noop.analytics.StreakChallenge
import com.noop.analytics.GoalRewards
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
    var questDays by remember { mutableStateOf<List<WeeklyQuests.Day>>(emptyList()) }
    var questNights by remember { mutableStateOf<List<WeeklyQuests.Night>>(emptyList()) }
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
    var swapQuestDialog by remember { mutableStateOf(false) }
    var chooseStreakLeg by remember { mutableStateOf(false) }
    var openedChestMessage by remember { mutableStateOf<String?>(null) }
    var showRewardCollection by remember { mutableStateOf(false) }
    var equippedFlame by remember { mutableStateOf(GoalsStore.equippedFlame(context)) }
    val zone = remember { ZoneId.systemDefault() }

    LaunchedEffect(viewModel.activeStrapId) {
        val now = Instant.now()
        val from = now.minusSeconds(400L * 86_400L).epochSecond
        val rows = runCatching { viewModel.repo.workoutsAcrossSources(from, now.epochSecond + 86_400L, 10_000) }
            .getOrDefault(emptyList())
        sessions = rows.mapNotNull { row ->
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
        val dayRows = runCatching { viewModel.repo.daysMerged(viewModel.activeStrapId) }.getOrDefault(emptyList())
        questDays = dayRows.takeLast(80).mapNotNull { day ->
            val date = runCatching { LocalDate.parse(day.day, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() ?: return@mapNotNull null
            WeeklyQuests.Day(date.atStartOfDay(zone).toInstant(), day.strain, day.steps)
        }
        lowRecoveryDays = dayRows.mapNotNull { day ->
            if ((day.recovery ?: 100.0) >= 34.0) return@mapNotNull null
            runCatching { LocalDate.parse(day.day, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        }.toSet()
        val asleepByDay = dayRows.mapNotNull { day -> day.totalSleepMin?.let { day.day to it } }.toMap()
        val sleeps = runCatching {
            viewModel.repo.sleepSessionsMerged(
                viewModel.activeStrapId,
                now.minusSeconds(400L * 86_400L).epochSecond,
                now.epochSecond + 86_400L,
                10_000,
            )
        }.getOrDefault(emptyList())
        questNights = sleeps.mapNotNull { sleep ->
            val onset = sleep.effectiveStartTs
            val end = sleep.endTs
            if (end <= onset) return@mapNotNull null
            val spanMin = (end - onset).toDouble() / 60.0
            val endDay = Instant.ofEpochSecond(end).atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val total = asleepByDay[endDay]?.coerceAtMost(spanMin) ?: spanMin * 0.92
            WeeklyQuests.Night(Instant.ofEpochSecond(onset), if (spanMin >= 180) total else spanMin)
        }
    }

    val questInputs = remember(sessions, questDays, questNights, lowRecoveryDays, zone) {
        WeeklyQuests.Inputs(
            sessions = sessions,
            liftSport = "Strength",
            nights = questNights,
            days = questDays,
            lowRecovery = lowRecoveryDays.map { it.atStartOfDay(zone).toInstant() }.toSet(),
        )
    }

    fun questInputsSince(from: Instant): WeeklyQuests.Inputs = questInputs.copy(
        sessions = questInputs.sessions.filter { it.start >= from },
        nights = questInputs.nights.filter { it.onset >= from },
        days = questInputs.days.filter { it.date >= from },
        lowRecovery = questInputs.lowRecovery.filterTo(mutableSetOf()) { it >= from },
    )

    fun persist(next: GoalsSnapshot) {
        snapshot = next
        GoalsStore.save(context, next)
    }

    fun awardChest(chest: GoalRewards.Chest) {
        if (snapshot.rewardChests.none { it.id == chest.id }) {
            persist(snapshot.copy(rewardChests = snapshot.rewardChests + chest))
        }
    }

    LaunchedEffect(
        snapshot.enabled, snapshot.questWeek, snapshot.questOutcomes, snapshot.questsFromEpochSecond,
        sessions, questDays, questNights, lowRecoveryDays, zone,
    ) {
        if (!snapshot.enabled) return@LaunchedEffect
        val now = Instant.now()
        val from = snapshot.questsFromEpochSecond?.let(Instant::ofEpochSecond) ?: now
        val counted = questInputsSince(from)
        var outcomes = snapshot.questOutcomes
        var week = snapshot.questWeek
        val currentStart = now.atZone(zone).toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant()
        if (week?.start != currentStart) {
            if (week != null) outcomes = (outcomes + WeeklyQuests.outcomes(week, counted, zone)).takeLast(60)
            week = WeeklyQuests.plan(
                questInputs, now, zone, week, outcomes,
                firstEver = snapshot.questWeek == null && outcomes.isEmpty(),
            )
        }
        if (week?.starter == true && !week.swapUsed) {
            val personal = WeeklyQuests.plan(questInputs, now, zone, null, outcomes, outcomes.isEmpty())
            if (!personal.starter) week = personal
        }
        week = week?.let { WeeklyQuests.eased(it, questInputs, now, zone) }
        if (snapshot.questsFromEpochSecond == null || week != snapshot.questWeek || outcomes != snapshot.questOutcomes) {
            persist(snapshot.copy(
                questsFromEpochSecond = snapshot.questsFromEpochSecond ?: now.epochSecond,
                questWeek = week,
                questOutcomes = outcomes,
            ))
        }
    }

    val today = LocalDate.now(zone)
    val streakReading = remember(snapshot, sessions, lowRecoveryDays, today, zone) {
        if (!snapshot.enabled || !snapshot.streaksEnabled) null else {
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
    }
    LaunchedEffect(snapshot.enabled, snapshot.streaksEnabled, snapshot.streakChallenge, snapshot.reachedChallenge, snapshot.streakLegOptions, snapshot.challengeEndTarget, snapshot.challengeEndReached, streakReading, today) {
        if (!snapshot.enabled || !snapshot.streaksEnabled) return@LaunchedEffect
        val reading = streakReading ?: return@LaunchedEffect
        var challenge = snapshot.streakChallenge
        var reachedChallenge = snapshot.reachedChallenge
        var options = snapshot.streakLegOptions
        var broken = snapshot.streakChallengeBroken
        var endTarget = snapshot.challengeEndTarget
        var endAt = snapshot.challengeEndAt
        var endReached = snapshot.challengeEndReached
        if (challenge == null) {
            val (base, target) = StreakChallenge.leg(reading.current, broken)
            challenge = StreakChallenge.Challenge(target, Instant.now().epochSecond, reading.since?.toEpochDay(), reading.current, base)
        } else {
            when (StreakChallenge.progress(challenge, reading).state) {
                StreakChallenge.State.COMPLETE -> {
                    val awarded = GoalRewards.Chest(
                        id = "ch-${challenge.acceptedEpochSecond}-${challenge.target}",
                        tier = GoalRewards.tierForLeg(challenge.legStart, challenge.target),
                        reason = "Day ${challenge.target}",
                        earnedEpochSecond = Instant.now().epochSecond,
                    )
                    awardChest(awarded)
                    reachedChallenge = challenge
                    options = StreakChallenge.options(challenge.target)
                    endReached = challenge.target
                    endTarget = null
                    endAt = null
                    val target = options[1]
                    challenge = StreakChallenge.Challenge(target, Instant.now().epochSecond, reading.since?.toEpochDay(), reading.current, challenge.target)
                    broken = false
                }
                StreakChallenge.State.BROKEN -> {
                    val ended = challenge
                    challenge = null; options = emptyList(); broken = true
                    reachedChallenge = null
                    endTarget = ended?.target
                    endAt = ended?.let { maxOf(it.startCount, it.peak ?: 0) }
                    endReached = null
                }
                StreakChallenge.State.ACTIVE -> {
                    if (reading.current > (challenge.peak ?: 0)) challenge = challenge.copy(peak = reading.current)
                    if (reading.current > 0 && broken) { broken = false; endTarget = null; endAt = null; endReached = null }
                }
            }
        }
        if (challenge != snapshot.streakChallenge || reachedChallenge != snapshot.reachedChallenge || options != snapshot.streakLegOptions || broken != snapshot.streakChallengeBroken ||
            endTarget != snapshot.challengeEndTarget || endAt != snapshot.challengeEndAt || endReached != snapshot.challengeEndReached) {
            persist(snapshot.copy(streakChallenge = challenge, reachedChallenge = reachedChallenge, streakLegOptions = options, streakChallengeBroken = broken,
                challengeEndTarget = endTarget, challengeEndAt = endAt, challengeEndReached = endReached))
        }
    }
    val recap = remember(snapshot, sessions, streakReading, today, zone) {
        if (!snapshot.enabled) null else GoalWeekRecap.calculate(today, streakReading, snapshot.goals, sessions, zone)
    }
    val activeQuestReadings = remember(snapshot.questWeek, snapshot.questsFromEpochSecond, questInputs, zone) {
        val week = snapshot.questWeek
        val from = snapshot.questsFromEpochSecond?.let(Instant::ofEpochSecond)
        if (week == null || from == null) emptyList() else WeeklyQuests.readings(week, questInputsSince(from), zone)
    }

    LaunchedEffect(snapshot.enabled, snapshot.streaksEnabled, snapshot.questWeek, snapshot.chestsFromEpochSecond,
        snapshot.streakStartEpochDay, activeQuestReadings, recap, questInputs, today, zone) {
        if (!snapshot.enabled || !snapshot.streaksEnabled) return@LaunchedEffect
        val chestStart = snapshot.chestsFromEpochSecond?.let(Instant::ofEpochSecond) ?: return@LaunchedEffect
        val week = snapshot.questWeek ?: return@LaunchedEffect
        val earningInputs = questInputsSince(chestStart)
        val paying = WeeklyQuests.readings(week, earningInputs, zone)
        val stamp = week.start.epochSecond
        paying.filter { it.complete }.forEach { result ->
            val tier = when (result.quest.tier) {
                WeeklyQuests.Tier.BRONZE -> GoalRewards.Tier.COMMON
                WeeklyQuests.Tier.SILVER -> GoalRewards.Tier.RARE
                WeeklyQuests.Tier.GOLD -> GoalRewards.Tier.EPIC
            }
            val tierName = result.quest.tier.name.lowercase().replaceFirstChar { it.uppercase() }
            awardChest(GoalRewards.Chest("q-$stamp-${result.quest.tier.ordinal}", tier, "$tierName weekly quest", Instant.now().epochSecond))
        }
        if (paying.size == 3 && paying.all { it.complete }) {
            val tier = if (WeeklyQuests.allThreeChestIsLegendary(week.start)) GoalRewards.Tier.LEGENDARY else GoalRewards.Tier.EPIC
            awardChest(GoalRewards.Chest("qa-$stamp", tier, "All three quests this week", Instant.now().epochSecond))
        }
        recap?.takeIf { it.perfect }?.let { perfect ->
            val start = perfect.start.atStartOfDay(zone).toInstant()
            awardChest(GoalRewards.Chest("pw-${start.epochSecond}", GoalRewards.Tier.RARE, "A perfect week", Instant.now().epochSecond))
        }
        val streakStart = snapshot.streakStartEpochDay?.let(LocalDate::ofEpochDay) ?: return@LaunchedEffect
        val lastWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).minusWeeks(1)
        val lastWeekEnd = lastWeekStart.plusWeeks(1)
        val lowMornings = lowRecoveryDays.count { !it.isBefore(maxOf(streakStart, lastWeekStart)) && it.isBefore(lastWeekEnd) }
        val showedUp = earningInputs.sessions.any { session ->
            val date = session.start.atZone(zone).toLocalDate()
            !date.isBefore(lastWeekStart) && date.isBefore(lastWeekEnd)
        }
        if (lowMornings >= 3 && showedUp) {
            val start = lastWeekStart.atStartOfDay(zone).toInstant().epochSecond
            val tier = if (GoalRewards.stableHash("hw-$start") % 4uL == 0uL) GoalRewards.Tier.EPIC else GoalRewards.Tier.RARE
            awardChest(GoalRewards.Chest("hw-$start", tier, "A hard week, and you still showed up", Instant.now().epochSecond))
        }
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
                                streakChallenge = if (it) snapshot.streakChallenge else null,
                                reachedChallenge = if (it) snapshot.reachedChallenge else null,
                                streakLegOptions = if (it) snapshot.streakLegOptions else emptyList(),
                                streakChallengeBroken = if (it) snapshot.streakChallengeBroken else false,
                                challengeEndTarget = if (it) snapshot.challengeEndTarget else null,
                                challengeEndAt = if (it) snapshot.challengeEndAt else null,
                                challengeEndReached = if (it) snapshot.challengeEndReached else null,
                                chestsFromEpochSecond = if (it && snapshot.streaksEnabled) snapshot.chestsFromEpochSecond else null,
                                questsFromEpochSecond = if (it) Instant.now().epochSecond else null,
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
                                    persist(snapshot.copy(
                                        streaksEnabled = enabled,
                                        streakStartEpochDay = if (enabled) today.toEpochDay() else null,
                                        streakChallenge = null,
                                        reachedChallenge = null,
                                        streakLegOptions = emptyList(),
                                        streakChallengeBroken = false,
                                        challengeEndTarget = null,
                                        challengeEndAt = null,
                                        challengeEndReached = null,
                                        chestsFromEpochSecond = if (enabled) Instant.now().epochSecond else null,
                                    ))
                                },
                            )
                        }
                        if (snapshot.streaksEnabled) {
                            val reading = streakReading ?: ActivityStreak.reading(ActivityStreak.Input(emptySet()), today)
                            Text(
                                uiString(com.noop.R.string.goals_streak_summary, reading.current, reading.longest),
                                style = NoopType.body,
                                color = Palette.accent,
                            )
                            snapshot.streakChallenge?.let { challenge ->
                                val challengeProgress = StreakChallenge.progress(challenge, reading)
                                if (snapshot.streakLegOptions.isNotEmpty()) {
                                    Text(uiString(com.noop.R.string.goals_streak_reached, challenge.base ?: challenge.startCount), style = NoopType.headline, color = Palette.accent)
                                    TextButton(onClick = { chooseStreakLeg = true }) { Text(uiString(com.noop.R.string.goals_streak_choose_milestone)) }
                                } else {
                                    Text(uiString(com.noop.R.string.goals_streak_milestone, challenge.target), style = NoopType.headline, color = Palette.textPrimary)
                                    LinearProgressIndicator(
                                        progress = { challengeProgress.done.toFloat().div(challenge.target).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = flameTokenColor(equippedFlame),
                                    )
                                    Text(uiString(com.noop.R.string.goals_streak_milestone_progress, challengeProgress.done, challenge.target), style = NoopType.footnote, color = Palette.textSecondary)
                                }
                            }
                            if (snapshot.streakChallengeBroken) Text(uiString(com.noop.R.string.goals_streak_broken), style = NoopType.footnote, color = Palette.textSecondary)
                            snapshot.rewardChests.firstOrNull { it.openedEpochSecond == null }?.let { chest ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(uiString(com.noop.R.string.goals_reward_unopened, snapshot.rewardChests.count { it.openedEpochSecond == null }),
                                        style = NoopType.footnote, color = Palette.textSecondary, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        val reward = GoalRewards.roll(chest, snapshot.ownedRewards.toSet(), reading.freezesBanked)
                                        val updated = snapshot.rewardChests.map { if (it.id == chest.id) it.copy(
                                            openedEpochSecond = Instant.now().epochSecond, reward = reward,
                                        ) else it }
                                        val message = when (reward) {
                                            is GoalRewards.Reward.Freeze -> uiString(com.noop.R.string.goals_reward_freeze, reward.count)
                                            is GoalRewards.Reward.Item -> uiString(com.noop.R.string.goals_reward_item)
                                        }
                                        val freezes = if (reward is GoalRewards.Reward.Freeze)
                                            snapshot.freezeBankEpochDays + List(reward.count) { today.toEpochDay() }
                                        else snapshot.freezeBankEpochDays
                                        val owned = (snapshot.ownedRewards + (reward as? GoalRewards.Reward.Item)?.id.orEmpty()).filter { it.isNotBlank() }.distinct()
                                        persist(snapshot.copy(rewardChests = updated, freezeBankEpochDays = freezes, ownedRewards = owned))
                                        openedChestMessage = message
                                    }) { Text(uiString(com.noop.R.string.goals_reward_open)) }
                                }
                            }
                            if (snapshot.ownedRewards.isNotEmpty()) Text(
                                uiString(com.noop.R.string.goals_reward_owned, snapshot.ownedRewards.size),
                                style = NoopType.footnote,
                                color = Palette.textSecondary,
                            )
                            TextButton(onClick = { showRewardCollection = true }) {
                                Text(uiString(com.noop.R.string.goals_reward_collection))
                            }
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
            recap?.let { weekRecap ->
                item {
                    NoopCard(tint = if (weekRecap.perfect) Palette.accent else null) {
                        Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                            Text(uiString(com.noop.R.string.goals_last_week), style = NoopType.title2, color = Palette.textPrimary)
                            if (snapshot.streaksEnabled) {
                                Text(
                                    if (weekRecap.perfect) uiString(com.noop.R.string.goals_perfect_week)
                                    else uiString(com.noop.R.string.goals_kept_days, weekRecap.daysKept),
                                    style = NoopType.headline,
                                    color = if (weekRecap.perfect) Palette.accent else Palette.textPrimary,
                                )
                                Text(
                                    uiString(com.noop.R.string.goals_days_trained, weekRecap.trainedDays),
                                    style = NoopType.footnote,
                                    color = Palette.textSecondary,
                                )
                            }
                            weekRecap.goals.forEach { result ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        editGoal = result.goal
                                        editTarget = when (result.goal.metric) {
                                            PeriodGoal.Metric.DISTANCE -> format(result.goal.target / 1000.0)
                                            PeriodGoal.Metric.TIME -> format(result.goal.target / 3600.0)
                                            else -> format(result.goal.target)
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(goalTitle(result.goal), style = NoopType.footnote, color = Palette.textSecondary, modifier = Modifier.weight(1f))
                                    Text(
                                        if (result.complete) uiString(com.noop.R.string.goals_recap_done)
                                        else uiString(com.noop.R.string.goals_recap_percent, result.percent),
                                        style = NoopType.footnote,
                                        color = if (result.complete) Palette.accent else Palette.textPrimary,
                                    )
                                }
                            }
                            weekRecap.goals.firstOrNull()?.let { result ->
                                TextButton(onClick = {
                                    editGoal = result.goal
                                    editTarget = when (result.goal.metric) {
                                        PeriodGoal.Metric.DISTANCE -> format(result.goal.target / 1000.0)
                                        PeriodGoal.Metric.TIME -> format(result.goal.target / 3600.0)
                                        else -> format(result.goal.target)
                                    }
                                }) { Text(uiString(com.noop.R.string.goals_recap_adjust)) }
                            }
                        }
                    }
                }
            }
            snapshot.questWeek?.let { week ->
                item {
                    NoopCard(tint = Palette.accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                            Text(uiString(com.noop.R.string.goals_quests_title), style = NoopType.title2, color = Palette.textPrimary)
                            Text(uiString(com.noop.R.string.goals_quests_detail), style = NoopType.footnote, color = Palette.textSecondary)
                            activeQuestReadings.forEach { reading ->
                                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap / 2)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            uiString(questTierString(reading.quest.tier)),
                                            style = NoopType.footnote,
                                            color = Palette.accent,
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            uiString(com.noop.R.string.goals_quest_value, questProgressValue(reading, done = true), questProgressValue(reading, done = false)),
                                            style = NoopType.footnote,
                                            color = if (reading.complete) Palette.accent else Palette.textSecondary,
                                        )
                                    }
                                    Text(questTitle(reading.quest.kind), style = NoopType.body, color = Palette.textPrimary)
                                    LinearProgressIndicator(
                                        progress = { reading.fraction.toFloat() },
                                        modifier = Modifier.fillMaxWidth().height(5.dp),
                                        color = Palette.accent,
                                        trackColor = Palette.surfaceInset,
                                    )
                                }
                            }
                            if (!week.swapUsed) {
                                TextButton(onClick = { swapQuestDialog = true }) {
                                    Text(uiString(com.noop.R.string.goals_quest_swap))
                                }
                            } else {
                                Text(uiString(com.noop.R.string.goals_quest_swap_used), style = NoopType.caption, color = Palette.textTertiary)
                            }
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

    if (chooseStreakLeg) AlertDialog(
        onDismissRequest = { chooseStreakLeg = false },
        title = { Text(uiString(com.noop.R.string.goals_streak_choose_milestone)) },
        text = { Column { snapshot.streakLegOptions.forEach { target ->
            TextButton(onClick = {
                val current = snapshot.streakChallenge
                if (current != null) persist(snapshot.copy(
                    streakChallenge = current.copy(target = target),
                    reachedChallenge = null,
                    streakLegOptions = emptyList(), streakChallengeBroken = false,
                    challengeEndTarget = null, challengeEndAt = null, challengeEndReached = null,
                ))
                chooseStreakLeg = false
            }) { Text(uiString(com.noop.R.string.goals_streak_milestone, target)) }
        } } },
        confirmButton = { TextButton(onClick = { chooseStreakLeg = false }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) } },
    )

    openedChestMessage?.let { message -> AlertDialog(
        onDismissRequest = { openedChestMessage = null },
        title = { Text(uiString(com.noop.R.string.goals_reward_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { openedChestMessage = null }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) } },
    ) }

    if (showRewardCollection) AlertDialog(
        onDismissRequest = { showRewardCollection = false },
        title = { Text(uiString(com.noop.R.string.goals_reward_collection)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(uiString(com.noop.R.string.goals_reward_owned, snapshot.ownedRewards.size), style = NoopType.footnote, color = Palette.textSecondary)
                listOf("ember", "sunset", "citrus", "ocean", "jade", "rose", "ice", "gold", "aurora", "void").forEach { id ->
                    val unlocked = id == "ember" || id in snapshot.ownedRewards
                    val active = equippedFlame == id
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(uiString(flameNameResource(id)), style = NoopType.body, color = if (unlocked) Palette.textPrimary else Palette.textTertiary, modifier = Modifier.weight(1f))
                        if (active) Text(uiString(com.noop.R.string.goals_reward_equipped), style = NoopType.footnote, color = flameTokenColor(id))
                        else if (unlocked) TextButton(onClick = {
                            equippedFlame = id
                            GoalsStore.setEquippedFlame(context, id)
                        }) { Text(uiString(com.noop.R.string.goals_reward_use)) }
                        else Text(uiString(com.noop.R.string.goals_reward_locked), style = NoopType.footnote, color = Palette.textTertiary)
                    }
                }
                snapshot.ownedRewards.filterNot { it in setOf("sunset", "citrus", "ocean", "jade", "rose", "ice", "gold", "aurora", "void") }
                    .forEach { id -> Text(uiString(com.noop.R.string.goals_reward_other_item, id), style = NoopType.footnote, color = Palette.textSecondary) }
            }
        },
        confirmButton = { TextButton(onClick = { showRewardCollection = false }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) } },
    )

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

    if (swapQuestDialog) {
        val week = snapshot.questWeek
        AlertDialog(
            onDismissRequest = { swapQuestDialog = false },
            title = { Text(uiString(com.noop.R.string.goals_quest_swap_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    week?.quests?.forEach { quest ->
                        TextButton(
                            onClick = {
                                val updated = WeeklyQuests.swap(week, quest.tier, questInputs, snapshot.questOutcomes, Instant.now(), zone)
                                persist(snapshot.copy(questWeek = updated))
                                swapQuestDialog = false
                            },
                        ) { Text(uiString(com.noop.R.string.goals_quest_option, uiString(questTierString(quest.tier)), questTitle(quest.kind))) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { swapQuestDialog = false }) { Text(uiString(com.noop.R.string.l10n_backup_sync_screen_cancel_77dfd213)) } },
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

private fun questTierString(tier: WeeklyQuests.Tier): Int = when (tier) {
    WeeklyQuests.Tier.BRONZE -> com.noop.R.string.goals_quest_bronze
    WeeklyQuests.Tier.SILVER -> com.noop.R.string.goals_quest_silver
    WeeklyQuests.Tier.GOLD -> com.noop.R.string.goals_quest_gold
}

private fun questTitle(kind: WeeklyQuests.Kind): String = when (kind) {
    WeeklyQuests.Kind.Sessions -> uiString(com.noop.R.string.goals_quest_sessions)
    WeeklyQuests.Kind.Minutes -> uiString(com.noop.R.string.goals_quest_minutes)
    is WeeklyQuests.Kind.Distance -> uiString(com.noop.R.string.goals_quest_distance, kind.sport)
    WeeklyQuests.Kind.Lifts -> uiString(com.noop.R.string.goals_quest_lifts)
    is WeeklyQuests.Kind.EarlyNights -> uiString(com.noop.R.string.goals_quest_early)
    is WeeklyQuests.Kind.LongNights -> uiString(com.noop.R.string.goals_quest_long, kind.minutes)
    is WeeklyQuests.Kind.StepDays -> uiString(com.noop.R.string.goals_quest_steps, format(kind.steps.toDouble()))
    WeeklyQuests.Kind.EasyAfterHard -> uiString(com.noop.R.string.goals_quest_easy)
}

private fun questProgressValue(reading: WeeklyQuests.Reading, done: Boolean): String {
    val value = if (done) reading.done else reading.quest.target
    return when (reading.quest.kind) {
        WeeklyQuests.Kind.Minutes -> uiString(com.noop.R.string.goals_quest_minutes_value, format(value / 60))
        is WeeklyQuests.Kind.Distance -> uiString(com.noop.R.string.goals_quest_distance_value, format(value / 1000))
        else -> format(value)
    }
}

private fun flameNameResource(id: String): Int = when (id) {
    "ember" -> com.noop.R.string.goals_flame_ember
    "sunset" -> com.noop.R.string.goals_flame_sunset
    "citrus" -> com.noop.R.string.goals_flame_citrus
    "ocean" -> com.noop.R.string.goals_flame_ocean
    "jade" -> com.noop.R.string.goals_flame_jade
    "rose" -> com.noop.R.string.goals_flame_rose
    "ice" -> com.noop.R.string.goals_flame_ice
    "gold" -> com.noop.R.string.goals_flame_gold
    "aurora" -> com.noop.R.string.goals_flame_aurora
    else -> com.noop.R.string.goals_flame_void
}

private fun flameTokenColor(id: String) = when (id) {
    "sunset", "rose" -> Palette.metricRose
    "citrus", "gold" -> Palette.metricAmber
    "ocean", "ice", "aurora" -> Palette.metricCyan
    "jade" -> Palette.statusPositive
    "void" -> Palette.metricPurple
    else -> Palette.accent
}
