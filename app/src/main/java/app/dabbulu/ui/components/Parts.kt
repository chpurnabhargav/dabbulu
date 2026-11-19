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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
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
import app.dabbulu.ui.art.Art
import app.dabbulu.ui.art.ArtIcon
import app.dabbulu.ui.art.CategoryArt
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import kotlinx.coroutines.launch

fun Category.uiColor() = Color(color)

/** A category button with its 3D icon. When selected it glows in the category colour and the icon pops up. */
@Composable
fun CategoryTile(
    label: String,
    art: Art,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    val lift by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = 0.45f, stiffness = 520f),
        label = "lift",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, label = "tilePress")
    val shape = RoundedCornerShape(18.dp)
    val bg = if (selected) {
        Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f)))
    } else {
        Brush.verticalGradient(listOf(Palette.CardHi, Palette.CardLo))
    }
    Column(
        modifier
            .height(70.dp)
            .graphicsLayer { scaleX = press; scaleY = press }
            .then(if (selected) Modifier.shadow(14.dp, shape, ambientColor = color, spotColor = color) else Modifier)
            .clip(shape)
            .background(bg)
            .then(
                if (dashed) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Palette.BorderHi,
                            cornerRadius = CornerRadius(18.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))),
                        )
                    }
                } else {
                    Modifier.border(1.5.dp, if (selected) color else Palette.Border, shape)
                },
            )
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ArtIcon(art, 38.dp, lift = { lift })
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = Type.Tiny,
            color = if (selected) Palette.Text else Palette.TextSoft,
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
fun Keypad(onKey: (PadKey) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        Grid(padKeys, columns = 3, gap = 8.dp, rowGap = 6.dp) { key, m ->
            val label = when (key) {
                PadKey.Back -> "Delete"
                PadKey.Dot -> "Decimal point"
                is PadKey.Digit -> key.d.toString()
            }
            Press3D(onClick = { onKey(key) }, modifier = m, description = label) {
                when (key) {
                    PadKey.Back -> LineIcon(Glyph.Backspace, Palette.Text, size = 22.dp)
                    PadKey.Dot -> Text(".", style = Type.Key, color = Palette.Text)
                    is PadKey.Digit -> Text(key.d.toString(), style = Type.Key, color = Palette.Text)
                }
            }
        }
    }
}

/** A thick rounded progress bar with a glossy fill. */
@Composable
fun Meter(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 10.dp) {
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
                    .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(color, Color.White, 0.3f), color))),
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
    scrim: Color = Color(0xA306070A),
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val progress = remember { Animatable(if (animateIn) 0f else 1f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                progress.animateTo(0f, tween(220, easing = FastOutLinearInEasing))
                onDismissed()
            }
        }
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 340f))
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
        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress.value
                    translationY = (1f - p) * size.height
                    rotationX = (1f - p) * 18f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    cameraDistance = 14f * density
                }
                .shadow(30.dp, sheetShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(sheetShape)
                .background(Brush.verticalGradient(listOf(Palette.SheetTop, Palette.SheetBottom)))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)), sheetShape)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3D47)),
            )
            Spacer(Modifier.height(12.dp))
            content(close)
        }
    }
}

/** One expense in a list: 3D icon tile, what it was, and the amount. */
@Composable
fun ExpenseRow(expense: Expense, category: Category, timeLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = category.uiColor()
    val title = expense.note.ifBlank { category.name }
    val sub = buildString {
        if (expense.note.isNotBlank()) append(category.shortName).append(" · ")
        append(expense.payment.label).append(" · ").append(timeLabel)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.07f))))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ArtIcon(CategoryArt.of(category.id), 32.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.Title, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = Type.Small, color = Palette.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text(Money.format(expense.amount), style = Type.Title, color = Palette.Text)
    }
}

/** A dark rounded card with a soft top sheen. */
@Composable
fun Card3D(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1C1E25), Color(0xFF131419))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f))), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun rememberHaptics() = LocalView.current
