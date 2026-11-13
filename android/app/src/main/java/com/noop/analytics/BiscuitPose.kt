package com.noop.analytics

import kotlin.math.min

/** A Biscuit outfit (parity twin of [BiscuitOutfit]). */
enum class BiscuitOutfit {
    COACH, GLASSES, NIGHTCAP, CROWN;

    val rawValue: String
        get() = name.lowercase()

    val title: String
        get() = rawValue.replaceFirstChar { it.uppercase() }
}

/**
 * A named Biscuit animation request (parity twin of [BiscuitAnimation]).
 *
 * The raw value is the clip name in the bundled artboard; [BiscuitClip.IDLE] is
 * `aura_attention_idle`, not `aura_idle`.
 */
enum class BiscuitClip(val clip: String) {
    APPEAR("aura_arrive"),
    IDLE("aura_attention_idle"),
    BLINK("aura_lid_pulse"),
    LOOK_AROUND("aura_gaze_drift"),
    TAP("aura_delight"),
    THINK("aura_consider"),
    BOUNCE("aura_spring"),
    SCROLL_UP("aura_scroll_rise"),
    SCROLL_DOWN("aura_scroll_fall"),
    HIDE("aura_depart"),
    WAVE("aura_sway"),
    LOOK_LEFT("aura_gaze_west"),
    LOOK_RIGHT("aura_gaze_east"),
    LOOK_UP("aura_gaze_north"),
    LOOK_DOWN("aura_gaze_south"),
    DOUBLE_BLINK("aura_lid_double"),
    NOD("aura_acknowledge"),
    SHAKE("aura_decline"),
    STRETCH("aura_stretch"),
    CELEBRATE("aura_celebrate"),
    WINK("aura_wink"),
    STARTLE("aura_startle"),
    SIGH("aura_sigh"),
    EYE_ROLL("aura_eye_roll"),
    SHRUG("aura_shrug"),
    YAWN("aura_yawn");

    val title: String
        get() = when (this) {
            APPEAR -> "Appear"
            IDLE -> "Idle float"
            BLINK -> "Blink"
            LOOK_AROUND -> "Look around"
            TAP -> "Tap / giggle"
            THINK -> "Think"
            BOUNCE -> "Bounce"
            SCROLL_UP -> "Scroll up"
            SCROLL_DOWN -> "Scroll down"
            HIDE -> "Hide"
            WAVE -> "Wave"
            LOOK_LEFT -> "Look left"
            LOOK_RIGHT -> "Look right"
            LOOK_UP -> "Look up"
            LOOK_DOWN -> "Look down"
            DOUBLE_BLINK -> "Double blink"
            NOD -> "Nod"
            SHAKE -> "Shake"
            STRETCH -> "Stretch"
            CELEBRATE -> "Celebrate"
            WINK -> "Wink"
            STARTLE -> "Startle"
            SIGH -> "Sigh"
            EYE_ROLL -> "Eye roll"
            SHRUG -> "Shrug"
            YAWN -> "Yawn"
        }
}

/** Camera framing (parity twin of [BiscuitFraming]). */
enum class BiscuitFraming(
    val zoom: Double,
    val eyeScale: Double,
    val eyeDrop: Double,
) {
    BODY(1.0, 1.0, 0.0),
    HEAD(1.28, 1.38, 7.0),
}

/**
 * A Biscuit body color (parity twin of [BiscuitColor]).
 *
 * [argb] packs channels with Swift's default `.rounded()` (ties away from zero).
 */
data class BiscuitColor(val red: Double, val green: Double, val blue: Double) {
    constructor(hex: Long) : this(
        ((hex ushr 16) and 255L).toDouble() / 255.0,
        ((hex ushr 8) and 255L).toDouble() / 255.0,
        (hex and 255L).toDouble() / 255.0,
    )

    val argb: Double
        get() {
            fun channel(x: Double): Long =
                (if (x.isFinite()) min(1.0, max(0.0, x)) else 0.0).let { kotlin.math.round(it * 255).toLong() }
            return (0xFF000000L or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue))
                .toDouble()
        }

    companion object {
        private fun max(a: Double, b: Double) = if (a > b) a else b

        val goodRecovery = BiscuitColor(0x4DB872L)
        val resting = BiscuitColor(0x26A68FL)
        val lowRecovery = BiscuitColor(0xE39A3BL)
        val toasted = BiscuitColor(0xC98945L)
    }
}

