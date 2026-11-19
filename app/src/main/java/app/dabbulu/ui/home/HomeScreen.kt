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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dabbulu.BudgetUse
import app.dabbulu.HomeUi
import app.dabbulu.domain.Category
import app.dabbulu.domain.DayGroup
import app.dabbulu.domain.Dates
import app.dabbulu.domain.DefaultCategories
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Money
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.CategoryIcon
import app.dabbulu.ui.components.ExpenseRow
import app.dabbulu.ui.components.Meter
import app.dabbulu.ui.components.Panel
import app.dabbulu.ui.components.Ring
import app.dabbulu.ui.components.SectionTitle
import app.dabbulu.ui.components.SoftButton
import app.dabbulu.ui.components.StatTile
import app.dabbulu.ui.components.uiColor
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import java.time.LocalDate

@Composable
fun HomeScreen(
    ui: HomeUi,
    contentPadding: PaddingValues,
    onSetBudget: () -> Unit,
    onSeeAll: () -> Unit,
    onInsights: () -> Unit,
    onEdit: (Expense) -> Unit,
) {
    val today = ui.stats.date
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                Text(Dates.long(today), style = Type.Label, color = Palette.TextDim)
                Spacer(Modifier.height(2.dp))
                Text("Dabbulu", style = Type.H1, color = Palette.Text)
            }
        }
        item { TodayCard(ui, onSetBudget) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("This week", Money.format(ui.weekTotal), Modifier.weight(1f))
                StatTile(if (ui.stats.cycle.monthStartDay == 1) "This month" else "This cycle", Money.format(ui.stats.spentThisMonth), Modifier.weight(1f))
                StatTile("Daily avg", Money.format(ui.dailyAverage), Modifier.weight(1f))
            }
        }
        item { MonthCard(ui, onInsights) }
        if (ui.categoryBudgets.isNotEmpty()) {
            item { SectionTitle("Category budgets", Modifier.padding(top = 6.dp)) }
            item { CategoryBudgets(ui.categoryBudgets) }
        }
        if (!ui.hasAny) {
            item { EmptyState() }
        } else {
            item { SectionTitle("Recent", Modifier.padding(top = 6.dp), action = "See all", onAction = onSeeAll) }
            items(ui.recent, key = { it.date.toEpochDay() }) { group ->
                DayCard(group, today, ui.categories, onEdit)
            }
        }
    }
}

