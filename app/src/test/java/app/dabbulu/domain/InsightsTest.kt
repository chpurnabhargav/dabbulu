package app.dabbulu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PeriodTest {
    private val today = LocalDate.of(2026, 11, 19) // Thursday

    @Test fun weekStartsMonday() {
        val w = Period.of(PeriodKind.Week, today)
        assertEquals(LocalDate.of(2026, 11, 16), w.start)
        assertEquals(LocalDate.of(2026, 11, 23), w.end)
        assertEquals("This week", w.label(today))
        assertEquals("9 – 15 Nov", w.previous().label(today))
    }

    @Test fun calendarMonth() {
        val m = Period.of(PeriodKind.Month, today)
        assertEquals(LocalDate.of(2026, 11, 1), m.start)
        assertEquals(30, m.days)
        assertEquals("November", m.label(today))
        assertEquals("October", m.previous().label(today))
        assertEquals("December 2025", Period.of(PeriodKind.Month, LocalDate.of(2025, 12, 3)).label(today))
    }

    @Test fun salaryCycleMonth() {
        val m = Period.of(PeriodKind.Month, today, monthStartDay = 5)
        assertEquals(LocalDate.of(2026, 11, 5), m.start)
        assertEquals(LocalDate.of(2026, 12, 5), m.end)
        assertEquals("5 Nov – 4 Dec", m.label(today))
        val early = Period.of(PeriodKind.Month, LocalDate.of(2026, 11, 3), monthStartDay = 5)
        assertEquals(LocalDate.of(2026, 10, 5), early.start)
    }

    @Test fun stepping() {
        val d = Period.of(PeriodKind.Day, today)
        assertEquals("Yesterday", d.previous().label(today))
        assertEquals(today.plusDays(1), d.next().start)
        assertEquals("2025", Period.of(PeriodKind.Year, today).previous().label(today))
    }

    @Test fun elapsedDays() {
        val m = Period.of(PeriodKind.Month, today)
        assertEquals(19, m.elapsedDays(today))
        assertEquals(31, m.previous().elapsedDays(today))
    }

    @Test fun allowanceFollowsSalaryCycle() {
        // Cycle 5 Nov – 4 Dec: from the 19th there are 16 days left including today.
        val cycle = Period.of(PeriodKind.Month, today, 5)
        val s = Allowance.stats(today, 16_000_00, 0, 0, cycle)
        assertEquals(16, s.budget!!.daysLeft)
        assertEquals(1_000_00, s.budget!!.todayAllowance)
    }
}

class InsightsCalcTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 19)
    private val cats = DefaultCategories.all.associateBy { it.id }
    private var nextId = 1L

    private fun e(date: LocalDate, amount: Long, cat: String, note: String = "", pay: PaymentMethod = PaymentMethod.UPI, hour: Int = 12) = Expense(
        id = nextId++, amount = amount, categoryId = cat, note = note, payment = pay,
        timestamp = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli(),
    )

    private val data = listOf(
        // October
        e(LocalDate.of(2026, 10, 3), 3_000_00, "shopping"),
        e(LocalDate.of(2026, 10, 10), 1_000_00, "food"),
        // November
        e(LocalDate.of(2026, 11, 2), 500_00, "shopping", pay = PaymentMethod.CARD),
        e(LocalDate.of(2026, 11, 4), 1_500_00, "food"),
        e(LocalDate.of(2026, 11, 5), 40_00, "snacks", "Chai", PaymentMethod.CASH),
        e(LocalDate.of(2026, 11, 6), 40_00, "snacks", "chai ", PaymentMethod.CASH),
        e(LocalDate.of(2026, 11, 7), 40_00, "snacks", "Chai", PaymentMethod.CASH),
        e(LocalDate.of(2026, 11, 19), 2_000_00, "groceries", hour = 19),
    )

    private fun month() = InsightsCalc.compute(data, Period.of(PeriodKind.Month, today), today, cats, monthlyBudget = 10_000_00, monthlyIncome = 50_000_00, zone = zone)

    @Test fun totalsAndComparison() {
        val i = month()
        assertEquals(4_120_00, i.total)
        assertEquals(4_000_00, i.previousTotal)
        assertEquals(6, i.count)
        assertEquals((4_120_00L / 19) / 100 * 100, i.dailyAverage)
    }

    @Test fun categoriesSortedByAmount() {
        assertEquals(listOf("groceries", "food", "shopping", "snacks"), month().categories.map { it.categoryId })
    }

    @Test fun savedAndSpentMore() {
        val i = month()
        assertEquals("shopping", i.savedIn.first().categoryId)
        assertEquals(-2_500_00, i.savedIn.first().change)
        assertEquals(listOf("groceries", "food", "snacks"), i.spentMoreIn.map { it.categoryId })
    }

    @Test fun findsRepeatingSmallSpends() {
        val leak = month().leaks.single()
        assertEquals("Chai", leak.title)
        assertEquals(3, leak.count)
        assertEquals(120_00, leak.total)
    }

    @Test fun needsWantsAndPayments() {
        val i = month()
        // food, snacks, shopping are wants by default
        assertEquals(2_120_00, i.wants)
        assertEquals(2_000_00, i.needs)
        assertEquals(PaymentMethod.UPI, i.byPayment.first().first)
    }

    @Test fun budgetIncomeAndProjection() {
        val i = month()
        assertEquals(10_000_00L, i.budget)
        assertEquals(50_000_00L, i.income)
        assertEquals((4_120_00L * 30 / 19) / 100 * 100, i.projected)
    }

    @Test fun barsCoverEveryDay() {
        val bars = month().bars
        assertEquals(30, bars.size)
        assertTrue(bars[18].highlight) // the 19th has the biggest spend
        assertEquals("1", bars[0].label)
        assertEquals("", bars[1].label)
    }

    @Test fun dayViewBucketsByTime() {
        val i = InsightsCalc.compute(data, Period.of(PeriodKind.Day, today), today, cats, 0, 0, zone)
        assertEquals(8, i.bars.size)
        assertEquals(2_000_00, i.bars[6].amount) // 6–9 pm
        assertNull(i.budget)
        assertNull(i.weekdays)
    }

    @Test fun weekdayPatternForMonth() {
        assertNotNull(month().weekdays)
    }

    @Test fun csvExport() {
        val csv = Csv.export(listOf(e(today, 1_250, "food", "Tea, \"special\"")), cats, zone)
        assertEquals("Date,Time,Amount,Category,Payment,Type,Note", csv.lines()[0])
        assertEquals("2026-11-19,12:00,12.50,Food & Dining,UPI,Want,\"Tea, \"\"special\"\"\"", csv.lines()[1])
    }
}