/**
 * Name-to-object bindings for the bundled Biscuit artboard
 * (parity twin of [BiscuitBindings]).
 */
data class BiscuitBindings(
    val nameToObject: Map<String, Map<String, Int>>,
    val bodyColorTargets: List<Int>,
)

/**
 * Parsed Biscuit artboard: the vector objects plus its animation clips
 * (parity twin of [PersonalityArtboard] as consumed by the Biscuit scene).
 */
data class BiscuitArtboard(
    val width: Double,
    val height: Double,
    val objects: List<BiscuitVectorObject>,
    val animations: List<BiscuitAnimation>,
) {
    /** Mirrors Swift `PersonalityArtboard.animation(named:)`: the last match that has tracks. */
    fun animation(named: String): BiscuitAnimation? =
        animations.lastOrNull { it.name == named && it.tracks.isNotEmpty() }
}

/**
 * The Biscuit scene: artboard plus bindings, with the pure pose stack helpers
 * (parity twin of [BiscuitScene]).
 */
class BiscuitScene(
    val artboard: BiscuitArtboard,
    val bindings: BiscuitBindings?,
) {
    fun apply(
        name: String,
        seconds: Double = 0.0,
        loop: Boolean = false,
        mix: Double = 1.0,
        to: MutableList<BiscuitVectorObject>,
    ) {
        BiscuitTimeline.apply(artboard.animation(name), seconds, loop, mix, to)
    }

    fun applyVisibility(visible: Boolean, age: Double, objects: MutableList<BiscuitVectorObject>) {
        val clip = artboard.animation("aura_arrive")
        if (visible && clip != null && age >= clip.seconds) {
            apply("aura_visible", to = objects)
        } else {
            apply(if (visible) "aura_arrive" else "aura_depart", seconds = age, to = objects)
        }
    }

    fun still(
        mood: BiscuitMood,
        outfits: Set<BiscuitOutfit> = emptySet(),
        color: BiscuitColor = BiscuitColor.resting,
        framing: BiscuitFraming = BiscuitFraming.BODY,
        isGhost: Boolean = false,
        cloudPhase: Double = 0.0,
    ): List<BiscuitVectorObject> {
        val objects = copy(artboard.objects).toMutableList()
        apply("aura_eye_base", to = objects)
        apply("aura_visible", to = objects)
        apply("aura_mood_" + mood.rawValue, to = objects)
        apply(if (isGhost) "aura_veil_on" else "aura_veil_off", seconds = Double.MAX_VALUE, to = objects)
        for (outfit in BiscuitOutfit.entries) {
            apply("aura_outfit_" + outfit.rawValue + (if (outfit in outfits) "_on" else "_off"), to = objects)
        }
        if (cloudPhase != 0.0) apply("aura_cloud_drift", seconds = cloudPhase, loop = true, to = objects)
        tint(color.argb, objects)
        frame(framing, objects)
        return objects
    }

    fun frame(framing: BiscuitFraming, objects: MutableList<BiscuitVectorObject>) {
        if (framing == BiscuitFraming.BODY) return
        val id = bindings?.nameToObject?.get("Noop Aura")?.get("Aura eyes") ?: return
        if (id !in objects.indices) return
        objects[id].values["scaleX"] = (objects[id].values["scaleX"] ?: 1.0) * framing.eyeScale
        objects[id].values["scaleY"] = (objects[id].values["scaleY"] ?: 1.0) * framing.eyeScale
        objects[id].values["y"] = (objects[id].values["y"] ?: 0.0) + framing.eyeDrop
    }

    fun tint(argb: Double, objects: MutableList<BiscuitVectorObject>) {
        for (id in bindings?.bodyColorTargets ?: emptyList()) {
            if (id in objects.indices) objects[id].values["colorValue"] = argb
        }
    }

    private fun copy(objects: List<BiscuitVectorObject>): List<BiscuitVectorObject> =
        objects.map { BiscuitVectorObject(it.type, it.values.toMutableMap()) }
}
