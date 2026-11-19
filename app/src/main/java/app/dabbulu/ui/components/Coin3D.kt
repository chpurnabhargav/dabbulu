package app.dabbulu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class CoinMotion { Still, Wobble, Spin }

private val faceStops = arrayOf(
    0f to Color(0xFFFFF8D1),
    0.28f to Color(0xFFFDE047),
    0.66f to Color(0xFFF59E0B),
    1f to Color(0xFFB45309),
)
private val edgeBack = Color(0xFF7A320A)
private val edgeFront = Color(0xFFC57510)

/**
 * A gold ₹ coin drawn with a real edge, so it reads as a 3D object as it turns.
 * [Wobble] sways gently; [Spin] does two full turns each time [spinKey] changes.
 */
@Composable
fun Coin3D(size: Dp, modifier: Modifier = Modifier, motion: CoinMotion = CoinMotion.Wobble, spinKey: Any? = Unit) {
    val angle: Float = when (motion) {
        CoinMotion.Still -> -20f
        CoinMotion.Wobble -> {
            val t = rememberInfiniteTransition(label = "coin")
            val a by t.animateFloat(
                initialValue = -26f,
                targetValue = 26f,
                animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "wobble",
            )
            a
        }
        CoinMotion.Spin -> {
            val anim = remember(spinKey) { Animatable(0f) }
            LaunchedEffect(spinKey) {
                anim.animateTo(720f, tween(1500, easing = CubicBezierEasing(0.25f, 0.7f, 0.3f, 1f)))
            }
            anim.value
        }
    }
    val measurer = rememberTextMeasurer()
    val glyphSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
    val glyph = remember(glyphSize, measurer) {
        measurer.measure(
            "₹",
            TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = glyphSize, color = Color(0xFF7C2D12)),
        )
    }

    Canvas(modifier.size(size)) {
        val rad = Math.toRadians(angle.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val r = this.size.minDimension / 2f * 0.86f
        val c = center
        val sx = abs(cosA).coerceAtLeast(0.03f)
        val depth = sinA * r * 0.2f

        // Edge: stacked discs from the back face to the front face.
        val steps = 8
        for (i in 0..steps) {
            val f = i / steps.toFloat()
            val x = c.x - depth / 2f + depth * f
            drawOval(
                color = lerp(edgeBack, edgeFront, f),
                topLeft = Offset(x - r * sx, c.y - r),
                size = Size(2f * r * sx, 2f * r),
            )
        }

        val face = Offset(c.x + depth / 2f, c.y)
        scale(sx, 1f, pivot = face) {
            drawCircle(
                brush = Brush.radialGradient(*faceStops, center = face + Offset(-0.36f * r, -0.44f * r), radius = r * 1.4f),
                radius = r,
                center = face,
            )
            drawCircle(Color(0x99FFF0B4), radius = r * 0.84f, center = face, style = Stroke(width = r * 0.07f))
            drawText(glyph, topLeft = face - Offset(glyph.size.width / 2f, glyph.size.height / 2f))
        }
    }
}
