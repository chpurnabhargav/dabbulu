package app.dabbulu.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.dabbulu.InsightsUi
import app.dabbulu.domain.CategoryStat
import app.dabbulu.domain.Dates
import app.dabbulu.domain.DefaultCategories
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Insights
import app.dabbulu.domain.Money
import app.dabbulu.domain.PaymentMethod
import app.dabbulu.domain.Period
import app.dabbulu.domain.PeriodKind
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.BarChart
import app.dabbulu.ui.components.CategoryIcon
import app.dabbulu.ui.components.ChangeChip
import app.dabbulu.ui.components.Donut
import app.dabbulu.ui.components.ExpenseRow
import app.dabbulu.ui.components.LegendRow
import app.dabbulu.ui.components.Meter
import app.dabbulu.ui.components.Panel
import app.dabbulu.ui.components.PeriodSelector
import app.dabbulu.ui.components.SplitBar
import app.dabbulu.ui.components.uiColor
import app.dabbulu.ui.home.EmptyState
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type

private val NeedColor = Color(0xFF60A5FA)
private val WantColor = Color(0xFFF59E0B)
private val PaymentColors = mapOf(
    PaymentMethod.UPI to Palette.Accent,
    PaymentMethod.CASH to Color(0xFFF5B94A),
    PaymentMethod.CARD to Color(0xFFA78BFA),
)

private fun pct(part: Long, whole: Long) = if (whole <= 0) "0%" else "${(part * 100 / whole)}%"

