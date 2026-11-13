package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Parity oracle for [BiscuitTimeline]. The expected literals are the verbatim stdout of a
 * standalone Swift build of Aura's `PersonalityAnimation` / `PersonalityTimeline`
 * (`swiftc -O twin.swift main.swift -o oracle`), which prints each case with `%.17g`
 * (full double round-trip precision).
 */
class BiscuitTimelineTest {
    private fun animation(
        fps: Double = 30.0,
        speed: Double = 1.0,
        loop: Int = 0,
        start: Double = 0.0,
        end: Double = 30.0,
        quantize: Boolean = false,
    ) = BiscuitAnimation("T", fps, 0.0, speed, loop, start, end, quantize, emptyList())

    private val objects = listOf(
        BiscuitVectorObject("rect", mutableMapOf("x" to 0.0)),
        BiscuitVectorObject("rect", mutableMapOf("fill" to 0.0)),
        BiscuitVectorObject("CubicEaseInterpolator", mutableMapOf("x1" to 0.42, "y1" to 0.0, "x2" to 0.58, "y2" to 1.0)),
        BiscuitVectorObject("ElasticInterpolator", mutableMapOf("amplitude" to 1.0, "period" to 0.5)),
        BiscuitVectorObject("ElasticInterpolator", mutableMapOf("amplitude" to 0.5, "period" to 0.5)),
        BiscuitVectorObject("ElasticInterpolator", mutableMapOf("amplitude" to 2.0, "period" to 0.25, "easingValue" to 0.0)),
        BiscuitVectorObject("ElasticInterpolator", mutableMapOf("amplitude" to 2.0, "period" to 0.25, "easingValue" to 2.0)),
        BiscuitVectorObject("ElasticInterpolator", mutableMapOf()),
    )

    private fun track(property: String, color: Boolean = false, objectIndex: Int = 0, keys: List<BiscuitAnimationKey> = emptyList()) =
        BiscuitAnimationTrack(objectIndex, property, color, keys)

    private fun sampled(track: BiscuitAnimationTrack, frame: Double): Double =
        BiscuitTimeline.sample(track, frame, objects)!!

    private fun key(frame: Double, value: Double, interpolation: Int = 1, interpolator: Int = 0) =
        BiscuitAnimationKey(frame, value, interpolation, interpolator)

