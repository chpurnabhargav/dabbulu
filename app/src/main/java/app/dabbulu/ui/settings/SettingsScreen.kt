package app.dabbulu.ui.settings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dabbulu.SettingsUi
import app.dabbulu.domain.AmountInput
import app.dabbulu.domain.Category
import app.dabbulu.domain.Money
import app.dabbulu.domain.PadKey
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.CategoryIcon
import app.dabbulu.ui.components.Grid
import app.dabbulu.ui.components.Keypad
import app.dabbulu.ui.components.Panel
import app.dabbulu.ui.components.Press3D
import app.dabbulu.ui.components.RoundIconButton
import app.dabbulu.ui.components.SheetOverlay
import app.dabbulu.ui.components.uiColor
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type

fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$n$suffix"
}

@Composable
fun SettingsScreen(
    ui: SettingsUi,
    versionName: String,
    contentPadding: PaddingValues,
    onBudget: () -> Unit,
    onStartDay: () -> Unit,
    onCategoryBudgets: () -> Unit,
    onIncome: () -> Unit,
    onReminder: (Boolean) -> Unit,
    onExport: () -> Unit,
) {
    val s = ui.settings
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Settings", style = Type.H1, color = Palette.Text, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) }

        item { GroupLabel("Budget") }
        item {
            Panel(padding = 0.dp) {
                SettingRow(Glyph.Target, "Monthly budget", if (s.monthlyBudget > 0) "Gives you a daily spending limit" else "Not set", if (s.monthlyBudget > 0) Money.format(s.monthlyBudget) else "Set", onBudget)
                Divider()
                SettingRow(Glyph.Calendar, "Month starts on", "Budget resets on this day", ordinal(s.monthStartDay), onStartDay)
                Divider()
                val n = ui.categories.count { it.budget != null }
                SettingRow(Glyph.Layers, "Category budgets", if (n == 0) "Limit food, shopping and more" else "$n set", if (n == 0) "Set" else "Edit", onCategoryBudgets)
            }
        }

        item { GroupLabel("Income") }
        item {
            Panel(padding = 0.dp) {
                SettingRow(Glyph.Income, "Monthly income", "To see how much you save", if (s.monthlyIncome > 0) Money.format(s.monthlyIncome) else "Set", onIncome)
            }
        }

        item { GroupLabel("Reminders") }
        item {
            Panel(padding = 0.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onReminder(!s.dailyReminder) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RowIcon(Glyph.Bell)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Daily reminder", style = Type.Title, color = Palette.Text)
                        Text("9 PM: a nudge to add anything you missed", style = Type.Small, color = Palette.TextDim)
                    }
                    Switch(
                        checked = s.dailyReminder,
                        onCheckedChange = onReminder,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Palette.Accent,
                            checkedThumbColor = Palette.OnAccent,
                            uncheckedTrackColor = Palette.Field,
                            uncheckedThumbColor = Palette.TextDim,
                            uncheckedBorderColor = Palette.Border,
                        ),
                    )
                }
            }
        }

        item { GroupLabel("Data") }
        item {
            Panel(padding = 0.dp) {
                SettingRow(Glyph.Download, "Export to CSV", "Open your expenses in Excel or Google Sheets", null, onExport)
            }
        }

        item { GroupLabel("Quick add") }
        item {
            Panel {
                Steps(
                    "Install Good Lock from the Galaxy Store, then its RegiStar module.",
                    "In RegiStar, turn on Back-Tap action: Double tap → Open app → Add expense.",
                    "Or: Settings → Advanced features → Side button → Double press → Open app → Add expense.",
                    "You can also add the Add expense tile to Quick Settings, or long-press the Dabbulu icon.",
                )
            }
        }
        item {
            Text(
                "Dabbulu $versionName · Everything stays on this phone and is included in your Google backup.",
                style = Type.Small,
                color = Palette.TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text.uppercase(), style = Type.Tiny, color = Palette.TextFaint, modifier = Modifier.padding(start = 6.dp, top = 8.dp))
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 64.dp), color = Palette.Border)

@Composable
private fun RowIcon(glyph: Glyph) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Palette.Field),
        contentAlignment = Alignment.Center,
    ) { LineIcon(glyph, Palette.TextSoft, size = 18.dp) }
}

@Composable
private fun SettingRow(glyph: Glyph, title: String, subtitle: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(glyph)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.Title, color = Palette.Text)
            Text(subtitle, style = Type.Small, color = Palette.TextDim)
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = Type.Num, color = if (value == "Set") Palette.Accent else Palette.Text)
        }
        Spacer(Modifier.width(6.dp))
        LineIcon(Glyph.ChevronRight, Palette.TextFaint, size = 16.dp)
    }
}

