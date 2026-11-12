package com.noop.ui

import android.content.Context
import com.noop.analytics.PeriodGoal
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class GoalsSnapshot(
    val enabled: Boolean = false,
    val streaksEnabled: Boolean = false,
    val streakStartEpochDay: Long? = null,
    val restPerWeek: Int = 2,
    val pauses: List<GoalPause> = emptyList(),
    val goals: List<PeriodGoal> = emptyList(),
)

data class GoalPause(val startEpochDay: Long, val endEpochDay: Long)

/** On-device goal settings, matching Aura's single `noop.goals.v1` stored value. */
object GoalsStore {
    private const val PREFS = "noop_goals"
    private const val KEY = "noop.goals.v1"

    fun load(context: Context): GoalsSnapshot {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return GoalsSnapshot()
        return runCatching {
            val root = JSONObject(raw)
            val goals = root.optJSONArray("goals") ?: JSONArray()
            val pauses = root.optJSONArray("pauses") ?: JSONArray()
            GoalsSnapshot(
                enabled = root.optBoolean("goalsOn", false),
                streaksEnabled = root.optBoolean("streaksOn", false),
                streakStartEpochDay = root.takeIf { it.has("startDay") && !it.isNull("startDay") }
                    ?.optLong("startDay"),
                restPerWeek = root.optInt("restPerWeek", 2).coerceIn(0, 3),
                pauses = List(pauses.length()) { index ->
                    val item = pauses.getJSONObject(index)
                    GoalPause(item.getLong("startDay"), item.getLong("endDay"))
                },
                goals = List(goals.length()) { index ->
                    val item = goals.getJSONObject(index)
                    PeriodGoal(
                        id = item.getString("id"),
                        period = PeriodGoal.Period.valueOf(item.getString("period").uppercase()),
                        metric = PeriodGoal.Metric.valueOf(item.getString("metric").uppercase()),
                        sport = item.optString("sport").takeIf { it.isNotBlank() && it != "null" },
                        target = item.getDouble("target"),
                        createdAt = Instant.ofEpochMilli(item.getLong("createdAt")),
                    )
                },
            )
        }.getOrDefault(GoalsSnapshot())
    }

    fun save(context: Context, snapshot: GoalsSnapshot) {
        val goals = JSONArray()
        val pauses = JSONArray()
        snapshot.pauses.forEach { pause ->
            pauses.put(JSONObject().put("startDay", pause.startEpochDay).put("endDay", pause.endEpochDay))
        }
        snapshot.goals.forEach { goal ->
            goals.put(
                JSONObject()
                    .put("id", goal.id)
                    .put("period", goal.period.name.lowercase())
                    .put("metric", goal.metric.name.lowercase())
                    .put("sport", goal.sport)
                    .put("target", goal.target)
                    .put("createdAt", goal.createdAt.toEpochMilli()),
            )
        }
        val json = JSONObject()
            .put("goalsOn", snapshot.enabled)
            .put("streaksOn", snapshot.streaksEnabled)
            .put("startDay", snapshot.streakStartEpochDay)
            .put("restPerWeek", snapshot.restPerWeek.coerceIn(0, 3))
            .put("pauses", pauses)
            .put("goals", goals)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, json.toString()).apply()
    }

    fun starterGoals(sessions: List<PeriodGoal.Session>, now: Instant, zone: java.time.ZoneId): List<PeriodGoal> {
        val sport = sessions
            .filter { it.distanceM > 0.0 && PeriodGoal.naturalMetric(it.sport) == PeriodGoal.Metric.DISTANCE }
            .groupingBy { it.sport }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()
            ?.key ?: "Running"
        val distance = PeriodGoal.suggestions(
            PeriodGoal.Period.WEEK, PeriodGoal.Metric.DISTANCE, sport, sessions, now, zone,
        )?.steady ?: 10_000.0
        val count = PeriodGoal.suggestions(
            PeriodGoal.Period.WEEK, PeriodGoal.Metric.SESSIONS, null, sessions, now, zone,
        )?.steady?.coerceAtLeast(2.0) ?: 3.0
        val time = PeriodGoal.suggestions(
            PeriodGoal.Period.MONTH, PeriodGoal.Metric.TIME, null, sessions, now, zone,
        )?.steady ?: 12.0 * 3600.0
        return listOf(
            PeriodGoal(UUID.randomUUID().toString(), PeriodGoal.Period.WEEK, PeriodGoal.Metric.DISTANCE, sport, distance, now),
            PeriodGoal(UUID.randomUUID().toString(), PeriodGoal.Period.WEEK, PeriodGoal.Metric.SESSIONS, null, count, now),
            PeriodGoal(UUID.randomUUID().toString(), PeriodGoal.Period.MONTH, PeriodGoal.Metric.TIME, null, time, now),
        )
    }
}
