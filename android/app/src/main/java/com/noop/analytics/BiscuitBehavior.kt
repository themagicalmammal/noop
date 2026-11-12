package com.noop.analytics

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A Biscuit mood (parity twin of [BiscuitMood]). The raw value is the lowercase name, matching
 * the Swift `String` raw value.
 */
enum class BiscuitMood {
    HAPPY, PROUD, CALM, NEUTRAL, FOCUSED, CURIOUS, WORRIED, SLEEPY, SAD, ASLEEP, RADIANT, PLAYFUL,
    SURPRISED, LISTENING, DELIGHTED, ANGRY, OPINIONATED, SKEPTICAL, SCARED, CONFIDENT, ANNOYED,
    AFFECTIONATE, EMBARRASSED, OVERWHELMED;

    val rawValue: String
        get() = name.lowercase()
}

/** One behavior frame (parity twin of [BiscuitBehaviorScheduler.Sample]). */
data class BiscuitBehaviorSample(
    var eyeX: Double = 0.0,
    var eyeY: Double = 0.0,
    var lid: Double = 1.0,
    var bodyX: Double = 0.0,
    var bodyY: Double = 0.0,
    var tilt: Double = 0.0,
    var curiosity: Double = 0.0,
    var floatSeconds: Double = 0.0,
    var floatStrength: Double = 1.0,
    var isQuiet: Boolean = false,
    var blinkID: Long = 0,
    var gazeID: Long = 0,
)

/** A closed double range (parity twin of Swift's `ClosedRange<Double>`). */
private data class Span(val lower: Double, val upper: Double)

/** Per-mood behavior parameters (parity twin of [BiscuitBehaviorScheduler.Profile]). */
private class BiscuitBehaviorProfile {
    var blink = Span(3.2, 7.4)
    var gaze = Span(6.0, 12.8)
    var hold = Span(0.55, 1.25)
    var attention = Span(11.0, 24.0)
    var doubleChance = 0.12
    var gazeSize = 9.0
    var attitude = 0.7
    var close = 0.065
    var reopen = 0.19
    var downBias = 0.0
    var period = 6.4
    var amplitude = 0.9
    var breath = Span(14.0, 22.0)
    var sleeping = false

    companion object {
        fun forMood(mood: BiscuitMood): BiscuitBehaviorProfile {
            val p = BiscuitBehaviorProfile()
            when (mood) {
                BiscuitMood.CURIOUS, BiscuitMood.PLAYFUL, BiscuitMood.LISTENING, BiscuitMood.SURPRISED -> {
                    p.gaze = Span(3.5, 7.6); p.hold = Span(0.7, 1.5); p.gazeSize = 11.0
                    p.attitude = 1.0; p.doubleChance = 0.17; p.attention = Span(8.0, 17.0); p.period = 5.8
                }
                BiscuitMood.HAPPY, BiscuitMood.DELIGHTED, BiscuitMood.RADIANT, BiscuitMood.AFFECTIONATE -> {
                    p.blink = Span(3.1, 6.9); p.gaze = Span(5.8, 11.4); p.doubleChance = 0.16
                    p.attitude = 0.85; p.amplitude = 1.0; p.period = 6.0
                }
                BiscuitMood.SKEPTICAL, BiscuitMood.OPINIONATED, BiscuitMood.CONFIDENT, BiscuitMood.PROUD -> {
                    p.blink = Span(4.2, 8.8); p.gaze = Span(7.0, 14.0); p.hold = Span(1.2, 2.25)
                    p.attitude = 1.2; p.gazeSize = 8.5; p.doubleChance = 0.06; p.period = 7.0
                }
                BiscuitMood.ANGRY, BiscuitMood.ANNOYED, BiscuitMood.FOCUSED -> {
                    p.blink = Span(4.5, 9.2); p.gaze = Span(8.0, 15.0); p.hold = Span(1.3, 2.2)
                    p.gazeSize = 7.0; p.attitude = 0.8; p.amplitude = 0.65; p.period = 7.4
                }
                BiscuitMood.CALM -> {
                    p.blink = Span(6.0, 11.5); p.gaze = Span(15.0, 24.0); p.hold = Span(0.9, 1.6)
                    p.gazeSize = 5.0; p.attitude = 0.35; p.doubleChance = 0.04
                    p.attention = Span(20.0, 35.0); p.amplitude = 0.5; p.period = 8.3; p.breath = Span(14.0, 22.0)
                }
                BiscuitMood.SLEEPY -> {
                    p.blink = Span(7.0, 13.0); p.gaze = Span(19.0, 30.0); p.hold = Span(0.9, 1.7)
                    p.gazeSize = 3.5; p.attitude = 0.2; p.close = 0.13; p.reopen = 0.36
                    p.doubleChance = 0.03; p.downBias = 3.0; p.amplitude = 0.35
                    p.period = 9.0; p.attention = Span(30.0, 45.0); p.breath = Span(10.0, 16.0)
                }
                BiscuitMood.ASLEEP -> {
                    p.sleeping = true; p.amplitude = 0.22; p.period = 9.8; p.breath = Span(6.0, 10.0)
                }
                BiscuitMood.SAD, BiscuitMood.WORRIED, BiscuitMood.EMBARRASSED -> {
                    p.blink = Span(4.5, 9.0); p.gaze = Span(9.0, 17.0); p.hold = Span(0.9, 1.9)
                    p.downBias = 3.0; p.gazeSize = 6.0; p.attitude = 0.45; p.reopen = 0.25
                    p.amplitude = 0.55; p.period = 7.8; p.attention = Span(16.0, 29.0)
                }
                BiscuitMood.SCARED, BiscuitMood.OVERWHELMED -> {
                    p.blink = Span(2.8, 6.3); p.gaze = Span(4.2, 8.6); p.hold = Span(0.5, 1.1)
                    p.gazeSize = 8.0; p.doubleChance = 0.22; p.attitude = 0.55
                    p.amplitude = 0.55; p.period = 6.2
                }
                BiscuitMood.NEUTRAL -> {}
            }
            return p
        }
    }
}

