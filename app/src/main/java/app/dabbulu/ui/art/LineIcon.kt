package app.dabbulu.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Outline icons on a 24x24 grid, drawn with a consistent stroke. */
enum class Glyph(vararg val paths: String) {
    // Categories
    Utensils("M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2", "M7 2v20", "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7"),
    Coffee("M10 2v2", "M14 2v2", "M6 2v2", "M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"),
    Car(
        "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2",
        "M9 17a2 2 0 1 1-4 0a2 2 0 1 1 4 0", "M9 17h6", "M19 17a2 2 0 1 1-4 0a2 2 0 1 1 4 0",
    ),
    Cart("M9 21a1 1 0 1 1-2 0a1 1 0 1 1 2 0", "M20 21a1 1 0 1 1-2 0a1 1 0 1 1 2 0", "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"),
    Bag("M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z", "M3 6h18", "M16 10a4 4 0 0 1-8 0"),
    Zap("M13 2L3 14h9l-1 8l10-12h-9l1-8z"),
    Users("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M13 7a4 4 0 1 1-8 0a4 4 0 1 1 8 0", "M22 21v-2a4 4 0 0 0-3-3.87", "M16 3.13a4 4 0 0 1 0 7.75"),
    House(
        "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
        "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
    ),
    Repeat("M17 2l4 4l-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "M7 22l-4-4l4-4", "M21 13v1a4 4 0 0 1-4 4H3"),
    Ticket("M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z", "M13 5v2", "M13 17v2", "M13 11v2"),
    HeartPulse(
        "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2c-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z",
        "M3.22 12H9.5l.5-1l2 4.5l2-7l1.5 3.5h5.27",
    ),
    Sparkles(
        "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
    ),
    Dots("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M17 12h.01", "M12 12h.01", "M7 12h.01"),
    Grid("M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z", "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z"),

    // Interface
    Clock("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M12 6v6l4 2"),
    Wallet(
        "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1",
        "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4",
    ),
    ChevronDown("M6 9l6 6l6-6"),
    ChevronLeft("M15 18l-6-6l6-6"),
    ChevronRight("M9 18l6-6l-6-6"),
    Pencil("M12 20h9", "M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1l1-4Z"),
    Backspace(
        "M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z",
        "M12 9l6 6", "M18 9l-6 6",
    ),
    Check("M20 6L9 17l-5-5"),
    Search("M19 11a8 8 0 1 1-16 0a8 8 0 1 1 16 0", "M21 21l-4.3-4.3"),
    Sliders("M20 7h-9", "M14 17H5", "M20 17a3 3 0 1 1-6 0a3 3 0 1 1 6 0", "M10 7a3 3 0 1 1-6 0a3 3 0 1 1 6 0"),
    Home(
        "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
        "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
    ),
    History("M3 12a9 9 0 1 0 9-9a9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2"),
    Chart("M3 3v16a2 2 0 0 0 2 2h16", "M18 17V9", "M13 17V5", "M8 17v-3"),
    TrendUp("M22 7l-8.5 8.5l-5-5L2 17", "M16 7h6v6"),
    TrendDown("M22 17l-8.5-8.5l-5 5L2 7", "M16 17h6v-6"),
    Plus("M5 12h14", "M12 5v14"),
    Close("M18 6L6 18", "M6 6l12 12"),
    Trash("M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"),
    Calendar("M8 2v4", "M16 2v4", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M3 10h18"),
    Phone("M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M12 18h.01"),
    Target("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M18 12a6 6 0 1 1-12 0a6 6 0 1 1 12 0", "M14 12a2 2 0 1 1-4 0a2 2 0 1 1 4 0"),
    ArrowLeft("M12 19l-7-7l7-7", "M19 12H5"),
    Info("M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0", "M12 16v-4", "M12 8h.01"),
    Undo("M9 14L4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11"),
    Bell("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0"),
    Download("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7 10l5 5l5-5", "M12 15V3"),
    Income("M12 2v20", "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"),
    Layers("M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z", "M2 12l9.17 4.17a2 2 0 0 0 1.66 0L22 12", "M2 17l9.17 4.17a2 2 0 0 0 1.66 0L22 17"),
    Leak("M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"),
}

private val cache = HashMap<Glyph, List<Path>>()

private fun Glyph.parsed(): List<Path> = cache.getOrPut(this) { paths.map { PathParser().parsePathString(it).toPath() } }

@Composable
fun LineIcon(glyph: Glyph, color: Color, size: Dp = 20.dp, strokeWidth: Float = 1.9f, modifier: Modifier = Modifier) {
    val paths = remember(glyph) { glyph.parsed() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            paths.forEach {
                drawPath(it, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

object CategoryGlyphs {
    private val byId = mapOf(
        "food" to Glyph.Utensils,
        "snacks" to Glyph.Coffee,
        "transport" to Glyph.Car,
        "groceries" to Glyph.Cart,
        "shopping" to Glyph.Bag,
        "bills" to Glyph.Zap,
        "family" to Glyph.Users,
        "rent" to Glyph.House,
        "subscriptions" to Glyph.Repeat,
        "entertainment" to Glyph.Ticket,
        "health" to Glyph.HeartPulse,
        "personal" to Glyph.Sparkles,
        "other" to Glyph.Dots,
    )

    fun of(categoryId: String): Glyph = byId[categoryId] ?: Glyph.Dots
}
