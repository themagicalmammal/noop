package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity pin for [BiscuitTouchLayer]: zone resolution, tap-streak escalation, sulk targeting,
 * squish announcement, and spring settle behavior of the Biscuit touch twin. All times are
 * epoch milliseconds; every expected value is derived from the Kotlin twin's documented
 * constants so the port cannot drift from the Aura reference.
 */
class BiscuitTouchTest {

    private val t0 = 1_000_000L

    private fun BiscuitTouchLayer.tap(u: Double, v: Double, at: Long): BiscuitTouchResult {
        began(u, v, at)
        return ended(at)
    }

    @Test
    fun bellyTapStartlesAndGetsAnnoyed() {
        val layer = BiscuitTouchLayer()
        val result = layer.tap(0.1, 0.5, t0)
        assertEquals(BiscuitReaction.STARTLE, result.reaction)
        assertEquals(BiscuitClip.STARTLE, result.clip)
        assertEquals(t0 + 6000L, layer.annoyedUntil)
    }

    @Test
    fun headTapPatsAndSchedulesContentWindow() {
        val layer = BiscuitTouchLayer()
        val result = layer.tap(0.0, -0.6, t0)
        assertEquals(BiscuitReaction.PAT, result.reaction)
        assertNull(result.clip)
        assertEquals(t0 + 900L, layer.nextChange(t0))
    }

    @Test
    fun sideTapsGiggle() {
        for (u in listOf(-0.8, 0.8)) {
            val layer = BiscuitTouchLayer()
            val result = layer.tap(u, 0.0, t0)
            assertEquals("u=$u", BiscuitReaction.GIGGLE, result.reaction)
            assertEquals("u=$u", BiscuitClip.TAP, result.clip)
        }
    }

    @Test
    fun eyesTapWincesAndSchedulesWinceWindow() {
        val layer = BiscuitTouchLayer()
        val result = layer.tap(0.2, -0.2, t0)
        assertEquals(BiscuitReaction.WINCE, result.reaction)
        assertNull(result.clip)
        assertEquals(t0 + 500L, layer.nextChange(t0))
    }

    @Test
    fun tapStreakEscalatesThroughMoods() {
        val layer = BiscuitTouchLayer()

        val r1 = layer.tap(0.1, 0.5, t0)
        assertEquals(BiscuitReaction.STARTLE, r1.reaction)
        assertEquals(BiscuitClip.STARTLE, r1.clip)
        assertEquals(t0 + 6000L, layer.annoyedUntil)

        val r2 = layer.tap(0.1, 0.5, t0 + 1000L)
        assertEquals(BiscuitReaction.WINK, r2.reaction)
        assertEquals(BiscuitClip.WINK, r2.clip)

        val r3 = layer.tap(0.1, 0.5, t0 + 2000L)
        assertEquals(BiscuitReaction.BOUNCE, r3.reaction)
        assertEquals(BiscuitClip.BOUNCE, r3.clip)

        val r4 = layer.tap(0.1, 0.5, t0 + 3000L)
        assertEquals(BiscuitReaction.EYE_ROLL, r4.reaction)
        assertEquals(BiscuitClip.EYE_ROLL, r4.clip)
        assertEquals(t0 + 18000L, layer.annoyedUntil)

        val r5 = layer.tap(0.1, 0.5, t0 + 4000L)
        assertEquals(BiscuitReaction.EYE_ROLL, r5.reaction)
        assertEquals(BiscuitClip.EYE_ROLL, r5.clip)
        assertEquals(t0 + 19000L, layer.annoyedUntil)

        val r6 = layer.tap(0.1, 0.5, t0 + 5000L)
        assertEquals(BiscuitReaction.ANGRY, r6.reaction)
        assertEquals(BiscuitClip.SHAKE, r6.clip)
        assertEquals(t0 + 7500L, layer.angryUntil)
        assertEquals(t0 + 25000L, layer.annoyedUntil)

        val r7 = layer.tap(-0.3, 0.0, t0 + 6000L)
        assertEquals(BiscuitReaction.IGNORE, r7.reaction)
        assertEquals(BiscuitClip.SIGH, r7.clip)
        assertEquals(t0 + 10500L, layer.sulkUntil)
        assertEquals(t0 + 31000L, layer.annoyedUntil)

        assertEquals(BiscuitMood.ANGRY, layer.moodOverride(t0 + 7000L))
        assertEquals(BiscuitMood.ANNOYED, layer.moodOverride(t0 + 7600L))
        assertNull(layer.moodOverride(t0 + 32000L))

        val r8 = layer.tap(0.0, 0.0, t0 + 8000L)
        assertEquals(BiscuitReaction.IGNORE, r8.reaction)
        assertNull(r8.clip)
    }

