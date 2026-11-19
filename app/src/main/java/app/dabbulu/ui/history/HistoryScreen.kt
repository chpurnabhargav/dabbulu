package app.dabbulu.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dabbulu.HistoryUi
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Money
import app.dabbulu.ui.art.ArtIcon
import app.dabbulu.ui.art.CategoryArt
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.uiColor
import app.dabbulu.ui.home.DayCard
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type

@Composable
fun HistoryScreen(
    ui: HistoryUi,
    contentPadding: PaddingValues,
    onQuery: (String) -> Unit,
    onFilter: (String?) -> Unit,
    onEdit: (Expense) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("History", style = Type.H1, color = Palette.Text, modifier = Modifier.padding(top = 8.dp))
        }
        item { SearchField(ui.query, onQuery) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip("All", null, Palette.Accent, ui.filter == null) { onFilter(null) } }
                items(ui.categories, key = { it.id }) { c ->
                    FilterChip(c.shortName, c.id, c.uiColor(), ui.filter == c.id) { onFilter(if (ui.filter == c.id) null else c.id) }
                }
            }
        }
        item {
            Text(
                "${ui.count} ${if (ui.count == 1) "expense" else "expenses"} · ${Money.format(ui.total)}",
                style = Type.Label,
                color = Palette.TextDim,
            )
        }
        if (ui.groups.isEmpty()) {
            item {
                Text(
                    if (ui.query.isBlank() && ui.filter == null) "Nothing here yet." else "No expenses match.",
                    style = Type.Body,
                    color = Palette.TextDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                )
            }
        }
        items(ui.groups, key = { it.date.toEpochDay() }) { group ->
            DayCard(group, ui.today, ui.byId, onEdit)
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Border, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(Glyph.Search, Palette.TextDim, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("Search notes, categories, amounts", style = Type.Body, color = Color(0xFF8B90A0))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Type.Body.copy(color = Palette.Text),
                cursorBrush = SolidColor(Palette.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search" },
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChange("") }
                    .padding(10.dp)
                    .semantics { contentDescription = "Clear search" },
            ) { LineIcon(Glyph.Close, Palette.TextDim, size = 16.dp) }
        }
    }
}

@Composable
private fun FilterChip(label: String, categoryId: String?, color: Color, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.10f)))
                } else {
                    Brush.verticalGradient(listOf(Palette.CardHi, Palette.CardLo))
                },
            )
            .border(1.dp, if (selected) color else Palette.Border, shape)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = if (categoryId != null) 8.dp else 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (categoryId != null) {
            ArtIcon(CategoryArt.of(categoryId), 26.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = Type.Label, color = if (selected) Palette.Text else Palette.TextSoft)
    }
}
