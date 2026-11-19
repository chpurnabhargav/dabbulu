package app.dabbulu.quickadd

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dabbulu.domain.Money
import app.dabbulu.domain.TodayStats
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.Coin3D
import app.dabbulu.ui.components.CoinMotion
import app.dabbulu.ui.components.Meter
import app.dabbulu.ui.components.Pill
import app.dabbulu.ui.components.SheetOverlay
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
    scrim: Color = Color(0xA306070A),
) {
    SheetOverlay(onDismissed = onDismissed, animateIn = animateIn, scrim = scrim) { close ->
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
                    delay(2600)
                    close()
                }
            }
        }
        AnimatedContent(
            targetState = saved,
            transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(120)) },
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
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Coin3D(92.dp, motion = CoinMotion.Spin, spinKey = saved.id)
        Spacer(Modifier.height(12.dp))
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Pill(onClick = onUndo, modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Undo, Palette.TextSoft, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Undo", style = Type.Label, color = Palette.Text)
                }
            }
            Button3D("Done", onClick = onDone, modifier = Modifier.weight(1f), height = 50.dp)
        }
    }
}