    @Test
    fun grumpyFirstTapEyeRollsAndExtendsAnnoyance() {
        val layer = BiscuitTouchLayer()
        layer.tap(0.1, 0.5, t0)
        val result = layer.tap(0.0, -0.6, t0 + 2000L)
        assertEquals(BiscuitReaction.EYE_ROLL, result.reaction)
        assertEquals(BiscuitClip.EYE_ROLL, result.clip)
        assertEquals(t0 + 10000L, layer.annoyedUntil)
    }

    private fun BiscuitTouchLayer.replayStreak() {
        for (i in 0L until 6L) tap(0.1, 0.5, t0 + 1000L * i)
        tap(-0.3, 0.0, t0 + 6000L)
    }

    @Test
    fun sulkFramePointsAwayFromTheFinger() {
        val layer = BiscuitTouchLayer()
        layer.replayStreak()
        assertNull(layer.step(t0 + 6100L))
        assertEquals(12.0, layer.gazeX.target, 0.0)
        assertEquals(-3.0, layer.gazeY.target, 0.0)
        assertEquals(4.0, layer.leanX.target, 0.0)
        assertEquals(0.0, layer.leanY.target, 0.0)
        assertEquals(0.08, layer.tilt.target, 0.0)
        assertEquals(0.8, layer.lids.target, 0.0)
    }

    @Test
    fun longPressAnnouncesSquishExactlyOnce() {
        val layer = BiscuitTouchLayer()
        layer.began(0.1, 0.1, t0)
        for (i in 1L..8L) assertNull("step $i", layer.step(t0 + 34L * i))
        assertEquals(BiscuitReaction.SQUISH, layer.step(t0 + 306L))
        for (i in 10L..16L) assertNull("step $i", layer.step(t0 + 34L * i))

        val result = layer.ended(t0 + 1600L)
        assertNull(result.reaction)
        assertNull(result.clip)

        for (i in 1L..200L) layer.step(t0 + 1600L + 34L * i)
        assertTrue(layer.isSettled)
    }

    @Test
    fun endedWithoutFingerIsANoOp() {
        val layer = BiscuitTouchLayer()
        val result = layer.ended(t0)
        assertNull(result.reaction)
        assertNull(result.clip)
    }

    @Test
    fun heldPressIsNotATap() {
        val layer = BiscuitTouchLayer()
        layer.began(0.0, 0.0, t0)
        val result = layer.ended(t0 + 400L)
        assertNull(result.reaction)
        assertNull(result.clip)
    }

    @Test
    fun draggedPressIsNotATap() {
        val layer = BiscuitTouchLayer()
        layer.began(0.1, 0.1, t0)
        layer.moved(0.6, 0.1)
        val result = layer.ended(t0 + 100L)
        assertNull(result.reaction)
        assertNull(result.clip)
    }

    @Test
    fun idleLayerIsSettledAndHasNoPendingChange() {
        val layer = BiscuitTouchLayer()
        assertTrue(layer.isSettled)
        assertNull(layer.nextChange(t0))
        assertNull(layer.moodOverride(t0))
        layer.began(0.1, 0.1, t0)
        assertTrue(layer.isActive)
        assertFalse(layer.isSettled)
    }

    @Test
    fun springFollowsTargetAndSettles() {
        val spring = BiscuitSpring(0.0)
        spring.target = 1.0
        val dt = 1.0 / 60.0
        repeat(600) { spring.step(dt, stiffness = 100.0, damping = 10.0) }
        assertTrue(spring.value > 0.9)
        repeat(3000) { spring.step(dt, stiffness = 100.0, damping = 10.0) }
        assertTrue(spring.settled())
    }
}
