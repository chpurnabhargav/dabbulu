package app.dabbulu.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class CategoryStat(val categoryId: String, val total: Long, val count: Int, val previous: Long, val share: Float) {
    val change: Long get() = total - previous
}

data class BarPoint(val label: String, val amount: Long, val highlight: Boolean = false, val caption: String = "")

/** Small spends that repeat and quietly add up (chai, snacks, cigarettes, auto rides…). */
data class Leak(val title: String, val categoryId: String, val count: Int, val total: Long)

data class WeekdayPattern(val bars: List<BarPoint>, val weekdayAverage: Long, val weekendAverage: Long)

data class Insights(
    val period: Period,
    val previous: Period,
    val total: Long,
    val previousTotal: Long,
    val count: Int,
    val dailyAverage: Long,
    val bars: List<BarPoint>,
    val peak: BarPoint?,
    val categories: List<CategoryStat>,
    val savedIn: List<CategoryStat>,
    val spentMoreIn: List<CategoryStat>,
    val needs: Long,
    val wants: Long,
    val byPayment: List<Pair<PaymentMethod, Long>>,
    val leaks: List<Leak>,
    val weekdays: WeekdayPattern?,
    val biggest: Expense?,
    /** Budget for this period (month: the monthly budget, year: 12×), null when not set or not meaningful. */
    val budget: Long?,
    /** Where the current month is heading at today's pace. */
    val projected: Long?,
    /** Income for this period (month: monthly income, year: 12×), null when not set. */
    val income: Long?,
) {
    val isEmpty: Boolean get() = count == 0
}

object InsightsCalc {

    private const val LEAK_MAX = 300_00L
    private const val LEAK_MIN_COUNT = 3

