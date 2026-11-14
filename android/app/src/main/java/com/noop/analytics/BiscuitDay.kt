package com.noop.analytics

import java.time.ZonedDateTime

/**
 * The day-level state that drives the Biscuit face (parity twin of `NoopBiscuitDay`).
 *
 * [recovery] is today's recovery ring value (or the most recent day that had one);
 * [hrvDip] is true when today's HRV vital reads below its low band; the streak
 * fields are only meaningful while a streak exists (they stay false otherwise).
 */
class BiscuitDay(
    var recovery: Double? = null,
    var hrvDip: Boolean = false,
    var trainedToday: Boolean = false,
    var milestoneToday: Boolean = false,
    var streakAtRisk: Boolean = false,
) {
    fun mood(now: ZonedDateTime = ZonedDateTime.now(), canSleep: Boolean = false): BiscuitMood {
        val hour = now.hour
        val lateNight = hour >= 23 || hour < 5
        if (milestoneToday) return BiscuitMood.RADIANT
        if (trainedToday) return BiscuitMood.PROUD
        if (streakAtRisk && hour >= 18) return BiscuitMood.WORRIED
        if (lateNight) return if (canSleep) BiscuitMood.ASLEEP else BiscuitMood.SLEEPY
        recovery?.let { if (it < 34) return BiscuitMood.SLEEPY }
        if (hrvDip) return BiscuitMood.SLEEPY
        recovery?.let { if (it >= 67) return BiscuitMood.HAPPY }
        return if (hour >= 20) BiscuitMood.CALM else BiscuitMood.NEUTRAL
    }

    fun outfits(
        now: ZonedDateTime = ZonedDateTime.now(),
        sessionLive: Boolean = false,
        readingData: Boolean = false,
    ): Set<BiscuitOutfit> {
        val hour = now.hour
        val outfits = mutableSetOf<BiscuitOutfit>()
        when {
            sessionLive -> outfits.add(BiscuitOutfit.COACH)
            milestoneToday -> outfits.add(BiscuitOutfit.CROWN)
            hour >= 22 || hour < 5 -> outfits.add(BiscuitOutfit.NIGHTCAP)
        }
        if (readingData) outfits.add(BiscuitOutfit.GLASSES)
        return outfits
    }

    companion object {
        /**
         * Build today's day from stored inputs (parity twin of `NoopBiscuitDay.current`).
         *
         * [hrvDip] is judged only when both the value and its low band are known. The
         * streak fields are populated only when [streak] exists; a milestone day is a
         * trained day whose current total is one of [ActivityStreak.milestones].
         */
        fun current(
            recovery: Double?,
            hrvValue: Double?,
            hrvLow: Double?,
            streak: ActivityStreak.Reading?,
        ): BiscuitDay {
            val day = BiscuitDay(recovery = recovery)
            if (hrvValue != null && hrvLow != null) day.hrvDip = hrvValue < hrvLow
            if (streak != null) {
                day.trainedToday = streak.todayCover == ActivityStreak.Cover.TRAINED
                day.milestoneToday = day.trainedToday && ActivityStreak.milestones.contains(streak.current)
                day.streakAtRisk = streak.atRisk && streak.current > 0
            }
            return day
        }
    }
}
