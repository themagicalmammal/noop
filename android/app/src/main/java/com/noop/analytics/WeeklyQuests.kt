package com.noop.analytics

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/** Deterministic Android port of Aura's `StrandFitness.WeeklyQuests`. */
object WeeklyQuests {
    enum class Area { MOVE, STRENGTH, SLEEP, RECOVER }
    enum class Tier(val aim: Double, internal val percentile: Double) {
        BRONZE(0.9, 0.5), SILVER(0.7, 0.7), GOLD(0.4, 0.85),
    }

    sealed interface Kind {
        val area: Area
        val isDayCount: Boolean get() = when (this) {
            is EarlyNights, is LongNights, is StepDays, EasyAfterHard -> true
            else -> false
        }
        val isLoad: Boolean get() = area == Area.MOVE || area == Area.STRENGTH

        data object Sessions : Kind { override val area = Area.MOVE }
        data object Minutes : Kind { override val area = Area.MOVE }
        data class Distance(val sport: String) : Kind { override val area = Area.MOVE }
        data object Lifts : Kind { override val area = Area.STRENGTH }
        data class EarlyNights(val byMinute: Int) : Kind { override val area = Area.SLEEP }
        data class LongNights(val minutes: Int) : Kind { override val area = Area.SLEEP }
        data class StepDays(val steps: Int) : Kind { override val area = Area.RECOVER }
        data object EasyAfterHard : Kind { override val area = Area.RECOVER }
    }

    data class Quest(
        val kind: Kind,
        val tier: Tier,
        val target: Double,
        val comeback: Boolean = false,
        val eased: Boolean = false,
    )
    data class Week(val start: Instant, val quests: List<Quest>, val swapUsed: Boolean = false, val starter: Boolean = false)
    data class Outcome(val weekStart: Instant, val tier: Tier, val done: Boolean)
    data class Night(val onset: Instant, val asleepMin: Double)
    data class Day(val date: Instant, val strain: Double?, val steps: Int?)
    data class Inputs(
        val sessions: List<PeriodGoal.Session>,
        val liftSport: String,
        val nights: List<Night> = emptyList(),
        val days: List<Day> = emptyList(),
        val lowRecovery: Set<Instant> = emptySet(),
    )

    data class Reading(val quest: Quest, val done: Double) {
        val complete get() = isComplete(quest, done)
        val fraction get() = if (quest.target > 0) (done / quest.target).coerceIn(0.0, 1.0) else 0.0
    }

    private data class Span(val start: Instant, val end: Instant) {
        fun contains(time: Instant) = time >= start && time < end
    }
    private data class Candidate(val kind: Kind, val weeks: List<Double>, val slip: Double, val spread: Double)
    private data class Option(val candidate: Candidate, val quest: Quest)

    fun plan(inputs: Inputs, now: Instant, zone: ZoneId, previous: Week?, outcomes: List<Outcome>, firstEver: Boolean): Week {
        val week = weekSpan(now, zone)
        val candidates = candidates(inputs, week.start, zone)
        val bias = drift(outcomes)
        val quests = mutableListOf<Quest>()
        val usedKinds = mutableListOf<Kind>()
        val usedAreas = mutableListOf<Area>()
        val lastKinds = previous?.quests?.associate { it.tier to it.kind }.orEmpty()
        val seed = stableHash(week.start.epochSecond.toString())
        listOf(Tier.GOLD, Tier.SILVER, Tier.BRONZE).forEach { tier ->
            val pick = choose(candidates, tier, bias[tier] ?: 0.0, usedKinds, usedAreas, lastKinds[tier], seed)
                ?: return@forEach
            quests += pick
            usedKinds += pick.kind
            usedAreas += pick.kind.area
        }
        if (quests.size != 3) return starterWeek(week, now, zone)
        var result = quests.sortedBy { it.tier.ordinal }
        if (firstEver) result = result.map { prorated(it, week, now, zone) }
        return Week(week.start, result)
    }

    fun swap(week: Week, tier: Tier, inputs: Inputs, outcomes: List<Outcome>, now: Instant, zone: ZoneId): Week {
        val index = week.quests.indexOfFirst { it.tier == tier }
        if (week.swapUsed || index < 0) return week
        val pick = if (week.starter) {
            starterAlternates(tier).firstOrNull { alt -> week.quests.none { it.kind == alt.kind && it.target == alt.target } }
        } else {
            val others = week.quests.filterIndexed { i, _ -> i != index }
            choose(
                candidates(inputs, week.start, zone), tier, drift(outcomes)[tier] ?: 0.0,
                week.quests.map { it.kind }, others.map { it.kind.area }, null,
                stableHash("swap-${week.start.epochSecond}"),
            )
        }
        val quests = week.quests.toMutableList()
        if (pick != null) quests[index] = pick
        return week.copy(quests = quests, swapUsed = true)
    }

