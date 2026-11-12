package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GoalWeekRecapTest {
    private val zone = ZoneId.of("UTC")

    @Test fun recapCountsKeptAndTrainedDaysAndWeeklyGoalProgress() {
        val today = LocalDate.parse("2026-11-10") // Tuesday
        val start = LocalDate.parse("2026-11-02")
        val days = listOf(
            ActivityStreak.Day.TRAINED, ActivityStreak.Day.REST, ActivityStreak.Day.PAUSED,
            ActivityStreak.Day.EARNED_REST, ActivityStreak.Day.FROZEN, ActivityStreak.Day.MISSED,
            ActivityStreak.Day.IDLE,
        ).mapIndexed { offset, day -> start.plusDays(offset.toLong()) to day }
        val reading = ActivityStreak.Reading(
            days = days, current = 1, longest = 4, atRisk = false,
            todayCover = ActivityStreak.Cover.NONE, restLeftThisWeek = 0,
            freezesLeftThisMonth = 0, freezesBanked = 0, since = start,
        )
        val session = PeriodGoal.Session(start.plusDays(2).atStartOfDay(zone).toInstant(), 3600.0, 0.0, 0.0, "Run")
        val goal = PeriodGoal("g", PeriodGoal.Period.WEEK, PeriodGoal.Metric.SESSIONS, null, 2.0, Instant.EPOCH)

        val recap = GoalWeekRecap.calculate(today, reading, listOf(goal), listOf(session), zone)!!

        assertEquals(start, recap.start)
        assertEquals(5, recap.daysKept)
        assertEquals(1, recap.trainedDays)
        assertEquals(1, recap.goals.size)
        assertFalse(recap.goals.single().complete)
        assertEquals(50, recap.goals.single().percent)
        assertFalse(recap.perfect)
    }

    @Test fun recapIsHiddenFromWednesdayOnwardAndAllowsGoalsWithoutStreakReading() {
        val wednesday = LocalDate.parse("2026-11-11")
        val goal = PeriodGoal("g", PeriodGoal.Period.WEEK, PeriodGoal.Metric.SESSIONS, null, 1.0, Instant.EPOCH)
        assertNull(GoalWeekRecap.calculate(wednesday, null, listOf(goal), emptyList(), zone))
        val monday = LocalDate.parse("2026-11-09")
        val recap = GoalWeekRecap.calculate(monday, null, listOf(goal), emptyList(), zone)
        assertTrue(recap != null)
        assertEquals(0, recap!!.daysKept)
        assertEquals(0, recap.trainedDays)
        assertEquals(1, recap.goals.size)
        assertEquals(0, recap.goals.single().percent)
    }
}
