package com.noop.analytics

import kotlin.math.abs
import kotlin.math.hypot

/** A one-shot reaction emitted by the Biscuit touch layer (parity twin of [BiscuitReaction]). */
enum class BiscuitReaction {
    PAT, GIGGLE, STARTLE, WINCE, WINK, BOUNCE, EYE_ROLL, ANGRY, IGNORE, SQUISH,
}

/** Which part of the biscuit body a normalized touch point lands on (parity twin of [BiscuitTouchZone]). */
internal sealed class BiscuitTouchZone {
    data object Head : BiscuitTouchZone()
    data object Eyes : BiscuitTouchZone()
    data object Belly : BiscuitTouchZone()
    data class Side(val sign: Double) : BiscuitTouchZone()

    companion object {
        fun resolve(u: Double, v: Double): BiscuitTouchZone = when {
            abs(u) < 0.5 && abs(v + 0.22) < 0.24 -> Eyes
            v < -0.42 -> Head
            abs(u) > 0.5 -> Side(if (u < 0) -1.0 else 1.0)
            else -> Belly
        }
    }
}

/** Result of releasing a press: the reaction plus the clip to play, if any. */
data class BiscuitTouchResult(val reaction: BiscuitReaction?, val clip: BiscuitClip?)

/** A damped spring toward a target (parity twin of [BiscuitSpring]). */
class BiscuitSpring(var value: Double) {
    var velocity: Double = 0.0
    var target: Double = value

    fun step(dt: Double, stiffness: Double, damping: Double) {
        val force = -stiffness * (value - target) - damping * velocity
        velocity += force * dt
        value += velocity * dt
    }

    fun settled(): Boolean = abs(value - target) < 0.002 && abs(velocity) < 0.02
}

/**
 * The interactive press/drag state of a biscuit: springs for gaze, lean, tilt, squash, and lids,
 * plus tap-streak reactions (parity twin of [BiscuitTouchLayer]).
 *
 * All times are epoch milliseconds; "distant past" is [Long.MIN_VALUE] / [Long.MAX_VALUE] as
 * appropriate. Durations are seconds as [Double], matching the Swift reference.
 */
class BiscuitTouchLayer {
    var gazeX = BiscuitSpring(0.0)
    var gazeY = BiscuitSpring(0.0)
    var leanX = BiscuitSpring(0.0)
    var leanY = BiscuitSpring(0.0)
    var tilt = BiscuitSpring(0.0)
    var squashX = BiscuitSpring(1.0)
    var squashY = BiscuitSpring(1.0)
    var lids = BiscuitSpring(1.0)

    var finger: Pair<Double, Double>? = null
        private set

    private var pressStart = 0L
    private var pressOrigin = 0.0 to 0.0
    private var squishAnnounced = false
    private var streak = 0
    private var lastTap = 0L
    private var contentUntil = 0L
    private var winceUntil = 0L
    var annoyedUntil = 0L
        private set
    var angryUntil = 0L
        private set
    var sulkUntil = 0L
        private set
    private var sulkSide = 1.0
    private var lastFrame: Long? = null

    fun moodOverride(at: Long): BiscuitMood? = when {
        at < angryUntil -> BiscuitMood.ANGRY
        at < sulkUntil || at < annoyedUntil -> BiscuitMood.ANNOYED
        else -> null
    }

    val isActive: Boolean get() = finger != null

    val isSettled: Boolean
        get() {
            if (finger != null) return false
            return listOf(gazeX, gazeY, leanX, leanY, tilt, squashX, squashY, lids)
                .all { it.settled() }
        }

    fun nextChange(after: Long): Long? =
        listOf(contentUntil, winceUntil, annoyedUntil, angryUntil, sulkUntil)
            .filter { it > after }
            .minOrNull()

    fun began(u: Double, v: Double, at: Long) {
        finger = u to v
        pressOrigin = u to v
        pressStart = at
        squishAnnounced = false

        squashY.velocity -= 0.6
        leanX.velocity -= u * 40
        leanY.velocity -= v * 30
    }

    fun moved(u: Double, v: Double) {
        finger = u to v
    }

    fun ended(at: Long, tapped: Boolean = true): BiscuitTouchResult {
        val finger = finger ?: return BiscuitTouchResult(null, null)
        this.finger = null
        val held = (at - pressStart) / 1000.0
        val travel = hypot(finger.second - pressOrigin.second, finger.first - pressOrigin.first)

        val depth = minOf(1.0, held / 0.6)
        squashY.velocity += 1.2 + 2.4 * depth
        squashX.velocity -= 0.8 + 1.6 * depth
        if (!tapped || held >= 0.35 || travel >= 0.15) return BiscuitTouchResult(null, null)
        return tapped(finger.first, finger.second, at)
    }

