package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeeklyQuestsTest {
    private val zone = ZoneId.of("UTC")
    private val monday = LocalDate.parse("2026-11-09").atStartOfDay(zone).toInstant()

    @Test fun noHistoryCreatesThreeTierStarterWeekWithAuraUnits() {
        val now = monday.plusSeconds(2 * 86_400L)
        val week = WeeklyQuests.plan(WeeklyQuests.Inputs(emptyList(), "Strength"), now, zone, null, emptyList(), true)
        assertTrue(week.starter)
        assertEquals(monday, week.start)
        assertEquals(listOf(WeeklyQuests.Tier.BRONZE, WeeklyQuests.Tier.SILVER, WeeklyQuests.Tier.GOLD), week.quests.map { it.tier })
        assertEquals(1.0, week.quests[0].target, 0.0)
        assertEquals(2700.0, week.quests[1].target, 0.0)
        assertEquals(2.0, week.quests[2].target, 0.0)
    }

    @Test fun starterWeekProratesRemainderAndSwapCanOnlyBeUsedOnce() {
        val now = monday
        val first = WeeklyQuests.plan(WeeklyQuests.Inputs(emptyList(), "Strength"), now, zone, null, emptyList(), true)
        assertEquals(3.0, first.quests.last().target, 0.0)
        val swapped = WeeklyQuests.swap(first, WeeklyQuests.Tier.BRONZE, WeeklyQuests.Inputs(emptyList(), "Strength"), emptyList(), now, zone)
        assertTrue(swapped.swapUsed)
        assertEquals(WeeklyQuests.Kind.Minutes, swapped.quests.first().kind)
        val secondSwap = WeeklyQuests.swap(swapped, WeeklyQuests.Tier.SILVER, WeeklyQuests.Inputs(emptyList(), "Strength"), emptyList(), now, zone)
        assertEquals(swapped, secondSwap)
    }

    @Test fun minuteQuestValuesUseSecondsAndRecoveryCanEaseHigherTierLoad() {
        val now = monday.plusSeconds(3 * 86_400L)
        val week = WeeklyQuests.Week(
            monday,
            listOf(
                WeeklyQuests.Quest(WeeklyQuests.Kind.Sessions, WeeklyQuests.Tier.BRONZE, 1.0),
                WeeklyQuests.Quest(WeeklyQuests.Kind.Minutes, WeeklyQuests.Tier.SILVER, 3600.0),
                WeeklyQuests.Quest(WeeklyQuests.Kind.Distance("Run"), WeeklyQuests.Tier.GOLD, 10_000.0),
            ),
        )
        val sessions = listOf(PeriodGoal.Session(monday.plusSeconds(3 * 3600), 1800.0, 2500.0, 0.0, "Run"))
        val inputs = WeeklyQuests.Inputs(sessions, "Strength", lowRecovery = setOf(monday, monday.plusSeconds(86_400)))
        assertEquals(1800.0, WeeklyQuests.done(week.quests[1], monday, inputs, zone), 0.0)
        val eased = WeeklyQuests.eased(week, inputs, now, zone)
        assertEquals(6000.0, eased.quests[2].target, 0.0)
        assertTrue(eased.quests[2].eased)
        assertFalse(eased.quests[0].eased)
    }

    @Test fun personalizedPlannerBuildsThreeDistinctTieredAreasDeterministically() {
        val sessions = mutableListOf<PeriodGoal.Session>()
        val days = mutableListOf<WeeklyQuests.Day>()
        val nights = mutableListOf<WeeklyQuests.Night>()
        for (weekOffset in 1L..8L) {
            val weekStart = monday.minusSeconds(weekOffset * 7 * 86_400L)
            for (dayOffset in 0L..6L) {
                val day = weekStart.plusSeconds(dayOffset * 86_400L)
                days += WeeklyQuests.Day(day, strain = if (dayOffset % 2L == 0L) 15.0 else 5.0, steps = 6000 + dayOffset.toInt() * 100)
                nights += WeeklyQuests.Night(day.plusSeconds(22 * 3600L), asleepMin = 430.0)
                if (dayOffset == 0L || dayOffset == 2L || dayOffset == 4L) {
                    sessions += PeriodGoal.Session(day.plusSeconds(8 * 3600L), 3600.0, 5000.0, 0.0, "Run")
                }
                if (dayOffset == 1L || dayOffset == 5L) {
                    sessions += PeriodGoal.Session(day.plusSeconds(12 * 3600L), 3600.0, 0.0, 0.0, "Strength")
                }
            }
        }
        val input = WeeklyQuests.Inputs(sessions, "Strength", nights, days)
        val now = monday.plusSeconds(12 * 3600L)
        val first = WeeklyQuests.plan(input, now, zone, null, emptyList(), firstEver = false)
        val again = WeeklyQuests.plan(input, now, zone, null, emptyList(), firstEver = false)
        assertFalse(first.starter)
        assertEquals(3, first.quests.size)
        assertEquals(first, again)
        assertEquals(3, first.quests.map { it.tier }.distinct().size)
        assertEquals(3, first.quests.map { it.kind.area }.distinct().size)
    }
}
