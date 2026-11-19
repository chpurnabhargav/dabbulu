package app.dabbulu.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Expense(
    val id: Long = 0,
    val amount: Long,
    val categoryId: String,
    val note: String,
    val payment: PaymentMethod,
    /** Epoch millis of when the money was spent. */
    val timestamp: Long,
    /** null = use the category's default need/want. */
    val wantOverride: Boolean? = null,
) {
    fun localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun localTime(zone: ZoneId = ZoneId.systemDefault()): LocalTime =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()
}

data class DayGroup(val date: LocalDate, val total: Long, val items: List<Expense>)

fun groupByDay(expenses: List<Expense>, zone: ZoneId = ZoneId.systemDefault()): List<DayGroup> =
    expenses
        .sortedByDescending { it.timestamp }
        .groupBy { it.localDate(zone) }
        .map { (date, items) -> DayGroup(date, items.sumOf { it.amount }, items) }

object Dates {
    private val dayMonth = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val fullDay = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val month = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

    fun relative(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(dayMonth)
    }

    fun long(date: LocalDate): String = date.format(fullDay)
    fun time(t: LocalTime): String = t.format(time)
    fun monthName(date: LocalDate): String = date.format(month)

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** When a past date is chosen, keep the current time of day so ordering still feels natural. */
    fun timestampFor(date: LocalDate, now: java.time.ZonedDateTime, keepTime: LocalTime? = null): Long {
        val t = keepTime ?: now.toLocalTime()
        return date.atTime(t).atZone(now.zone).toInstant().toEpochMilli()
    }
}
