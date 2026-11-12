package com.noop.analytics

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodGoalTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-12T12:00:00Z")

    private fun goal(metric: PeriodGoal.Metric, sport: String? = null, target: Double = 10.0) =
        PeriodGoal("g1", PeriodGoal.Period.WEEK, metric, sport, target, now)

    @Test fun naturalMetricMatchesAuraSportRules() {
        assertEquals(PeriodGoal.Metric.DISTANCE, PeriodGoal.naturalMetric("Trail Running"))
        assertEquals(PeriodGoal.Metric.SESSIONS, PeriodGoal.naturalMetric("Weight training"))
        assertEquals(PeriodGoal.Metric.TIME, PeriodGoal.naturalMetric("Boxing"))
        assertEquals(PeriodGoal.Metric.TIME, PeriodGoal.naturalMetric(null))
    }

    @Test fun periodIntervalUsesMondayAndLocalCalendarBoundaries() {
        val weekly = goal(PeriodGoal.Metric.SESSIONS).interval(now, zone)
        assertEquals(Instant.parse("2026-11-09T00:00:00Z"), weekly.start)
        assertEquals(Instant.parse("2026-11-16T00:00:00Z"), weekly.end)
    }

    @Test fun progressFiltersBySportAndCountsSessions() {
        val g = goal(PeriodGoal.Metric.SESSIONS, sport = "Running", target = 3.0)
        val sessions = listOf(
            PeriodGoal.Session(Instant.parse("2026-11-10T09:00:00Z"), 1800.0, 5000.0, 30.0, "Running"),
            PeriodGoal.Session(Instant.parse("2026-11-11T09:00:00Z"), 2400.0, 6000.0, 40.0, "Walking"),
            PeriodGoal.Session(Instant.parse("2026-11-12T09:00:00Z"), 3600.0, 8000.0, 50.0, "Running"),
        )
        val result = g.progress(sessions, now, zone)
        assertEquals(2.0, result.done, 0.0)
        assertEquals(4.0, result.daysLeft.toDouble(), 0.0)
        assertEquals(listOf(0.0, 1.0, 1.0, 2.0), result.daily)
        assertFalse(result.complete)
    }

    @Test fun niceTargetsRoundToMetricSpecificSteps() {
        assertEquals(24_000.0, PeriodGoal.nice(24_100.0, PeriodGoal.Metric.DISTANCE), 0.0)
        assertEquals(3_600.0, PeriodGoal.nice(3_400.0, PeriodGoal.Metric.TIME), 0.0)
        assertEquals(500.0, PeriodGoal.nice(475.0, PeriodGoal.Metric.CLIMB), 0.0)
        assertEquals(6.0, PeriodGoal.nice(6.2, PeriodGoal.Metric.SESSIONS), 0.0)
    }

    @Test fun suggestionsUseRecentCompletedPeriods() {
        val g = goal(PeriodGoal.Metric.SESSIONS)
        val currentStart = g.interval(now, zone).start
        val sessions = (1..4).flatMap { week ->
            val start = currentStart.minusSeconds(week * 7L * 86_400L)
            listOf(1L, 3L).map { day ->
                PeriodGoal.Session(start.plusSeconds(day * 86_400L), 1800.0, 0.0, 0.0, "Run")
            }
        }
        val suggestion = PeriodGoal.suggestions(
            PeriodGoal.Period.WEEK, PeriodGoal.Metric.SESSIONS, null, sessions, now, zone,
        )
        assertTrue(suggestion != null)
        assertEquals(2.0, suggestion!!.steady, 0.0)
        assertEquals(3.0, suggestion.build, 0.0)
    }
}