    fun compute(
        all: List<Expense>,
        period: Period,
        today: LocalDate,
        categories: Map<String, Category>,
        monthlyBudget: Long,
        monthlyIncome: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Insights {
        val prevPeriod = period.previous()
        val withDates = all.map { it to it.localDate(zone) }
        val cur = withDates.filter { it.second in period }
        val prev = withDates.filter { it.second in prevPeriod }.map { it.first }
        val items = cur.map { it.first }
        val total = items.sumOf { it.amount }
        val prevTotal = prev.sumOf { it.amount }

        val bars = bars(period, cur, zone)
        val peak = bars.filter { it.amount > 0 }.maxByOrNull { it.amount }

        val curByCat = items.groupBy { it.categoryId }
        val prevByCat = prev.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val stats = (curByCat.keys + prevByCat.keys).map { id ->
            val list = curByCat[id].orEmpty()
            val t = list.sumOf { it.amount }
            CategoryStat(id, t, list.size, prevByCat[id] ?: 0L, if (total > 0) t.toFloat() / total else 0f)
        }
        val categoriesNow = stats.filter { it.total > 0 }.sortedByDescending { it.total }
        val hasPrev = prevTotal > 0
        val savedIn = if (hasPrev) stats.filter { it.change < 0 }.sortedBy { it.change }.take(4) else emptyList()
        val spentMore = if (hasPrev) stats.filter { it.change > 0 }.sortedByDescending { it.change }.take(4) else emptyList()

        val wants = items.filter { it.wantOverride ?: categories[it.categoryId]?.isWant ?: false }.sumOf { it.amount }

        val byPayment = PaymentMethod.entries
            .map { m -> m to items.filter { it.payment == m }.sumOf { it.amount } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        val leaks = items
            .filter { it.amount <= LEAK_MAX }
            .groupBy { it.categoryId to it.note.trim().lowercase() }
            .filter { it.value.size >= LEAK_MIN_COUNT }
            .map { (key, list) ->
                val title = list.first().note.trim().ifBlank { categories[key.first]?.name ?: "Other" }
                Leak(title.replaceFirstChar { it.uppercase() }, key.first, list.size, list.sumOf { it.amount })
            }
            .sortedByDescending { it.total }
            .take(3)

        val weekdays = if (period.kind == PeriodKind.Month || period.kind == PeriodKind.Year) weekdayPattern(period, today, cur) else null

        val isMonth = period.kind == PeriodKind.Month
        val isYear = period.kind == PeriodKind.Year
        val budget = when {
            monthlyBudget <= 0 -> null
            isMonth -> monthlyBudget
            isYear -> monthlyBudget * 12
            else -> null
        }
        val income = when {
            monthlyIncome <= 0 -> null
            isMonth -> monthlyIncome
            isYear -> monthlyIncome * 12
            else -> null
        }
        val elapsed = period.elapsedDays(today)
        val projected = if ((isMonth || isYear) && period.isCurrent(today) && total > 0) (total * period.days / elapsed) / 100 * 100 else null

        return Insights(
            period = period,
            previous = prevPeriod,
            total = total,
            previousTotal = prevTotal,
            count = items.size,
            dailyAverage = (total / elapsed) / 100 * 100,
            bars = bars,
            peak = peak,
            categories = categoriesNow,
            savedIn = savedIn,
            spentMoreIn = spentMore,
            needs = total - wants,
            wants = wants,
            byPayment = byPayment,
            leaks = leaks,
            weekdays = weekdays,
            biggest = items.maxByOrNull { it.amount },
            budget = budget,
            projected = projected,
            income = income,
        )
    }

    private fun bars(period: Period, cur: List<Pair<Expense, LocalDate>>, zone: ZoneId): List<BarPoint> {
        fun mark(points: List<BarPoint>): List<BarPoint> {
            val max = points.maxOfOrNull { it.amount } ?: 0L
            return if (max <= 0) points else points.map { it.copy(highlight = it.amount == max) }
        }
        return when (period.kind) {
            PeriodKind.Day -> {
                val labels = listOf("12a", "3a", "6a", "9a", "12p", "3p", "6p", "9p")
                val sums = LongArray(8)
                cur.forEach { (e, _) -> sums[e.localTime(zone).hour / 3] += e.amount }
                mark(labels.mapIndexed { i, l -> BarPoint(l, sums[i], caption = "$l–${labels[(i + 1) % 8]}") })
            }
            PeriodKind.Week -> {
                val byDay = cur.groupBy { it.second }.mapValues { (_, v) -> v.sumOf { it.first.amount } }
                mark((0 until 7).map { i ->
                    val d = period.start.plusDays(i.toLong())
                    BarPoint(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), byDay[d] ?: 0L, caption = Dates.long(d))
                })
            }
            PeriodKind.Month -> {
                val byDay = cur.groupBy { it.second }.mapValues { (_, v) -> v.sumOf { it.first.amount } }
                mark((0 until period.days).map { i ->
                    val d = period.start.plusDays(i.toLong())
                    val label = if (i == 0 || d.dayOfMonth % 5 == 0) d.dayOfMonth.toString() else ""
                    BarPoint(label, byDay[d] ?: 0L, caption = Dates.long(d))
                })
            }
            PeriodKind.Year -> {
                val byMonth = cur.groupBy { it.second.monthValue }.mapValues { (_, v) -> v.sumOf { it.first.amount } }
                mark((1..12).map { m ->
                    val name = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    BarPoint(name.take(1), byMonth[m] ?: 0L, caption = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                })
            }
        }
    }

    private fun weekdayPattern(period: Period, today: LocalDate, cur: List<Pair<Expense, LocalDate>>): WeekdayPattern? {
        val lastDay = if (period.isCurrent(today)) today else period.end.minusDays(1)
        if (lastDay.isBefore(period.start)) return null
        val dayCounts = IntArray(7)
        var d = period.start
        while (!d.isAfter(lastDay)) {
            dayCounts[d.dayOfWeek.value - 1]++
            d = d.plusDays(1)
        }
        val sums = LongArray(7)
        cur.forEach { (e, date) -> sums[date.dayOfWeek.value - 1] += e.amount }
        val averages = (0 until 7).map { if (dayCounts[it] > 0) sums[it] / dayCounts[it] else 0L }
        val bars = (0 until 7).map { i ->
            val dow = DayOfWeek.of(i + 1)
            BarPoint(dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), averages[i], caption = dow.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
        }
        val max = averages.maxOrNull() ?: 0L
        val weekdayDays = dayCounts.take(5).sum()
        val weekendDays = dayCounts.drop(5).sum()
        return WeekdayPattern(
            bars = if (max > 0) bars.map { it.copy(highlight = it.amount == max) } else bars,
            weekdayAverage = if (weekdayDays > 0) sums.take(5).sum() / weekdayDays else 0L,
            weekendAverage = if (weekendDays > 0) sums.drop(5).sum() / weekendDays else 0L,
        )
    }
}

object Csv {
    fun export(expenses: List<Expense>, categories: Map<String, Category>, zone: ZoneId = ZoneId.systemDefault()): String {
        val sb = StringBuilder("Date,Time,Amount,Category,Payment,Type,Note\n")
        expenses.sortedBy { it.timestamp }.forEach { e ->
            val cat = categories[e.categoryId]
            val want = e.wantOverride ?: cat?.isWant ?: false
            sb.append(e.localDate(zone)).append(',')
                .append(e.localTime(zone).withNano(0).withSecond(0)).append(',')
                .append(Money.format(e.amount, symbol = false).replace(",", "")).append(',')
                .append(quote(cat?.name ?: e.categoryId)).append(',')
                .append(e.payment.label).append(',')
                .append(if (want) "Want" else "Need").append(',')
                .append(quote(e.note)).append('\n')
        }
        return sb.toString()
    }

    private fun quote(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
