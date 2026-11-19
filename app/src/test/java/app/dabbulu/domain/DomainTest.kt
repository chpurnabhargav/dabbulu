package app.dabbulu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MoneyTest {
    @Test fun groupsIndianStyle() {
        assertEquals("0", Money.groupIndian("0"))
        assertEquals("999", Money.groupIndian("999"))
        assertEquals("1,000", Money.groupIndian("1000"))
        assertEquals("12,345", Money.groupIndian("12345"))
        assertEquals("1,25,000", Money.groupIndian("125000"))
        assertEquals("12,34,567", Money.groupIndian("1234567"))
        assertEquals("1,23,45,678", Money.groupIndian("12345678"))
    }

    @Test fun formatsPaise() {
        assertEquals("₹0", Money.format(0))
        assertEquals("₹250", Money.format(25_000))
        assertEquals("₹12.50", Money.format(1_250))
        assertEquals("₹0.05", Money.format(5))
        assertEquals("₹1,25,000", Money.format(12_500_000))
        assertEquals("-₹110", Money.format(-11_000))
        assertEquals("1,000", Money.format(100_000, symbol = false))
    }
}

class AmountInputTest {
    private fun type(s: String): AmountInput = s.fold(AmountInput()) { acc, c ->
        acc.press(
            when (c) {
                '.' -> PadKey.Dot
                '<' -> PadKey.Back
                else -> PadKey.Digit(c)
            },
        )
    }

    @Test fun typesWholeRupees() {
        val a = type("250")
        assertEquals(25_000, a.paise)
        assertEquals("250", a.display)
    }

    @Test fun emptyShowsZero() {
        assertTrue(AmountInput().isEmpty)
        assertEquals("0", AmountInput().display)
        assertEquals(0, AmountInput().paise)
    }

    @Test fun decimalsUpToTwoPlaces() {
        assertEquals(1_250, type("12.5").paise)
        assertEquals("12.5", type("12.5").display)
        assertEquals(1_299, type("12.999").paise)
        assertEquals("12.99", type("12.999").display)
    }

    @Test fun onlyOneDecimalPoint() {
        assertEquals("1.5", type("1..5").display)
    }

    @Test fun dotFirstBecomesZeroPoint() {
        assertEquals("0.", type(".").display)
        assertEquals(50, type(".5").paise)
    }

    @Test fun leadingZeroIsReplaced() {
        assertEquals("7", type("07").display)
    }

    @Test fun backspace() {
        assertEquals("25", type("250<").display)
        assertEquals("0", type("5<").display)
        assertEquals("0", type("<").display)
    }

    @Test fun indianGroupingWhileTyping() {
        assertEquals("1,25,000", type("125000").display)
    }

    @Test fun capsAtSevenDigits() {
        assertEquals("99,99,999", type("999999999").display)
    }

    @Test fun roundTripsFromPaise() {
        assertEquals("250", AmountInput.fromPaise(25_000).raw)
        assertEquals("12.50", AmountInput.fromPaise(1_250).raw)
        assertEquals(1_250, AmountInput.fromPaise(1_250).paise)
        assertTrue(AmountInput.fromPaise(0).isEmpty)
    }
}

class AllowanceTest {
    private val nov19 = LocalDate.of(2026, 11, 19) // 12 days left including today

    @Test fun splitsWhatIsLeftOverRemainingDays() {
        val b = Allowance.compute(monthlyBudget = 20_000_00, spentBeforeToday = 12_320_00, spentToday = 0, today = nov19)
        assertEquals(12, b.daysLeft)
        assertEquals(640_00, b.todayAllowance) // 7,680 / 12
        assertEquals(640_00, b.leftToday)
        assertEquals(7_680_00, b.monthLeft)
    }

    @Test fun spendingTodayReducesLeftToday() {
        val b = Allowance.compute(20_000_00, 12_320_00, 250_00, nov19)
        assertEquals(390_00, b.leftToday)
        assertEquals(7_430_00, b.monthLeft)
    }

    @Test fun roundsDownToWholeRupees() {
        val b = Allowance.compute(10_000_00, 0, 0, LocalDate.of(2026, 11, 1)) // 30 days
        assertEquals(333_00, b.todayAllowance)
    }