@Composable
private fun Steps(vararg steps: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, s ->
            Row {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Palette.Accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", style = Type.Tiny, color = Palette.Accent) }
                Spacer(Modifier.width(12.dp))
                Text(s, style = Type.Body, color = Palette.TextSoft, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Category budgets list: tap a category to set or change its monthly limit. */
@Composable
fun CategoryBudgetsScreen(ui: SettingsUi, contentPadding: PaddingValues, onBack: () -> Unit, onEdit: (Category) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Glyph.ArrowLeft, "Back", onBack, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Text("Category budgets", style = Type.H1, color = Palette.Text)
            }
        }
        item {
            Text("Spent so far this budget month, and the limit for each category.", style = Type.Body, color = Palette.TextDim)
        }
        item {
            Panel(padding = 0.dp) {
                ui.categories.forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider(Modifier.padding(start = 66.dp), color = Palette.Border)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(c) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(c.id, c.uiColor(), size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = Type.Title, color = Palette.Text)
                            Text("${Money.format(ui.categorySpent[c.id] ?: 0L)} spent", style = Type.Small, color = Palette.TextDim)
                        }
                        Text(c.budget?.let { Money.format(it) } ?: "Set", style = Type.Num, color = if (c.budget == null) Palette.Accent else Palette.Text)
                        Spacer(Modifier.width(6.dp))
                        LineIcon(Glyph.ChevronRight, Palette.TextFaint, size = 16.dp)
                    }
                }
            }
        }
    }
}

/** Enter a whole-rupee amount with the keypad (budgets, income). */
@Composable
fun AmountSheet(
    title: String,
    subtitle: String,
    current: Long,
    onSave: (Long) -> Unit,
    onDismissed: () -> Unit,
    removeLabel: String? = null,
    hint: (Long) -> String = { "" },
    animateIn: Boolean = true,
) {
    var input by remember { mutableStateOf(AmountInput(if (current > 0) (current / 100).toString() else "")) }
    SheetOverlay(onDismissed = onDismissed, animateIn = animateIn) { close ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = Type.H2, color = Palette.Text)
            Text(subtitle, style = Type.Body, color = Palette.TextDim, textAlign = TextAlign.Center)
            Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("₹", style = Type.H1, color = Palette.TextDim)
                Spacer(Modifier.width(6.dp))
                Text(input.display, style = Type.Hero.copy(fontSize = Type.Hero.fontSize * 1.25f), color = if (input.isEmpty) Palette.TextFaint else Palette.Text)
            }
            Text(hint(input.paise).ifEmpty { " " }, style = Type.Label, color = Palette.TextSoft)
            Keypad(onKey = { k -> if (k != PadKey.Dot) input = input.press(k) }, allowDot = false)
            Button3D(
                text = if (input.paise > 0) "Save ${Money.format(input.paise)}" else "Enter an amount",
                onClick = { onSave(input.paise); close() },
                enabled = input.paise > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            if (current > 0 && removeLabel != null) {
                TextButton(onClick = { onSave(0); close() }) {
                    Text(removeLabel, style = Type.Label, color = Palette.Danger)
                }
            }
        }
    }
}

/** Pick the day the budget month starts (1–28). */
@Composable
fun StartDaySheet(current: Int, onSave: (Int) -> Unit, onDismissed: () -> Unit) {
    var day by remember { mutableIntStateOf(current) }
    SheetOverlay(onDismissed = onDismissed) { close ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Budget month starts on", style = Type.H2, color = Palette.Text)
            Text("Pick your salary day. Your monthly budget resets on this day.", style = Type.Body, color = Palette.TextDim, textAlign = TextAlign.Center)
            Grid((1..28).toList(), columns = 7, gap = 6.dp) { d, m ->
                val selected = d == day
                Press3D(
                    onClick = { day = d },
                    modifier = m,
                    height = 40.dp,
                    shape = RoundedCornerShape(12.dp),
                    top = if (selected) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Palette.Accent, Palette.Accent))
                    } else {
                        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Palette.KeyTop, Palette.KeyBottom))
                    },
                    edgeColor = if (selected) Palette.AccentEdge else Palette.KeyEdge,
                ) {
                    Text("$d", style = Type.Label, color = if (selected) Palette.OnAccent else Palette.Text)
                }
            }
            Button3D("Start on the ${ordinal(day)}", onClick = { onSave(day); close() }, modifier = Modifier.fillMaxWidth())
        }
    }
}
