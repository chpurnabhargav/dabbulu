package app.dabbulu.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.dabbulu.domain.Period
import app.dabbulu.domain.PeriodKind
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import java.time.LocalDate

val Sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.015f)))

fun View.tick() = performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

fun View.confirm() = performHapticFeedback(
    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
)

/** A key that sits on a thin raised edge and sinks onto it when pressed. */
@Composable
fun Press3D(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    edge: Dp = 2.5.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    top: Brush = Brush.verticalGradient(listOf(Palette.KeyTop, Palette.KeyBottom)),
    edgeColor: Color = Palette.KeyEdge,
    glow: Color? = null,
    enabled: Boolean = true,
    haptic: Boolean = true,
    description: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed && enabled) edge else 0.dp, tween(60), label = "press")
    val view = LocalView.current
    Box(
        modifier
            .height(height + edge)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = edge)
                .then(if (glow != null && enabled) Modifier.shadow(14.dp, shape, ambientColor = glow, spotColor = glow) else Modifier)
                .clip(shape)
                .background(edgeColor),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = drop)
                .clip(shape)
                .background(top)
                .border(1.dp, Sheen, shape)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                    if (haptic) view.tick()
                    onClick()
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** Primary action. Mint when [enabled], quiet grey when not. */
@Composable
fun Button3D(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Accent,
    height: Dp = 54.dp,
    glyph: Glyph? = null,
) {
    val top = if (enabled) {
        Brush.verticalGradient(listOf(lerp(color, Color.White, 0.16f), color))
    } else {
        Brush.verticalGradient(listOf(Palette.KeyTop, Palette.KeyBottom))
    }
    val fg = if (enabled) Palette.OnAccent else Palette.TextFaint
    Press3D(
        onClick = onClick,
        modifier = modifier,
        height = height,
        edge = 3.dp,
        shape = RoundedCornerShape(18.dp),
        top = top,
        edgeColor = if (enabled) lerp(color, Color.Black, 0.55f) else Palette.KeyEdge,
        glow = color.copy(alpha = 0.6f),
        enabled = enabled,
        haptic = false,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (glyph != null) {
                LineIcon(glyph, fg, size = 18.dp, strokeWidth = 2.4f)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = Type.Button, color = fg, maxLines = 1)
        }
    }
}

/** Secondary action: a quiet raised button. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: Glyph? = null, color: Color = Palette.Text, height: Dp = 48.dp) {
    Press3D(onClick = onClick, modifier = modifier, height = height, shape = RoundedCornerShape(16.dp), haptic = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) {
                LineIcon(glyph, color, size = 17.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = Type.Label, color = color, maxLines = 1)
        }
    }
}

/** A rounded chip used for the date and small actions. */
@Composable
fun Pill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "pill")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Border, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Segmented control with a raised, sliding selection. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, height: Dp = 40.dp) {
    val shape = RoundedCornerShape(14.dp)
    val view = LocalView.current
    BoxWithConstraints(
        modifier
            .height(height)
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Border, shape)
            .padding(3.dp),
    ) {
        val n = options.size.coerceAtLeast(1)
        val w = maxWidth / n
        val x by animateDpAsState(w * selected.coerceIn(0, n - 1), spring(dampingRatio = 0.8f, stiffness = 500f), label = "seg")
        if (selected in options.indices) {
            Box(
                Modifier
                    .offset(x = x)
                    .width(w)
                    .fillMaxHeight()
                    .shadow(4.dp, RoundedCornerShape(11.dp))
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2E3038), Color(0xFF26282F))))
                    .border(1.dp, Sheen, RoundedCornerShape(11.dp)),
            )
        }
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(role = Role.Tab) { if (i != selected) { view.tick(); onSelect(i) } }
                        .semantics { this.selected = i == selected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = Type.Label,
                        color = if (i == selected) Palette.Text else Palette.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun RoundIconButton(glyph: Glyph, description: String, onClick: () -> Unit, enabled: Boolean = true, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.Field)
            .border(1.dp, Palette.Border, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        LineIcon(glyph, if (enabled) Palette.TextSoft else Palette.TextFaint.copy(alpha = 0.5f), size = 18.dp)
    }
}

/**
 * Day / Week / Month / Year switch with ‹ › to step through periods.
 * When [onAll] is given, an "All" option is shown first and [allSelected] hides the stepper.
 */
@Composable
fun PeriodSelector(
    period: Period,
    today: LocalDate,
    onKind: (PeriodKind) -> Unit,
    onStep: (Period) -> Unit,
    modifier: Modifier = Modifier,
    allSelected: Boolean = false,
    onAll: (() -> Unit)? = null,
) {
    val kinds = PeriodKind.entries
    val options = (if (onAll != null) listOf("All") else emptyList()) + kinds.map { it.label }
    val offset = if (onAll != null) 1 else 0
    val selected = if (allSelected) 0 else kinds.indexOf(period.kind) + offset
    Column(modifier) {
        Segmented(options, selected, onSelect = { i ->
            if (onAll != null && i == 0) onAll() else onKind(kinds[i - offset])
        }, modifier = Modifier.fillMaxWidth())
        if (!allSelected) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Glyph.ChevronLeft, "Previous ${period.kind.noun}", { onStep(period.previous()) })
                Text(
                    period.label(today),
                    style = Type.Title,
                    color = Palette.Text,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                val canNext = !period.next().start.isAfter(today)
                RoundIconButton(Glyph.ChevronRight, "Next ${period.kind.noun}", { if (canNext) onStep(period.next()) }, enabled = canNext)
            }
        }
    }
}
