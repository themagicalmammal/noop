package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Parity pin for the Biscuit pose stack: [BiscuitColor] packing, artboard clip lookup,
 * [BiscuitScene] pose stacking / visibility / framing, and the raw enum values that must
 * match the Aura reference strings.
 */
class BiscuitPoseTest {

    private fun clip(
        name: String,
        objectIndex: Int,
        property: String,
        first: Double,
        last: Double,
        interpolation: Int = 1,
        start: Double = 0.0,
        end: Double = 30.0,
        fps: Double = 30.0,
        loop: Int = 0,
        color: Boolean = false,
    ) = BiscuitAnimation(
        name = name,
        fps = fps,
        duration = (end - start) / fps,
        speed = 1.0,
        loop = loop,
        start = start,
        end = end,
        quantize = false,
        tracks = listOf(
            BiscuitAnimationTrack(
                objectIndex = objectIndex,
                property = property,
                color = color,
                keys = listOf(
                    BiscuitAnimationKey(frame = start, value = first, interpolation = interpolation, interpolator = -1),
                    BiscuitAnimationKey(frame = end, value = last, interpolation = interpolation, interpolator = -1),
                ),
            ),
        ),
    )

    private fun BiscuitScene.objects(): MutableList<BiscuitVectorObject> =
        artboard.objects.map { BiscuitVectorObject(it.type, it.values.toMutableMap()) }.toMutableList()

    private fun BiscuitVectorObject.valueOf(property: String): Double = values[property] ?: Double.NaN

    private fun stillScene(): BiscuitScene = BiscuitScene(
        BiscuitArtboard(
            100.0,
            100.0,
            listOf(BiscuitVectorObject("Body", mutableMapOf("x" to 0.0, "alpha" to 1.0, "hat" to 0.0, "cloud" to 0.0))),
            listOf(
                clip("aura_eye_base", 0, "x", 2.0, 2.0, interpolation = 0),
                clip("aura_visible", 0, "x", 1.0, 1.0, interpolation = 0),
                clip("aura_mood_neutral", 0, "x", 5.0, 5.0, interpolation = 0),
                clip("aura_veil_off", 0, "alpha", 1.0, 0.85),
                clip("aura_veil_on", 0, "alpha", 0.3, 0.3, interpolation = 0),
                clip("aura_outfit_coach_off", 0, "hat", 0.0, 0.0, interpolation = 0),
                clip("aura_outfit_glasses_off", 0, "hat", 0.0, 0.0, interpolation = 0),
                clip("aura_outfit_nightcap_off", 0, "hat", 0.0, 0.0, interpolation = 0),
                clip("aura_outfit_crown_off", 0, "hat", 0.0, 0.0, interpolation = 0),
                clip("aura_outfit_crown_on", 0, "hat", 3.0, 3.0, interpolation = 0),
                clip("aura_cloud_drift", 0, "cloud", 0.0, 100.0, loop = 1),
            ),
        ),
        BiscuitBindings(emptyMap(), listOf(0)),
    )

    @Test
    fun colorArgbMatchesSwiftRounding() {
        assertEquals(0xFF4DB872L, BiscuitColor(0x4DB872L).argb.toLong())
        assertEquals(0xFF808080L, BiscuitColor(0.5, 0.5, 0.5).argb.toLong())
        assertEquals(0xFF00FF00L, BiscuitColor(-1.0, 2.0, 0.0).argb.toLong())
        assertEquals(0xFF000000L, BiscuitColor(Double.NaN, Double.NaN, Double.NaN).argb.toLong())
        assertEquals(0xFF26A68FL, BiscuitColor.resting.argb.toLong())
        assertEquals(0xFF4DB872L, BiscuitColor.goodRecovery.argb.toLong())
        assertEquals(0xFFE39A3BL, BiscuitColor.lowRecovery.argb.toLong())
        assertEquals(0xFFC98945L, BiscuitColor.toasted.argb.toLong())
    }

    @Test
    fun animationNamedReturnsLastNonEmptyMatch() {
        val empty = BiscuitAnimation("c", 30.0, 1.0, 1.0, 0, 0.0, 30.0, false, emptyList())
        val first = clip("c", 0, "x", 1.0, 1.0, interpolation = 0)
        val artboard = BiscuitArtboard(100.0, 100.0, emptyList(), listOf(empty, first))
        assertEquals(first, artboard.animation("c"))
        assertNull(artboard.animation("missing"))

        val noTracks = BiscuitArtboard(100.0, 100.0, emptyList(), listOf(empty))
        assertNull(noTracks.animation("c"))
    }

    @Test
    fun stillPoseStackOrderWins() {
        val scene = stillScene()
        val objects = scene.still(BiscuitMood.NEUTRAL)
        assertEquals(5.0, objects[0].valueOf("x"), 0.0)
        assertEquals(0.85, objects[0].valueOf("alpha"), 0.0)
        assertEquals(0.0, objects[0].valueOf("hat"), 0.0)
        assertEquals(0xFF26A68FL.toDouble(), objects[0].valueOf("colorValue"), 0.0)
        assertEquals(0.0, scene.artboard.objects[0].valueOf("x"), 0.0)
    }

