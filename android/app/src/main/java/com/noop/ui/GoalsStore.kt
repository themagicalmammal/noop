package com.noop.ui

import android.content.Context
import com.noop.analytics.PeriodGoal
import com.noop.analytics.WeeklyQuests
import com.noop.analytics.StreakChallenge
import com.noop.analytics.GoalRewards
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class GoalsSnapshot(
    val enabled: Boolean = false,
    val streaksEnabled: Boolean = false,
    val streakStartEpochDay: Long? = null,
    val restPerWeek: Int = 2,
    val pauses: List<GoalPause> = emptyList(),
    val goals: List<PeriodGoal> = emptyList(),
    val questsFromEpochSecond: Long? = null,
    val questWeek: WeeklyQuests.Week? = null,
    val questOutcomes: List<WeeklyQuests.Outcome> = emptyList(),
    val streakChallenge: StreakChallenge.Challenge? = null,
    val reachedChallenge: StreakChallenge.Challenge? = null,
    val streakLegOptions: List<Int> = emptyList(),
    val streakChallengeBroken: Boolean = false,
    val challengeEndTarget: Int? = null,
    val challengeEndAt: Int? = null,
    val challengeEndReached: Int? = null,
    val rewardChests: List<GoalRewards.Chest> = emptyList(),
    val ownedRewards: List<String> = emptyList(),
    val freezeBankEpochDays: List<Long> = emptyList(),
    val chestsFromEpochSecond: Long? = null,
)

data class GoalPause(val startEpochDay: Long, val endEpochDay: Long)

/** On-device goal settings, matching Aura's single `noop.goals.v1` stored value. */
object GoalsStore {
    private const val PREFS = "noop_goals"
    private const val KEY = "noop.goals.v1"
    private const val FLAME_PREFS = "noop_prefs"
    private const val FLAME_KEY = "noop.flame.skin"

    fun equippedFlame(context: Context): String = context.getSharedPreferences(FLAME_PREFS, Context.MODE_PRIVATE)
        .getString(FLAME_KEY, "ember")?.takeIf { it.isNotBlank() } ?: "ember"