/** A splitmix64 deterministic random source (parity twin of [BiscuitBehaviorScheduler.Random]). */
private class BiscuitBehaviorRandom(var state: Long) {
    fun unit(): Double {
        state += 0x9E3779B97F4A7C15uL.toLong()
        var z = state
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return ((z xor (z ushr 31)) ushr 11).toDouble() / 9007199254740992.0
    }

    fun value(r: Span): Double = r.lower + unit() * (r.upper - r.lower)
}

/** One scheduled blink (parity twin of [BiscuitBehaviorScheduler.Blink]). */
private class BiscuitBlink(
    val start: Double,
    val close: Double,
    val open: Double,
    val second: Double,
    val id: Long,
) {
    val duration: Double get() = second + close + 0.025 + open
}

/** One scheduled gaze (parity twin of [BiscuitBehaviorScheduler.Gaze]). */
private class BiscuitGaze(
    val start: Double,
    val hold: Double,
    val x: Double,
    val y: Double,
    val attitude: Double,
    val id: Long,
) {
    val duration: Double get() = 0.22 + hold + 0.46 + 0.14
}

/**
 * The deterministic blink / gaze / attention scheduler (parity twin of
 * [BiscuitBehaviorScheduler]). All randomness comes from [seed] through splitmix64, so two
 * instances with the same seed produce the same behavior on every platform.
 */
class BiscuitBehaviorScheduler(seed: Long) {
    var breathsOnly: Boolean = false

    private val blinkRandom = BiscuitBehaviorRandom(seed xor 0xA0761D6478BD642FuL.toLong())
    private val gazeRandom = BiscuitBehaviorRandom(seed xor 0xE7037ED1A0B428DBuL.toLong())
    private val attentionRandom = BiscuitBehaviorRandom(seed xor 0x8EBC6AF09C88C6E3uL.toLong())
    private val breathRandom = BiscuitBehaviorRandom(seed xor 0x589965CC75374CC3uL.toLong())
    private val phaseOffset: Double = (seed.toULong() % 10007uL).toDouble() / 10007.0 * 2.0 * PI

    private var nextBreath: Double? = null
    private var breathStart = Double.NEGATIVE_INFINITY
    private val breathLength = 2.8
    private var blink: BiscuitBlink? = null
    private var gaze: BiscuitGaze? = null
    private var nextBlink = 0.0
    private var nextGaze = 0.0
    private var nextAttention = 0.0
    private var attentionStart = Double.NEGATIVE_INFINITY
    private var attentionSign = 1.0
    private var blinkCount: Long = 0
    private var gazeCount: Long = 0
    private var profile = BiscuitBehaviorProfile()
    private var lastMood: BiscuitMood? = null
    private var lastTime = Double.NEGATIVE_INFINITY
    private var needsArming = true
    private var wasEnabled: Boolean? = null
    private var quietUntil = 0.0
    private var interruptionAt = Double.NEGATIVE_INFINITY
    private var interrupted = BiscuitBehaviorSample()
    private var lastSample = BiscuitBehaviorSample()
    private var floatAnchorTime = 0.0
    private var floatAnchorPhase = 0.0
    private var oldFrequency = 1.0 / 6.4
    private var frequency = 1.0 / 6.4
    private var oldAmplitude = 0.9
    private var amplitude = 0.9

