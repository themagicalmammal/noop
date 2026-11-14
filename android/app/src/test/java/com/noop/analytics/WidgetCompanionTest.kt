package com.noop.analytics

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetCompanionTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 14)
    private val yesterday = today.minusDays(1)

    private fun at(hour: Int): ZonedDateTime =
        ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, hour, 0, 0, 0, zone)

    private fun stamp(day: LocalDate): Instant = day.atStartOfDay(zone).toInstant()

    @Test fun asleepHoursWinEverything() {
        val companion = WidgetCompanion(
            streaksOn = true, streakDays = 12, trainedOn = stamp(today),
            restOn = stamp(today), milestoneOn = stamp(today),
        )
        for (hour in listOf(0, 1, 5, 23)) {
            assertEquals("asleep", companion.mood(at(hour)))
        }
        assertEquals("radiant", companion.mood(at(6))) // 06:00 is waking hours: the milestone shows
        assertEquals("asleep", WidgetCompanion().mood(at(2)))
    }

    @Test fun moodPriorityIsMilestoneThenTrainedThenRest() {
        assertEquals("radiant", WidgetCompanion(
            trainedOn = stamp(today), restOn = stamp(today), milestoneOn = stamp(today),
        ).mood(at(12)))
        assertEquals("proud", WidgetCompanion(trainedOn = stamp(today), restOn = stamp(today)).mood(at(12)))
        assertEquals("calm", WidgetCompanion(restOn = stamp(today)).mood(at(12)))
    }

    @Test fun onlyTodaysEventsCount() {
        val companion = WidgetCompanion(trainedOn = stamp(yesterday), restOn = stamp(yesterday), milestoneOn = stamp(yesterday))
        assertEquals("happy", companion.mood(at(12)))
        // A trained stamp that is today in the local zone but yesterday UTC still counts: local is the clock.
        assertEquals("proud", WidgetCompanion(trainedOn = stamp(today)).mood(at(12)))
    }

    @Test fun worriedNeedsAnActiveStreakAndTheEvening() {
        val worried = WidgetCompanion(streaksOn = true, streakDays = 3)
        assertEquals("worried", worried.mood(at(16)))
        assertEquals("happy", worried.mood(at(15)))
        assertEquals("happy", WidgetCompanion(streaksOn = true, streakDays = 0).mood(at(18)))
        assertEquals("happy", WidgetCompanion(streaksOn = false, streakDays = 9).mood(at(18)))
        // An event still outranks the worry.
        assertEquals("proud", WidgetCompanion(streaksOn = true, streakDays = 3, trainedOn = stamp(today)).mood(at(20)))
    }

    @Test fun roadShareClampsToTheLeg() {
        assertEquals(0.0, WidgetCompanion.roadShare(10, 20, 5), 0.0)
        assertEquals(0.5, WidgetCompanion.roadShare(10, 20, 15), 0.0)
        assertEquals(1.0, WidgetCompanion.roadShare(10, 20, 25), 0.0)
        assertEquals(1.0, WidgetCompanion.roadShare(10, 20, 20), 0.0)
        // A zero-length leg divides by 1, not zero.
        assertEquals(0.0, WidgetCompanion.roadShare(10, 10, 9), 0.0)
        assertEquals(1.0, WidgetCompanion.roadShare(10, 10, 11), 0.0)
        // The snapshot form reports no leg as empty.
        assertEquals(0.5, WidgetCompanion(legStart = 10, legTarget = 20, streakDays = 15).roadShare(), 0.0)
        assertEquals(0.0, WidgetCompanion(streakDays = 15).roadShare(), 0.0)
    }

    @Test fun encodeOmitsNullsAndRoundTrips() {
        val instant = Instant.ofEpochSecond(1_760_000_000L)
        val companion = WidgetCompanion(
            streaksOn = true, streakDays = 12, legStart = 7, legTarget = 14,
            trainedOn = instant, restOn = instant, milestoneOn = null,
            ownsFire = true, ownsRoad = false, biscuitOn = true,
        )
        val json = companion.encode()
        assertFalse(json.has("milestoneOn"))
        assertTrue(json.has("legStart"))
        assertTrue(json.has("trainedOn"))
        assertTrue(json.has("restOn"))
        assertEquals(14, json.getInt("legTarget"))
        val back = WidgetCompanion.fromJson(json)
        assertEquals(companion, back)
    }

    @Test fun fromJsonOfAnEmptyObjectIsTheDefaultCompanion() {
        val back = WidgetCompanion.fromJson(JSONObject())
        assertEquals(WidgetCompanion(), back)
        assertNull(back.biscuitOn)
        assertNull(back.legStart)
    }

    @Test fun biscuitOnIsTriState() {
        assertNull(WidgetCompanion.fromJson(JSONObject()).biscuitOn)
        assertEquals(true, WidgetCompanion.fromJson(JSONObject().put("biscuitOn", true)).biscuitOn)
        assertEquals(false, WidgetCompanion.fromJson(JSONObject().put("biscuitOn", false)).biscuitOn)
        // A null biscuitOn key is "not decided", not false.
        assertNull(WidgetCompanion.fromJson(JSONObject().put("biscuitOn", JSONObject.NULL)).biscuitOn)
    }

    @Test fun datesAreAcceptedInAllThreeStoredForms() {
        val swiftForm = WidgetCompanion.swiftDate(Instant.ofEpochSecond(1_760_000_000L))
        val swift = WidgetCompanion.fromJson(JSONObject().put("trainedOn", swiftForm))
        assertEquals(Instant.ofEpochSecond(1_760_000_000L), swift.trainedOn)
        val unix = WidgetCompanion.fromJson(JSONObject().put("trainedOn", 1_760_000_000.0))
        assertEquals(Instant.ofEpochSecond(1_760_000_000L), unix.trainedOn)
        val millis = WidgetCompanion.fromJson(JSONObject().put("trainedOn", 1_760_000_000_000.0))
        assertEquals(Instant.ofEpochMilli(1_760_000_000_000L), millis.trainedOn)
    }

    @Test fun storedShapeMatchesAura() {
        assertEquals("noop.widget.companion.v1", WidgetCompanion.storageKey)
        assertEquals(
            listOf("asleep", "radiant", "proud", "calm", "worried", "happy"),
            WidgetCompanion.moods,
        )
    }
}
