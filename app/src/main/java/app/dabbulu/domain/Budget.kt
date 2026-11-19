package app.dabbulu.domain

import java.time.LocalDate

/** Today's numbers. [budget] is null when no monthly budget is set. */
data class TodayStats(
    val date: LocalDate,
    val spentToday: Long,
    val spentThisMonth: Long,
    val budget: DayBudget?,
)

data class DayBudget(
    val monthlyBudget: Long,
    /** What can be spent today: what's left of the month at the start of today, split over the days left. */
    val todayAllowance: Long,
    val leftToday: Long,
    val monthLeft: Long,
    /** Days left in the month, including today. */
    val daysLeft: Int,
)

object Allowance {

    fun compute(monthlyBudget: Long, spentBeforeToday: Long, spentToday: Long, today: LocalDate): DayBudget {
        val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
        val remainingAtStart = monthlyBudget - spentBeforeToday
        // Rounded down to whole rupees so the number reads cleanly.
        val allowance = if (remainingAtStart > 0) (remainingAtStart / daysLeft) / 100 * 100 else 0L
        return DayBudget(
            monthlyBudget = monthlyBudget,
            todayAllowance = allowance,
            leftToday = allowance - spentToday,
            monthLeft = monthlyBudget - spentBeforeToday - spentToday,
            daysLeft = daysLeft,
        )
    }

    fun stats(date: LocalDate, monthlyBudget: Long, spentBeforeToday: Long, spentToday: Long): TodayStats =
        TodayStats(
            date = date,
            spentToday = spentToday,
            spentThisMonth = spentBeforeToday + spentToday,
            budget = if (monthlyBudget > 0) compute(monthlyBudget, spentBeforeToday, spentToday, date) else null,
        )
}

/** The live line under the amount in Quick Add. */
data class BudgetLine(val text: String, val warn: Boolean)

object BudgetLines {

    fun forEntry(stats: TodayStats?, amount: Long, entryDate: LocalDate, dateLabel: String): BudgetLine {
        if (stats == null) return BudgetLine("", false)
        if (entryDate != stats.date) return BudgetLine("Adding to $dateLabel", false)
        val b = stats.budget
        if (b == null) {
            return if (amount > 0) {
                BudgetLine(Money.format(stats.spentToday + amount) + " spent today with this", false)
            } else {
                BudgetLine(Money.format(stats.spentToday) + " spent today", false)
            }
        }
        val left = if (amount > 0) b.leftToday - amount else b.leftToday
        return when {
            left < 0 -> BudgetLine(Money.format(-left) + " over today's limit", true)
            amount > 0 -> BudgetLine(Money.format(left) + " left today after this", false)
            else -> BudgetLine(Money.format(left) + " left to spend today", false)
        }
    }
}
