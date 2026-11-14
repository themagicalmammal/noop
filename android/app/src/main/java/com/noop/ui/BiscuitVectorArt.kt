package com.noop.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.provider.Settings
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noop.analytics.BiscuitAnimation
import com.noop.analytics.BiscuitAnimationKey
import com.noop.analytics.BiscuitAnimationTrack
import com.noop.analytics.BiscuitArtboard
import com.noop.analytics.BiscuitBindings
import com.noop.analytics.BiscuitController
import com.noop.analytics.BiscuitFraming
import com.noop.analytics.BiscuitMood
import com.noop.analytics.BiscuitOutfit
import com.noop.analytics.BiscuitScene
import com.noop.analytics.BiscuitVectorObject
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Small Android Canvas renderer for Aura's bundled Biscuit artboard.
 *
 * Owns the parsed [BiscuitScene] (artboard + bindings) shared with the live
 * [BiscuitController]; all pose computation happens in the controller (or
 * [BiscuitScene.still] for static art) and this class only draws the resulting
 * object list, mirroring the reference `BiscuitDrawing`.
 */
internal class BiscuitVectorArt private constructor(val scene: BiscuitScene) {
    /**
     * Draws a pose object list, centered and fitted to the canvas. Mirrors the reference
     * `BiscuitDrawing`: `scale = min(width, height) / bleed / 320 * framing.zoom`,
     * translated so 160 artboard units land on the center.
     */
    fun drawObjects(
        target: Canvas,
        width: Float,
        height: Float,
        objects: List<BiscuitVectorObject>,
        framing: BiscuitFraming = BiscuitFraming.BODY,
    ) {
        val bleed = if (framing == BiscuitFraming.HEAD) 1.25f else 1.0f
        val scale = min(width, height) / bleed / 320f * framing.zoom.toFloat()
        target.save()
        target.translate(width / 2f - 160f * scale, height / 2f - 160f * scale)
        target.scale(scale, scale)
        drawShapes(target, objects)
        target.restore()
    }

