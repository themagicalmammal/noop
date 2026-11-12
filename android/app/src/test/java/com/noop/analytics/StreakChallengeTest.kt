package com.noop.analytics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakChallengeTest {
    @Test fun auraMilestoneLadderAndAnnualExtension() {
        assertEquals(listOf(7, 14, 20, 30), StreakChallenge.ladder.take(4))
        assertEquals(listOf(1095, 1460), StreakChallenge.options(1000))
        assertEquals(0 to 3, StreakChallenge.leg(0, afterBreak = true))
        assertEquals(7 to 14, StreakChallenge.leg(10, afterBreak = false))
    }

    @Test fun challengeBreaksWhenTheTrackedRunEnds() {
        val start = LocalDate.of(2026, 1, 1)
        val challenge = StreakChallenge.Challenge(7, start.toEpochDay(), start.toEpochDay(), 1, 0)
        val stillRunning = ActivityStreak.Reading(emptyList(), 4, 4, false, ActivityStreak.Cover.TRAINED, 0, 0, 0, start)
        val restarted = stillRunning.copy(current = 2, since = start.plusDays(5))
        assertEquals(StreakChallenge.State.ACTIVE, StreakChallenge.progress(challenge, stillRunning).state)
        assertEquals(StreakChallenge.State.BROKEN, StreakChallenge.progress(challenge, restarted).state)
        assertTrue(StreakChallenge.progress(challenge, stillRunning.copy(current = 7)).state == StreakChallenge.State.COMPLETE)
    }
}