    private fun tapped(u: Double, v: Double, at: Long): BiscuitTouchResult {
        if (at < sulkUntil) {
            squashY.velocity -= 0.8
            return BiscuitTouchResult(BiscuitReaction.IGNORE, null)
        }
        streak = if (at - lastTap < 1500L) streak + 1 else 1
        lastTap = at
        val grumpy = at < annoyedUntil
        return when (streak) {
            1 -> if (grumpy) {
                annoyedUntil = maxOf(annoyedUntil, at + 8000L)
                BiscuitTouchResult(BiscuitReaction.EYE_ROLL, BiscuitClip.EYE_ROLL)
            } else {
                zoneReaction(BiscuitTouchZone.resolve(u, v), at)
            }
            2 -> BiscuitTouchResult(BiscuitReaction.WINK, BiscuitClip.WINK)
            3 -> {
                squashY.velocity -= 1.4
                BiscuitTouchResult(BiscuitReaction.BOUNCE, BiscuitClip.BOUNCE)
            }
            4, 5 -> {
                annoyedUntil = at + 15000L
                BiscuitTouchResult(BiscuitReaction.EYE_ROLL, BiscuitClip.EYE_ROLL)
            }
            6 -> {
                angryUntil = at + 2500L
                annoyedUntil = at + 20000L
                BiscuitTouchResult(BiscuitReaction.ANGRY, BiscuitClip.SHAKE)
            }
            else -> {
                sulkSide = if (u > 0) -1.0 else 1.0
                sulkUntil = at + 4500L
                annoyedUntil = at + 25000L
                streak = 0
                BiscuitTouchResult(BiscuitReaction.IGNORE, BiscuitClip.SIGH)
            }
        }
    }

    private fun zoneReaction(zone: BiscuitTouchZone, at: Long): BiscuitTouchResult = when (zone) {
        is BiscuitTouchZone.Head -> {
            contentUntil = at + 900L
            squashY.velocity -= 0.9
            BiscuitTouchResult(BiscuitReaction.PAT, null)
        }
        is BiscuitTouchZone.Side -> {
            tilt.velocity -= zone.sign * 1.1
            leanX.velocity -= zone.sign * 60
            BiscuitTouchResult(BiscuitReaction.GIGGLE, BiscuitClip.TAP)
        }
        is BiscuitTouchZone.Belly -> {
            annoyedUntil = at + 6000L
            BiscuitTouchResult(BiscuitReaction.STARTLE, BiscuitClip.STARTLE)
        }
        is BiscuitTouchZone.Eyes -> {
            winceUntil = at + 500L
            squashY.velocity -= 0.7
            BiscuitTouchResult(BiscuitReaction.WINCE, null)
        }
    }

    /** Advance the springs toward the current targets; returns a `.squish` announcement once. */
    fun step(to: Long): BiscuitReaction? {
        val last = lastFrame ?: to
        val dt = minOf(1.0 / 30.0, maxOf(0.0, (to - last) / 1000.0))
        lastFrame = to
        var announce: BiscuitReaction? = null

        var gazeX = 0.0; var gazeY = 0.0
        var leanX = 0.0; var leanY = 0.0
        var turn = 0.0
        var squashX = 1.0; var squashY = 1.0
        var lid = 1.0
        val finger = finger
        if (finger != null) {
            val u = finger.first.coerceIn(-1.4, 1.4)
            val v = finger.second.coerceIn(-1.4, 1.4)
            gazeX = u * 11; gazeY = v * 8
            leanX = -u * 5; leanY = -v * 4
            turn = -u * 0.06
            val depth = minOf(1.0, (to - pressStart) / 1000.0 / 0.6)
            squashX = 1.015 + 0.06 * depth
            squashY = 0.98 - 0.08 * depth
            when (BiscuitTouchZone.resolve(pressOrigin.first, pressOrigin.second)) {
                is BiscuitTouchZone.Eyes -> lid = 0.25
                is BiscuitTouchZone.Head -> if (depth > 0.4) lid = 0.35
                else -> {}
            }
            if (depth > 0.5 && !squishAnnounced) { squishAnnounced = true; announce = BiscuitReaction.SQUISH }
        } else if (to < sulkUntil) {
            gazeX = sulkSide * 12; gazeY = -3.0
            leanX = sulkSide * 4; leanY = 0.0
            turn = sulkSide * 0.08
            lid = 0.8
        } else {
            if (to < contentUntil) lid = 0.3
            if (to < winceUntil) lid = 0.15
            if (to < annoyedUntil) lid = minOf(lid, 0.82)
        }
        this.gazeX.target = gazeX; this.gazeY.target = gazeY
        this.leanX.target = leanX; this.leanY.target = leanY
        this.tilt.target = turn
        this.squashX.target = squashX; this.squashY.target = squashY
        this.lids.target = lid

        this.gazeX.step(dt, stiffness = 320.0, damping = 30.0)
        this.gazeY.step(dt, stiffness = 320.0, damping = 30.0)
        this.leanX.step(dt, stiffness = 120.0, damping = 13.0)
        this.leanY.step(dt, stiffness = 120.0, damping = 13.0)
        this.tilt.step(dt, stiffness = 110.0, damping = 12.0)
        this.squashX.step(dt, stiffness = 260.0, damping = 11.0)
        this.squashY.step(dt, stiffness = 260.0, damping = 11.0)
        this.lids.step(dt, stiffness = 220.0, damping = 24.0)
        return announce
    }
}
