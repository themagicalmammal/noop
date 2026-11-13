package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BiscuitController]: the pose stack, request interruption,
 * pause/resume time accounting, scroll hold, and visibility flow.
 */
class BiscuitControllerTest {

    private fun clip(
        name: String,
        property: String,
        value: Double,
        frames: Int = 30,
    ) = BiscuitAnimation(
        name = name,
        fps = 30.0,
        duration = frames / 30.0,
        speed = 1.0,
        loop = 0,
        start = 0.0,
        end = frames.toDouble(),
        quantize = false,
        tracks = listOf(
            BiscuitAnimationTrack(
                objectIndex = 0,
                property = property,
                color = false,
                keys = listOf(BiscuitAnimationKey(0.0, value, 0, 0)),
            ),
        ),
    )

    private fun scene() = BiscuitScene(
        BiscuitArtboard(
            width = 320.0,
            height = 320.0,
            objects = listOf(BiscuitVectorObject("body", mutableMapOf())),
            animations = listOf(
                clip("aura_arrive", "opacity", 0.0, frames = 60),
                clip("aura_depart", "opacity", 0.0, frames = 60),
                clip("aura_visible", "opacity", 1.0),
                clip("aura_attention_idle", "float", 0.0, frames = 180),
                clip("aura_mood_neutral", "openness", 0.0),
                clip("aura_mood_happy", "openness", 1.0),
                clip("aura_delight", "bounce", 5.0),
                clip("aura_spring", "bounce", 9.0),
                clip("aura_scroll_rise", "lift", 3.0, frames = 90),
                clip("aura_scroll_fall", "lift", 3.0, frames = 90),
            ),
        ),
        bindings = null,
    )

    private class FakeClock(var now: Long) : () -> Long {
        override fun invoke(): Long = now
    }

    @Test
    fun moodChangeCrossfadesPoseValues() {
        val clock = FakeClock(0L)
        val controller = BiscuitController(scene(), clock = clock)
        val start = 1000L

        // Initial pose shows the neutral mood.
        assertEquals(0.0, controller.pose(start, reducedMotion = false)[0].values["openness"]!!, 0.0)

        // The pose that detects the mood change starts a 0.24 s blend at 0.
        controller.mood = BiscuitMood.HAPPY
        controller.pose(start + 120L, reducedMotion = false)

        // At the midpoint of the blend the values are half mixed.
        val mid = controller.pose(start + 240L, reducedMotion = false)[0].values["openness"]
        assertEquals(0.5, mid!!, 1e-9)

        // After the blend completes the new mood is fully applied.
        val done = controller.pose(start + 400L, reducedMotion = false)[0].values["openness"]
        assertEquals(1.0, done!!, 0.0)
    }

    @Test
    fun moodChangeCrossfadesColorValueWithColorLerp() {
        val moodScene = BiscuitScene(
            BiscuitArtboard(
                width = 320.0,
                height = 320.0,
                objects = listOf(BiscuitVectorObject("body", mutableMapOf())),
                animations = listOf(
                    clip("aura_mood_neutral", "colorValue", 0.0),
                    clip("aura_mood_happy", "colorValue", 0xFF000000L.toDouble()),
                ),
            ),
            bindings = null,
        )
        val clock = FakeClock(0L)
        val controller = BiscuitController(moodScene, clock = clock)
        val start = 1000L

        controller.pose(start, reducedMotion = false)
        controller.mood = BiscuitMood.HAPPY
        controller.pose(start + 120L, reducedMotion = false)
        val mid = controller.pose(start + 240L, reducedMotion = false)[0].values["colorValue"]
        // Half mix of 0x00000000 toward 0xFF000000 leaves only the alpha channel (0x7F).
        assertEquals(0x7F000000L, mid!!.toLong())
    }

