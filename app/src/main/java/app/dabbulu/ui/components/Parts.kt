package app.dabbulu.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.dabbulu.domain.Category
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Money
import app.dabbulu.domain.PadKey
import app.dabbulu.ui.art.CategoryGlyphs
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import kotlinx.coroutines.launch

fun Category.uiColor() = Color(color)

/** A category's outline icon on a softly tinted, slightly raised tile. */
@Composable
fun CategoryIcon(categoryId: String, color: Color, size: Dp = 42.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.10f))))
            .border(1.dp, color.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
    ) {
        LineIcon(CategoryGlyphs.of(categoryId), lerp(color, Color.White, 0.15f), size = size * 0.48f, strokeWidth = 2f)
    }
}

/** A category choice in the Quick Add grid. */
@Composable
fun CategoryTile(
    label: String,
    glyph: Glyph,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, label = "tilePress")
    val pick by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "tilePick")
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .height(66.dp)
            .graphicsLayer { scaleX = press; scaleY = press }
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.14f) else Palette.Field)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) color.copy(alpha = 0.85f) else Palette.Border, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LineIcon(
            glyph, lerp(Palette.TextSoft, lerp(color, Color.White, 0.1f), 0.55f + 0.45f * pick), size = 22.dp, strokeWidth = 2f,
            modifier = Modifier.graphicsLayer { translationY = -2.dp.toPx() * pick },
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = Type.Tiny,
            color = if (selected) Palette.Text else Palette.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Lays [items] out in rows of [columns] with equal widths. */
@Composable
fun <T> Grid(items: List<T>, columns: Int, gap: Dp, rowGap: Dp = gap, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { cell(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val padKeys: List<PadKey> = "123456789".map { PadKey.Digit(it) } + listOf(PadKey.Dot, PadKey.Digit('0'), PadKey.Back)

@Composable
fun Keypad(onKey: (PadKey) -> Unit, modifier: Modifier = Modifier, allowDot: Boolean = true) {
    Box(modifier) {
        Grid(padKeys, columns = 3, gap = 8.dp, rowGap = 6.dp) { key, m ->
            val label = when (key) {
                PadKey.Back -> "Delete"
                PadKey.Dot -> "Decimal point"
                is PadKey.Digit -> key.d.toString()
            }
            if (key == PadKey.Dot && !allowDot) {
                Spacer(m)
            } else {
                Press3D(onClick = { onKey(key) }, modifier = m, description = label) {
                    when (key) {
                        PadKey.Back -> LineIcon(Glyph.Backspace, Palette.TextSoft, size = 22.dp)
                        PadKey.Dot -> Text("·", style = Type.Key, color = Palette.Text)
                        is PadKey.Digit -> Text(key.d.toString(), style = Type.Key, color = Palette.Text)
                    }
                }
            }
        }
    }
}

/** Rounded progress bar. */
@Composable
fun Meter(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "meter")
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Palette.Track),
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.22f), color))),
            )
        }
    }
}

/**
 * Bottom sheet that tilts up into place in 3D over a dimmed background.
 * Tapping outside or pressing back closes it; [content] gets a `close` function too.
 */
@Composable
fun SheetOverlay(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    animateIn: Boolean = true,
    scrim: Color = Color(0xB0040506),
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val progress = remember { Animatable(if (animateIn) 0f else 1f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                progress.animateTo(0f, tween(200, easing = FastOutLinearInEasing))
                onDismissed()
            }
        }
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 380f))
    }
    BackHandler(onBack = close)

    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
        )
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress.value
                    translationY = (1f - p) * size.height
                    rotationX = (1f - p) * 14f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    cameraDistance = 14f * density
                }
                .shadow(24.dp, sheetShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(sheetShape)
                .background(Brush.verticalGradient(listOf(Palette.SheetTop, Palette.SheetBottom)))
                .border(1.dp, Sheen, sheetShape)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Palette.BorderHi),
            )
            Spacer(Modifier.height(14.dp))
            content(close)
        }
    }
}

/** One expense in a list. */
@Composable
fun ExpenseRow(expense: Expense, category: Category, timeLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = expense.note.ifBlank { category.name }
    val sub = buildString {
        if (expense.note.isNotBlank()) append(category.shortName).append(" · ")
        append(expense.payment.label).append(" · ").append(timeLabel)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(category.id, category.uiColor(), size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.Title, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = Type.Small, color = Palette.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text(Money.format(expense.amount), style = Type.Num, color = Palette.Text)
    }
}

/** A raised card: layered surface, soft shadow and a faint top edge. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF17181D), Color(0xFF121317))))
            .border(1.dp, Sheen, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.H2, color = Palette.Text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = Type.Label,
                color = Palette.Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

/** "↓ 12%" in green when spending went down, "↑ 18%" in amber when it went up. */
@Composable
fun ChangeChip(now: Long, before: Long, modifier: Modifier = Modifier) {
    if (before <= 0 && now <= 0) return
    val pct = if (before > 0) (now - before) * 100 / before else 0L
    val same = before > 0 && pct == 0L
    val up = now > before
    val color = if (same) Palette.TextDim else if (up) Palette.Warn else Palette.Good
    val text = when {
        before <= 0 -> "New"
        same -> "Same"
        else -> "${kotlin.math.abs(pct).coerceAtMost(999)}%"
    }
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!same) {
            LineIcon(if (up) Glyph.TrendUp else Glyph.TrendDown, color, size = 14.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = Type.Tiny, color = color)
    }
}

/** A labelled number on a small raised tile. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Palette.Text) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .shadow(6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF17181D), Color(0xFF121317))))
            .border(1.dp, Sheen, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = Type.Small, color = Palette.TextDim, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = Type.Num.copy(fontSize = Type.Num.fontSize * 1.08f), color = valueColor, maxLines = 1)
    }
}
