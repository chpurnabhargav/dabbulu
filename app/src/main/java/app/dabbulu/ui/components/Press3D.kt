package app.dabbulu.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type

private val topSheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent))

fun View.tick() = performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

fun View.confirm() = performHapticFeedback(
    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
)

/**
 * A key with a raised edge underneath. When pressed, the top slides down onto the edge,
 * so it feels like pushing a real button.
 */
@Composable
fun Press3D(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    edge: Dp = 3.dp,
    shape: Shape = RoundedCornerShape(14.dp),
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
                .then(if (glow != null && enabled) Modifier.shadow(16.dp, shape, ambientColor = glow, spotColor = glow) else Modifier)
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
                .border(1.dp, topSheen, shape)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                    if (haptic) view.tick()
                    onClick()
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** The big call-to-action button (Add, Save). Lights up in the accent colour when [enabled]. */
@Composable
fun Button3D(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Accent,
    height: Dp = 56.dp,
    glyph: Glyph? = null,
) {
    val top = if (enabled) {
        Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(color, Color.White, 0.28f), color))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF25272F), Color(0xFF1C1E24)))
    }
    val edge = if (enabled) androidx.compose.ui.graphics.lerp(color, Color.Black, 0.5f) else Palette.KeyEdge
    val fg = if (enabled) Palette.OnAccent else Color(0xFF9CA3AF)
    Press3D(
        onClick = onClick,
        modifier = modifier,
        height = height,
        edge = 4.dp,
        shape = RoundedCornerShape(18.dp),
        top = top,
        edgeColor = edge,
        glow = color,
        enabled = enabled,
        haptic = false,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (glyph != null) {
                LineIcon(glyph, fg, size = 18.dp, strokeWidth = 2.6f)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = Type.Button, color = fg, maxLines = 1)
        }
    }
}

/** A rounded dark chip used for date, payment method and small actions. */
@Composable
fun Pill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "pill")
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .height(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF23252D), Color(0xFF1A1C22))))
            .border(1.dp, Palette.Border, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
