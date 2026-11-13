package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Parity oracle for [BiscuitBehaviorScheduler]. The expected literals are the verbatim stdout of a
 * standalone Swift build of Aura's `BiscuitBehaviorScheduler` (`swiftc -O BiscuitBehaviorScheduler.swift
 * main.swift -o oracle`), which prints every sample field with Swift's default `Double`
 * round-trip precision. The driver ran one scheduler through a mood walk, breaths-only mode, an
 * interaction interruption, and a disabled mood, then a second scheduler (seed 1) through a
 * curious mood; each test replays one driver session so the scheduler state matches. All assertions
 * use exact equality, so the Kotlin twin is pinned bit-for-bit.
 */
class BiscuitBehaviorTest {
    private data class Expect(
        val floatSeconds: Double,
        val floatStrength: Double,
        val lid: Double,
        val eyeX: Double = 0.0,
        val eyeY: Double = 0.0,
        val bodyX: Double = 0.0,
        val bodyY: Double = 0.0,
        val tilt: Double = 0.0,
        val curiosity: Double = 0.0,
        val blinkID: Long = 0L,
        val gazeID: Long = 0L,
        val isQuiet: Boolean,
        val movingQuickly: Boolean,
        val nextEvent: Double,
    )

    private class Reporter(private val scheduler: BiscuitBehaviorScheduler) {
        var mood: BiscuitMood = BiscuitMood.NEUTRAL

        fun check(t: Double, expect: Expect, enabled: Boolean = true) {
            val sample = scheduler.sample(t, mood, enabled)
            assertEquals("floatSeconds @ $t", expect.floatSeconds, sample.floatSeconds, 0.0)
            assertEquals("floatStrength @ $t", expect.floatStrength, sample.floatStrength, 0.0)
            assertEquals("lid @ $t", expect.lid, sample.lid, 0.0)
            assertEquals("eyeX @ $t", expect.eyeX, sample.eyeX, 0.0)
            assertEquals("eyeY @ $t", expect.eyeY, sample.eyeY, 0.0)
            assertEquals("bodyX @ $t", expect.bodyX, sample.bodyX, 0.0)
            assertEquals("bodyY @ $t", expect.bodyY, sample.bodyY, 0.0)
            assertEquals("tilt @ $t", expect.tilt, sample.tilt, 0.0)
            assertEquals("curiosity @ $t", expect.curiosity, sample.curiosity, 0.0)
            assertEquals("blinkID @ $t", expect.blinkID, sample.blinkID)
            assertEquals("gazeID @ $t", expect.gazeID, sample.gazeID)
            assertEquals("isQuiet @ $t", expect.isQuiet, sample.isQuiet)
            assertEquals("isMovingQuickly @ $t", expect.movingQuickly, scheduler.isMovingQuickly(t))
            assertEquals("nextEvent @ $t", expect.nextEvent, scheduler.nextEvent(t), 0.0)
        }
    }

    private val inf = Double.POSITIVE_INFINITY

    @Test
    fun driverSessionOneMatchesSwiftOracle() {
        val s = BiscuitBehaviorScheduler(0xC0FFEE12345678L)
        val r = Reporter(s)

        r.mood = BiscuitMood.NEUTRAL
        r.check(0.0, Expect(0.0, 0.8170845565298762, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 0.0))
        r.check(0.5, Expect(0.46602418293896186, 0.34218294779871306, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 0.5))
        r.check(1.2, Expect(1.1184340289397319, 0.5260192408718484, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 3.037715010252103))
        r.check(3.0, Expect(2.7962116099481817, 0.8028887873982327, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 3.037715010252103))
        r.check(5.0, Expect(4.661372850640141, 0.7935015140441299, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 9.373116237781938))

        r.mood = BiscuitMood.HAPPY
        r.check(8.0, Expect(7.46306643689637, 0.7797950065726651, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 9.373116237781938))
        r.check(11.5, Expect(10.925577130925321, 0.8497520403428424, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 11.5))
        r.check(13.0, Expect(12.430008194834393, 0.5985938531196201, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 16.62360465642243))

        r.mood = BiscuitMood.SLEEPY
        r.check(15.0, Expect(14.440480091505258, 0.8347654583461662, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 16.62360465642243))
        r.check(20.0, Expect(18.00134937809359, 0.28605337921722046, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 20.0))
        r.check(30.0, Expect(24.83476104616973, 0.2800873080687603, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 30.0))

        r.mood = BiscuitMood.ASLEEP
        r.check(45.0, Expect(35.28415558883974, 0.28865972141550805, 1.0, isQuiet = true, movingQuickly = true, nextEvent = inf))
        r.check(60.0, Expect(44.85880137025587, 0.19662596673716123, 1.0, isQuiet = true, movingQuickly = true, nextEvent = inf))

        r.mood = BiscuitMood.NEUTRAL
        r.check(62.0, Expect(46.10786420080534, 0.19894057456918435, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 62.0))
        r.check(70.0, Expect(53.48759316824257, 0.8502630242441302, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 70.0))

        s.breathsOnly = true
        r.mood = BiscuitMood.CALM
        r.check(72.5, Expect(55.83687262142605, 0.0, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 72.5))
        r.check(80.0, Expect(61.342518838292186, 0.0, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 84.02521585840206))

        s.breathsOnly = false
        s.noteInteraction(81.0, 82.0)
        r.check(83.0, Expect(63.484404119239024, 0.49559786390503385, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 84.02521585840206))

        r.mood = BiscuitMood.ANGRY
        r.check(85.0, Expect(64.9123034004929, 0.49750088738430626, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 85.0), enabled = false)
        r.check(87.0, Expect(66.46647946390559, 0.6485414923659112, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 87.0), enabled = false)
        r.check(88.0, Expect(67.27111293136102, 0.6491738191416355, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 88.0))
        r.check(90.0, Expect(68.88242159236846, 0.6499040363609779, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 94.71994381416447))
    }

    @Test
    fun driverSessionTwoMatchesSwiftOracle() {
        val s = BiscuitBehaviorScheduler(1L)
        val r = Reporter(s)
        r.mood = BiscuitMood.CURIOUS

        r.check(0.0, Expect(0.0, 0.8100565091076737, 1.0, isQuiet = true, movingQuickly = true, nextEvent = 0.0))
        r.check(2.0, Expect(2.1455106073754595, 0.819512393101762, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 3.9841986929442896))
        r.check(
            4.0,
            Expect(
                4.29002354736958,
                0.8288629739050241,
                0.861028615257276,
                blinkID = 1L,
                isQuiet = false,
                movingQuickly = true,
                nextEvent = 4.4175623751067095,
            ),
        )
        r.check(
            6.0,
            Expect(
                6.432568790808065,
                0.8380047396057746,
                1.0,
                eyeX = 9.084429637477566,
                eyeY = -1.6696753186322688,
                bodyX = 1.3079076191705115,
                bodyY = -0.5766258985196472,
                tilt = 0.03923722857511534,
                curiosity = 0.03844172656797648,
                gazeID = 1L,
                isQuiet = false,
                movingQuickly = true,
                nextEvent = 8.618194614120249,
            ),
        )
        r.check(8.0, Expect(8.572222789429238, 0.8468364898974475, 1.0, isQuiet = false, movingQuickly = false, nextEvent = 8.618194614120249))
    }
}