    private val gazeSpacing: Double
        get() = if (breathsOnly) 1.8 else 1.0

    private fun smooth(t: Double): Double {
        val x = min(1.0, max(0.0, t))
        return x * x * (3.0 - 2.0 * x)
    }

    private fun floatPhase(t: Double): Double {
        val d = max(0.0, t - floatAnchorTime)
        val ramp = 1.2
        val u = min(d, ramp)
        val integral = oldFrequency * u +
            (frequency - oldFrequency) * (u * u * u / (ramp * ramp) - 0.5 * u * u * u * u / (ramp * ramp * ramp))
        val slowVariation = 0.045 * (sin(t / 13.0 + phaseOffset) - sin(floatAnchorTime / 13.0 + phaseOffset))
        return floatAnchorPhase + integral + max(0.0, d - ramp) * frequency + slowVariation
    }

    private fun floatAmplitude(t: Double): Double =
        oldAmplitude + (amplitude - oldAmplitude) * smooth((t - floatAnchorTime) / 1.2)

    fun noteInteraction(time: Double, end: Double) {
        if (!time.isFinite() || !end.isFinite()) return
        if (time >= quietUntil) {
            interrupted = lastSample
            interruptionAt = time
        }
        quietUntil = max(quietUntil, max(time, end))
        blink = null
        gaze = null
        attentionStart = Double.NEGATIVE_INFINITY
        needsArming = true
    }

    private fun arm(t: Double) {
        nextBlink = t + blinkRandom.value(profile.blink) * 0.7
        nextGaze = t + gazeRandom.value(profile.gaze) * gazeSpacing
        nextAttention = if (breathsOnly) Double.POSITIVE_INFINITY else t + attentionRandom.value(profile.attention)
        needsArming = false
    }

