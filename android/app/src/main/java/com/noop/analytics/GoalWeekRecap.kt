package com.noop.analytics

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Previous-week recap data shown on Monday and Tuesday, matching Aura's Goals screen cadence. */
object GoalWeekRecap {
    data class GoalResult(val goal: PeriodGoal, val complete: Boolean, val percent: Int)
    data class Recap(
        val start: LocalDate,
        val endExclusive: LocalDate,
        val daysKept: Int,
        val trainedDays: Int,
        val goals: List<GoalResult>,
    ) {
        val perfect: Boolean get() = daysKept >= 7
    }

    fun calculate(
        today: LocalDate,
        reading: ActivityStreak.Reading?,
        goals: List<PeriodGoal>,
        sessions: List<PeriodGoal.Session>,
        zone: ZoneId,
    ): Recap? {
        val mondayOffset = today.dayOfWeek.value - DayOfWeek.MONDAY.value
        if (mondayOffset !in 0..1) return null
        if (reading?.days.isNullOrEmpty() && goals.isEmpty()) return null

        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
        val end = start.plusWeeks(1)
        val days = reading?.days.orEmpty().filter { (date, _) -> !date.isBefore(start) && date.isBefore(end) }
        val kept = days.count { (_, day) ->
            day == ActivityStreak.Day.TRAINED || day == ActivityStreak.Day.EARNED_REST ||
                day == ActivityStreak.Day.REST || day == ActivityStreak.Day.PAUSED || day == ActivityStreak.Day.FROZEN
        }
        val trained = days.count { (_, day) -> day == ActivityStreak.Day.TRAINED }
        val span = PeriodGoal.Span(start.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant())
        val now = today.atStartOfDay(zone).toInstant()
        val weeklyGoals = goals.filter { it.period == PeriodGoal.Period.WEEK }.map { goal ->
            val progress = goal.progress(sessions, span, now, zone)
            GoalResult(goal, progress.complete, (progress.fraction * 100).toInt().coerceIn(0, 100))
        }
        return Recap(start, end, kept, trained, weeklyGoals)
    }
}