    fun setEquippedFlame(context: Context, id: String) {
        context.getSharedPreferences(FLAME_PREFS, Context.MODE_PRIVATE).edit().putString(FLAME_KEY, id).apply()
    }

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
                streakStartEpochDay = root.takeIf { it.has("startDay") && !it.isNull("startDay") }?.let {
                    val value = it.get("startDay")
                    if (value is Number && value.toDouble() < 10_000_000.0) value.toLong()
                    else instantFromStoredDate((value as Number).toDouble()).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                },
                restPerWeek = root.optInt("restPerWeek", 2).coerceIn(0, 3),
                pauses = List(pauses.length()) { index ->
                    val item = pauses.getJSONObject(index)
                    if (item.has("startDay")) GoalPause(item.getLong("startDay"), item.getLong("endDay"))
                    else GoalPause(
                        instantFromStoredDate(item.getDouble("start")).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay(),
                        instantFromStoredDate(item.getDouble("end")).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay(),
                    )
                },
                goals = List(goals.length()) { index ->
                    val item = goals.getJSONObject(index)
                    PeriodGoal(
                        id = item.getString("id"),
                        period = PeriodGoal.Period.valueOf(item.getString("period").uppercase()),
                        metric = PeriodGoal.Metric.valueOf(item.getString("metric").uppercase()),
                        sport = item.optString("sport").takeIf { it.isNotBlank() && it != "null" },
                        target = item.getDouble("target"),
                        createdAt = instantFromStoredDate(item.getDouble("createdAt")),
                    )
                },
                questsFromEpochSecond = root.takeIf { it.has("questsFrom") && !it.isNull("questsFrom") }
                    ?.optDouble("questsFrom")?.let { instantFromStoredDate(it).epochSecond },
                questWeek = root.optJSONObject("questWeek")?.let(::readWeek),
                questOutcomes = root.optJSONArray("questOutcomes")?.let { list ->
                    List(list.length()) { index ->
                        val item = list.getJSONObject(index)
                        WeeklyQuests.Outcome(
                            instantFromStoredDate(item.getDouble("weekStart")),
                            readTier(item, "tier"),
                            item.getBoolean("done"),
                        )
                    }
                }.orEmpty(),
                streakChallenge = readChallenge(root.optJSONObject("challenge")),
                reachedChallenge = readChallenge(root.optJSONObject("reachedLeg")),
                streakLegOptions = root.optJSONArray("legChoice")?.let { a -> List(a.length()) { a.getInt(it) } }.orEmpty(),
                streakChallengeBroken = root.optJSONObject("challengeEnd")?.has("broken") == true,
                challengeEndTarget = root.optJSONObject("challengeEnd")?.optJSONObject("broken")?.optInt("target"),
                challengeEndAt = root.optJSONObject("challengeEnd")?.optJSONObject("broken")?.optInt("at"),
                challengeEndReached = root.optJSONObject("challengeEnd")?.optJSONObject("reached")?.optInt("_0"),
                rewardChests = root.optJSONArray("chests")?.let { a -> List(a.length()) { i ->
                    val c = a.getJSONObject(i)
                    val reward = c.optJSONObject("reward")?.let { r -> when {
                        r.has("freeze") -> GoalRewards.Reward.Freeze(r.getJSONObject("freeze").getInt("_0"))
                        r.has("item") -> GoalRewards.Reward.Item(r.getJSONObject("item").getString("_0"))
                        else -> null
                    } }
                    GoalRewards.Chest(c.getString("id"), GoalRewards.Tier.entries[c.getInt("tier")], c.getString("reason"),
                        instantFromStoredDate(c.getDouble("earnedAt")).epochSecond,
                        c.takeIf { it.has("openedAt") && !it.isNull("openedAt") }?.let { instantFromStoredDate(it.getDouble("openedAt")).epochSecond }, reward)
                } }.orEmpty(),
                ownedRewards = root.optJSONArray("owned")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty(),
                freezeBankEpochDays = root.optJSONArray("freezeBank")?.let { a -> List(a.length()) { i ->
                    instantFromStoredDate(a.getDouble(i)).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                } }.orEmpty(),
                chestsFromEpochSecond = root.takeIf { it.has("chestsFrom") && !it.isNull("chestsFrom") }
                    ?.optDouble("chestsFrom")?.let { instantFromStoredDate(it).epochSecond },
            )
        }.getOrDefault(GoalsSnapshot())
    }

    fun save(context: Context, snapshot: GoalsSnapshot) {
        val goals = JSONArray()
        val pauses = JSONArray()
        snapshot.pauses.forEach { pause ->
            pauses.put(JSONObject()
                .put("start", swiftDate(LocalDate.ofEpochDay(pause.startEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .put("end", swiftDate(LocalDate.ofEpochDay(pause.endEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant())))
        }
        snapshot.goals.forEach { goal ->
            goals.put(
                JSONObject()
                    .put("id", goal.id)
                    .put("period", goal.period.name.lowercase())
                    .put("metric", goal.metric.name.lowercase())
                    .put("sport", goal.sport)
                    .put("target", goal.target)
                    .put("createdAt", swiftDate(goal.createdAt)),
            )
        }
        val outcomes = JSONArray().apply {
            snapshot.questOutcomes.takeLast(60).forEach { outcome ->
                put(JSONObject().put("weekStart", swiftDate(outcome.weekStart))
                    .put("tier", outcome.tier.ordinal).put("done", outcome.done))
            }
        }
        val json = JSONObject()
            .put("goalsOn", snapshot.enabled)
            .put("streaksOn", snapshot.streaksEnabled)
            .put("startDay", snapshot.streakStartEpochDay?.let {
                swiftDate(LocalDate.ofEpochDay(it).atStartOfDay(ZoneId.systemDefault()).toInstant())
            })
            .put("restPerWeek", snapshot.restPerWeek.coerceIn(0, 3))
            .put("pauses", pauses)
            .put("goals", goals)
            .put("questsFrom", snapshot.questsFromEpochSecond?.let { swiftDate(Instant.ofEpochSecond(it)) })
            .put("questWeek", snapshot.questWeek?.let(::writeWeek))
            .put("questOutcomes", outcomes)
            .put("challenge", snapshot.streakChallenge?.let(::writeChallenge))
            .put("reachedLeg", snapshot.reachedChallenge?.let(::writeChallenge))
            .put("legChoice", JSONArray(snapshot.streakLegOptions))
            .put("challengeEnd", when {
                snapshot.streakLegOptions.isNotEmpty() && snapshot.challengeEndReached != null -> JSONObject().put("reached", JSONObject().put("_0", snapshot.challengeEndReached))
                snapshot.streakChallengeBroken && snapshot.challengeEndTarget != null -> JSONObject().put("broken", JSONObject()
                    .put("target", snapshot.challengeEndTarget).put("at", snapshot.challengeEndAt ?: 0))
                else -> null
            })
            .put("chests", JSONArray().apply { snapshot.rewardChests.forEach { c ->
                put(JSONObject().put("id", c.id).put("tier", c.tier.ordinal).put("reason", c.reason)
                    .put("earnedAt", swiftDate(Instant.ofEpochSecond(c.earnedEpochSecond)))
                    .put("openedAt", c.openedEpochSecond?.let { swiftDate(Instant.ofEpochSecond(it)) })
                    .put("reward", c.reward?.let { reward -> when (reward) {
                        is GoalRewards.Reward.Freeze -> JSONObject().put("freeze", JSONObject().put("_0", reward.count))
                        is GoalRewards.Reward.Item -> JSONObject().put("item", JSONObject().put("_0", reward.id))
                    } }))
            } })
            .put("owned", JSONArray(snapshot.ownedRewards))
            .put("freezeBank", JSONArray().apply { snapshot.freezeBankEpochDays.forEach { day ->
                put(swiftDate(LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant()))
            } })
            .put("chestsFrom", snapshot.chestsFromEpochSecond?.let { swiftDate(Instant.ofEpochSecond(it)) })
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, json.toString()).apply()
    }

    private fun writeWeek(week: WeeklyQuests.Week): JSONObject = JSONObject()
        .put("start", swiftDate(week.start))
        .put("swapUsed", week.swapUsed)
        .put("starter", week.starter)
        .put("quests", JSONArray().apply {
            week.quests.forEach { quest ->
                put(JSONObject()
                    .put("kind", writeKind(quest.kind))
                    .put("tier", quest.tier.ordinal)
                    .put("target", quest.target)
                    .put("comeback", quest.comeback)
                    .put("eased", quest.eased))
            }
        })

    private fun writeChallenge(challenge: StreakChallenge.Challenge): JSONObject = JSONObject()
        .put("target", challenge.target)
        .put("acceptedAt", swiftDate(Instant.ofEpochSecond(challenge.acceptedEpochSecond)))
        .put("runStart", challenge.runStartEpochDay?.let { swiftDate(LocalDate.ofEpochDay(it).atStartOfDay(ZoneId.systemDefault()).toInstant()) })
        .put("startCount", challenge.startCount).put("base", challenge.base).put("peak", challenge.peak)

    private fun readChallenge(item: JSONObject?): StreakChallenge.Challenge? = item?.let {
        StreakChallenge.Challenge(it.getInt("target"), instantFromStoredDate(it.getDouble("acceptedAt")).epochSecond,
            it.takeIf { value -> value.has("runStart") && !value.isNull("runStart") }?.let { value ->
                instantFromStoredDate(value.getDouble("runStart")).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            },
            it.getInt("startCount"), it.takeIf { value -> value.has("base") && !value.isNull("base") }?.getInt("base"),
            it.takeIf { value -> value.has("peak") && !value.isNull("peak") }?.getInt("peak"))
    }

    private fun readWeek(value: JSONObject): WeeklyQuests.Week {
        val quests = value.optJSONArray("quests") ?: JSONArray()
        return WeeklyQuests.Week(
            start = instantFromStoredDate(value.getDouble("start")),
            quests = List(quests.length()) { index ->
                val quest = quests.getJSONObject(index)
                WeeklyQuests.Quest(
                    kind = readKind(quest.getJSONObject("kind")),
                    tier = readTier(quest, "tier"),
                    target = quest.getDouble("target"),
                    comeback = quest.optBoolean("comeback"),
                    eased = quest.optBoolean("eased"),
                )
            },
            swapUsed = value.optBoolean("swapUsed"),
            starter = value.optBoolean("starter"),
        )
    }

    private fun writeKind(kind: WeeklyQuests.Kind): JSONObject = when (kind) {
        WeeklyQuests.Kind.Sessions -> JSONObject().put("sessions", JSONObject())
        WeeklyQuests.Kind.Minutes -> JSONObject().put("minutes", JSONObject())
        is WeeklyQuests.Kind.Distance -> JSONObject().put("distance", JSONObject().put("sport", kind.sport))
        WeeklyQuests.Kind.Lifts -> JSONObject().put("lifts", JSONObject())
        is WeeklyQuests.Kind.EarlyNights -> JSONObject().put("earlyNights", JSONObject().put("byMinute", kind.byMinute))
        is WeeklyQuests.Kind.LongNights -> JSONObject().put("longNights", JSONObject().put("minutes", kind.minutes))
        is WeeklyQuests.Kind.StepDays -> JSONObject().put("stepDays", JSONObject().put("steps", kind.steps))
        WeeklyQuests.Kind.EasyAfterHard -> JSONObject().put("easyAfterHard", JSONObject())
    }

    private fun readKind(value: JSONObject): WeeklyQuests.Kind = when {
        value.has("sessions") -> WeeklyQuests.Kind.Sessions
        value.has("minutes") -> WeeklyQuests.Kind.Minutes
        value.has("distance") -> WeeklyQuests.Kind.Distance(value.getJSONObject("distance").getString("sport"))
        value.has("lifts") -> WeeklyQuests.Kind.Lifts
        value.has("earlyNights") -> WeeklyQuests.Kind.EarlyNights(value.getJSONObject("earlyNights").getInt("byMinute"))
        value.has("longNights") -> WeeklyQuests.Kind.LongNights(value.getJSONObject("longNights").getInt("minutes"))
        value.has("stepDays") -> WeeklyQuests.Kind.StepDays(value.getJSONObject("stepDays").getInt("steps"))
        value.has("easyAfterHard") -> WeeklyQuests.Kind.EasyAfterHard
        else -> error("Unknown goal quest kind")
    }

    private fun readTier(value: JSONObject, key: String): WeeklyQuests.Tier {
        val stored = value.get(key)
        return when (stored) {
            is Number -> WeeklyQuests.Tier.entries[stored.toInt().coerceIn(0, WeeklyQuests.Tier.entries.lastIndex)]
            is String -> WeeklyQuests.Tier.valueOf(stored.uppercase())
            else -> error("Invalid quest tier")
        }
    }

    /** Swift JSONEncoder's default Date encoding is seconds since 2001-01-01. */
    private fun swiftDate(instant: Instant): Double =
        instant.epochSecond - DATE_REFERENCE_EPOCH_SECONDS.toDouble() + instant.nano / 1_000_000_000.0

    /** Accept the legacy Android millisecond/Unix-second forms while writing the shared Swift form. */
    private fun instantFromStoredDate(value: Double): Instant = when {
        value > 100_000_000_000.0 -> Instant.ofEpochMilli(value.toLong())
        value > 900_000_000.0 -> Instant.ofEpochSecond(value.toLong())
        else -> Instant.ofEpochSecond(DATE_REFERENCE_EPOCH_SECONDS + value.toLong())
    }

    private const val DATE_REFERENCE_EPOCH_SECONDS = 978_307_200L

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