    private fun drawShapes(canvas: Canvas, objects: List<BiscuitVectorObject>) {
        val children = objects.indices.drop(1).groupBy { objects[it].values["parentId"]?.toInt() ?: 0 }
        for (shape in objects.indices.reversed()) {
            if (objects[shape].type != "Shape") continue
            val sv = objects[shape].values
            if ((sv["drawableFlags"]?.toInt() ?: 0) and 1 != 0) continue
            val ancestry = generateSequence(shape) { index -> if (index > 0) objects[index].values["parentId"]?.toInt() else null }
                .toList().asReversed()
            canvas.save()
            var opacity = 1.0
            ancestry.forEach { index ->
                val v = objects[index].values
                opacity *= (v["opacity"] ?: 1.0).coerceIn(0.0, 1.0)
                canvas.translate((v["x"] ?: 0.0).toFloat(), (v["y"] ?: 0.0).toFloat())
                canvas.rotate(Math.toDegrees(v["rotation"] ?: 0.0).toFloat())
                canvas.scale((v["scaleX"] ?: 1.0).toFloat(), (v["scaleY"] ?: 1.0).toFloat())
            }
            val shapeChildren = children[shape].orEmpty()
            val pathIndex = shapeChildren.firstOrNull { objects[it].type in PATH_TYPES }
            if (pathIndex != null) {
                val path = makePath(pathIndex, objects, children)
                shapeChildren.filter { objects[it].type == "Fill" || objects[it].type == "Stroke" }.forEach { paintIndex ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (255 * opacity).toInt().coerceIn(0, 255) }
                    val fill = objects[paintIndex].type == "Fill"
                    paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
                    if (!fill) paint.strokeWidth = (objects[paintIndex].values["thickness"] ?: 1.0).toFloat()
                    val paintChildren = children[paintIndex].orEmpty()
                    val source = paintChildren.firstOrNull { objects[it].type in COLOR_TYPES } ?: return@forEach
                    val colorObject = objects[source]
                    val opacityPaint = colorObject.values["opacity"] ?: 1.0
                    when (colorObject.type) {
                        "SolidColor" -> paint.color = withAlpha(colorObject.values["colorValue"] ?: 0.0, opacity * opacityPaint)
                        "RadialGradient", "LinearGradient" -> {
                            val stops = children[source].orEmpty().filter { objects[it].type == "GradientStop" }
                                .sortedBy { objects[it].values["position"] ?: 0.0 }
                            if (stops.size >= 2) {
                                val colors = stops.map { withAlpha(objects[it].values["colorValue"] ?: 0.0, opacity * opacityPaint) }.toIntArray()
                                val positions = stops.map { (objects[it].values["position"] ?: 0.0).toFloat().coerceIn(0f, 1f) }.toFloatArray()
                                val x0 = (colorObject.values["startX"] ?: 0.0).toFloat(); val y0 = (colorObject.values["startY"] ?: 0.0).toFloat()
                                val x1 = (colorObject.values["endX"] ?: 1.0).toFloat(); val y1 = (colorObject.values["endY"] ?: 1.0).toFloat()
                                paint.shader = if (colorObject.type == "RadialGradient")
                                    RadialGradient(x0, y0, max(1f, hypot(x1 - x0, y1 - y0)), colors, positions, Shader.TileMode.CLAMP)
                                else LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
                            }
                        }
                    }
                    canvas.drawPath(path, paint)
                    paint.shader = null
                }
            }
            canvas.restore()
        }
    }

    private fun makePath(index: Int, objects: List<BiscuitVectorObject>, children: Map<Int, List<Int>>): Path {
        val obj = objects[index]
        val v = obj.values
        val path = Path()
        when (obj.type) {
            "Ellipse", "Rectangle" -> {
                val width = (v["width"] ?: 0.0).toFloat(); val height = (v["height"] ?: 0.0).toFloat()
                val rect = RectF(-width * (v["originX"] ?: .5).toFloat(), -height * (v["originY"] ?: .5).toFloat(),
                    width * (1 - (v["originX"] ?: .5)).toFloat(), height * (1 - (v["originY"] ?: .5)).toFloat())
                if (obj.type == "Ellipse") path.addOval(rect, Path.Direction.CW)
                else path.addRoundRect(rect, (v["cornerRadiusTL"] ?: 0.0).toFloat(), (v["cornerRadiusTL"] ?: 0.0).toFloat(), Path.Direction.CW)
            }
            "PointsPath" -> {
                val vertices = children[index].orEmpty().filter { objects[it].type.endsWith("Vertex") }.map { objects[it].values }
                if (vertices.isEmpty()) return path
                fun handle(vertex: Map<String, Double>, prefix: String): Pair<Float, Float> {
                    val angle = vertex["${prefix}Rotation"] ?: 0.0
                    val distance = vertex["${prefix}Distance"] ?: 0.0
                    return ((vertex["x"] ?: 0.0) + cos(angle) * distance).toFloat() to
                        ((vertex["y"] ?: 0.0) + sin(angle) * distance).toFloat()
                }
                val first = vertices.first(); path.moveTo((first["x"] ?: 0.0).toFloat(), (first["y"] ?: 0.0).toFloat())
                val closed = (v["isClosed"] ?: 0.0) != 0.0
                val count = if (closed) vertices.size else vertices.size - 1
                for (i in 0 until count) {
                    val from = vertices[i]; val to = vertices[(i + 1) % vertices.size]
                    if (from.containsKey("outRotation") || to.containsKey("inRotation")) {
                        val c1 = handle(from, "out"); val c2 = handle(to, "in")
                        path.cubicTo(c1.first, c1.second, c2.first, c2.second, (to["x"] ?: 0.0).toFloat(), (to["y"] ?: 0.0).toFloat())
                    } else path.lineTo((to["x"] ?: 0.0).toFloat(), (to["y"] ?: 0.0).toFloat())
                }
                if (closed) path.close()
            }
        }
        return path
    }

    private fun withAlpha(value: Double, opacity: Double): Int {
        val argb = value.toLong().toInt()
        val alpha = (Color.alpha(argb) * opacity).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(argb), Color.green(argb), Color.blue(argb))
    }

    companion object {
        private val PATH_TYPES = setOf("PointsPath", "Ellipse", "Rectangle", "Star")
        private val COLOR_TYPES = setOf("SolidColor", "RadialGradient", "LinearGradient")
        @Volatile private var cached: BiscuitVectorArt? = null

        fun load(context: Context): BiscuitVectorArt {
            cached?.let { return it }
            val json = context.assets.open("biscuit/noop-aura.json").bufferedReader().use { it.readText() }
            val boardJson = JSONArray(json).getJSONObject(0)
            val objectsJson = boardJson.getJSONArray("objects")
            val objects = List(objectsJson.length()) { i ->
                val obj = objectsJson.getJSONObject(i)
                BiscuitVectorObject(obj.getString("type"), obj.getJSONObject("values").toDoubleMap())
            }
            val animationsJson = boardJson.getJSONArray("animations")
            val animations = List(animationsJson.length()) { i ->
                val anim = animationsJson.getJSONObject(i)
                val tracksJson = anim.getJSONArray("tracks")
                val tracks = List(tracksJson.length()) { j ->
                    val track = tracksJson.getJSONObject(j); val keysJson = track.getJSONArray("keys")
                    val keys = List(keysJson.length()) { k ->
                        val key = keysJson.getJSONObject(k)
                        BiscuitAnimationKey(
                            key.getDouble("frame"),
                            key.getDouble("value"),
                            key.optInt("interpolation", 0),
                            key.optInt("interpolator", 0),
                        )
                    }
                    BiscuitAnimationTrack(track.getInt("object"), track.getString("property"), track.optBoolean("color", false), keys)
                }
                BiscuitAnimation(
                    anim.getString("name"),
                    anim.getDouble("fps"),
                    anim.getDouble("duration"),
                    anim.optDouble("speed", 1.0),
                    anim.optInt("loop", 0),
                    anim.getDouble("start"),
                    anim.getDouble("end"),
                    anim.optBoolean("quantize", false),
                    tracks,
                )
            }
            val result = BiscuitVectorArt(BiscuitScene(BiscuitArtboard(boardJson.getDouble("width"), boardJson.getDouble("height"), objects, animations), loadBindings(context)))
            cached = result
            return result
        }

        private fun loadBindings(context: Context): BiscuitBindings? {
            val json = context.assets.open("biscuit/noop-aura.bindings.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val boards = root.optJSONObject("nameToObject") ?: return null
            val nameToObject = buildMap {
                boards.keys().forEach { board ->
                    val inner = boards.getJSONObject(board)
                    put(board, buildMap { inner.keys().forEach { name -> put(name, inner.getInt(name)) } })
                }
            }
            val targets = root.optJSONArray("bodyColorTargets")
            val bodyColorTargets = if (targets != null) List(targets.length()) { targets.getInt(it) } else emptyList()
            return BiscuitBindings(nameToObject, bodyColorTargets)
        }

        private fun JSONObject.toDoubleMap(): MutableMap<String, Double> = buildMap {
            keys().forEach { key -> optDouble(key).takeIf { it.isFinite() }?.let { put(key, it) } }
        }.toMutableMap()
    }
}

