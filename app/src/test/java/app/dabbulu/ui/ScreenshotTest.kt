package app.dabbulu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.dabbulu.BudgetUse
import app.dabbulu.HistoryUi
import app.dabbulu.HomeUi
import app.dabbulu.InsightsUi
import app.dabbulu.SettingsUi
import app.dabbulu.data.AppSettings
import app.dabbulu.domain.Allowance
import app.dabbulu.domain.AmountInput
import app.dabbulu.domain.DefaultCategories
import app.dabbulu.domain.Expense
import app.dabbulu.domain.InsightsCalc
import app.dabbulu.domain.PadKey
import app.dabbulu.domain.PaymentMethod
import app.dabbulu.domain.Period
import app.dabbulu.domain.PeriodKind
import app.dabbulu.domain.TodayStats
import app.dabbulu.domain.groupByDay
import app.dabbulu.quickadd.QuickAddSheet
import app.dabbulu.quickadd.QuickAddUi
import app.dabbulu.quickadd.QuickPhase
import app.dabbulu.ui.components.SheetOverlay
import app.dabbulu.ui.form.ExpenseForm
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.form.FormMode
import app.dabbulu.ui.form.FormState
import app.dabbulu.ui.form.buildFormUi
import app.dabbulu.ui.history.HistoryScreen
import app.dabbulu.ui.home.HomeScreen
import app.dabbulu.ui.insights.InsightsScreen
import app.dabbulu.ui.settings.SettingsScreen
import app.dabbulu.ui.theme.DabbuluTheme
import app.dabbulu.ui.theme.Palette
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Renders the real screens with sample data into build/outputs/roborazzi.
 * Run: ./gradlew testDebugUnitTest -Proborazzi.test.record=true
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.now()
    private val cats = DefaultCategories.all.map { if (it.id == "food") it.copy(budget = 6_000_00) else if (it.id == "shopping") it.copy(budget = 3_000_00) else it }
    private val byId = cats.associateBy { it.id }
    private val noop = object : FormActions {
        override fun key(k: PadKey) {}
        override fun pick(id: String) {}
        override fun toggleAll() {}
        override fun setDate(d: LocalDate) {}
        override fun setPayment(p: PaymentMethod) {}
        override fun setWant(want: Boolean) {}
        override fun note(s: String) {}
        override fun submit() {}
    }
    private val pad = PaddingValues(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 40.dp)

    private var id = 1L
    private fun e(daysAgo: Long, h: Int, amount: Long, cat: String, note: String = "", pay: PaymentMethod = PaymentMethod.UPI) = Expense(
        id++, amount, cat, note, pay,
        today.minusDays(daysAgo).atTime(LocalTime.of(h, 15)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    private val sample: List<Expense> = buildList {
        add(e(0, 13, 250_00, "food", "Lunch at office canteen"))
        add(e(0, 16, 40_00, "snacks", "Chai", PaymentMethod.CASH))
        add(e(0, 9, 180_00, "transport", "Auto to station"))
        add(e(1, 20, 1_249_00, "shopping", "", PaymentMethod.CARD))
        add(e(1, 18, 612_50, "groceries", "Vegetables + milk"))
        add(e(1, 16, 40_00, "snacks", "Chai", PaymentMethod.CASH))
        add(e(2, 8, 299_00, "subscriptions", "Music", PaymentMethod.CARD))
        add(e(2, 11, 2_000_00, "family", "Sent to Amma"))
        add(e(3, 16, 40_00, "snacks", "Chai", PaymentMethod.CASH))
        add(e(4, 21, 860_00, "food", "Dinner with friends"))
        add(e(5, 19, 1_450_00, "groceries", "Monthly groceries"))
        add(e(6, 10, 499_00, "health", "Medicines"))
        add(e(8, 22, 1_200_00, "entertainment", "Movie"))
        add(e(10, 13, 320_00, "food", "Biryani"))
        add(e(12, 9, 2_400_00, "bills", "Electricity"))
        add(e(14, 18, 650_00, "transport", "Petrol"))
        // Last month
        add(e(25, 20, 4_800_00, "shopping", "Clothes", PaymentMethod.CARD))
        add(e(28, 13, 2_200_00, "food", "Restaurants"))
        add(e(32, 12, 3_100_00, "groceries"))
        add(e(35, 9, 1_100_00, "transport"))
        add(e(40, 19, 900_00, "entertainment"))
    }

    private val startDay = 1
    private val cycle = Period.of(PeriodKind.Month, today, startDay)
    private fun spent(p: Period) = sample.filter { it.localDate() in p }.sumOf { it.amount }
    private fun stats(): TodayStats {
        val todaySpent = sample.filter { it.localDate() == today }.sumOf { it.amount }
        return Allowance.stats(today, 20_000_00, spent(cycle) - todaySpent, todaySpent, cycle)
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { DabbuluTheme { Box(Modifier.fillMaxSize().background(Palette.Bg)) { content() } } }
        compose.mainClock.advanceTimeBy(1200)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun OtherApp() {
        Column(Modifier.fillMaxSize().background(Color(0xFFECEEF2)).padding(20.dp)) {
            Spacer(Modifier.height(50.dp))
            repeat(9) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFD5D9E0)))
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    private fun quickUi(state: FormState, phase: QuickPhase = QuickPhase.Entry) =
        QuickAddUi(buildFormUi(state, cats, emptyMap(), stats(), today, FormMode.Add), phase, stats())

    @Test fun quickAddEmpty() = shoot("01_quick_add_empty") {
        OtherApp()
        QuickAddSheet(quickUi(FormState(date = today)), noop, {}, {}, animateIn = false, autoClose = false)
    }

    @Test fun quickAddFilled() = shoot("02_quick_add_filled") {
        OtherApp()
        QuickAddSheet(quickUi(FormState(amount = AmountInput("250"), categoryId = "food", date = today, note = "Lunch")), noop, {}, {}, animateIn = false, autoClose = false)
    }

    @Test fun quickAddAll() = shoot("03_quick_add_all_categories") {
        OtherApp()
        QuickAddSheet(quickUi(FormState(amount = AmountInput("1249"), date = today, showAllCategories = true, categoryId = "health", payment = PaymentMethod.CARD)), noop, {}, {}, animateIn = false, autoClose = false)
    }

    @Test fun quickAddSaved() = shoot("04_quick_add_saved") {
        OtherApp()
        QuickAddSheet(
            quickUi(FormState(amount = AmountInput("250"), categoryId = "food", date = today), QuickPhase.Saved(1, 250_00, "Food & Dining", PaymentMethod.UPI, true)),
            noop, {}, {}, animateIn = false, autoClose = false,
        )
    }

    @Test fun home() = shoot("05_home") {
        val s = stats()
        val last = cycle.previous()
        val byCycle = sample.filter { it.localDate() in cycle }.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        HomeScreen(
            HomeUi(
                stats = s,
                recent = groupByDay(sample.take(10)),
                categories = byId,
                hasAny = true,
                weekTotal = spent(Period.of(PeriodKind.Week, today)),
                dailyAverage = s.spentThisMonth / cycle.elapsedDays(today),
                lastCycleTotal = spent(last),
                lastCycleLabel = last.label(today),
                monthlyBudget = 20_000_00,
                monthlyIncome = 60_000_00,
                categoryBudgets = cats.mapNotNull { c -> c.budget?.let { BudgetUse(c, byCycle[c.id] ?: 0L, it) } }.sortedByDescending { it.fraction },
            ),
            pad, {}, {}, {}, {},
        )
    }

    @Test fun homeFirstRun() = shoot("06_home_first_run") {
        val s = Allowance.stats(today, 0, 0, 0, cycle)
        HomeScreen(HomeUi(s, emptyList(), byId, false, 0, 0, 0, "", 0, 0, emptyList()), pad, {}, {}, {}, {})
    }

    private fun insights(kind: PeriodKind) = InsightsUi(
        InsightsCalc.compute(sample, Period.of(kind, today, startDay), today, byId, 20_000_00, 60_000_00),
        today,
        byId,
    )

    @Test fun insightsMonth() = shoot("07_insights_month") {
        InsightsScreen(insights(PeriodKind.Month), pad, {}, {}, {})
    }

    @Test fun insightsWeek() = shoot("08_insights_week") {
        InsightsScreen(insights(PeriodKind.Week), pad, {}, {}, {})
    }

    @Test fun history() = shoot("09_history") {
        val p = Period.of(PeriodKind.Month, today, startDay)
        val list = sample.filter { it.localDate() in p }
        HistoryScreen(
            HistoryUi(groupByDay(list), list.size, list.sumOf { it.amount }, cats, byId, "", null, today, p, startDay),
            pad, {}, {}, {}, {}, {},
        )
    }

    @Test fun settings() = shoot("10_settings") {
        SettingsScreen(
            SettingsUi(AppSettings(20_000_00, 1, 60_000_00, true), cats, emptyMap()),
            "0.2.0", pad, {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun editSheet() = shoot("11_edit_expense") {
        val ui = buildFormUi(FormState.from(sample[0]), cats, emptyMap(), stats(), today, FormMode.Edit)
        SheetOverlay(onDismissed = {}, animateIn = false) { ExpenseForm(ui, noop, onDelete = {}) }
    }
}
