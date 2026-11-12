package com.noop.analytics

import java.time.LocalDate

/** Milestone challenge progression mirrored from Aura's StrandFitness.StreakChallenge. */
object StreakChallenge {
    val ladder = listOf(7, 14, 20, 30, 50, 75, 100, 150, 200, 250, 300, 365, 500, 730, 1000)
    const val comeback = 3

    data class Challenge(
        val target: Int,
        val acceptedEpochSecond: Long,
        val runStartEpochDay: Long?,
        val startCount: Int,
        val base: Int?,
        val peak: Int? = null,
    ) { val legStart get() = minOf(base ?: startCount, target - 1) }

    enum class State { ACTIVE, COMPLETE, BROKEN }
    data class Progress(val state: State, val done: Int)

    fun rungs(after: Int, count: Int = 1): List<Int> {
        val result = ladder.filter { it > after }.take(count).toMutableList()
        var year = ((maxOf(after, ladder.last()) / 365) + 1) * 365
        while (result.size < count) { if (year > (result.lastOrNull() ?: after)) result += year; year += 365 }
        return result
    }

    fun leg(current: Int, afterBreak: Boolean): Pair<Int, Int> {
        if (afterBreak && current < comeback) return 0 to comeback
        val target = rungs(current).first()
        return (ladder.lastOrNull { it <= current && it < target } ?: 0) to target
    }

    fun options(after: Int) = rungs(after, 2)

    fun progress(challenge: Challenge, reading: ActivityStreak.Reading): Progress {
        val runStart = challenge.runStartEpochDay?.let(LocalDate::ofEpochDay)
        if (runStart != null && (reading.current <= 0 || reading.since != runStart)) return Progress(State.BROKEN, 0)
        if (runStart == null && reading.current > 0 && reading.since != null && reading.since.isBefore(java.time.Instant.ofEpochSecond(challenge.acceptedEpochSecond).atZone(java.time.ZoneId.systemDefault()).toLocalDate())) {
            return Progress(State.ACTIVE, 0)
        }
        return if (reading.current >= challenge.target) Progress(State.COMPLETE, challenge.target)
        else Progress(State.ACTIVE, reading.current)
    }
}