    @Test
    fun newRequestInterruptsPreviousAndExpiresAfterClipLength() {
        val clock = FakeClock(0L)
        val controller = BiscuitController(scene(), clock = clock)
        val start = 1000L

        clock.now = start
        controller.play(BiscuitClip.TAP)
        // A second request interrupts the first and becomes active.
        controller.play(BiscuitClip.BOUNCE)
        assertEquals(BiscuitClip.BOUNCE, controller.lastAnimation)

        // At full envelope the new clip's value is applied.
        val bounced = controller.pose(start + 120L, reducedMotion = false)[0].values["bounce"]
        assertEquals(9.0, bounced!!, 0.0)

        // After the clip duration the request expires and the value returns.
        assertNull(controller.pose(start + 2000L, reducedMotion = false)[0].values["bounce"])
    }

    @Test
    fun pauseFreezesTimeAndResumeShiftsOrigin() {
        val clock = FakeClock(4000L)
        val controller = BiscuitController(scene(), clock = clock)
        controller.play(BiscuitClip.TAP)

        val during = controller.pose(4120L, reducedMotion = false)[0].values["bounce"]
        assertEquals(5.0, during!!, 0.0)

        // Pause at the frozen instant, then jump the clock: the pose must stay fixed.
        clock.now = 4120L
        controller.isPaused = true
        clock.now = 100_000L
        val frozen = controller.pose(100_000L, reducedMotion = false)[0].values["bounce"]
        assertEquals(5.0, frozen!!, 0.0)

        // Resume shifts the origin by the paused duration, so the request is still
        // inside its clip, then expires as real time passes.
        controller.isPaused = false
        val after = controller.pose(100_500L, reducedMotion = false)[0].values["bounce"]
        assertEquals(5.0, after!!, 0.0)
        assertNull(controller.pose(102_000L, reducedMotion = false)[0].values["bounce"])
    }

    @Test
    fun scrollAccumulatesPastThresholdBeforePlaying() {
        val clock = FakeClock(10_000L)
        val controller = BiscuitController(scene(), clock = clock)
        val start = 10_000L

        // Below the 6 px threshold nothing plays.
        controller.reactToScroll(5.9)
        assertEquals(BiscuitClip.IDLE, controller.lastAnimation)
        assertNull(controller.pose(start, reducedMotion = false)[0].values["lift"])

        // Crossing the threshold plays a scroll clip, fully mixed at 120 ms.
        controller.reactToScroll(0.2)
        assertEquals(BiscuitClip.SCROLL_DOWN, controller.lastAnimation)
        val lifted = controller.pose(start + 120L, reducedMotion = false)[0].values["lift"]
        assertEquals(3.0, lifted!!, 0.0)

        // The held scroll request (0.65 s hold phase) expires just past 3.16 s.
        assertNull(controller.pose(start + 3200L, reducedMotion = false)[0].values["lift"])
    }

    @Test
    fun visibilityArrivesOnlyAfterClipCompletes() {
        val hidden = BiscuitController(scene(), appearsOnStart = false, clock = FakeClock(0L))
        val start = 10_000L
        assertEquals(1.0, hidden.pose(start, reducedMotion = false)[0].values["opacity"]!!, 0.0)

        val fresh = BiscuitController(scene(), appearsOnStart = true, clock = FakeClock(10_000L))
        assertEquals(0.0, fresh.pose(start, reducedMotion = false)[0].values["opacity"]!!, 0.0)
        assertEquals(1.0, fresh.pose(start + 2500L, reducedMotion = false)[0].values["opacity"]!!, 0.0)
    }

    @Test
    fun frugalCountsStirAndDisablesCloudMotion() {
        val frugal = BiscuitController(scene(), frugal = true, clock = FakeClock(0L))
        assertFalse(frugal.cloudMotion)
        assertEquals(0, frugal.stir)
        frugal.mood = BiscuitMood.HAPPY
        assertEquals(1, frugal.stir)

        val normal = BiscuitController(scene(), frugal = false, clock = FakeClock(0L))
        assertTrue(normal.cloudMotion)
    }

    @Test
    fun hideAndAppearToggleVisibility() {
        val clock = FakeClock(0L)
        val controller = BiscuitController(scene(), clock = clock)
        assertTrue(controller.visible)
        controller.play(BiscuitClip.HIDE)
        assertFalse(controller.visible)
        controller.play(BiscuitClip.APPEAR)
        assertTrue(controller.visible)
        assertEquals(BiscuitClip.APPEAR, controller.lastAnimation)
    }
}
