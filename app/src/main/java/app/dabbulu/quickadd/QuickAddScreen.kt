package app.dabbulu.quickadd

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dabbulu.domain.Money
import app.dabbulu.domain.TodayStats
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.Meter
import app.dabbulu.ui.components.SheetOverlay
import app.dabbulu.ui.components.SoftButton
import app.dabbulu.ui.components.confirm
import app.dabbulu.ui.form.ExpenseForm
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import kotlinx.coroutines.delay

/**
 * The Quick Add popup. Shown by [QuickAddActivity] over whatever app is open,
 * and inside Dabbulu when the + button is tapped.
 */
@Composable
fun QuickAddSheet(
    ui: QuickAddUi?,
    actions: FormActions,
    onUndo: () -> Unit,
    onDismissed: () -> Unit,
    animateIn: Boolean = true,
    autoClose: Boolean = true,
) {
    SheetOverlay(onDismissed = onDismissed, animateIn = animateIn) { close ->
        if (ui == null) {
            Spacer(Modifier.height(420.dp))
            return@SheetOverlay
        }
        val saved = ui.phase as? QuickPhase.Saved
        val view = LocalView.current
        LaunchedEffect(saved?.id) {
            if (saved != null) {
                view.confirm()
                if (autoClose) {
                    delay(2400)
                    close()
                }
            }
        }
        AnimatedContent(
            targetState = saved,
            transitionSpec = { (fadeIn(tween(200)) + scaleIn(initialScale = 0.97f)) togetherWith fadeOut(tween(120)) },
            label = "phase",
        ) { s ->
            if (s == null) {
                ExpenseForm(ui.form, actions)
            } else {
                SavedCard(s, ui.stats, onUndo = onUndo, onDone = close)
            }
        }
    }
}

@Composable
fun SavedCard(saved: QuickPhase.Saved, stats: TodayStats?, onUndo: () -> Unit, onDone: () -> Unit) {
    val pop = remember(saved.id) { Animatable(0.4f) }
    LaunchedEffect(saved.id) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 400f)) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .shadow(18.dp, CircleShape, ambientColor = Palette.Accent, spotColor = Palette.Accent)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(lerp(Palette.Accent, Color.White, 0.2f), Palette.Accent)))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(Glyph.Check, Palette.OnAccent, size = 36.dp, strokeWidth = 3f)
        }
        Spacer(Modifier.height(16.dp))
        Text("${Money.format(saved.amount)} added", style = Type.H1, color = Palette.Text)
        Spacer(Modifier.height(4.dp))
        Text("${saved.categoryName} · ${saved.payment.label}", style = Type.Body, color = Palette.TextDim)
        Spacer(Modifier.height(20.dp))
        val b = stats?.budget
        if (saved.forToday && b != null) {
            val over = b.leftToday < 0
            val used = if (b.todayAllowance <= 0) 1f else ((b.todayAllowance - b.leftToday).toFloat() / b.todayAllowance)
            Meter(used, if (over) Palette.Warn else Palette.Accent)
            Spacer(Modifier.height(10.dp))
            Text(
                if (over) {
                    "${Money.format(-b.leftToday)} over today's ${Money.format(b.todayAllowance)}"
                } else {
                    "${Money.format(b.leftToday)} left of ${Money.format(b.todayAllowance)} today"
                },
                style = Type.Label,
                color = if (over) Palette.Warn else Palette.TextSoft,
                textAlign = TextAlign.Center,
            )
        } else if (saved.forToday && stats != null) {
            Text("${Money.format(stats.spentToday)} spent today", style = Type.Label, color = Palette.TextSoft)
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            SoftButton("Undo", onClick = onUndo, glyph = Glyph.Undo, modifier = Modifier.weight(1f), height = 50.dp)
            Button3D("Done", onClick = onDone, modifier = Modifier.weight(1f), height = 50.dp)
        }
    }
}