@Composable
internal fun BiscuitArt(
    controller: BiscuitController? = null,
    mood: String = BiscuitMood.HAPPY.rawValue,
    outfit: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderer = remember(context) { BiscuitVectorArt.load(context) }
    val reducedMotion = remember(context) {
        Settings.Global.getInt(context.contentResolver, "animator_enabled", 1) == 0
    }
    val active = if (controller != null) controller else remember(renderer) {
        BiscuitController(
            renderer.scene,
            mood = BiscuitMood.entries.firstOrNull { it.rawValue == mood } ?: BiscuitMood.NEUTRAL,
            outfits = outfit?.let { name -> BiscuitOutfit.entries.firstOrNull { o -> o.rawValue == name }?.let(::setOf) } ?: emptySet(),
        )
    }
    if (controller == null) {
        LaunchedEffect(mood) {
            active.mood = BiscuitMood.entries.firstOrNull { it.rawValue == mood } ?: BiscuitMood.NEUTRAL
        }
        LaunchedEffect(outfit) {
            active.outfits = outfit?.let { name -> BiscuitOutfit.entries.firstOrNull { o -> o.rawValue == name }?.let(::setOf) } ?: emptySet()
        }
    }
    var elapsed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val started = android.os.SystemClock.elapsedRealtime()
        while (true) {
            elapsed = (android.os.SystemClock.elapsedRealtime() - started) / 1000f
            delay(33)
        }
    }
    ComposeCanvas(
        modifier
            .fillMaxWidth()
            .height(260.dp)
            .pointerInput(active) {
                awaitPointerEventScope {
                    var down: PointerInputChange? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = down ?: event.changes.firstOrNull() ?: continue
                        if (down == null) {
                            down = change
                            active.touchBegan(change.position.x.toDouble(), change.position.y.toDouble(), size.width.toDouble(), size.height.toDouble())
                        } else if (!change.pressed) {
                            active.touchEnded()
                            down = null
                        } else {
                            active.touchMoved(change.position.x.toDouble(), change.position.y.toDouble(), size.width.toDouble(), size.height.toDouble())
                        }
                        change.consume()
                    }
                }
            }
    ) {
        drawIntoCanvas { native ->
            val objects = active.pose(System.currentTimeMillis(), reducedMotion)
            renderer.drawObjects(native.nativeCanvas, size.width, size.height, objects, active.framing)
        }
    }
}