@Composable
private fun TodayCard(ui: HomeUi, onSetBudget: () -> Unit) {
    val stats = ui.stats
    val b = stats.budget
    Panel(padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (b != null) {
                    val over = b.leftToday < 0
                    Text(if (over) "Over today's limit" else "Left to spend today", style = Type.Label, color = Palette.TextDim)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Money.format(if (over) -b.leftToday else b.leftToday),
                        style = Type.Hero,
                        color = if (over) Palette.Warn else Palette.Text,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${Money.format(stats.spentToday)} spent of ${Money.format(b.todayAllowance)}",
                        style = Type.Small,
                        color = Palette.TextDim,
                    )
                } else {
                    Text("Spent today", style = Type.Label, color = Palette.TextDim)
                    Spacer(Modifier.height(4.dp))
                    Text(Money.format(stats.spentToday), style = Type.Hero, color = Palette.Text, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text("Set a budget to get a daily limit", style = Type.Small, color = Palette.TextDim)
                }
            }
            if (b != null) {
                val used = if (b.todayAllowance <= 0) 1f else ((b.todayAllowance - b.leftToday).toFloat() / b.todayAllowance)
                val over = b.leftToday < 0
                Ring(used, if (over) Palette.Warn else Palette.Accent, Modifier.size(84.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(used.coerceAtMost(9.99f) * 100).toInt()}%", style = Type.Title, color = Palette.Text)
                        Text("used", style = Type.Tiny, color = Palette.TextDim)
                    }
                }
            }
        }
        if (b == null) {
            Spacer(Modifier.height(16.dp))
            SoftButton("Set a monthly budget", onClick = onSetBudget, glyph = Glyph.Target, color = Palette.Accent, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MonthCard(ui: HomeUi, onInsights: () -> Unit) {
    val stats = ui.stats
    val b = stats.budget
    val today = stats.date
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stats.cycle.label(today), style = Type.Title, color = Palette.Text, modifier = Modifier.weight(1f))
            val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, stats.cycle.end).toInt()
            Text(if (daysLeft <= 1) "Last day" else "$daysLeft days left", style = Type.Small, color = Palette.TextDim)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Money.format(stats.spentThisMonth), style = Type.Amount, color = Palette.Text)
            Spacer(Modifier.width(8.dp))
            Text(
                if (b != null) "of ${Money.format(b.monthlyBudget)}" else "spent",
                style = Type.Label,
                color = Palette.TextDim,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (b != null) {
            Spacer(Modifier.height(12.dp))
            val over = b.monthLeft < 0
            Meter(stats.spentThisMonth.toFloat() / b.monthlyBudget, if (over) Palette.Warn else Palette.Accent)
            Spacer(Modifier.height(10.dp))
            Text(
                if (over) "${Money.format(-b.monthLeft)} over budget" else "${Money.format(b.monthLeft)} left · resets ${Dates.relative(stats.cycle.end, today)}",
                style = Type.Small,
                color = if (over) Palette.Warn else Palette.TextSoft,
            )
        }
        if (ui.monthlyIncome > 0) {
            Spacer(Modifier.height(8.dp))
            val saved = ui.monthlyIncome - stats.spentThisMonth
            Text(
                if (saved >= 0) "Saving ${Money.format(saved)} of your ${Money.format(ui.monthlyIncome)} income so far" else "${Money.format(-saved)} more than your income",
                style = Type.Small,
                color = if (saved >= 0) Palette.Good else Palette.Warn,
            )
        }
        if (ui.lastCycleTotal > 0) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Palette.Border)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ui.lastCycleLabel, style = Type.Small, color = Palette.TextDim)
                    val budget = ui.monthlyBudget
                    val verdict = when {
                        budget <= 0 -> Money.format(ui.lastCycleTotal) + " spent"
                        ui.lastCycleTotal <= budget -> Money.format(ui.lastCycleTotal) + " · saved " + Money.format(budget - ui.lastCycleTotal)
                        else -> Money.format(ui.lastCycleTotal) + " · " + Money.format(ui.lastCycleTotal - budget) + " over"
                    }
                    Text(verdict, style = Type.Label, color = Palette.TextSoft)
                }
                Text(
                    "Insights",
                    style = Type.Label,
                    color = Palette.Accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onInsights)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgets(list: List<BudgetUse>) {
    Panel(padding = 16.dp) {
        list.forEachIndexed { i, u ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            val over = u.spent > u.budget
            val warn = u.fraction >= 0.8f
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(u.category.id, u.category.uiColor(), size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(u.category.name, style = Type.Label, color = Palette.Text, modifier = Modifier.weight(1f))
                        Text(
                            "${Money.format(u.spent)} / ${Money.format(u.budget)}",
                            style = Type.Small,
                            color = if (over) Palette.Warn else Palette.TextDim,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Meter(u.fraction, if (warn) Palette.Warn else u.category.uiColor(), height = 6.dp)
                }
            }
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
        Panel(padding = 2.dp) {
            group.items.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 68.dp, end = 14.dp), color = Palette.Border)
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
fun EmptyState(title: String = "No expenses yet", body: String = "Double tap the back of your phone, or tap + below, to add your first one.") {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Palette.Accent.copy(alpha = 0.12f))
                .border(1.dp, Palette.Accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { LineIcon(Glyph.Wallet, Palette.Accent, size = 28.dp) }
        Spacer(Modifier.height(14.dp))
        Text(title, style = Type.H2, color = Palette.Text)
        Spacer(Modifier.height(6.dp))
        Text(body, style = Type.Body, color = Palette.TextDim, textAlign = TextAlign.Center)
    }
}