    @Test fun overspentMonthGivesZeroAllowance() {
        val b = Allowance.compute(5_000_00, 6_000_00, 100_00, nov19)
        assertEquals(0, b.todayAllowance)
        assertEquals(-100_00, b.leftToday)
        assertEquals(-1_100_00, b.monthLeft)
    }

    @Test fun lastDayOfMonth() {
        val b = Allowance.compute(1_000_00, 900_00, 0, LocalDate.of(2026, 11, 30))
        assertEquals(1, b.daysLeft)
        assertEquals(100_00, b.todayAllowance)
    }

    @Test fun noBudgetMeansNoDayBudget() {
        val s = Allowance.stats(nov19, monthlyBudget = 0, spentBeforeToday = 500_00, spentToday = 120_00)
        assertNull(s.budget)
        assertEquals(620_00, s.spentThisMonth)
    }
}

class BudgetLineTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val withBudget = Allowance.stats(today, 20_000_00, 12_320_00, 0)

    @Test fun showsLeftTodayWhenEmpty() {
        assertEquals(BudgetLine("₹640 left to spend today", false), BudgetLines.forEntry(withBudget, 0, today, "Today"))
    }

    @Test fun previewsWhatsLeftAfterThis() {
        assertEquals(BudgetLine("₹390 left today after this", false), BudgetLines.forEntry(withBudget, 250_00, today, "Today"))
    }

    @Test fun warnsWhenOverToday() {
        assertEquals(BudgetLine("₹110 over today's limit", true), BudgetLines.forEntry(withBudget, 750_00, today, "Today"))
    }

    @Test fun pastDateSaysWhereItGoes() {
        val line = BudgetLines.forEntry(withBudget, 250_00, today.minusDays(1), "Yesterday")
        assertEquals("Adding to Yesterday", line.text)
        assertFalse(line.warn)
    }

    @Test fun noBudgetShowsSpentToday() {
        val s = Allowance.stats(today, 0, 0, 300_00)
        assertEquals("₹300 spent today", BudgetLines.forEntry(s, 0, today, "Today").text)
        assertEquals("₹550 spent today with this", BudgetLines.forEntry(s, 250_00, today, "Today").text)
    }
}

class CategoriesTest {
    private val all = DefaultCategories.all

    @Test fun defaultOrderWithoutHistory() {
        assertEquals(listOf("food", "snacks", "transport", "groceries", "shopping", "bills", "family"), quickPicks(all, emptyMap(), null).map { it.id })
    }

    @Test fun mostUsedComeFirst() {
        val picks = quickPicks(all, mapOf("rent" to 1, "snacks" to 9, "transport" to 4), null)
        assertEquals(listOf("snacks", "transport", "rent", "food", "groceries", "shopping", "bills"), picks.map { it.id })
    }

    @Test fun selectedCategoryAlwaysVisible() {
        val picks = quickPicks(all, emptyMap(), "health")
        assertEquals(7, picks.size)
        assertEquals("health", picks.last().id)
    }

    @Test fun categoryIdsAreUniqueAndHaveArt() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test fun paymentCycles() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.UPI.next())
        assertEquals(PaymentMethod.UPI, PaymentMethod.CARD.next())
        assertEquals(PaymentMethod.UPI, PaymentMethod.from("nonsense"))
    }
}

class GroupingTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(y: Int, m: Int, d: Int, h: Int, amount: Long, id: Long) = Expense(
        id = id, amount = amount, categoryId = "food", note = "", payment = PaymentMethod.UPI,
        timestamp = LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli(),
    )

    @Test fun groupsByLocalDayNewestFirst() {
        val groups = groupByDay(
            listOf(at(2026, 11, 18, 9, 100, 1), at(2026, 11, 19, 8, 200, 2), at(2026, 11, 19, 21, 300, 3)),
            zone,
        )
        assertEquals(listOf(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 18)), groups.map { it.date })
        assertEquals(500, groups[0].total)
        assertEquals(listOf(3L, 2L), groups[0].items.map { it.id })
    }

    @Test fun relativeLabels() {
        val today = LocalDate.of(2026, 11, 19)
        assertEquals("Today", Dates.relative(today, today))
        assertEquals("Yesterday", Dates.relative(today.minusDays(1), today))
        assertEquals("Tue, 17 Nov", Dates.relative(today.minusDays(2), today))
    }
}
