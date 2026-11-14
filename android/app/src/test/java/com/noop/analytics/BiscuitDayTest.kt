package com.noop.analytics

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Parity pin for [BiscuitDay] mood / outfit / day-building logic (Aura `NoopBiscuitDay`). */
class BiscuitDayTest {

    private fun at(hour: Int) = ZonedDateTime.of(2026, 11, 14, hour, 0, 0, 0, ZoneOffset.UTC)

    private fun day(
        recovery: Double? = null,
        hrvDip: Boolean = false,
        trained: Boolean = false,
        milestone: Boolean = false,
        atRisk: Boolean = false,
    ) = BiscuitDay(recovery, hrvDip, trained, milestone, atRisk)

    // ── mood ──

    @Test
    fun milestoneMoodIsRadiantAtAllHours() {
        val d = day(milestone = true)
        for (hour in 0..23) assertEquals(BiscuitMood.RADIANT, d.mood(at(hour)))
    }

    @Test
    fun trainedMoodIsProud() {
        val d = day(trained = true, milestone = false)
        assertEquals(BiscuitMood.PROUD, d.mood(at(12)))
        assertEquals(BiscuitMood.PROUD, d.mood(at(23), canSleep = true))
        assertEquals(BiscuitMood.PROUD, d.mood(at(19), canSleep = false))
    }

    @Test
    fun atRiskMoodIsWorriedOnlyEvening() {
        val d = day(atRisk = true)
        assertEquals(BiscuitMood.WORRIED, d.mood(at(18)))
        assertEquals(BiscuitMood.WORRIED, d.mood(at(22)))
        assertEquals(BiscuitMood.NEUTRAL, d.mood(at(17)))
        assertEquals(BiscuitMood.SLEEPY, d.mood(at(0)))  // late night beats the at-risk branch
    }

    @Test
    fun lateNightMoodIsAsleepOnlyWhenCanSleep() {
        val d = day()
        for (hour in intArrayOf(23, 0, 1, 2, 3, 4)) {
            assertEquals(BiscuitMood.ASLEEP, d.mood(at(hour), canSleep = true))
            assertEquals(BiscuitMood.SLEEPY, d.mood(at(hour), canSleep = false))
        }
        assertEquals(BiscuitMood.NEUTRAL, d.mood(at(5)))
    }

    @Test
    fun lowRecoveryIsSleepy() {
        assertEquals(BiscuitMood.SLEEPY, day(recovery = 33.9).mood(at(12)))
        assertEquals(BiscuitMood.SLEEPY, day(recovery = 33.9).mood(at(21)))
        assertEquals(BiscuitMood.CALM, day(recovery = 34.0).mood(at(21)))
    }

    @Test
    fun hrvDipIsSleepy() {
        assertEquals(BiscuitMood.SLEEPY, day(hrvDip = true).mood(at(12)))
        assertEquals(BiscuitMood.SLEEPY, day(hrvDip = true).mood(at(21)))
    }

    @Test
    fun goodRecoveryIsHappy() {
        assertEquals(BiscuitMood.HAPPY, day(recovery = 67.0).mood(at(12)))
        assertEquals(BiscuitMood.HAPPY, day(recovery = 90.0).mood(at(21)))
        assertEquals(BiscuitMood.CALM, day(recovery = 66.9).mood(at(21)))
    }

    @Test
    fun fallbackMoodTurnsCalmAfter20() {
        val d = day()
        assertEquals(BiscuitMood.NEUTRAL, d.mood(at(19)))
        assertEquals(BiscuitMood.CALM, d.mood(at(20)))
        assertEquals(BiscuitMood.CALM, d.mood(at(21)))
        assertEquals(BiscuitMood.NEUTRAL, d.mood(at(5)))
    }

    @Test
    fun nullRecoverySkipsRecoveryBranches() {
        assertEquals(BiscuitMood.NEUTRAL, day(recovery = null).mood(at(12)))
        assertEquals(BiscuitMood.CALM, day(recovery = null).mood(at(21)))
    }

    // ── outfits ──

