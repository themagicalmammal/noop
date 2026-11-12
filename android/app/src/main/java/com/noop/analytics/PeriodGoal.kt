package com.noop.analytics

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/** Period-based training targets, mirrored from Aura's `StrandFitness.PeriodGoal`. */
data class PeriodGoal(
    val id: String,
    val period: Period,
    val metric: Metric,
    val sport: String?,
    val target: Double,
    val createdAt: Instant,
) {
    enum class Period { WEEK, MONTH, YEAR }
    enum class Metric { DISTANCE, TIME, CLIMB, SESSIONS }

    data class Session(
        val start: Instant,
        val durationS: Double,
        val distanceM: Double,
        val climbM: Double,
        val sport: String,
    )

    data class Span(val start: Instant, val end: Instant) {
        val durationS: Double get() = (end.toEpochMilli() - start.toEpochMilli()) / 1000.0
    }

    data class Progress(
        val interval: Span,
        val done: Double,
        val target: Double,
        val expected: Double,
        val elapsed: Double,
        val daysLeft: Int,
        val daily: List<Double>,
    ) {
        val fraction: Double get() = if (target > 0) done / target else 0.0
        val complete: Boolean get() = done >= target
        val ahead: Double get() = done - expected
        val remaining: Double get() = max(0.0, target - done)
        val perDay: Double get() = if (daysLeft > 0) remaining / daysLeft else remaining
    }

    fun interval(containing: Instant, zone: ZoneId): Span {
        val localDate = containing.atZone(zone).toLocalDate()
        val startDate = when (period) {
            Period.WEEK -> localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Period.MONTH -> localDate.withDayOfMonth(1)
            Period.YEAR -> localDate.withDayOfYear(1)
        }
        val endDate = when (period) {
            Period.WEEK -> startDate.plusWeeks(1)
            Period.MONTH -> startDate.plusMonths(1)
            Period.YEAR -> startDate.plusYears(1)
        }
        return Span(startDate.atStartOfDay(zone).toInstant(), endDate.atStartOfDay(zone).toInstant())
    }

    fun value(session: Session): Double = when (metric) {
        Metric.DISTANCE -> session.distanceM
        Metric.TIME -> session.durationS
        Metric.CLIMB -> session.climbM
        Metric.SESSIONS -> 1.0
    }

    fun matches(session: Session): Boolean = sport == null || session.sport == sport

    fun progress(sessions: List<Session>, now: Instant, zone: ZoneId): Progress =
        progress(sessions, interval(now, zone), now, zone)

    fun progress(sessions: List<Session>, span: Span, now: Instant, zone: ZoneId): Progress {
        val inside = sessions.filter { matches(it) && it.start >= span.start && it.start < span.end }
        val done = inside.sumOf(::value)
        val clock = now.coerceIn(span.start, span.end)
        val elapsed = if (span.durationS > 0) {
            (clock.toEpochMilli() - span.start.toEpochMilli()) / (span.durationS * 1000.0)
        } else 1.0
        val todayStart = clock.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val daysLeft = if (clock >= span.end) 0 else max(
            1,
            ChronoUnit.DAYS.between(todayStart.atZone(zone).toLocalDate(), span.end.atZone(zone).toLocalDate()).toInt(),
        )
        val daily = mutableListOf<Double>()
        var day = span.start
        var cumulative = 0.0
        val nextDay = todayStart.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
        val through = minOf(span.end, nextDay)
        while (day < through) {
            val next = minOf(span.end, day.atZone(zone).plusDays(1).toInstant())
            cumulative += inside.asSequence()
                .filter { it.start >= day && it.start < next }
                .sumOf(::value)
            daily += cumulative
            day = next
        }
        return Progress(span, done, target, target * elapsed, elapsed, daysLeft, daily)
    }

    companion object {
        fun naturalMetric(sport: String?): Metric {
            val name = sport?.lowercase() ?: return Metric.TIME
            return when {
                listOf("run", "cycl", "bik", "ride", "walk", "hik", "ski", "row").any(name::contains) -> Metric.DISTANCE
                listOf("strength", "lift", "weight").any(name::contains) -> Metric.SESSIONS
                else -> Metric.TIME
            }
        }

        data class Suggestions(val steady: Double, val build: Double)

        fun suggestions(
            period: Period,
            metric: Metric,
            sport: String?,
            sessions: List<Session>,
            now: Instant,
            zone: ZoneId,
        ): Suggestions? {
            val probe = PeriodGoal("suggestion", period, metric, sport, 1.0, now)
            val lookback = when (period) { Period.WEEK -> 4; Period.MONTH -> 3; Period.YEAR -> 1 }
            val totals = mutableListOf<Double>()
            var cursor = probe.interval(now, zone).start
            repeat(lookback) {
                val previousDate = cursor.atZone(zone).toLocalDate().minusDays(1)
                val span = probe.interval(previousDate.atStartOfDay(zone).toInstant(), zone)
                totals += sessions.asSequence()
                    .filter { probe.matches(it) && it.start >= span.start && it.start < span.end }
                    .sumOf(probe::value)
                cursor = span.start
            }
            var average = if (totals.isEmpty()) 0.0 else totals.average()
            if (average <= 0 && period == Period.YEAR) {
                val span = probe.interval(now, zone)
                val soFar = sessions.asSequence()
                    .filter { probe.matches(it) && it.start >= span.start && it.start < now }
                    .sumOf(probe::value)
                val gone = (now.toEpochMilli() - span.start.toEpochMilli()) / (span.durationS * 1000.0)
                if (gone > 0.05) average = soFar / gone
            }
            if (average <= 0) return null
            val steady = nice(average, metric)
            return Suggestions(steady, max(nice(average * 1.1, metric), steady + step(steady, metric)))
        }

        fun step(value: Double, metric: Metric): Double = when (metric) {
            Metric.DISTANCE -> if (value < 50_000) 1_000.0 else if (value < 200_000) 5_000.0 else if (value < 1_000_000) 25_000.0 else 100_000.0
            Metric.TIME -> if (value < 3 * 3600) 900.0 else if (value < 20 * 3600) 1800.0 else if (value < 100 * 3600) 3600.0 else 18_000.0
            Metric.CLIMB -> if (value < 1000) 50.0 else if (value < 10_000) 250.0 else 1000.0
            Metric.SESSIONS -> 1.0
        }

        fun nice(value: Double, metric: Metric): Double {
            val increment = step(value, metric)
            return max(increment, floor(value / increment + 0.5) * increment)
        }
    }
}