    @Test
    fun frameMathMatchesSwiftOracle() {
        val fA = animation()
        assertEquals(0.0, fA.frameAt(-1.0, false), 0.0)
        assertEquals(0.0, fA.frameAt(0.0, false), 0.0)
        assertEquals(3.0, fA.frameAt(0.1, false), 0.0)
        assertEquals(15.0, fA.frameAt(0.5, false), 0.0)
        assertEquals(10.0, fA.frameAt(1.0 / 3.0, false), 0.0)
        assertEquals(30.0, fA.frameAt(1.0, false), 0.0)
        assertEquals(30.0, fA.frameAt(1.0001, false), 0.0)
        assertEquals(30.0, fA.frameAt(2.5, false), 0.0)
        assertEquals(0.0, fA.frameAt(-0.3, true), 0.0)
        assertEquals(15.0, fA.frameAt(0.5, true), 0.0)
        assertEquals(30.0, fA.frameAt(1.0, true), 0.0)
        assertEquals(30.0, fA.frameAt(1.2, true), 0.0)
        assertEquals(30.0, fA.frameAt(3.9, true), 0.0)

        val fB = animation(quantize = true)
        assertEquals(0.0, fB.frameAt(0.0, false), 0.0)
        assertEquals(15.0, fB.frameAt(0.5, false), 0.0)
        assertEquals(16.0, fB.frameAt(0.55, false), 0.0)
        assertEquals(10.0, fB.frameAt(1.0 / 3.0, false), 0.0)
        assertEquals(30.0, fB.frameAt(1.0, false), 0.0)

        val fC = animation(loop = 2)
        assertEquals(0.0, fC.frameAt(0.0, true), 0.0)
        assertEquals(15.0, fC.frameAt(0.5, true), 0.0)
        assertEquals(30.0, fC.frameAt(1.0, true), 0.0)
        assertEquals(15.0, fC.frameAt(1.5, true), 0.0)
        assertEquals(0.0, fC.frameAt(2.0, true), 0.0)
        assertEquals(15.0, fC.frameAt(2.5, true), 0.0)
        assertEquals(30.0, fC.frameAt(3.0, true), 0.0)
        assertEquals(3.0, fC.frameAt(29.9, true), 0.0)

        val fD = animation(loop = 1)
        assertEquals(0.0, fD.frameAt(30.0, true), 0.0)
        assertEquals(0.99999900000000252, fD.frameAt(30.0333333, true), 0.0)

        assertEquals(15.0, animation().frameAt(0.5, true), 0.0)
        assertEquals(30.0, animation().frameAt(5.0, true), 0.0)

        assertEquals(7.0, animation(start = 7.0, end = 7.0).frameAt(12.3, true), 0.0)

        val fG = animation(speed = 2.0)
        assertEquals(24.0, fG.frameAt(0.4, false), 0.0)
        assertEquals(30.0, fG.frameAt(1.0, false), 0.0)
        assertEquals(30.0, fG.frameAt(15.0, false), 0.0)
        assertEquals(30.0, fG.frameAt(1.2, true), 0.0)

        val fH = animation(speed = 0.5)
        assertEquals(7.5, fH.frameAt(0.5, false), 0.0)
        assertEquals(30.0, fH.frameAt(2.0, false), 0.0)

        val fI = animation(fps = 10.0, loop = 1, start = 10.0, end = 20.0)
        assertEquals(10.0, fI.frameAt(0.0, false), 0.0)
        assertEquals(17.0, fI.frameAt(0.7, false), 0.0)
        assertEquals(18.5, fI.frameAt(0.85, false), 0.0)
        assertEquals(20.0, fI.frameAt(100.0, false), 0.0)
        assertEquals(13.0, fI.frameAt(1.3, true), 0.0)

        val fJ = animation(fps = 25.0, end = 100.0)
        assertEquals(15.0, fJ.frameAt(0.6, false), 0.0)
        assertEquals(30.862499999999997, fJ.frameAt(1.2345, false), 0.0)

        assertEquals(1.0, fA.seconds, 0.0)
        assertEquals(0.5, fG.seconds, 0.0)
    }

    @Test
    fun sampleLinearAndHoldMatchSwiftOracle() {
        val tLin = track("x", keys = listOf(key(0.0, 10.0), key(10.0, 20.0)))
        assertEquals(10.0, sampled(tLin, -3.0), 0.0)
        assertEquals(10.0, sampled(tLin, 0.0), 0.0)
        assertEquals(12.5, sampled(tLin, 2.5), 0.0)
        assertEquals(15.0, sampled(tLin, 5.0), 0.0)
        assertEquals(19.999000000000002, sampled(tLin, 9.999), 0.0)
        assertEquals(20.0, sampled(tLin, 10.0), 0.0)
        assertEquals(20.0, sampled(tLin, 12.0), 0.0)

        val tHold = track("x", keys = listOf(key(0.0, 10.0, 0), key(10.0, 20.0, 0)))
        assertEquals(10.0, sampled(tHold, 0.0), 0.0)
        assertEquals(10.0, sampled(tHold, 5.0), 0.0)
        assertEquals(10.0, sampled(tHold, 9.999), 0.0)
        assertEquals(20.0, sampled(tHold, 10.0), 0.0)

        val tExact = track("x", keys = listOf(key(0.0, 5.0), key(10.0, 15.0), key(20.0, 25.0)))
        assertEquals(15.0, sampled(tExact, 10.0), 0.0)
        assertEquals(15.000000099999999, sampled(tExact, 10.0000001), 0.0)

        assertNull(BiscuitTimeline.sample(track("x"), 0.0, objects))

        val tOne = track("x", keys = listOf(key(3.0, 42.0)))
        assertEquals(42.0, sampled(tOne, 5.0), 0.0)
        assertEquals(42.0, sampled(tOne, 5.0), 0.0)

        val tOut = track("x", keys = listOf(key(0.0, 0.0, 1, 99), key(1.0, 10.0, 1, 99)))
        assertEquals(5.0, sampled(tOut, 0.5), 0.0)
    }

