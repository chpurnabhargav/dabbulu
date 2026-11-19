package app.dabbulu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.dabbulu.domain.BarPoint
import app.dabbulu.domain.Money
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import kotlin.math.min

/**
 * Column chart with rounded bars that grow in. Tap a bar to see its value;
 * by default the highest bar is shown. A dashed line marks the average.
 */
@Composable
fun BarChart(points: List<BarPoint>, modifier: Modifier = Modifier, height: Dp = 150.dp, color: Color = Palette.Accent) {
    val grow = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { grow.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    val defaultIndex = points.indexOfFirst { it.highlight }
    var picked by remember(points) { mutableIntStateOf(defaultIndex) }
    val measurer = rememberTextMeasurer()
    val labelStyle = Type.Tiny.copy(color = Palette.TextFaint)
    val max = (points.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)
    val nonZero = points.filter { it.amount > 0 }
    val average = if (nonZero.isEmpty()) 0L else points.sumOf { it.amount } / points.size

    Column(modifier) {
        val p = points.getOrNull(picked)
        Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p?.caption ?: "Tap a bar", style = Type.Small, color = Palette.TextDim, modifier = Modifier.weight(1f))
            if (p != null) Text(Money.format(p.amount), style = Type.Num, color = Palette.Text)
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures { pos ->
                        if (points.isNotEmpty()) picked = (pos.x / (size.width / points.size)).toInt().coerceIn(0, points.size - 1)
                    }
                },
        ) {
            if (points.isEmpty()) return@Canvas
            val labelH = 18.dp.toPx()
            val chartH = size.height - labelH
            val slot = size.width / points.size
            val bw = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            val r = CornerRadius(min(bw / 2f, 6.dp.toPx()))
            points.forEachIndexed { i, pt ->
                val x = i * slot + (slot - bw) / 2f
                drawRoundRect(Palette.Track.copy(alpha = 0.55f), Offset(x, 0f), Size(bw, chartH), r)
                val h = (pt.amount.toFloat() / max) * chartH * grow.value
                if (h > 0f) {
                    val c = if (i == picked) color else color.copy(alpha = 0.42f)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(lerp(c, Color.White, 0.18f), c), startY = chartH - h, endY = chartH),
                        Offset(x, chartH - h), Size(bw, h.coerceAtLeast(2f)), r,
                    )
                }
                if (pt.label.isNotEmpty()) {
                    val layout = measurer.measure(pt.label, if (i == picked) labelStyle.copy(color = Palette.TextSoft) else labelStyle)
                    drawText(layout, topLeft = Offset(i * slot + slot / 2f - layout.size.width / 2f, chartH + 5.dp.toPx()))
                }
            }
            if (average > 0) {
                val y = chartH - (average.toFloat() / max) * chartH * grow.value
                drawLine(
                    Palette.TextDim.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
        }
    }
}

/**
 * Ring chart with real depth: each slice is drawn over a darker extruded copy,
 * so the ring sits on the card like a 3D object.
 */
@Composable
fun Donut(slices: List<Pair<Color, Long>>, modifier: Modifier = Modifier, thickness: Dp = 22.dp, depth: Dp = 6.dp, center: @Composable () -> Unit = {}) {
    val sweep = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { sweep.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = thickness.toPx()
            val dz = depth.toPx()
            val d = min(size.width, size.height - dz) - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - dz - d) / 2f)
            val gap = if (slices.size > 1) 2.2f else 0f
            fun ring(offsetY: Float, shade: Float) {
                var start = -90f
                slices.forEach { (c, v) ->
                    val angle = 360f * v / total * sweep.value
                    if (angle > gap) {
                        drawArc(
                            color = lerp(c, Color.Black, shade),
                            startAngle = start + gap / 2f,
                            sweepAngle = angle - gap,
                            useCenter = false,
                            topLeft = topLeft + Offset(0f, offsetY),
                            size = Size(d, d),
                            style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        )
                    }
                    start += angle
                }
            }
            if (slices.isEmpty()) {
                drawArc(Palette.Track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
            } else {
                val steps = 6
                for (s in steps downTo 1) ring(dz * s / steps, 0.55f)
                ring(0f, 0f)
            }
        }
        center()
    }
}

/** Progress ring (today's limit used), with a soft extruded edge. */
@Composable
fun Ring(fraction: Float, color: Color, modifier: Modifier = Modifier, thickness: Dp = 9.dp, center: @Composable () -> Unit = {}) {
    val f = remember { Animatable(0f) }
    LaunchedEffect(fraction) { f.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing)) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = thickness.toPx()
            val d = size.minDimension - stroke - 3.dp.toPx()
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f - 1.5.dp.toPx())
            drawArc(Palette.Track, 0f, 360f, false, tl + Offset(0f, 3.dp.toPx()), Size(d, d), style = Stroke(stroke))
            drawArc(Color(0xFF2A2C33), 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            if (f.value > 0f) {
                drawArc(lerp(color, Color.Black, 0.5f), -90f, 360f * f.value, false, tl + Offset(0f, 3.dp.toPx()), Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(listOf(lerp(color, Color.White, 0.25f), color, lerp(color, Color.White, 0.25f))),
                    -90f, 360f * f.value, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        center()
    }
}

/** A single bar split into coloured parts (needs vs wants, UPI vs cash…). */
@Composable
fun SplitBar(parts: List<Pair<Color, Long>>, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val total = parts.sumOf { it.second }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Palette.Track),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (total > 0) {
            parts.filter { it.second > 0 }.forEach { (c, v) ->
                Box(
                    Modifier
                        .weight(v.toFloat() / total)
                        .fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.2f), c))),
                )
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, size: Dp = 9.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun LegendRow(color: Color, label: String, value: String, share: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(color)
        Spacer(Modifier.width(10.dp))
        Text(label, style = Type.Body, color = Palette.TextSoft, modifier = Modifier.weight(1f))
        if (share != null) {
            Text(share, style = Type.Small, color = Palette.TextDim)
            Spacer(Modifier.width(10.dp))
        }
        Text(value, style = Type.Num, color = Palette.Text)
    }
}
