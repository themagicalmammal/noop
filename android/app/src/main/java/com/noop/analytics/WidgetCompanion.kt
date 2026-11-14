package com.noop.analytics

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The home-screen companion's on-disk snapshot, mirrored from Aura's `WidgetCompanion`
 * (StrandiOSShared). The three companion widgets (Mood face, Keeper's Fire, Chest Road)
 * all read this one value instead of recomputing it, so they can never disagree.
 *
 * Dates are stored as Swift-epoch doubles (seconds since 2001-01-01), the shared form
 * GoalsStore uses for every stored instant.
 */
data class WidgetCompanion(
    val streaksOn: Boolean = false,
    val streakDays: Int = 0,
    val legStart: Int? = null,
    val legTarget: Int? = null,
    val trainedOn: Instant? = null,
    val restOn: Instant? = null,
    val milestoneOn: Instant? = null,
    val ownsFire: Boolean = false,
    val ownsRoad: Boolean = false,
    val biscuitOn: Boolean? = null,
) {

    /** The face's mood right now, in exactly Aura's priority order: asleep hours beat
     *  everything; then the day's events (milestone beats trained beats rest); then the
     *  end-of-day worry; else happy. */
    fun mood(at: ZonedDateTime): String {
        val hour = at.hour
        if (hour >= 23 || hour < 6) return "asleep"
        val today = at.toLocalDate()
        if (sameDayToday(milestoneOn, today)) return "radiant"
        if (sameDayToday(trainedOn, today)) return "proud"
        if (sameDayToday(restOn, today)) return "calm"
        if (streaksOn && streakDays > 0 && hour >= 16) return "worried"
        return "happy"
    }

    private fun sameDayToday(stamp: Instant?, today: LocalDate): Boolean =
        stamp?.atZone(ZoneId.systemDefault())?.toLocalDate() == today

    /** The road's fill, clamped: 0 before the leg starts, 1 at or beyond the target,
     *  and 0 with no leg at all (the face is asleep anyway in that case). */
    fun roadShare(): Double =
        legStart?.let { start -> legTarget?.let { target -> WidgetCompanion.roadShare(start, target, streakDays) } } ?: 0.0

    fun encode(): JSONObject {
        val json = JSONObject()
            .put("streaksOn", streaksOn)
            .put("streakDays", streakDays)
            .put("ownsFire", ownsFire)
            .put("ownsRoad", ownsRoad)
        legStart?.let { json.put("legStart", it) }
        legTarget?.let { json.put("legTarget", it) }
        trainedOn?.let { json.put("trainedOn", swiftDate(it)) }
        restOn?.let { json.put("restOn", swiftDate(it)) }
        milestoneOn?.let { json.put("milestoneOn", swiftDate(it)) }
        biscuitOn?.let { json.put("biscuitOn", it) }
        return json
    }

    /** Tolerant read, in the GoalsStore style: absent keys become null, and dates are
     *  accepted in any of the three stored forms. */
    companion object {
        val moods = listOf("asleep", "radiant", "proud", "calm", "worried", "happy")
        const val storageKey = "noop.widget.companion.v1"

        /** The road fill as a pure helper, so the clamp is testable without a snapshot. */
        fun roadShare(legStart: Int, legTarget: Int, streakDays: Int): Double {
            val span = (legTarget - legStart).coerceAtLeast(1)
            return ((streakDays - legStart).toDouble() / span).coerceIn(0.0, 1.0)
        }

        /** Swift JSONEncoder writes dates as seconds since 2001-01-01; the shared form. */
        fun swiftDate(instant: Instant): Double =
            instant.epochSecond - DATE_REFERENCE_EPOCH_SECONDS.toDouble() + instant.nano / 1_000_000_000.0

        /** Accepts the legacy millisecond / Unix-second forms, writes the shared Swift form. */
        fun instantFromStoredDate(value: Double): Instant = when {
            value > 100_000_000_000.0 -> Instant.ofEpochMilli(value.toLong())
            value > 900_000_000.0 -> Instant.ofEpochSecond(value.toLong())
            else -> Instant.ofEpochSecond(DATE_REFERENCE_EPOCH_SECONDS + value.toLong())
        }

        private const val DATE_REFERENCE_EPOCH_SECONDS = 978_307_200L

        fun fromJson(json: JSONObject): WidgetCompanion {
            val optionalInt: (String) -> Int? = { key ->
                if (json.has(key) && !json.isNull(key) && json.get(key) is Number) json.getInt(key) else null
            }
            val optionalDate: (String) -> Instant? = { key ->
                if (json.has(key) && !json.isNull(key) && json.get(key) is Number)
                    instantFromStoredDate(json.getDouble(key)) else null
            }
            val optionalBool: (String) -> Boolean? = { key ->
                if (json.has(key) && !json.isNull(key)) json.getBoolean(key) else null
            }
            return WidgetCompanion(
                streaksOn = json.optBoolean("streaksOn", false),
                streakDays = json.optInt("streakDays", 0),
                legStart = optionalInt("legStart"),
                legTarget = optionalInt("legTarget"),
                trainedOn = optionalDate("trainedOn"),
                restOn = optionalDate("restOn"),
                milestoneOn = optionalDate("milestoneOn"),
                ownsFire = json.optBoolean("ownsFire", false),
                ownsRoad = json.optBoolean("ownsRoad", false),
                biscuitOn = optionalBool("biscuitOn"),
            )
        }
    }
}
