package app.dabbulu.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class PeriodKind(val label: String, val noun: String) {
    Day("Day", "day"),
    Week("Week", "week"),
    Month("Month", "month"),
    Year("Year", "year"),
}

/**
 * A span of days: [start] inclusive to [end] exclusive.
 * Weeks start on Monday. Months follow the budget cycle, which starts on [monthStartDay] (1–28),
 * so someone paid on the 5th can track 5 Nov – 4 Dec as one "month".
 */
data class Period(val kind: PeriodKind, val start: LocalDate, val end: LocalDate, val monthStartDay: Int = 1) {

    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt()

    operator fun contains(d: LocalDate): Boolean = !d.isBefore(start) && d.isBefore(end)

    fun previous(): Period = of(kind, start.minusDays(1), monthStartDay)
    fun next(): Period = of(kind, end, monthStartDay)
    fun isCurrent(today: LocalDate): Boolean = today in this

    /** Days that have happened so far (all of them for a past period, at least 1). */
    fun elapsedDays(today: LocalDate): Int = when {
        today.isBefore(start) -> 1
        today in this -> ChronoUnit.DAYS.between(start, today).toInt() + 1
        else -> days
    }

    fun label(today: LocalDate): String = when (kind) {
        PeriodKind.Day -> {
            val rel = Dates.relative(start, today)
            if (start.year != today.year && rel != "Today" && rel != "Yesterday") "$rel ${start.year}" else rel
        }
        PeriodKind.Week -> if (isCurrent(today)) "This week" else range(today)
        PeriodKind.Month ->
            if (monthStartDay == 1) {
                if (start.year == today.year) start.format(MONTH) else start.format(MONTH_YEAR)
            } else {
                range(today)
            }
        PeriodKind.Year -> start.year.toString()
    }

    private fun range(today: LocalDate): String {
        val last = end.minusDays(1)
        val yearSuffix = if (last.year != today.year) " ${last.year}" else ""
        return if (start.month == last.month) {
            "${start.dayOfMonth} – ${last.dayOfMonth} ${last.format(SHORT_MONTH)}$yearSuffix"
        } else {
            "${start.dayOfMonth} ${start.format(SHORT_MONTH)} – ${last.dayOfMonth} ${last.format(SHORT_MONTH)}$yearSuffix"
        }
    }

    companion object {
        private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
        private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        private val SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

        fun of(kind: PeriodKind, anchor: LocalDate, monthStartDay: Int = 1): Period {
            val sd = monthStartDay.coerceIn(1, 28)
            return when (kind) {
                PeriodKind.Day -> Period(kind, anchor, anchor.plusDays(1), sd)
                PeriodKind.Week -> {
                    val s = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    Period(kind, s, s.plusDays(7), sd)
                }
                PeriodKind.Month -> {
                    val s = if (anchor.dayOfMonth >= sd) anchor.withDayOfMonth(sd) else anchor.minusMonths(1).withDayOfMonth(sd)
                    Period(kind, s, s.plusMonths(1), sd)
                }
                PeriodKind.Year -> {
                    val s = anchor.withDayOfYear(1)
                    Period(kind, s, s.plusYears(1), sd)
                }
            }
        }
    }
}
