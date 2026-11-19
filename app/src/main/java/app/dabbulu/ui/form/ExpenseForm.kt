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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dabbulu.domain.Category
import app.dabbulu.domain.PaymentMethod
import app.dabbulu.ui.art.CategoryGlyphs
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.CategoryTile
import app.dabbulu.ui.components.Grid
import app.dabbulu.ui.components.Keypad
import app.dabbulu.ui.components.Pill
import app.dabbulu.ui.components.Segmented
import app.dabbulu.ui.components.SoftButton
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
    fontWeight = FontWeight.Bold,
    fontSize = when {
        length <= 6 -> 52.sp
        length <= 8 -> 44.sp
        length <= 10 -> 38.sp
        else -> 32.sp
    },
    letterSpacing = (-1.5).sp,
    fontFeatureSettings = "tnum",
)

/**
 * Amount, date, payment method, note, category and keypad.
 * Used by Quick Add, and by the edit sheet (which passes [onDelete] and shows need/want).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseForm(
    ui: FormUi,
    actions: FormActions,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val s = ui.state
    val editing = onDelete != null
    var noteFocused by remember { mutableStateOf(false) }
    var dateMenu by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Pill(onClick = { dateMenu = true }, description = "Date: ${ui.dateLabel}. Tap to change.") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineIcon(Glyph.Calendar, Palette.TextDim, size = 15.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(ui.dateLabel, style = Type.Label, color = Palette.Text)
                        Spacer(Modifier.width(4.dp))
                        LineIcon(Glyph.ChevronDown, Palette.TextFaint, size = 13.dp)
                    }
                }
                DropdownMenu(
                    expanded = dateMenu,
                    onDismissRequest = { dateMenu = false },
                    containerColor = Palette.CardHi,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    DropdownMenuItem(text = { Text("Today", style = Type.Body) }, onClick = { actions.setDate(ui.today); dateMenu = false })
                    DropdownMenuItem(text = { Text("Yesterday", style = Type.Body) }, onClick = { actions.setDate(ui.today.minusDays(1)); dateMenu = false })
                    DropdownMenuItem(text = { Text("Pick a date…", style = Type.Body) }, onClick = { dateMenu = false; datePicker = true })
                }
            }
            Spacer(Modifier.width(10.dp))
            Segmented(
                options = PaymentMethod.entries.map { it.label },
                selected = s.payment.ordinal,
                onSelect = { actions.setPayment(PaymentMethod.entries[it]) },
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.clearFocus() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.height(62.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("₹", style = Type.H1.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold), color = Palette.TextDim)
                Spacer(Modifier.width(6.dp))
                Text(
                    s.amount.display,
                    style = amountStyle(s.amount.display.length),
                    color = if (s.amount.isEmpty) Palette.TextFaint else Palette.Text,
                    maxLines = 1,
                    modifier = Modifier.semantics { contentDescription = "Amount ₹${s.amount.display}" },
                )
                Caret()
            }
            Text(
                ui.line.text.ifEmpty { " " },
                style = Type.Label,
                color = if (ui.line.warn) Palette.Warn else Palette.TextDim,
            )
        }

        NoteField(
            value = s.note,
            onChange = actions::note,
            onFocus = { noteFocused = it },
            onDone = { focus.clearFocus() },
        )

        if (editing) {
            Segmented(
                options = listOf("Need", "Want"),
                selected = if (ui.isWant) 1 else 0,
                onSelect = { actions.setWant(it == 1) },
                modifier = Modifier.fillMaxWidth(),
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
                    glyph = CategoryGlyphs.of(tile.c.id),
                    color = tile.c.uiColor(),
                    selected = tile.c.id == s.categoryId,
                    onClick = { actions.pick(tile.c.id) },
                    modifier = m,
                )
                Tile.More -> CategoryTile("More", Glyph.Grid, Palette.TextSoft, false, { actions.toggleAll() }, m)
                Tile.Less -> CategoryTile("Back", Glyph.ChevronLeft, Palette.TextSoft, false, { actions.toggleAll() }, m)
            }
        }

        AnimatedVisibility(
            visible = !noteFocused && !s.showAllCategories,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Keypad(onKey = actions::key)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            if (onDelete != null) {
                SoftButton("Delete", onClick = onDelete, glyph = Glyph.Trash, color = Palette.Danger, modifier = Modifier.weight(0.42f), height = 54.dp)
            }
            Button3D(
                text = ui.submitLabel,
                onClick = { actions.submit() },
                enabled = s.ready,
                modifier = Modifier.weight(1f),
            )
        }
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
            .size(width = 2.5.dp, height = 40.dp)
            .graphicsLayer { alpha = a }
            .clip(RoundedCornerShape(2.dp))
            .background(Palette.Accent),
    )
}

@Composable
private fun NoteField(value: String, onChange: (String) -> Unit, onFocus: (Boolean) -> Unit, onDone: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Border, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(Glyph.Pencil, Palette.TextDim, size = 15.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("Add a note (optional)", style = Type.Body, color = Palette.TextFaint)
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
