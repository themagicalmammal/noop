package com.noop.analytics

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

/** A vector object from Aura's bundled Biscuit artboard (parity twin of [VectorObject]). */
data class BiscuitVectorObject(val type: String, var values: MutableMap<String, Double>)

/** One keyframe on a Biscuit animation track (parity twin of [AnimationKey]). */
data class BiscuitAnimationKey(
    val frame: Double,
    val value: Double,
    val interpolation: Int,
    val interpolator: Int,
)

/** One animated property of one object (parity twin of [AnimationTrack]). */
data class BiscuitAnimationTrack(
    val objectIndex: Int,
    val property: String,
    val color: Boolean,
    val keys: List<BiscuitAnimationKey>,
)

/**
 * A Biscuit animation clip (parity twin of [PersonalityAnimation]).
 *
 * Frame math, sampling, easing, and color lerp must stay bit-identical to the Aura reference
 * implementation; the expected literals in [com.noop.analytics.BiscuitTimelineTest] are the
 * standalone Swift oracle output.
 */
data class BiscuitAnimation(
    val name: String,
    val fps: Double,
    val duration: Double,
    val speed: Double,
    val loop: Int,
    val start: Double,
    val end: Double,
    val quantize: Boolean,
    val tracks: List<BiscuitAnimationTrack>,
) {
    val seconds: Double
        get() = (end - start) / fps / max(speed, 0.0001)

    fun frameAt(seconds: Double, looping: Boolean): Double {
        val span = end - start
        if (span <= 0) return start
        var offset = max(0.0, seconds) * fps * speed
        if (looping && loop != 0) {
            if (loop == 2) {
                offset = offset % (span * 2)
                if (offset > span) offset = span * 2 - offset
            } else {
                offset %= span
            }
        } else {
            offset = min(span, offset)
        }
        val result = start + offset
        return if (quantize) floor(result) else result
    }
}

/** Pure port of Aura's PersonalityTimeline (frame sampling, easing, color lerp). */
object BiscuitTimeline {
    fun apply(
        animation: BiscuitAnimation?,
        seconds: Double,
        looping: Boolean = false,
        mix: Double = 1.0,
        objects: MutableList<BiscuitVectorObject>,
    ) {
        if (animation == null || mix <= 0) return
        val frame = animation.frameAt(seconds, looping)
        for (track in animation.tracks) {
            if (track.objectIndex !in objects.indices) continue
            val value = sample(track, frame, objects) ?: continue
            val old = objects[track.objectIndex].values[track.property] ?: 0.0
            objects[track.objectIndex].values[track.property] =
                if (track.color) colorLerp(old, value, mix) else old + (value - old) * mix
        }
    }

    fun sample(track: BiscuitAnimationTrack, frame: Double, objects: List<BiscuitVectorObject>): Double? {
        val first = track.keys.firstOrNull() ?: return null
        val last = track.keys.last()
        if (frame <= first.frame) return first.value
        if (frame >= last.frame) return last.value
        val nextIndex = track.keys.indexOfFirst { it.frame >= frame }
        if (nextIndex <= 0) return last.value
        val from = track.keys[nextIndex - 1]
        val to = track.keys[nextIndex]
        if (frame == to.frame) return to.value
        if (from.interpolation == 0) return from.value
        var t = (frame - from.frame) / (to.frame - from.frame)
        if (from.interpolator in objects.indices) {
            t = ease(t, objects[from.interpolator])
        }
        return if (track.color) colorLerp(from.value, to.value, t) else from.value + (to.value - from.value) * t
    }

    fun colorLerp(from: Double, to: Double, t: Double): Double {
        val a = from.toLong().coerceIn(0L, 0xFFFFFFFFL)
        val b = to.toLong().coerceIn(0L, 0xFFFFFFFFL)
        var result = 0L
        for (shift in listOf(0, 8, 16, 24)) {
            val x = ((a shr shift) and 255L).toDouble()
            val y = ((b shr shift) and 255L).toDouble()
            val channel = roundTowardZero(x + (y - x) * t).coerceIn(0L, 255L)
            result = result or (channel shl shift)
        }
        return result.toDouble()
    }

    fun ease(t: Double, target: BiscuitVectorObject): Double {
        val v = target.values
        if (target.type == "CubicEaseInterpolator") {
            val x1 = v["x1"] ?: 0.42
            val x2 = v["x2"] ?: 0.58
            val y1 = v["y1"] ?: 0.0
            val y2 = v["y2"] ?: 1.0
            fun bezier(u: Double, a: Double, b: Double): Double {
                return 3 * (1 - u) * (1 - u) * u * a + 3 * (1 - u) * u * u * b + u * u * u
            }
            var lo = 0.0
            var hi = 1.0
            repeat(24) {
                val mid = (lo + hi) / 2
                if (bezier(mid, x1, x2) < t) lo = mid else hi = mid
            }
            return bezier((lo + hi) / 2, y1, y2)
        }
        if (target.type == "ElasticInterpolator") {
            val amplitude = v["amplitude"] ?: 1.0
            val period = max(0.00001, if (v["period"] == 0.0) 0.5 else (v["period"] ?: 1.0))
            val s = if (amplitude < 1) period / 4 else period / (2 * PI) * asin(1 / amplitude)
            fun actualAmplitude(time: Double): Double {
                if (amplitude < 1 && abs(time) < abs(s) && abs(s) > 0) {
                    val fraction = abs(time) / abs(s)
                    return amplitude * fraction + 1 - fraction
                }
                return amplitude
            }
            return when ((v["easingValue"] ?: 1.0).toInt()) {
                0 -> {
                    val time = t - 1
                    -actualAmplitude(time) * 2.0.pow(10 * time) * sin((-time - s) * 2 * PI / period)
                }
                2 -> {
                    val time = 2 * t - 1
                    if (time < 0) {
                        -0.5 * actualAmplitude(time) * 2.0.pow(10 * time) * sin((-time - s) * 2 * PI / period)
                    } else {
                        0.5 * actualAmplitude(time) * 2.0.pow(-10 * time) * sin((time - s) * 2 * PI / period) + 1
                    }
                }
                else -> actualAmplitude(t) * 2.0.pow(-10 * t) * sin((t - s) * 2 * PI / period) + 1
            }
        }
        return t
    }

    /** Swift's `rounded(.towardZero)`: truncation toward zero. */
    private fun roundTowardZero(x: Double): Long {
        val whole = floor(abs(x)).toLong()
        return if (x < 0) -whole else whole
    }
}
