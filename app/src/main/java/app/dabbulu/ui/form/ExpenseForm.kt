package app.dabbulu.ui.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dabbulu.domain.Category
import app.dabbulu.ui.art.CategoryArt
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.CategoryTile
import app.dabbulu.ui.components.Coin3D
import app.dabbulu.ui.components.CoinMotion
import app.dabbulu.ui.components.Grid
import app.dabbulu.ui.components.Keypad
import app.dabbulu.ui.components.Pill
import app.dabbulu.ui.components.tick
import app.dabbulu.ui.components.uiColor
import app.dabbulu.ui.theme.Jakarta
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import java.time.Instant
import java.time.ZoneOffset

private sealed interface Tile {
    data class Cat(val c: Category) : Tile
    data object More : Tile
    data object Less : Tile
}

private fun amountStyle(length: Int) = TextStyle(
    fontFamily = Jakarta,
    fontWeight = FontWeight.ExtraBold,
    fontSize = when {
        length <= 6 -> 54.sp
        length <= 8 -> 46.sp
        length <= 10 -> 40.sp
        else -> 34.sp
    },
    letterSpacing = (-1.5).sp,
    fontFeatureSettings = "tnum",
)

/**
 * The amount + category + keypad form. Used by Quick Add (mode Add) and by the edit sheet,
 * which passes [onDelete].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseForm(
    ui: FormUi,
    actions: FormActions,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    coinMotion: CoinMotion = CoinMotion.Wobble,
) {
    val s = ui.state
    var noteFocused by remember { mutableStateOf(false) }
    var dateMenu by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val view = LocalView.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Pill(onClick = { dateMenu = true }, description = "Date: ${ui.dateLabel}. Tap to change.") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineIcon(Glyph.Clock, Palette.TextSoft, size = 16.dp)
                        Spacer(Modifier.width(7.dp))
                        val label = if (s.date == ui.today && s.keepTime == null) "Today · now" else ui.dateLabel
                        Text(label, style = Type.Label, color = Palette.Text)
                    }
                }
                DropdownMenu(
                    expanded = dateMenu,
                    onDismissRequest = { dateMenu = false },
                    containerColor = Palette.CardHi,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    DropdownMenuItem(text = { Text("Today", style = Type.Body) }, onClick = { actions.setDate(ui.today); dateMenu = false })
                    DropdownMenuItem(text = { Text("Yesterday", style = Type.Body) }, onClick = { actions.setDate(ui.today.minusDays(1)); dateMenu = false })
                    DropdownMenuItem(text = { Text("Pick a date…", style = Type.Body) }, onClick = { dateMenu = false; datePicker = true })
                }
            }
            Spacer(Modifier.weight(1f))
            if (onDelete != null) {
                Pill(onClick = onDelete, description = "Delete expense") {
                    LineIcon(Glyph.Trash, Palette.Danger, size = 18.dp)
                }
                Spacer(Modifier.width(8.dp))
            }
            Pill(onClick = { view.tick(); actions.cyclePayment() }, description = "Paid with ${s.payment.label}. Tap to change.") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Wallet, Palette.TextSoft, size = 16.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(s.payment.label, style = Type.Label.copy(fontWeight = FontWeight.Bold), color = Palette.Text)
                    Spacer(Modifier.width(4.dp))
                    LineIcon(Glyph.ChevronDown, Palette.TextDim, size = 14.dp)
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.clearFocus() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Coin3D(42.dp, motion = coinMotion)
                Spacer(Modifier.width(12.dp))
                Text(
                    s.amount.display,
                    style = amountStyle(s.amount.display.length),
                    color = if (s.amount.isEmpty) Palette.TextFaint else Palette.Text,
                    maxLines = 1,
                    modifier = Modifier.semantics { contentDescription = "Amount ₹${s.amount.display}" },
                )
                Caret()
            }
            if (ui.line.text.isNotEmpty()) {
                Text(ui.line.text, style = Type.Label, color = if (ui.line.warn) Palette.Warn else Palette.TextDim)
            }
            Spacer(Modifier.height(8.dp))
            NoteField(
                value = s.note,
                onChange = actions::note,
                onFocus = { noteFocused = it },
                onDone = { focus.clearFocus() },
            )
        }

        val tiles: List<Tile> = if (s.showAllCategories) {
            ui.all.map { Tile.Cat(it) } + Tile.Less
        } else {
            ui.quick.map { Tile.Cat(it) } + Tile.More
        }
        Grid(tiles, columns = 4, gap = 8.dp) { tile, m ->
            when (tile) {
                is Tile.Cat -> CategoryTile(
                    label = tile.c.shortName,
                    art = CategoryArt.of(tile.c.id),
                    color = tile.c.uiColor(),
                    selected = tile.c.id == s.categoryId,
                    onClick = { view.tick(); actions.pick(tile.c.id) },
                    modifier = m,
                )
                Tile.More -> CategoryTile("More", CategoryArt.More, Palette.TextSoft, false, { actions.toggleAll() }, m, dashed = true)
                Tile.Less -> CategoryTile("Back", CategoryArt.More, Palette.TextSoft, false, { actions.toggleAll() }, m, dashed = true)
            }
        }

        AnimatedVisibility(
            visible = !noteFocused && !s.showAllCategories,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Keypad(onKey = actions::key)
        }

        Button3D(
            text = ui.submitLabel,
            onClick = { actions.submit() },
            enabled = s.ready,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (datePicker) {
        val todayUtc = ui.today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = s.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            },
        )
        val colors = DatePickerDefaults.colors(
            containerColor = Palette.CardHi,
            selectedDayContainerColor = Palette.Accent,
            selectedDayContentColor = Palette.OnAccent,
            todayDateBorderColor = Palette.Accent,
            todayContentColor = Palette.Accent,
        )
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { actions.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    datePicker = false
                }) { Text("Use date", color = Palette.Accent, style = Type.Label) }
            },
            dismissButton = {
                TextButton(onClick = { datePicker = false }) { Text("Cancel", color = Palette.TextDim, style = Type.Label) }
            },
            colors = colors,
        ) {
            DatePicker(state = state, colors = colors)
        }
    }
}

@Composable
private fun Caret() {
    val t = rememberInfiniteTransition(label = "caret")
    val a by t.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 1000
            },
        ),
        label = "blink",
    )
    Box(
        Modifier
            .padding(start = 4.dp)
            .size(width = 3.dp, height = 42.dp)
            .graphicsLayer { alpha = a }
            .clip(RoundedCornerShape(2.dp))
            .background(Palette.Accent),
    )
}

@Composable
private fun NoteField(value: String, onChange: (String) -> Unit, onFocus: (Boolean) -> Unit, onDone: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Border, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(Glyph.Pencil, Palette.TextDim, size = 14.dp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(190.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("Add a note (optional)", style = Type.Body, color = Color(0xFF8B90A0))
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Type.Body.copy(color = Palette.Text),
                cursorBrush = SolidColor(Palette.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocus(it.isFocused) }
                    .semantics { contentDescription = "Note" },
            )
        }
    }
}