    fun allThreeChestIsLegendary(weekStart: Instant): Boolean = stableHash("qa-${weekStart.epochSecond}") % 4L == 0L

    fun done(quest: Quest, weekStart: Instant, inputs: Inputs, zone: ZoneId): Double =
        value(quest.kind, weekSpan(weekStart, zone), inputs, zone) ?: 0.0

    fun isComplete(quest: Quest, done: Double): Boolean = done >= quest.target - 1e-9

    fun readings(week: Week, inputs: Inputs, zone: ZoneId): List<Reading> =
        week.quests.map { Reading(it, done(it, week.start, inputs, zone)) }

    fun outcomes(week: Week, inputs: Inputs, zone: ZoneId): List<Outcome> =
        week.quests.map { Outcome(week.start, it.tier, isComplete(it, done(it, week.start, inputs, zone))) }

    fun eased(week: Week, inputs: Inputs, now: Instant, zone: ZoneId): Week {
        val span = weekSpan(week.start, zone)
        val today = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val lowDays = inputs.lowRecovery.count { span.contains(it) && it <= today }
        if (lowDays < 2) return week
        val quests = week.quests.map { quest ->
            if (!quest.kind.isLoad || quest.tier == Tier.BRONZE || quest.eased) return@map quest
            val current = value(quest.kind, span, inputs, zone) ?: 0.0
            if (isComplete(quest, current)) return@map quest
            val lowered = rounded(max(quest.target * 0.6, current), quest.kind)
            if (lowered >= quest.target) quest else quest.copy(
                target = max(lowered, minimum(quest.kind)).let { if (quest.kind.isDayCount) minOf(it, 7.0) else it },
                eased = true,
            )
        }
        return week.copy(quests = quests)
    }

    private fun candidates(inputs: Inputs, before: Instant, zone: ZoneId): List<Candidate> {
        val weeks = (8 downTo 1).mapNotNull { back ->
            val date = before.atZone(zone).toLocalDate().minusWeeks(back.toLong())
            spanForWeek(date, zone)
        }
        val firstSession = inputs.sessions.minOfOrNull { it.start }
        val kinds = mutableListOf<Kind>(Kind.Sessions, Kind.Minutes, Kind.Lifts, Kind.EasyAfterHard)
        val first = weeks.firstOrNull()?.start ?: before
        val sportTotals = inputs.sessions.asSequence().filter { it.start >= first && it.start < before && it.distanceM > 0 }
            .groupingBy { it.sport }.fold(0.0) { acc, session -> acc + session.distanceM }
        sportTotals.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key?.let { kinds += Kind.Distance(it) }
        val pastNights = inputs.nights.filter { it.onset < before && it.onset >= first }
        earlyThreshold(pastNights, zone)?.let { kinds += Kind.EarlyNights(it) }
        longThreshold(pastNights)?.let { kinds += Kind.LongNights(it) }
        val pastSteps = inputs.days.filter { it.date < before && it.date >= first }.mapNotNull { it.steps }
        stepThreshold(pastSteps)?.let { kinds += Kind.StepDays(it) }

        return kinds.mapNotNull { kind ->
            val sessionKind = kind == Kind.Sessions || kind == Kind.Minutes || kind is Kind.Distance || kind == Kind.Lifts
            val values = weeks.mapNotNull { span ->
                if (sessionKind && (firstSession == null || span.end <= firstSession)) null
                else value(kind, span, inputs, zone)
            }
            if (values.size < 3 || values.none { it > 0 } || (kind == Kind.Lifts && values.count { it > 0 } < 2)) return@mapNotNull null
            val usual = quantile(values, 0.5)
            val recent = values.takeLast(2).sum() / minOf(2, values.size)
            val slip = if (usual > 0) max(0.0, (usual - recent) / usual) else 0.0
            val mean = values.average()
            val sd = sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
            Candidate(kind, values, slip, if (mean > 0) sd / mean else 9.0)
        }
    }

    private fun choose(
        candidates: List<Candidate>, tier: Tier, bias: Double, usedKinds: List<Kind>, usedAreas: List<Area>,
        lastKind: Kind?, seed: Long,
    ): Quest? {
        var options = candidates.mapNotNull { candidate ->
            if (candidate.kind in usedKinds) null else {
                val comeback = tier == Tier.GOLD && candidate.slip >= 1.0 / 6
                target(candidate, tier, bias, comeback)?.let { Option(candidate, Quest(candidate.kind, tier, it, comeback)) }
            }
        }
        if (options.isEmpty()) return null
        val fresh = options.filter { it.quest.kind.area !in usedAreas }
        if (fresh.isNotEmpty()) options = fresh
        val notRepeat = options.filter { it.quest.kind != lastKind }
        if (notRepeat.isNotEmpty()) options = notRepeat
        val rng = SplitMix64(seed + (tier.ordinal + 1L) * 0x9E37L)
        val scored = options.map { Triple(it, if (it.quest.comeback) 1.0 + it.candidate.slip else 0.0, rng.unit()) }
        return when (tier) {
            Tier.GOLD -> scored.maxByOrNull { (_, comebackScore, draw) -> if (comebackScore > 0) comebackScore else draw }?.first?.quest
            Tier.SILVER -> scored.maxByOrNull { it.third }?.first?.quest
            Tier.BRONZE -> scored.minByOrNull { it.first.candidate.spread + it.third * 0.05 }?.first?.quest
        }
    }

