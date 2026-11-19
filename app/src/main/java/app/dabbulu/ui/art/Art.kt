package app.dabbulu.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/*
 * A tiny vector "3D art" engine: each illustration is a stack of shapes with gradient fills,
 * drawn on a 64x64 grid (the same numbers as the SVG mockup), so the art stays sharp at any size.
 * Gradient positions are fractions of each shape's bounding box, like SVG's objectBoundingBox.
 */

sealed interface Fill
data class Solid(val color: Long) : Fill
data class Linear(
    val stops: List<Pair<Float, Long>>,
    val x1: Float = 0f, val y1: Float = 0f, val x2: Float = 0f, val y2: Float = 1f,
) : Fill
data class Radial(
    val stops: List<Pair<Float, Long>>,
    val cx: Float = 0.5f, val cy: Float = 0.5f, val r: Float = 0.5f,
) : Fill

sealed interface Geo
data class PathGeo(val d: String) : Geo
data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : Geo
data class Circle(val cx: Float, val cy: Float, val r: Float) : Geo
data class RRect(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float = 0f) : Geo

data class Layer(
    val geo: Geo,
    val fill: Fill? = null,
    val stroke: Fill? = null,
    val strokeWidth: Float = 0f,
    val alpha: Float = 1f,
    val rotate: Float = 0f,
    val pivotX: Float = 32f,
    val pivotY: Float = 32f,
)

class Art(val layers: List<Layer>)

private class Prepared(val path: Path, val fill: Brush?, val stroke: Brush?, val layer: Layer)

internal fun Geo.toPath(): Path = when (this) {
    is PathGeo -> PathParser().parsePathString(d).toPath()
    is Ellipse -> Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }
    is Circle -> Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
    is RRect -> Path().apply { addRoundRect(RoundRect(x, y, x + w, y + h, CornerRadius(r, r))) }
}

private fun stops(list: List<Pair<Float, Long>>) = list.map { it.first to Color(it.second) }.toTypedArray()

private fun Fill.toBrush(b: Rect): Brush = when (this) {
    is Solid -> SolidColor(Color(color))
    is Linear -> Brush.linearGradient(
        colorStops = stops(stops),
        start = Offset(b.left + x1 * b.width, b.top + y1 * b.height),
        end = Offset(b.left + x2 * b.width, b.top + y2 * b.height),
    )
    is Radial -> Brush.radialGradient(
        colorStops = stops(stops),
        center = Offset(b.left + cx * b.width, b.top + cy * b.height),
        radius = (r * max(b.width, b.height)).coerceAtLeast(0.01f),
    )
}

private fun Art.prepare(): List<Prepared> = layers.map { l ->
    val path = l.geo.toPath()
    val bounds = path.getBounds()
    Prepared(path, l.fill?.toBrush(bounds), l.stroke?.toBrush(bounds), l)
}

/**
 * Draws [art] at [size]. [lift] (0..1, read while drawing so animating it only redraws)
 * pops the art up and slightly larger, used for the selected category.
 */
@Composable
fun ArtIcon(art: Art, size: Dp, modifier: Modifier = Modifier, lift: () -> Float = { 0f }) {
    val prepared = remember(art) { art.prepare() }
    Canvas(modifier.size(size)) {
        val up = lift()
        val s = this.size.minDimension / 64f
        translate(top = -3.dp.toPx() * up) {
            scale(1f + 0.15f * up, pivot = center) {
                scale(s, s, pivot = Offset.Zero) {
                    prepared.forEach { p ->
                        val l = p.layer
                        rotate(l.rotate, pivot = Offset(l.pivotX, l.pivotY)) {
                            p.fill?.let { drawPath(p.path, it, alpha = l.alpha) }
                            p.stroke?.let {
                                drawPath(
                                    p.path, it, alpha = l.alpha,
                                    style = Stroke(width = l.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
