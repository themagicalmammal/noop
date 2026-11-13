package com.noop.analytics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.random.Random

/**
 * Drives one live Biscuit companion: mood/outfit/color/ghost/visibility state with timed
 * crossfades, touch and scroll input, behavior scheduling, and the per-frame pose stack.
 *
 * Parity twin of Aura's `BiscuitController` (`Packages/Biscuit/Sources/Biscuit/`).
 * Time is Long epoch-millis; `Date.distantPast` is `0L`. The scene is injected instead of
 * Aura's global `BiscuitScene.result`.
 */
class BiscuitController(
    val scene: BiscuitScene,
    mood: BiscuitMood = BiscuitMood.NEUTRAL,
    outfits: Set<BiscuitOutfit> = emptySet(),
    bodyColor: BiscuitColor = BiscuitColor.resting,
    appearsOnStart: Boolean = true,
    val frugal: Boolean = false,
    val framing: BiscuitFraming = BiscuitFraming.BODY,
    behaviorSeed: Long? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    var mood: BiscuitMood = mood
        set(value) {
            val changed = field != value
            field = value
            if (changed) stirred()
        }
    var outfits: Set<BiscuitOutfit> = outfits
        set(value) {
            previousOutfits = field
            outfitStart = now
            val changed = field != value
            field = value
            if (changed) stirred()
        }
    var bodyColor: BiscuitColor = bodyColor
        set(value) {
            previousColor = field
            colorStart = now
            val changed = field != value
            field = value
            if (changed) stirred()
        }
    var isGhost: Boolean = false
        set(value) {
            ghostStart = now
            val changed = field != value
            field = value
            if (changed) stirred()
        }
    var cloudMotion: Boolean = true
    var floats: Boolean = true
    var looksAround: Boolean = true
    var visible: Boolean = true
        set(value) {
            visibilityStart = now
            val changed = field != value
            field = value
            if (changed) stirred()
        }
    var stir = 0
        private set
    var lastAnimation: BiscuitClip = BiscuitClip.IDLE
        private set
    var isPaused: Boolean = false
        set(value) {
            val was = field
            field = value
            if (field && !was) pausedAt = clock()
            if (!field && was) {
                val p = pausedAt
                if (p != null) {
                    val delta = clock() - p
                    origin += delta
                    ghostStart += delta
                    shownStart += delta
                    outfitStart += delta
                    colorStart += delta
                    visibilityStart += delta
                    val active = request
                    if (active != null) {
                        active.started += delta
                        active.lastInput += delta
                    }
                    pausedAt = null
                }
            }
        }

    private var ghostStart = 0L
    private var shownMood: BiscuitMood = mood
    private var shownPrevious: BiscuitMood = mood
    private var shownStart = 0L
    private var previousOutfits: Set<BiscuitOutfit> = outfits
    private var previousColor: BiscuitColor = bodyColor
    private var outfitStart = 0L
    private var colorStart = 0L
    private var visibilityStart: Long
    private var origin: Long
    private var pausedAt: Long? = null
    private var request: Request? = null
    private var interrupted: Interrupted? = null
    private val touch = BiscuitTouchLayer()
    private val scheduler = BiscuitBehaviorScheduler(behaviorSeed ?: Random.nextLong())
    var onReaction: ((BiscuitReaction) -> Unit)? = null
    private var scrollAccumulator = 0.0

    init {
        scheduler.breathsOnly = frugal
        origin = clock()
        visibilityStart = origin
        if (!appearsOnStart) visibilityStart = 0L
        if (frugal) cloudMotion = false
    }

    private val now: Long get() = pausedAt ?: clock()

    private class Request(
        val name: String,
        var started: Long,
        var lastInput: Long,
        var holdsWhileScrolling: Boolean = false,
    ) {
        fun age(at: Long): Double {
            val age = max(0.0, (at - started) / 1000.0)
            if (!holdsWhileScrolling) return age
            val heldPhase = min(0.65, max(0.0, (lastInput - started) / 1000.0))
            val releaseAge = max(0.0, (at - lastInput) / 1000.0 - 0.16)
            return min(0.65, age) + releaseAge - if (releaseAge > 0) max(0.0, min(0.65, age) - heldPhase) else 0.0
        }
    }

    private data class Interrupted(val request: Request, val at: Long)

    fun play(animation: BiscuitClip) {
        val date = now
        lastAnimation = animation
        val duration = scene.artboard.animation(named = animation.clip)?.seconds ?: 0.8
        val t = max(0.0, (date - origin) / 1000.0)
        scheduler.noteInteraction(t, t + duration + 0.8)
        when (animation) {
            BiscuitClip.APPEAR -> {
                visible = true
                visibilityStart = date
            }
            BiscuitClip.HIDE -> {
                visible = false
                visibilityStart = date
            }
            BiscuitClip.IDLE -> {
                request = null
                floats = true
            }
            else -> {
                val active = request
                if (active != null) interrupted = Interrupted(active, date)
                request = Request(animation.clip, date, date)
            }
        }
        stirred()
    }

    fun touchBegan(x: Double, y: Double, width: Double, height: Double) {
        val offset = bodyOffset(x, y, width, height) ?: return
        touch.began(offset.first, offset.second, at = now)
        noteInteraction(1.2)
        stirred()
    }

    fun touchMoved(x: Double, y: Double, width: Double, height: Double) {
        if (!touch.isActive) return
        val offset = bodyOffset(x, y, width, height) ?: return
        touch.moved(offset.first, offset.second)
    }

    fun touchEnded(cancelled: Boolean = false) {
        val result = touch.ended(at = now, tapped = !cancelled)
        result.clip?.let { play(it) }
        result.reaction?.let { onReaction?.invoke(it) }
        noteInteraction(1.6)
        stirred()
    }

    fun poke() {
        touchBegan(1.0, 0.55, 1.0, 1.0)
        touchEnded()
    }

    private fun bodyOffset(x: Double, y: Double, width: Double, height: Double): Pair<Double, Double>? {
        val scale = min(width, height) / 320.0 * framing.zoom
        if (scale <= 0.0) return null
        val px = (x - width / 2.0) / scale
        val py = (y - height / 2.0) / scale - 8.0
        return px / 90.0 to py / 80.0
    }

    private fun stirred() {
        if (frugal) stir += 1
    }

    fun isMoving(at: Long): Boolean =
        isMovingQuickly(at) || scheduler.isMoving(max(0.0, ((pausedAt ?: at) - origin) / 1000.0))

    fun isMovingQuickly(at: Long): Boolean {
        val d = pausedAt ?: at
        if (request != null || interrupted != null || !touch.isSettled) return true
        if ((touch.moodOverride(at = d) ?: mood) != shownMood) return true
        val length: (String, Double) -> Double = { clip, fallback ->
            scene.artboard.animation(named = clip)?.seconds ?: fallback
        }
        val blends: List<Pair<Long, Double>> = listOf(
            shownStart to 0.3,
            outfitStart to 0.2,
            colorStart to 0.45,
            ghostStart to length(if (isGhost) "aura_veil_on" else "aura_veil_off", 1.2),
            visibilityStart to length(if (visible) "aura_arrive" else "aura_depart", 1.2),
        )
        if (blends.any { (d - it.first) / 1000.0 < it.second }) return true
        return scheduler.isMovingQuickly(max(0.0, (d - origin) / 1000.0))
    }

    fun nextBeat(after: Long): Long {
        val d = pausedAt ?: after
        val next = origin + (scheduler.nextEvent(max(0.0, (d - origin) / 1000.0)) * 1000.0).toLong()
        val change = touch.nextChange(after = d)
        return if (change != null && change < next) change else next
    }

    private fun noteInteraction(seconds: Double) {
        val t = max(0.0, (now - origin) / 1000.0)
        scheduler.noteInteraction(t, t + seconds)
    }

    fun reactToScroll(delta: Double) {
        if (!delta.isFinite() || delta == 0.0) return
        if (sign(scrollAccumulator) != sign(delta)) scrollAccumulator = 0.0
        scrollAccumulator += delta
        if (abs(scrollAccumulator) < 6.0 && request?.holdsWhileScrolling != true) return
        val action = if (scrollAccumulator > 0.0) BiscuitClip.SCROLL_DOWN else BiscuitClip.SCROLL_UP
        val active = request
        if (active != null && active.holdsWhileScrolling && active.name != action.clip && abs(scrollAccumulator) < 6.0) return
        val date = now
        val current = request
        if (current != null && current.holdsWhileScrolling && current.name == action.clip && current.age(at = date) < 1.75) {
            if ((date - current.lastInput) / 1000.0 <= 0.16) {
                current.lastInput = date
                request = current
            } else {
                play(action)
                request?.holdsWhileScrolling = true
            }
        } else {
            play(action)
            request?.holdsWhileScrolling = true
        }
        scrollAccumulator = 0.0
        val t = max(0.0, (date - origin) / 1000.0)
        scheduler.noteInteraction(t, t + 2.45)
    }

    fun pose(at: Long, reducedMotion: Boolean): List<BiscuitVectorObject> {
        val d = pausedAt ?: at
        val elapsed = max(0.0, (d - origin) / 1000.0)
        val active = request
        if (active != null) {
            val clip = scene.artboard.animation(named = active.name)
            if (clip != null && active.age(at = d) > clip.seconds) request = null
        }
        val faded = interrupted
        if (faded != null && (d - faded.at) / 1000.0 > 0.18) interrupted = null
        val effective = touch.moodOverride(at = d) ?: mood
        if (effective != shownMood) {
            shownPrevious = shownMood
            shownMood = effective
            shownStart = d
        }
        val base = copyObjects(scene.artboard.objects).toMutableList()
        scene.apply("aura_eye_base", to = base)
        val progress: (Long, Double) -> Double = { start, seconds ->
            if (reducedMotion) 1.0
            else {
                val t = min(1.0, max(0.0, (d - start) / 1000.0 / seconds))
                t * t * (3.0 - 2.0 * t)
            }
        }
        val moodPose: (BiscuitMood) -> MutableList<BiscuitVectorObject> = { m ->
            val objects = copyObjects(base).toMutableList()
            scene.apply("aura_mood_" + m.rawValue, to = objects)
            objects
        }
        val old = moodPose(shownPrevious)
        val objects = moodPose(shownMood)
        val blend = progress(shownStart, 0.24)
        if (blend < 1.0) {
            for (i in objects.indices) {
                val target = objects[i]
                for (key in target.values.keys.toList()) {
                    val value = target.values[key]!!
                    val previous = old[i].values[key] ?: value
                    target.values[key] = if (key == "colorValue") {
                        BiscuitTimeline.colorLerp(previous, value, blend)
                    } else {
                        previous + (value - previous) * blend
                    }
                }
            }
        }
        scene.apply(
            if (isGhost) "aura_veil_on" else "aura_veil_off",
            seconds = if (reducedMotion) Double.MAX_VALUE else max(0.0, (d - ghostStart) / 1000.0),
            to = objects,
        )
        val outfitMix = progress(outfitStart, 0.16)
        for (outfit in BiscuitOutfit.entries) {
            scene.apply("aura_outfit_" + outfit.rawValue + if (outfit in previousOutfits) "_on" else "_off", to = objects)
            scene.apply(
                "aura_outfit_" + outfit.rawValue + if (outfit in outfits) "_on" else "_off",
                mix = outfitMix,
                to = objects,
            )
        }
        scene.tint(BiscuitTimeline.colorLerp(previousColor.argb, bodyColor.argb, progress(colorStart, 0.4)), objects)
        scene.frame(framing, objects)
        if (!reducedMotion) {
            if (cloudMotion) scene.apply("aura_cloud_drift", seconds = elapsed, loop = true, to = objects)
            val behavior = scheduler.sample(elapsed, shownMood, looksAround && visible && !touch.isActive)
            touch.step(to = d)?.let { onReaction?.invoke(it) }
            if (floats) {
                scene.apply("aura_hover", seconds = behavior.floatSeconds, loop = true, mix = behavior.floatStrength, to = objects)
            }
            val names = scene.bindings?.nameToObject?.get("Noop Aura")
            if (names != null) {
                fun add(node: String, property: String, value: Double) {
                    val id = names[node] ?: return
                    objects[id].values[property] = (objects[id].values[property] ?: 0.0) + value
                }

                fun multiply(node: String, property: String, value: Double) {
                    val id = names[node] ?: return
                    objects[id].values[property] = (objects[id].values[property] ?: 1.0) * value
                }

                add("Aura gaze", "x", behavior.eyeX); add("Aura gaze", "y", behavior.eyeY)
                multiply("Aura lids", "scaleY", behavior.lid)
                add("Reaction", "x", behavior.bodyX); add("Reaction", "y", behavior.bodyY)
                add("Reaction", "rotation", behavior.tilt)
                multiply("Aura eye left", "scaleY", 1.0 - behavior.curiosity)
                multiply("Aura eye right", "scaleY", 1.0 + behavior.curiosity)

                add("Aura gaze", "x", touch.gazeX.value); add("Aura gaze", "y", touch.gazeY.value)
                multiply("Aura lids", "scaleY", max(0.05, touch.lids.value))
                add("Reaction", "x", touch.leanX.value)
                add("Reaction", "y", touch.leanY.value + 75.0 * (1.0 - touch.squashY.value))
                add("Reaction", "rotation", touch.tilt.value)
                multiply("Reaction", "scaleX", touch.squashX.value)
                multiply("Reaction", "scaleY", touch.squashY.value)
            }
            val envelope: (Request, BiscuitAnimation) -> Double = { r, clip ->
                val age = r.age(at = d)
                if (age > clip.seconds) 0.0
                else min(1.0, age / 0.12) * min(1.0, max(0.0, (clip.seconds - age) / 0.18))
            }
            val fading = interrupted
            if (fading != null) {
                val clip = scene.artboard.animation(named = fading.request.name)
                if (clip != null) {
                    val out = 1.0 - progress(fading.at, 0.18)
                    scene.apply(fading.request.name, seconds = fading.request.age(at = d), mix = envelope(fading.request, clip) * out, to = objects)
                }
            }
            val pending = request
            if (pending != null) {
                val clip = scene.artboard.animation(named = pending.name)
                if (clip != null) {
                    scene.apply(pending.name, seconds = pending.age(at = d), mix = envelope(pending, clip), to = objects)
                }
            }
        }
        scene.applyVisibility(visible, if (reducedMotion) Double.MAX_VALUE else max(0.0, (d - visibilityStart) / 1000.0), objects)
        return objects
    }

    private fun copyObjects(objects: List<BiscuitVectorObject>): List<BiscuitVectorObject> =
        objects.map { BiscuitVectorObject(it.type, it.values.toMutableMap()) }
}