    fun sample(requestedTime: Double, mood: BiscuitMood, enabled: Boolean = true): BiscuitBehaviorSample {
        if (!requestedTime.isFinite()) return lastSample
        val t = max(0.0, max(requestedTime, if (lastTime.isFinite()) lastTime else 0.0))
        val gap = if (lastTime.isFinite()) t - lastTime else 0.0
        if (lastMood != mood) {
            val initial = lastMood == null
            val phase = floatPhase(t)
            val amp = floatAmplitude(t)
            val blend = smooth((t - floatAnchorTime) / 1.2)
            oldFrequency += (frequency - oldFrequency) * blend
            profile = BiscuitBehaviorProfile.forMood(mood)
            frequency = 1.0 / profile.period
            oldAmplitude = if (initial) profile.amplitude else amp
            amplitude = profile.amplitude
            if (initial) oldFrequency = frequency
            floatAnchorPhase = phase
            floatAnchorTime = t
            lastMood = mood
            noteInteraction(t, t + 0.6)
        }
        if (gap > 2.5 && !breathsOnly) {
            noteInteraction(t, t + 0.8)
        }
        if (wasEnabled == false && enabled) noteInteraction(t, t + 0.6)
        wasEnabled = enabled
        lastTime = t
        val out = BiscuitBehaviorSample()
        out.floatSeconds = floatPhase(t) * 6.1
        out.floatStrength = floatAmplitude(t) * (0.90 + 0.10 * sin(t / 19.0 + phaseOffset))
        if (breathsOnly) {
            if (nextBreath == null) nextBreath = t + breathRandom.value(Span(2.0, 5.0))
            var next = nextBreath
            while (next != null && t >= next) {
                breathStart = next
                next = next + breathLength + breathRandom.value(profile.breath)
                nextBreath = next
            }
            val age = t - breathStart
            val rise = if (age >= 0.0 && age < breathLength) sin(PI * age / breathLength) else 0.0
            out.floatStrength *= rise * rise
        }
        val quiet = !enabled || profile.sleeping || t < quietUntil
        if (quiet) {
            out.isQuiet = true
            val fade = if (enabled && !profile.sleeping) 1.0 - smooth((t - interruptionAt) / 0.14) else 0.0
            out.eyeX = interrupted.eyeX * fade
            out.eyeY = interrupted.eyeY * fade
            out.bodyX = interrupted.bodyX * fade
            out.bodyY = interrupted.bodyY * fade
            out.tilt = interrupted.tilt * fade
            out.curiosity = interrupted.curiosity * fade
            out.floatStrength *=
                if (t < quietUntil && !profile.sleeping) 0.42 + 0.58 * (1.0 - smooth((t - interruptionAt) / 0.28)) else 1.0
            blink = null
            gaze = null
            attentionStart = Double.NEGATIVE_INFINITY
            needsArming = true
            lastSample = out
            return out
        }
        if (needsArming) arm(quietUntil)

        while (t >= nextBlink) {
            blinkCount += 1
            val doubleBlink = blinkRandom.unit() < profile.doubleChance
            val b = BiscuitBlink(
                start = nextBlink,
                close = profile.close * blinkRandom.value(Span(0.88, 1.12)),
                open = profile.reopen * blinkRandom.value(Span(0.88, 1.15)),
                second = if (doubleBlink) blinkRandom.value(Span(0.31, 0.43)) else 0.0,
                id = blinkCount,
            )
            blink = b
            val quietMultiplier = if (blinkRandom.unit() < 0.12) 1.5 else 1.0
            nextBlink = b.start + b.duration + blinkRandom.value(profile.blink) * quietMultiplier
        }
        while (t >= nextGaze) {
            gazeCount += 1
            val direction = if (gazeRandom.unit() < 0.5) -1.0 else 1.0
            val x = direction * profile.gazeSize * gazeRandom.value(Span(0.65, 1.0))
            val y = profile.downBias + gazeRandom.value(Span(-3.0, 2.5))
            val g = BiscuitGaze(
                start = nextGaze,
                hold = gazeRandom.value(profile.hold),
                x = x,
                y = y,
                attitude = profile.attitude,
                id = gazeCount,
            )
            gaze = g
            val multiplier = if (gazeRandom.unit() < 0.18) 1.45 else 1.0
            nextGaze = g.start + g.duration + gazeRandom.value(profile.gaze) * multiplier * gazeSpacing
        }
        while (t >= nextAttention) {
            attentionStart = nextAttention
            attentionSign = if (attentionRandom.unit() < 0.5) -1.0 else 1.0
            nextAttention += 1.8 + attentionRandom.value(profile.attention)
        }
        val b = blink
        if (b != null && t <= b.start + b.duration) {
            fun pulse(age: Double): Double {
                if (age < 0.0 || age > b.close + 0.025 + b.open) return 0.0
                if (age < b.close) return smooth(age / b.close)
                if (age < b.close + 0.025) return 1.0
                return 1.0 - smooth((age - b.close - 0.025) / b.open)
            }
            val age = t - b.start
            out.lid = max(0.055, 1.0 - max(pulse(age), if (b.second > 0.0) pulse(age - b.second) else 0.0))
            out.blinkID = b.id
        }
        val g = gaze
        if (g != null && t <= g.start + g.duration) {
            val age = t - g.start
            val eye = smooth(age / 0.20) * (1.0 - smooth((age - 0.22 - g.hold) / 0.35))
            val follow = smooth((age - 0.11) / 0.25) * (1.0 - smooth((age - 0.22 - g.hold) / 0.46))
            out.eyeX = g.x * eye
            out.eyeY = g.y * eye
            out.bodyX = g.x * 0.14 * follow * g.attitude
            out.bodyY = -0.6 * follow * g.attitude
            out.tilt = g.x * 0.0042 * follow * g.attitude
            out.curiosity = 0.04 * follow * g.attitude
            out.gazeID = g.id
        }
        val a = t - attentionStart
        if (a >= 0.0 && a < 1.8) {
            val e = smooth(a / 0.38) * (1.0 - smooth((a - 0.95) / 0.7)) * if (out.gazeID == 0L) 1.0 else 0.25
            out.tilt += attentionSign * 0.020 * e * profile.attitude
            out.curiosity += attentionSign * 0.03 * e
        }
        out.floatStrength *= 0.42 + 0.58 * smooth((t - quietUntil) / 1.4)
        lastSample = out
        return out
    }

    fun isMoving(t: Double): Boolean =
        isMovingQuickly(t) || (breathsOnly && (nextBreath == null || t - breathStart < breathLength))

    fun isMovingQuickly(t: Double): Boolean {
        if (t < quietUntil + 0.3) return true
        if (needsArming && !profile.sleeping) return true
        val b = blink
        if (b != null && t <= b.start + b.duration) return true
        val g = gaze
        if (g != null && t <= g.start + g.duration) return true
        if (t - attentionStart >= 0.0 && t - attentionStart < 1.8) return true
        return false
    }

    fun nextEvent(t: Double): Double {
        var next = if (breathsOnly) (nextBreath ?: t) else Double.POSITIVE_INFINITY
        if (!profile.sleeping) next = minOf(next, nextBlink, nextGaze, nextAttention)
        return max(t, next)
    }
}