@Composable
fun InsightsScreen(
    ui: InsightsUi,
    contentPadding: PaddingValues,
    onKind: (PeriodKind) -> Unit,
    onStep: (Period) -> Unit,
    onEdit: (Expense) -> Unit,
) {
    val d = ui.data
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Insights", style = Type.H1, color = Palette.Text, modifier = Modifier.padding(top = 6.dp)) }
        item { PeriodSelector(d.period, ui.today, onKind, onStep) }
        if (d.isEmpty) {
            item {
                EmptyState(
                    "Nothing spent in ${d.period.label(ui.today).lowercase().replaceFirstChar { it.uppercase() }}",
                    if (d.previousTotal > 0) "You spent ${Money.format(d.previousTotal)} in ${d.previous.label(ui.today)}." else "Expenses you add will show up here.",
                )
            }
            return@LazyColumn
        }
        item { Summary(d, ui) }
        item {
            Panel {
                Text(
                    when (d.period.kind) {
                        PeriodKind.Day -> "By time of day"
                        PeriodKind.Week, PeriodKind.Month -> "Day by day"
                        PeriodKind.Year -> "Month by month"
                    },
                    style = Type.Title, color = Palette.Text,
                )
                Spacer(Modifier.height(10.dp))
                BarChart(d.bars)
            }
        }
        item { Categories(d, ui) }
        if (d.savedIn.isNotEmpty()) item { Changes("Where you saved", "Less than ${d.previous.label(ui.today)}", d.savedIn, ui, good = true) }
        if (d.spentMoreIn.isNotEmpty()) item { Changes("Where you spent more", "More than ${d.previous.label(ui.today)}", d.spentMoreIn, ui, good = false) }
        item { NeedsWants(d) }
        if (d.byPayment.isNotEmpty()) item { Payments(d) }
        if (d.leaks.isNotEmpty()) item { Leaks(d, ui) }
        d.weekdays?.let { w ->
            if (w.bars.any { it.amount > 0 }) {
                item {
                    Panel {
                        Text("Average by weekday", style = Type.Title, color = Palette.Text)
                        Spacer(Modifier.height(10.dp))
                        BarChart(w.bars, height = 110.dp, color = Color(0xFF60A5FA))
                        Spacer(Modifier.height(12.dp))
                        val ratio = if (w.weekdayAverage > 0) w.weekendAverage.toFloat() / w.weekdayAverage else 0f
                        Text(
                            "Weekends ${Money.format(w.weekendAverage)}/day · weekdays ${Money.format(w.weekdayAverage)}/day" +
                                if (ratio >= 1.2f) " — weekends cost ${"%.1f".format(ratio)}×" else "",
                            style = Type.Small, color = Palette.TextSoft,
                        )
                    }
                }
            }
        }
        d.biggest?.let { e ->
            item {
                Column {
                    Text("Biggest expense", style = Type.Label, color = Palette.TextDim, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
                    Panel(padding = 2.dp) {
                        ExpenseRow(
                            e, ui.categories[e.categoryId] ?: DefaultCategories.fallback,
                            Dates.relative(e.localDate(), ui.today) + " · " + Dates.time(e.localTime()),
                            onClick = { onEdit(e) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(d: Insights, ui: InsightsUi) {
    Panel(padding = 20.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Spent", style = Type.Label, color = Palette.TextDim)
                Spacer(Modifier.height(2.dp))
                Text(Money.format(d.total), style = Type.Hero, color = Palette.Text, maxLines = 1)
            }
            if (d.previousTotal > 0) ChangeChip(d.total, d.previousTotal, Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${d.count} ${if (d.count == 1) "expense" else "expenses"} · ${Money.format(d.dailyAverage)}/day" +
                if (d.previousTotal > 0) " · ${d.previous.label(ui.today)}: ${Money.format(d.previousTotal)}" else "",
            style = Type.Small, color = Palette.TextDim,
        )
        d.budget?.let { budget ->
            Spacer(Modifier.height(16.dp))
            val over = d.total > budget
            Meter(d.total.toFloat() / budget, if (over) Palette.Warn else Palette.Accent)
            Spacer(Modifier.height(8.dp))
            Text(
                if (over) "${Money.format(d.total - budget)} over the ${Money.format(budget)} budget" else "${Money.format(budget - d.total)} under the ${Money.format(budget)} budget",
                style = Type.Label, color = if (over) Palette.Warn else Palette.Good,
            )
            d.projected?.let { p ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "At this pace: about ${Money.format(p)} by ${Dates.relative(d.period.end.minusDays(1), ui.today)}" +
                        if (p > budget) " (${Money.format(p - budget)} over)" else "",
                    style = Type.Small, color = if (p > budget) Palette.Warn else Palette.TextSoft,
                )
            }
        }
        if (d.budget == null) {
            d.projected?.let { p ->
                Spacer(Modifier.height(8.dp))
                Text("At this pace: about ${Money.format(p)} by ${Dates.relative(d.period.end.minusDays(1), ui.today)}", style = Type.Small, color = Palette.TextSoft)
            }
        }
        d.income?.let { income ->
            Spacer(Modifier.height(8.dp))
            val saved = income - d.total
            Text(
                if (saved >= 0) "Saved ${Money.format(saved)} · ${pct(saved, income)} of income" else "Spent ${Money.format(-saved)} more than your income",
                style = Type.Label, color = if (saved >= 0) Palette.Good else Palette.Warn,
            )
        }
    }
}

@Composable
private fun Categories(d: Insights, ui: InsightsUi) {
    val top = d.categories.take(5)
    val rest = d.total - top.sumOf { it.total }
    val slices = top.map { (ui.categories[it.categoryId]?.uiColor() ?: Palette.TextDim) to it.total } +
        if (rest > 0) listOf(Palette.TextFaint to rest) else emptyList()
    Panel {
        Text("Where it went", style = Type.Title, color = Palette.Text)
        Spacer(Modifier.height(12.dp))
        Donut(slices, Modifier.fillMaxWidth().height(176.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Money.format(d.total), style = Type.H2, color = Palette.Text)
                Text("${d.categories.size} categories", style = Type.Tiny, color = Palette.TextDim)
            }
        }
        Spacer(Modifier.height(16.dp))
        d.categories.forEachIndexed { i, s ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            val c = ui.categories[s.categoryId] ?: DefaultCategories.fallback
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(c.id, c.uiColor(), size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, style = Type.Label, color = Palette.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Money.format(s.total), style = Type.Num, color = Palette.Text)
                    }
                    Spacer(Modifier.height(6.dp))
                    Meter(s.share, c.uiColor(), height = 5.dp)
                    Spacer(Modifier.height(5.dp))
                    Row {
                        Text("${s.count}× · ${(s.share * 100).toInt()}%", style = Type.Tiny, color = Palette.TextDim, modifier = Modifier.weight(1f))
                        if (d.previousTotal > 0 && s.change != 0L) {
                            Text(
                                (if (s.change > 0) "↑ " else "↓ ") + Money.format(kotlin.math.abs(s.change)),
                                style = Type.Tiny,
                                color = if (s.change > 0) Palette.Warn else Palette.Good,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Changes(title: String, subtitle: String, list: List<CategoryStat>, ui: InsightsUi, good: Boolean) {
    val color = if (good) Palette.Good else Palette.Warn
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { LineIcon(if (good) Glyph.TrendDown else Glyph.TrendUp, color, size = 16.dp, strokeWidth = 2.2f) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = Type.Title, color = Palette.Text)
                Text(subtitle, style = Type.Tiny, color = Palette.TextDim)
            }
        }
        Spacer(Modifier.height(14.dp))
        list.forEachIndexed { i, s ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            val c = ui.categories[s.categoryId] ?: DefaultCategories.fallback
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(c.id, c.uiColor(), size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = Type.Label, color = Palette.Text)
                    Text("${Money.format(s.previous)} → ${Money.format(s.total)}", style = Type.Tiny, color = Palette.TextDim)
                }
                Text((if (good) "−" else "+") + Money.format(kotlin.math.abs(s.change)), style = Type.Num, color = color)
            }
        }
    }
}

@Composable
private fun NeedsWants(d: Insights) {
    Panel {
        Text("Needs vs wants", style = Type.Title, color = Palette.Text)
        Spacer(Modifier.height(12.dp))
        SplitBar(listOf(NeedColor to d.needs, WantColor to d.wants))
        Spacer(Modifier.height(14.dp))
        LegendRow(NeedColor, "Needs", Money.format(d.needs), pct(d.needs, d.total))
        Spacer(Modifier.height(8.dp))
        LegendRow(WantColor, "Wants", Money.format(d.wants), pct(d.wants, d.total))
        if (d.wants > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Wants are the easiest place to save. Change need/want on any expense by tapping it.", style = Type.Small, color = Palette.TextDim)
        }
    }
}

@Composable
private fun Payments(d: Insights) {
    Panel {
        Text("How you paid", style = Type.Title, color = Palette.Text)
        Spacer(Modifier.height(12.dp))
        SplitBar(d.byPayment.map { (PaymentColors[it.first] ?: Palette.TextDim) to it.second })
        Spacer(Modifier.height(14.dp))
        d.byPayment.forEachIndexed { i, (m, v) ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            LegendRow(PaymentColors[m] ?: Palette.TextDim, m.label, Money.format(v), pct(v, d.total))
        }
    }
}

@Composable
private fun Leaks(d: Insights, ui: InsightsUi) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Palette.Warn.copy(alpha = 0.14f))
                    .border(1.dp, Palette.Warn.copy(alpha = 0.2f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) { LineIcon(Glyph.Leak, Palette.Warn, size = 16.dp) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Money leaks", style = Type.Title, color = Palette.Text)
                Text("Small spends that keep repeating", style = Type.Tiny, color = Palette.TextDim)
            }
        }
        Spacer(Modifier.height(14.dp))
        d.leaks.forEachIndexed { i, l ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            val c = ui.categories[l.categoryId] ?: DefaultCategories.fallback
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(c.id, c.uiColor(), size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.title, style = Type.Label, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${l.count} times · about ${Money.format(l.total / l.count)} each", style = Type.Tiny, color = Palette.TextDim)
                }
                Text(Money.format(l.total), style = Type.Num, color = Palette.Text)
            }
        }
    }
}