    @Test
    fun sampleWithInterpolatorsMatchesSwiftOracle() {
        val tCubic = track("x", keys = listOf(key(0.0, 0.0, 1, 2), key(1.0, 1.0, 1, 2)))
        assertEquals(0.12916191173107416, sampled(tCubic, 0.25), 0.0)
        assertEquals(0.49999995529651642, sampled(tCubic, 0.5), 0.0)
        assertEquals(0.87083808826892584, sampled(tCubic, 0.75), 0.0)

        val tColor = track("fill", color = true, objectIndex = 1, keys = listOf(key(0.0, 0xFF224466.toDouble(), 1, 0), key(1.0, 0x11335577.toDouble(), 1, 0)))
        assertEquals(3274066026.0, sampled(tColor, 0.25), 0.0)
        assertEquals(2284473454.0, sampled(tColor, 0.5), 0.0)
        assertEquals(1278103666.0, sampled(tColor, 0.75), 0.0)

        val tElastic = track("x", keys = listOf(key(0.0, 0.0, 1, 3), key(1.0, 10.0, 1, 3)))
        assertEquals(4.2793859718231566, sampled(tElastic, 0.05), 0.0)
        assertEquals(11.767766952966369, sampled(tElastic, 0.25), 0.0)
        assertEquals(9.6875, sampled(tElastic, 0.5), 0.0)
        assertEquals(10.055242717280199, sampled(tElastic, 0.75), 0.0)
        assertEquals(9.988826925726217, sampled(tElastic, 0.95), 0.0)

        val tElasticLow = track("x", keys = listOf(key(0.0, 0.0, 1, 4), key(1.0, 10.0, 1, 4)))
        assertEquals(10.883883476483184, sampled(tElasticLow, 0.25), 0.0)
        assertEquals(9.84375, sampled(tElasticLow, 0.5), 0.0)
        assertEquals(10.027621358640101, sampled(tElasticLow, 0.75), 0.0)

        val tElastic0 = track("x", keys = listOf(key(0.0, 0.0, 1, 5), key(1.0, 10.0, 1, 5)))
        assertEquals(0.055242717280199041, sampled(tElastic0, 0.25), 0.0)
        assertEquals(1.7677669529663707, sampled(tElastic0, 0.75), 0.0)

        val tElastic2 = track("x", keys = listOf(key(0.0, 0.0, 1, 6), key(1.0, 10.0, 1, 6)))
        assertEquals(0.15624999999999997, sampled(tElastic2, 0.25), 0.0)
        assertEquals(9.84375, sampled(tElastic2, 0.75), 0.0)

        val tElasticDef = track("x", keys = listOf(key(0.0, 0.0, 1, 7), key(1.0, 10.0, 1, 7)))
        assertEquals(10.3125, sampled(tElasticDef, 0.5), 0.0)
    }

    @Test
    fun colorLerpMatchesSwiftOracle() {
        val from = 0x11223344.toDouble()
        val to = 0x55667788.toDouble()
        assertEquals(287454020.0, BiscuitTimeline.colorLerp(from, to, 0.0), 0.0)
        assertEquals(573785173.0, BiscuitTimeline.colorLerp(from, to, 0.25), 0.0)
        assertEquals(860116326.0, BiscuitTimeline.colorLerp(from, to, 0.5), 0.0)
        assertEquals(1146447479.0, BiscuitTimeline.colorLerp(from, to, 0.75), 0.0)
        assertEquals(1432778632.0, BiscuitTimeline.colorLerp(from, to, 1.0), 0.0)
        assertEquals(4386.0, BiscuitTimeline.colorLerp(from, to, -0.5), 0.0)
        assertEquals(2005440938.0, BiscuitTimeline.colorLerp(from, to, 1.5), 0.0)

        assertEquals(0.0, BiscuitTimeline.colorLerp(0.0, 4294967295.0, 0.0), 0.0)
        assertEquals(2139062143.0, BiscuitTimeline.colorLerp(0.0, 4294967295.0, 0.5), 0.0)
        assertEquals(4294967295.0, BiscuitTimeline.colorLerp(0.0, 4294967295.0, 1.0), 0.0)
        assertEquals(2139062143.0, BiscuitTimeline.colorLerp(-5.0, 4294967296.0, 0.5), 0.0)

        assertEquals(0.0, BiscuitTimeline.colorLerp(0.0, 1.0, 0.5), 0.0)
        assertEquals(1.0, BiscuitTimeline.colorLerp(1.0, 2.0, 0.5), 0.0)

        // Fractional products pin the truncation semantics (Swift `rounded(.towardZero)`).
        assertEquals(28.0, BiscuitTimeline.colorLerp(0.0, 115.0, 0.25), 0.0)
        assertEquals(28.0, BiscuitTimeline.colorLerp(0.0, 100.0, 0.29), 0.0)
        assertEquals(126.0, BiscuitTimeline.colorLerp(0.0, 253.0, 0.5), 0.0)
        assertEquals(252.0, BiscuitTimeline.colorLerp(0.0, 255.0, 0.99), 0.0)
        assertEquals(0.0, BiscuitTimeline.colorLerp(0.0, 1.0, 0.6), 0.0)
    }

