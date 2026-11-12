package com.noop.analytics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStreakTest {
    @Test fun unusedWeeklyRestDaysKeepTheActivityStreakAlive() {
        val monday = LocalDate.parse("2026-11-02")
        val reading = ActivityStreak.reading(
            ActivityStreak.Input(trained = setOf(monday, monday.plusDays(3))),
            today = monday.plusDays(4),
        )

        assertEquals(4, reading.current)
        assertEquals(ActivityStreak.Day.REST, reading.day(monday.plusDays(1)))
        assertEquals(ActivityStreak.Day.REST, reading.day(monday.plusDays(2)))
        assertEquals(ActivityStreak.Day.PENDING, reading.day(monday.plusDays(4)))
        assertEquals(ActivityStreak.Cover.FREEZE, reading.todayCover)
        assertFalse(reading.atRisk)
    }

    @Test fun missedDayBreaksRunButAValidEarnedFreezeCanCoverIt() {
        val monday = LocalDate.parse("2026-11-02")
        val training = setOf(monday, monday.plusDays(8))
        val broken = ActivityStreak.reading(
            ActivityStreak.Input(trained = training, restPerWeek = 0, freezesPerMonth = 0),
            today = monday.plusDays(8),
        )
        val coveredTraining = setOf(monday) + (2L..8L).map { monday.plusDays(it) }
        val covered = ActivityStreak.reading(
            ActivityStreak.Input(
                trained = coveredTraining,
                restPerWeek = 0,
                freezesPerMonth = 0,
                earnedFreezes = listOf(monday.plusDays(1)),
            ),
            today = monday.plusDays(8),
        )

        assertEquals(1, broken.current)
        assertEquals(8, covered.current)
        assertEquals(ActivityStreak.Day.FROZEN, covered.day(monday.plusDays(1)))
    }

    @Test fun pausesDoNotBreakAStreakAndMilestonesAdvance() {
        val day = LocalDate.parse("2026-11-02")
        val reading = ActivityStreak.reading(
            ActivityStreak.Input(trained = setOf(day), paused = setOf(day.plusDays(1)), restPerWeek = 0),
            today = day.plusDays(1),
        )
        assertEquals(1, reading.current)
        assertEquals(ActivityStreak.Day.PAUSED, reading.day(day.plusDays(1)))
        assertTrue(reading.atRisk.not())
        assertEquals(14, ActivityStreak.nextMilestone(7))
        assertEquals(1095, ActivityStreak.nextMilestone(1000))
    }
}
