package app.dabbulu.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Simple outline icons on a 24x24 grid (stroke style, like the mockup). */
enum class Glyph(vararg val paths: String) {
    Clock("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M12 6v6l4 2"),
    Wallet(
        "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1",
        "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4",
    ),
    ChevronDown("M6 9l6 6l6-6"),
    Pencil("M12 20h9", "M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1l1-4Z"),
    Backspace(
        "M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z",
        "M12 9l6 6", "M18 9l-6 6",
    ),
    Check("M20 6L9 17l-5-5"),
    Search("M19 11a8 8 0 1 1-16 0a8 8 0 1 1 16 0", "M21 21l-4.3-4.3"),
    Sliders(
        "M20 7h-9", "M14 17H5",
        "M20 17a3 3 0 1 1-6 0a3 3 0 1 1 6 0",
        "M10 7a3 3 0 1 1-6 0a3 3 0 1 1 6 0",
    ),
    Home(
        "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
        "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
    ),
    History("M3 12a9 9 0 1 0 9-9a9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2"),
    Plus("M5 12h14", "M12 5v14"),
    Close("M18 6L6 18", "M6 6l12 12"),
    Trash("M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"),
    Calendar("M8 2v4", "M16 2v4", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M3 10h18"),
    Phone("M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M12 18h.01"),
    Target("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M18 12a6 6 0 1 1-12 0a6 6 0 1 1 12 0", "M14 12a2 2 0 1 1-4 0a2 2 0 1 1 4 0"),
    ArrowLeft("M12 19l-7-7l7-7", "M19 12H5"),
    Info("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M12 16v-4", "M12 8h.01"),
    Undo("M9 14L4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11"),
}

@Composable
fun LineIcon(glyph: Glyph, color: Color, size: Dp = 20.dp, strokeWidth: Float = 2f, modifier: Modifier = Modifier) {
    val paths = remember(glyph) { glyph.paths.map { PathGeo(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            paths.forEach {
                drawPath(it, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
