package com.noop.analytics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

/** Activity-day streak rules mirrored from Aura's `StrandFitness.ActivityStreak`. */
object ActivityStreak {
    enum class Day(val counts: Boolean) {
        IDLE(false), TRAINED(true), EARNED_REST(true), REST(true), PAUSED(false), FROZEN(false), MISSED(false), PENDING(false),
    }

    enum class Cover { TRAINED, EARNED_REST, REST, PAUSED, FREEZE, NONE }

    data class Input(
        val trained: Set<LocalDate>,
        val lowRecovery: Set<LocalDate> = emptySet(),
        val paused: Set<LocalDate> = emptySet(),
        val restPerWeek: Int = 2,
        val freezesPerMonth: Int = 1,
        val earnedFreezes: List<LocalDate> = emptyList(),
    )

    data class Reading(
        val days: List<Pair<LocalDate, Day>>,
        val current: Int,
        val longest: Int,
        val atRisk: Boolean,
        val todayCover: Cover,
        val restLeftThisWeek: Int,
        val freezesLeftThisMonth: Int,
        val freezesBanked: Int,
        val since: LocalDate?,
    ) {
        fun day(date: LocalDate): Day? = days.lastOrNull { it.first == date }?.second
    }

    val milestones = listOf(7, 14, 30, 50, 100, 150, 200, 250, 300, 365, 500, 730, 1000)

    fun nextMilestone(after: Int): Int = milestones.firstOrNull { it > after } ?: ((after / 365) + 1) * 365

    fun reading(input: Input, today: LocalDate): Reading {
        val trained = input.trained.filterTo(mutableSetOf()) { it <= today }
        val low = input.lowRecovery
        val paused = input.paused
        val restCap = input.restPerWeek.coerceIn(0, 3)
        val freezeCap = input.freezesPerMonth.coerceAtLeast(0)
        val bank = input.earnedFreezes.filter { it <= today }.sorted().toMutableList()
        val first = trained.minOrNull() ?: return Reading(
            days = emptyList(), current = 0, longest = 0, atRisk = false,
            todayCover = if (today in paused) Cover.PAUSED else Cover.NONE,
            restLeftThisWeek = restCap, freezesLeftThisMonth = freezeCap,
            freezesBanked = bank.size, since = null,
        )

        val out = mutableListOf<Pair<LocalDate, Day>>()
        var run = 0
        var longest = 0
        var since: LocalDate? = null
        val restUsed = mutableMapOf<Pair<Int, Int>, Int>()
        val freezeUsed = mutableMapOf<Pair<Int, Int>, Int>()
        fun week(date: LocalDate): Pair<Int, Int> {
            // Aura's calendar forces Monday as week start but keeps the locale calendar's
            // one-day first-week rule; ISO's four-day rule would assign early January differently.
            val fields = WeekFields.of(DayOfWeek.MONDAY, 1)
            return date.get(fields.weekBasedYear()) to date.get(fields.weekOfWeekBasedYear())
        }
        fun month(date: LocalDate): Pair<Int, Int> = date.year to date.monthValue

        var date = first
        while (!date.isAfter(today)) {
            val isToday = date == today
            val week = week(date)
            val month = month(date)
            val day = when {
                date in trained -> Day.TRAINED
                run == 0 && since == null -> Day.IDLE
                date in low -> Day.EARNED_REST
                date in paused -> Day.PAUSED
                isToday -> Day.PENDING
                restUsed.getOrDefault(week, 0) < restCap -> {
                    restUsed[week] = restUsed.getOrDefault(week, 0) + 1
                    Day.REST
                }
                freezeUsed.getOrDefault(month, 0) < freezeCap -> {
                    freezeUsed[month] = freezeUsed.getOrDefault(month, 0) + 1
                    Day.FROZEN
                }
                else -> {
                    val index = bank.indexOfFirst { !it.isAfter(date) }
                    if (index >= 0) {
                        bank.removeAt(index)
                        Day.FROZEN
                    } else Day.MISSED
                }
            }
            when (day) {
                Day.TRAINED, Day.EARNED_REST, Day.REST -> {
                    if (run == 0 && since == null) since = date
                    run++
                    longest = maxOf(longest, run)
                }
                Day.MISSED -> { run = 0; since = null }
                Day.IDLE, Day.PAUSED, Day.FROZEN, Day.PENDING -> Unit
            }
            out += date to day
            date = date.plusDays(1)
        }

        val restLeft = (restCap - restUsed.getOrDefault(week(today), 0)).coerceAtLeast(0)
        val freezeLeft = (freezeCap - freezeUsed.getOrDefault(month(today), 0)).coerceAtLeast(0)
        val todayDay = out.lastOrNull()?.second ?: Day.IDLE
        val cover = when (todayDay) {
            Day.TRAINED -> Cover.TRAINED
            Day.EARNED_REST -> Cover.EARNED_REST
            Day.PAUSED -> Cover.PAUSED
            else -> if (restLeft > 0) Cover.REST else if (freezeLeft > 0 || bank.isNotEmpty()) Cover.FREEZE else Cover.NONE
        }
        val alive = run > 0
        return Reading(
            days = out,
            current = if (alive) run else 0,
            longest = longest,
            atRisk = alive && todayDay == Day.PENDING && cover == Cover.NONE,
            todayCover = cover,
            restLeftThisWeek = restLeft,
            freezesLeftThisMonth = freezeLeft,
            freezesBanked = bank.size,
            since = if (alive) since else null,
        )
    }
}
