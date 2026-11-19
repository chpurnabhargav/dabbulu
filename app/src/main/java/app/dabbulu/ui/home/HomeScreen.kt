package app.dabbulu.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dabbulu.HomeUi
import app.dabbulu.domain.Category
import app.dabbulu.domain.DayGroup
import app.dabbulu.domain.Dates
import app.dabbulu.domain.DefaultCategories
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Money
import app.dabbulu.domain.TodayStats
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Button3D
import app.dabbulu.ui.components.Card3D
import app.dabbulu.ui.components.Coin3D
import app.dabbulu.ui.components.CoinMotion
import app.dabbulu.ui.components.ExpenseRow
import app.dabbulu.ui.components.Meter
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import java.time.LocalDate

@Composable
fun HomeScreen(
    ui: HomeUi,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onSetBudget: () -> Unit,
    onSeeAll: () -> Unit,
    onEdit: (Expense) -> Unit,
    coinMotion: CoinMotion = CoinMotion.Wobble,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(ui.stats.date, onOpenSettings) }
        item { TodayCard(ui.stats, onSetBudget, coinMotion) }
        item { MonthCard(ui.stats) }
        if (!ui.hasAny) {
            item { EmptyState() }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent", style = Type.H2, color = Palette.Text, modifier = Modifier.weight(1f))
                    Text(
                        "See all",
                        style = Type.Label,
                        color = Palette.Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSeeAll)
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                    )
                }
            }
            items(ui.recent, key = { it.date.toEpochDay() }) { group ->
                DayCard(group, ui.stats.date, ui.categories, onEdit)
            }
        }
    }
}

@Composable
private fun Header(today: LocalDate, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Dates.long(today), style = Type.Label, color = Palette.TextDim)
            Spacer(Modifier.height(2.dp))
            Text("Dabbulu", style = Type.H1, color = Palette.Text)
        }
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF23252D), Color(0xFF1A1C22))))
                .border(1.dp, Palette.Border, CircleShape)
                .clickable(onClick = onOpenSettings)
                .semantics { contentDescription = "Settings" },
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(Glyph.Sliders, Palette.TextSoft, size = 20.dp)
        }
    }
}

@Composable
private fun TodayCard(stats: TodayStats, onSetBudget: () -> Unit, coinMotion: CoinMotion) {
    val b = stats.budget
    Card3D(padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Coin3D(64.dp, motion = coinMotion)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                if (b != null) {
                    val over = b.leftToday < 0
                    Text(if (over) "Over today's limit" else "Left to spend today", style = Type.Label, color = Palette.TextDim)
                    Text(
                        Money.format(if (over) -b.leftToday else b.leftToday),
                        style = Type.Hero,
                        color = if (over) Palette.Warn else Palette.Text,
                        maxLines = 1,
                    )
                    Text(
                        "of ${Money.format(b.todayAllowance)} · ${Money.format(stats.spentToday)} spent",
                        style = Type.Small,
                        color = Palette.TextDim,
                    )
                } else {
                    Text("Spent today", style = Type.Label, color = Palette.TextDim)
                    Text(Money.format(stats.spentToday), style = Type.Hero, color = Palette.Text, maxLines = 1)
                    Text("Set a budget to get a daily limit", style = Type.Small, color = Palette.TextDim)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (b != null) {
            val used = if (b.todayAllowance <= 0) 1f else (b.todayAllowance - b.leftToday).toFloat() / b.todayAllowance
            Meter(used, if (b.leftToday < 0) Palette.Warn else Palette.Accent)
        } else {
            Button3D("Set monthly budget", onClick = onSetBudget, modifier = Modifier.fillMaxWidth(), height = 50.dp, glyph = Glyph.Target)
        }
    }
}

@Composable
private fun MonthCard(stats: TodayStats) {
    val b = stats.budget
    Card3D {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Dates.monthName(stats.date), style = Type.Title, color = Palette.Text, modifier = Modifier.weight(1f))
            if (b != null) {
                Text(if (b.daysLeft == 1) "Last day" else "${b.daysLeft} days left", style = Type.Small, color = Palette.TextDim)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Money.format(stats.spentThisMonth), style = Type.H2, color = Palette.Text)
            Spacer(Modifier.width(6.dp))
            Text(
                if (b != null) "of ${Money.format(b.monthlyBudget)}" else "spent this month",
                style = Type.Label,
                color = Palette.TextDim,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (b != null) {
            Spacer(Modifier.height(12.dp))
            val over = b.monthLeft < 0
            Meter(stats.spentThisMonth.toFloat() / b.monthlyBudget, if (over) Palette.Warn else Palette.Accent, height = 8.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                if (over) "${Money.format(-b.monthLeft)} over this month's budget" else "${Money.format(b.monthLeft)} left this month",
                style = Type.Small,
                color = if (over) Palette.Warn else Palette.TextSoft,
            )
        }
    }
}

@Composable
fun DayCard(group: DayGroup, today: LocalDate, categories: Map<String, Category>, onEdit: (Expense) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
            Text(Dates.relative(group.date, today), style = Type.Label, color = Palette.TextDim, modifier = Modifier.weight(1f))
            Text(Money.format(group.total), style = Type.Label, color = Palette.TextDim)
        }
        Card3D(padding = 4.dp) {
            group.items.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = Palette.Border.copy(alpha = 0.6f))
                ExpenseRow(
                    expense = e,
                    category = categories[e.categoryId] ?: DefaultCategories.fallback,
                    timeLabel = Dates.time(e.localTime()),
                    onClick = { onEdit(e) },
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Coin3D(88.dp, motion = CoinMotion.Still)
        Spacer(Modifier.height(14.dp))
        Text("No expenses yet", style = Type.H2, color = Palette.Text)
        Spacer(Modifier.height(6.dp))
        Text(
            "Double tap the back of your phone, or tap the + button below, to add your first one.",
            style = Type.Body,
            color = Palette.TextDim,
            textAlign = TextAlign.Center,
        )
    }
}