    @Test
    fun outfitChainPrefersSessionThenMilestoneThenNightcap() {
        val d = day()
        assertEquals(setOf(BiscuitOutfit.COACH), d.outfits(at(12), sessionLive = true))
        assertEquals(setOf(BiscuitOutfit.COACH, BiscuitOutfit.GLASSES), d.outfits(at(23), sessionLive = true, readingData = true))
        val m = day(milestone = true)
        assertEquals(setOf(BiscuitOutfit.CROWN), m.outfits(at(12)))
        assertEquals(setOf(BiscuitOutfit.CROWN), m.outfits(at(23)))
        for (hour in intArrayOf(22, 23, 0, 1, 2, 3, 4)) {
            assertEquals(setOf(BiscuitOutfit.NIGHTCAP), d.outfits(at(hour)))
        }
        assertEquals(emptySet<BiscuitOutfit>(), d.outfits(at(5)))
        assertEquals(emptySet<BiscuitOutfit>(), d.outfits(at(21)))
    }

    @Test
    fun glassesAreAdditive() {
        val d = day()
        assertEquals(
            setOf(BiscuitOutfit.NIGHTCAP, BiscuitOutfit.GLASSES),
            d.outfits(at(23), readingData = true),
        )
        val m = day(milestone = true)
        assertEquals(
            setOf(BiscuitOutfit.CROWN, BiscuitOutfit.GLASSES),
            m.outfits(at(12), readingData = true),
        )
    }

    // ── current(...) ──

    @Test
    fun currentPassesRecoveryAndHrvDip() {
        assertEquals(41.0, BiscuitDay.current(41.0, 20.0, 15.0, null).recovery!!, 0.0)
        assertTrue(BiscuitDay.current(null, 20.0, 25.0, null).hrvDip)
        assertFalse(BiscuitDay.current(null, 25.0, 25.0, null).hrvDip)
        assertFalse(BiscuitDay.current(null, null, 25.0, null).hrvDip)
        assertFalse(BiscuitDay.current(null, 20.0, null, null).hrvDip)
    }

    @Test
    fun currentPopulatesStreakFieldsOnlyWithStreak() {
        val none = BiscuitDay.current(null, null, null, null)
        assertFalse(none.trainedToday)
        assertFalse(none.milestoneToday)
        assertFalse(none.streakAtRisk)

        fun reading(
            cover: ActivityStreak.Cover,
            current: Int,
            atRisk: Boolean = false,
        ) = ActivityStreak.Reading(
            days = emptyList(),
            current = current,
            longest = current,
            atRisk = atRisk,
            todayCover = cover,
            restLeftThisWeek = 0,
            freezesLeftThisMonth = 0,
            freezesBanked = 0,
            since = null,
        )

        val trained = BiscuitDay.current(null, null, null, reading(ActivityStreak.Cover.TRAINED, 6))
        assertTrue(trained.trainedToday)
        assertFalse(trained.milestoneToday)
        assertFalse(trained.streakAtRisk)

        val milestone = BiscuitDay.current(null, null, null, reading(ActivityStreak.Cover.TRAINED, 7))
        assertTrue(milestone.milestoneToday)

        val atRisk = BiscuitDay.current(null, null, null, reading(ActivityStreak.Cover.NONE, 12, atRisk = true))
        assertTrue(atRisk.streakAtRisk)
        val atRiskZero = BiscuitDay.current(null, null, null, reading(ActivityStreak.Cover.NONE, 0, atRisk = true))
        assertFalse(atRiskZero.streakAtRisk)
    }

    // ── BiscuitColor.recovery ──

    @Test
    fun recoveryColorBandEdges() {
        assertEquals(BiscuitColor.resting, BiscuitColor.recovery(null))
        assertEquals(BiscuitColor.lowRecovery, BiscuitColor.recovery(33.99))
        assertEquals(BiscuitColor.resting, BiscuitColor.recovery(34.0))
        assertEquals(BiscuitColor.resting, BiscuitColor.recovery(66.99))
        assertEquals(BiscuitColor.goodRecovery, BiscuitColor.recovery(67.0))
    }
}

private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
private fun assertFalse(b: Boolean) = org.junit.Assert.assertFalse(b)