    private fun target(candidate: Candidate, tier: Tier, bias: Double, comeback: Boolean): Double? {
        val p = ((if (comeback) 0.5 else tier.percentile) + bias).coerceIn(0.3, 0.95)
        var t = rounded(quantile(candidate.weeks, p), candidate.kind)
        val best = candidate.weeks.maxOrNull() ?: 0.0
        val cap = if (candidate.kind.isDayCount || isCount(candidate.kind)) best + 1 else best * 1.1
        t = minOf(t, rounded(cap, candidate.kind))
        if (candidate.kind.isDayCount) t = minOf(t, 7.0)
        if (tier != Tier.BRONZE && !comeback) {
            val belowTier = Tier.entries[tier.ordinal - 1]
            val below = rounded(quantile(candidate.weeks, (belowTier.percentile + bias).coerceIn(0.3, 0.95)), candidate.kind)
            t = max(t, below + step(candidate.kind))
            if (candidate.kind.isDayCount) t = minOf(t, 7.0)
            if (t <= below) return null
        }
        t = max(t, minimum(candidate.kind))
        return t.takeIf { it > 0 }
    }

    private fun drift(outcomes: List<Outcome>): Map<Tier, Double> {
        val recent = outcomes.sortedBy { it.weekStart }.takeLast(18)
        return Tier.entries.mapNotNull { tier ->
            val mine = recent.filter { it.tier == tier }.takeLast(6)
            if (mine.size < 3) return@mapNotNull null
            val rate = mine.count { it.done }.toDouble() / mine.size
            tier to when {
                rate > minOf(tier.aim + 0.15, 0.95) -> 0.1
                rate < tier.aim - 0.2 -> -0.1
                else -> 0.0
            }
        }.toMap()
    }

    private fun value(kind: Kind, span: Span, inputs: Inputs, zone: ZoneId): Double? {
        val sessions = inputs.sessions.filter { span.contains(it.start) }
        return when (kind) {
            Kind.Sessions -> sessions.size.toDouble()
            Kind.Minutes -> sessions.sumOf { it.durationS }
            is Kind.Distance -> sessions.filter { it.sport == kind.sport }.sumOf { it.distanceM }
            Kind.Lifts -> sessions.count { it.sport == inputs.liftSport }.toDouble()
            is Kind.EarlyNights -> {
                val nights = nightsIn(span, inputs.nights, zone)
                if (nights.size < 4) null else nights.count { minuteAfterSix(it.onset, zone) <= kind.byMinute }.toDouble()
            }
            is Kind.LongNights -> {
                val nights = nightsIn(span, inputs.nights, zone)
                if (nights.size < 4) null else nights.count { it.asleepMin >= kind.minutes }.toDouble()
            }
            is Kind.StepDays -> {
                val days = inputs.days.filter { span.contains(it.date) && it.steps != null }
                if (days.size < 4) null else days.count { (it.steps ?: 0) >= kind.steps }.toDouble()
            }
            Kind.EasyAfterHard -> {
                val all = inputs.days.filter { it.strain != null }.sortedBy { it.date }
                val strains = all.mapNotNull { it.strain }
                val inWeek = all.filter { span.contains(it.date) }
                if (strains.size < 14 || inWeek.size < 4) null else {
                    val hard = quantile(strains, 0.75)
                    val easy = quantile(strains, 0.4)
                    val byDay = all.associateBy { it.date.atZone(zone).toLocalDate() }
                    inWeek.count { day ->
                        val date = day.date.atZone(zone).toLocalDate()
                        val previous = byDay[date.minusDays(1)]?.strain
                        previous != null && previous >= hard && (day.strain ?: Double.POSITIVE_INFINITY) <= easy
                    }.toDouble()
                }
            }
        }
    }

    private fun nightsIn(span: Span, nights: List<Night>, zone: ZoneId): List<Night> =
        nights.filter { night -> span.contains(night.onset.minusSeconds(18 * 3600L)) }
            .groupBy { it.onset.minusSeconds(18 * 3600L).atZone(zone).toLocalDate() }
            .values.mapNotNull { it.maxByOrNull(Night::asleepMin) }