    @Test
    fun easeMatchesSwiftOracle() {
        val cubic = objects[2]
        assertEquals(0.081659843848035887, BiscuitTimeline.ease(0.2, cubic), 0.0)
        assertEquals(0.49999995529651642, BiscuitTimeline.ease(0.5, cubic), 0.0)
        assertEquals(0.91834015615196407, BiscuitTimeline.ease(0.8, cubic), 0.0)
        assertEquals(1.8904176651010857e-06, BiscuitTimeline.ease(0.001, cubic), 0.0)
        assertEquals(0.99999810958233493, BiscuitTimeline.ease(0.999, cubic), 0.0)

        val elastic = objects[3]
        assertEquals(1.176776695296637, BiscuitTimeline.ease(0.25, elastic), 0.0)
        assertEquals(0.96875, BiscuitTimeline.ease(0.5, elastic), 0.0)
        assertEquals(1.0055242717280199, BiscuitTimeline.ease(0.75, elastic), 0.0)

        val elasticSmall = BiscuitVectorObject("ElasticInterpolator", mutableMapOf("amplitude" to 0.3, "period" to 0.4))
        assertEquals(1.0375000000000001, BiscuitTimeline.ease(0.25, elasticSmall), 0.0)
        assertEquals(1.0, BiscuitTimeline.ease(0.5, elasticSmall), 0.0)
        assertEquals(0.99882812499999996, BiscuitTimeline.ease(0.75, elasticSmall), 0.0)

        val plain = BiscuitVectorObject("rect", mutableMapOf())
        assertEquals(0.3, BiscuitTimeline.ease(0.3, plain), 0.0)
        assertEquals(0.7, BiscuitTimeline.ease(0.7, plain), 0.0)
    }

    @Test
    fun applyMatchesSwiftOracle() {
        val applyObjects = {
            mutableListOf(
                BiscuitVectorObject("rect", mutableMapOf("x" to 0.0)),
                BiscuitVectorObject("rect", mutableMapOf("fill" to 0xFF000000.toDouble())),
                BiscuitVectorObject("CubicEaseInterpolator", mutableMapOf("x1" to 0.42, "y1" to 0.0, "x2" to 0.58, "y2" to 1.0)),
            )
        }
        val applyAnimation = BiscuitAnimation(
            "t", 10.0, 0.0, 1.0, 1, 0.0, 2.0, false,
            listOf(
                BiscuitAnimationTrack(0, "x", false, listOf(key(0.0, 10.0), key(10.0, 100.0))),
                BiscuitAnimationTrack(1, "fill", true, listOf(key(0.0, 0xFF000000.toDouble()), key(10.0, 0x00FF0000.toDouble()))),
                BiscuitAnimationTrack(5, "x", false, listOf(key(0.0, 1.0), key(10.0, 2.0))),
                BiscuitAnimationTrack(0, "y", false, emptyList()),
            ),
        )

        val full = applyObjects()
        BiscuitTimeline.apply(applyAnimation, 0.5, mix = 1.0, objects = full)
        assertEquals(28.0, full[0].values["x"]!!, 0.0)
        assertEquals(3425894400.0, full[1].values["fill"]!!, 0.0)

        val half = applyObjects()
        BiscuitTimeline.apply(applyAnimation, 0.5, mix = 0.5, objects = half)
        assertEquals(14.0, half[0].values["x"]!!, 0.0)
        assertEquals(3843620864.0, half[1].values["fill"]!!, 0.0)

        val none = applyObjects()
        BiscuitTimeline.apply(null, 0.5, objects = none)
        BiscuitTimeline.apply(applyAnimation, 0.5, mix = 0.0, objects = none)
        assertEquals(0.0, none[0].values["x"]!!, 0.0)
        assertEquals(4278190080.0, none[1].values["fill"]!!, 0.0)
    }
}