    @Test
    fun stillPoseAppliesOutfitGhostAndCloud() {
        val scene = stillScene()

        val crown = scene.still(BiscuitMood.NEUTRAL, outfits = setOf(BiscuitOutfit.CROWN))
        assertEquals(3.0, crown[0].valueOf("hat"), 0.0)

        val ghost = scene.still(BiscuitMood.NEUTRAL, isGhost = true)
        assertEquals(1.0 + (0.3 - 1.0) * 1.0, ghost[0].valueOf("alpha"), 0.0)

        val drifted = scene.still(BiscuitMood.NEUTRAL, cloudPhase = 0.5)
        assertEquals(50.0, drifted[0].valueOf("cloud"), 0.0)
    }

    @Test
    fun headFramingScalesEyesOncePerCall() {
        val scene = frameScene(eyesAt = 1)
        val objects = scene.objects()
        scene.frame(BiscuitFraming.HEAD, objects)
        assertEquals(1.38, objects[1].valueOf("scaleX"), 0.0)
        assertEquals(1.38, objects[1].valueOf("scaleY"), 0.0)
        assertEquals(7.0, objects[1].valueOf("y"), 0.0)
        scene.frame(BiscuitFraming.HEAD, objects)
        assertEquals(1.38 * 1.38, objects[1].valueOf("scaleX"), 0.0)
        assertEquals(14.0, objects[1].valueOf("y"), 0.0)
    }

    @Test
    fun bodyFramingIsANoOp() {
        val scene = frameScene(eyesAt = 1)
        val objects = scene.objects()
        scene.frame(BiscuitFraming.BODY, objects)
        assertEquals(1.0, objects[1].valueOf("scaleX"), 0.0)
        assertEquals(0.0, objects[1].valueOf("y"), 0.0)
    }

    @Test
    fun framingWithoutBindingsOrObjectIsANoOp() {
        val noBindings = frameScene(eyesAt = null)
        val objects = noBindings.objects()
        noBindings.frame(BiscuitFraming.HEAD, objects)
        assertEquals(1.0, objects[1].valueOf("scaleX"), 0.0)

        val outOfRange = frameScene(eyesAt = 5)
        val objects2 = outOfRange.objects()
        outOfRange.frame(BiscuitFraming.HEAD, objects2)
        assertEquals(1.0, objects2[1].valueOf("scaleX"), 0.0)
    }

    @Test
    fun visibilityArrivesDepartsAndParksVisible() {
        val scene = visibilityScene()
        val objects = scene.objects()

        scene.applyVisibility(visible = true, age = 0.5, objects = objects)
        assertEquals(5.0, objects[0].valueOf("y"), 0.0)

        scene.applyVisibility(visible = false, age = 0.5, objects = objects)
        assertEquals(0.0, objects[0].valueOf("y"), 0.0)

        scene.applyVisibility(visible = true, age = 1.0, objects = objects)
        assertEquals(42.0, objects[0].valueOf("y"), 0.0)
    }

    @Test
    fun applyNoOpsAreHonest() {
        val scene = visibilityScene()
        val objects = scene.objects()
        val before = objects[0].values.toMap()

        scene.apply("aura_missing", to = objects)
        assertEquals(before, objects[0].values)

        scene.apply("aura_arrive", mix = 0.0, to = objects)
        assertEquals(before, objects[0].values)

        scene.apply("aura_arrive", seconds = 100.0, to = objects)
        assertEquals(0.0, objects[0].valueOf("y"), 0.0)
    }

    @Test
    fun enumRawValuesMatchSwift() {
        assertEquals("aura_attention_idle", BiscuitClip.IDLE.clip)
        assertEquals("aura_arrive", BiscuitClip.APPEAR.clip)
        assertEquals("aura_depart", BiscuitClip.HIDE.clip)
        assertEquals("neutral", BiscuitMood.NEUTRAL.rawValue)
        assertEquals("crown", BiscuitOutfit.CROWN.rawValue)
        assertEquals(1.28, BiscuitFraming.HEAD.zoom, 0.0)
        assertEquals(1.38, BiscuitFraming.HEAD.eyeScale, 0.0)
        assertEquals(7.0, BiscuitFraming.HEAD.eyeDrop, 0.0)
    }

    private fun frameScene(eyesAt: Int?): BiscuitScene = BiscuitScene(
        BiscuitArtboard(
            100.0,
            100.0,
            listOf(
                BiscuitVectorObject("Body", mutableMapOf()),
                BiscuitVectorObject("Eyes", mutableMapOf("scaleX" to 1.0, "scaleY" to 1.0, "y" to 0.0)),
            ),
            emptyList(),
        ),
        if (eyesAt != null) {
            BiscuitBindings(mapOf("Noop Aura" to mapOf("Aura eyes" to eyesAt)), emptyList())
        } else {
            null
        },
    )

    private fun visibilityScene(): BiscuitScene = BiscuitScene(
        BiscuitArtboard(
            100.0,
            100.0,
            listOf(BiscuitVectorObject("Body", mutableMapOf("y" to 0.0))),
            listOf(
                clip("aura_arrive", 0, "y", 10.0, 0.0),
                clip("aura_depart", 0, "y", -1.0, 1.0),
                clip("aura_visible", 0, "y", 42.0, 42.0, interpolation = 0),
            ),
        ),
        null,
    )
}