    private fun minuteAfterSix(time: Instant, zone: ZoneId): Int {
        val local = time.atZone(zone)
        return (local.hour * 60 + local.minute - 18 * 60 + 1440) % 1440
    }

    private fun earlyThreshold(nights: List<Night>, zone: ZoneId): Int? {
        if (nights.size < 14) return null
        return (quantile(nights.map { minuteAfterSix(it.onset, zone).toDouble() }, 0.5).toInt() / 15) * 15
    }

    private fun longThreshold(nights: List<Night>): Int? {
        if (nights.size < 14) return null
        val mins = nights.map { it.asleepMin }
        if (quantile(mins, 0.75) >= 420) return 420
        val own = (quantile(mins, 0.6) / 15).let { kotlin.math.floor(it + 0.5).toInt() * 15 }
        return own.takeIf { it >= 300 }
    }

    private fun stepThreshold(steps: List<Int>): Int? {
        if (steps.size < 14) return null
        val threshold = (quantile(steps.map(Int::toDouble), 0.5) / 1000).let { kotlin.math.floor(it + 0.5).toInt() * 1000 }
        return threshold.takeIf { it >= 4000 }
    }

    private fun starterWeek(week: Span, now: Instant, zone: ZoneId): Week {
        val start = week.start.atZone(zone).toLocalDate()
        val cut = listOf(
            prorated(Quest(Kind.Sessions, Tier.BRONZE, 1.0), week, now, zone),
            prorated(Quest(Kind.Minutes, Tier.SILVER, 60.0 * 60), week, now, zone),
            prorated(Quest(Kind.Sessions, Tier.GOLD, 3.0), week, now, zone),
        ).toMutableList()
        cut[2] = cut[2].copy(target = max(cut[2].target, cut[0].target + 1))
        return Week(start.atStartOfDay(zone).toInstant(), cut, starter = true)
    }

    private fun starterAlternates(tier: Tier): List<Quest> = when (tier) {
        Tier.BRONZE -> listOf(Quest(Kind.Minutes, tier, 20.0 * 60))
        Tier.SILVER -> listOf(Quest(Kind.Sessions, tier, 2.0))
        Tier.GOLD -> listOf(Quest(Kind.Minutes, tier, 150.0 * 60))
    }

    private fun prorated(quest: Quest, week: Span, now: Instant, zone: ZoneId): Quest {
        val start = week.start.atZone(zone).toLocalDate()
        val elapsed = java.time.temporal.ChronoUnit.DAYS.between(start, now.atZone(zone).toLocalDate()).toInt()
        val left = max(1, 7 - elapsed)
        if (left >= 7) return quest
        var target = max(minimum(quest.kind), rounded(quest.target * left / 7.0, quest.kind))
        if (quest.kind.isDayCount) target = minOf(target, left.toDouble())
        return quest.copy(target = target)
    }

    private fun isCount(kind: Kind) = kind == Kind.Sessions || kind == Kind.Lifts
    private fun rounded(value: Double, kind: Kind): Double = when (kind) {
        Kind.Minutes -> floor(value / 300.0 + 0.5) * 300.0
        is Kind.Distance -> {
            val increment = if (value < 5000) 500.0 else 1000.0
            floor(value / increment + 0.5) * increment
        }
        else -> floor(value + 0.5)
    }
    private fun step(kind: Kind) = when (kind) { Kind.Minutes -> 600.0; is Kind.Distance -> 1000.0; else -> 1.0 }
    private fun minimum(kind: Kind) = when (kind) { Kind.Minutes -> 1200.0; is Kind.Distance -> 1000.0; else -> 1.0 }

    private fun quantile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val x = p * (sorted.size - 1)
        val lo = floor(x).toInt()
        val hi = minOf(sorted.lastIndex, lo + 1)
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (x - lo)
    }

    private fun weekSpan(instant: Instant, zone: ZoneId): Span = spanForWeek(instant.atZone(zone).toLocalDate(), zone)
    private fun spanForWeek(date: java.time.LocalDate, zone: ZoneId): Span {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant()
        return Span(start, start.atZone(zone).plusWeeks(1).toInstant())
    }

    private fun stableHash(value: String): Long {
        var hash = 0xcbf29ce484222325UL.toLong()
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = (hash xor (byte.toLong() and 0xffL)) * 0x100000001b3L
        }
        return hash
    }

    private class SplitMix64(seed: Long) {
        private var state = seed
        fun unit(): Double {
            state += 0x9E3779B97F4A7C15UL.toLong()
            var z = state
            z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
            z = (z xor (z ushr 27)) * 0x94D049BB133111EBUL.toLong()
            z = z xor (z ushr 31)
            return (z ushr 11).toDouble() / (1L shl 53).toDouble()
        }
    }
}
