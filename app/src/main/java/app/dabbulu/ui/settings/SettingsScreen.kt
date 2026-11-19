package app.dabbulu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.dabbulu.domain.AmountInput
import app.dabbulu.domain.Money
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.Card3D
import app.dabbulu.ui.components.Coin3D
import app.dabbulu.ui.components.Keypad
import app.dabbulu.ui.components.SheetOverlay
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import java.time.LocalDate

@Composable
fun SettingsScreen(
    monthlyBudget: Long,
    versionName: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEditBudget: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF23252D), Color(0xFF1A1C22))))
                        .border(1.dp, Palette.Border, CircleShape)
                        .clickable(onClick = onBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) { LineIcon(Glyph.ArrowLeft, Palette.TextSoft, size = 20.dp) }
                Spacer(Modifier.width(14.dp))
                Text("Settings", style = Type.H1, color = Palette.Text)
            }
        }
        item {
            Card3D(padding = 0.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditBudget)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LineIcon(Glyph.Target, Palette.Accent, size = 22.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Monthly budget", style = Type.Title, color = Palette.Text)
                        Text(
                            if (monthlyBudget > 0) "Sets your daily spending limit" else "Not set. Add one to get a daily limit",
                            style = Type.Small,
                            color = Palette.TextDim,
                        )
                    }
                    Text(if (monthlyBudget > 0) Money.format(monthlyBudget) else "Set", style = Type.Title, color = Palette.Accent)
                }
            }
        }
        item {
            Card3D {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Phone, Palette.Accent, size = 22.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("Add expenses with a back tap", style = Type.Title, color = Palette.Text)
                }
                Spacer(Modifier.height(12.dp))
                Steps(
                    "On your Samsung, install Good Lock from the Galaxy Store, then its RegiStar module.",
                    "In RegiStar, turn on Back-Tap action. Set Double tap → Open app → Add expense.",
                    "Or: Settings → Advanced features → Side button → Double press → Open app → Add expense.",
                    "You can also add the Add expense tile to Quick Settings, or long-press the Dabbulu icon.",
                )
            }
        }
        item {
            Card3D {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Info, Palette.TextDim, size = 22.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Dabbulu $versionName", style = Type.Title, color = Palette.Text)
                        Text("Everything stays on this phone. Android backs it up with your Google account.", style = Type.Small, color = Palette.TextDim)
                    }
                }
            }
        }
    }
}

@Composable
private fun Steps(vararg steps: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, s ->
            Row {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Palette.Accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", style = Type.Tiny, color = Palette.Accent) }
                Spacer(Modifier.width(12.dp))
                Text(s, style = Type.Body, color = Palette.TextSoft, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Sheet for setting the monthly budget, using the same 3D keypad. */
@Composable
fun BudgetSheet(current: Long, today: LocalDate, onSave: (Long) -> Unit, onDismissed: () -> Unit, animateIn: Boolean = true) {
    var input by remember { mutableStateOf(AmountInput.fromPaise(current).let { AmountInput(it.raw.substringBefore('.')) }) }
    SheetOverlay(onDismissed = onDismissed, animateIn = animateIn) { close ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Monthly budget", style = Type.H2, color = Palette.Text)
            Text("How much do you want to spend in a month?", style = Type.Body, color = Palette.TextDim)
            Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Coin3D(42.dp)
                Spacer(Modifier.width(12.dp))
                Text(input.display, style = Type.Hero.copy(fontSize = Type.Hero.fontSize * 1.3f), color = if (input.isEmpty) Palette.TextFaint else Palette.Text)
            }
            val perDay = input.paise / today.lengthOfMonth()
            Text(
                if (input.paise > 0) "That's about ${Money.format(perDay / 100 * 100)} a day" else " ",
                style = Type.Label,
                color = Palette.TextSoft,
            )
            Keypad(onKey = { k -> if (k != app.dabbulu.domain.PadKey.Dot) input = input.press(k) })
            Button3D(
                text = if (input.paise > 0) "Save ${Money.format(input.paise)} a month" else "Enter an amount",
                onClick = { onSave(input.paise); close() },
                enabled = input.paise > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            if (current > 0) {
                TextButton(onClick = { onSave(0); close() }) {
                    Text("Remove budget", style = Type.Label, color = Palette.Danger)
                }
            }
        }
    }
}
