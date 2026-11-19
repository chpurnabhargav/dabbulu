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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.dabbulu.HistoryUi
import app.dabbulu.HomeUi
import app.dabbulu.domain.Allowance
import app.dabbulu.domain.AmountInput
import app.dabbulu.domain.DefaultCategories
import app.dabbulu.domain.Expense
import app.dabbulu.domain.PadKey
import app.dabbulu.domain.PaymentMethod
import app.dabbulu.domain.TodayStats
import app.dabbulu.domain.groupByDay
import app.dabbulu.quickadd.QuickAddSheet
import app.dabbulu.quickadd.QuickAddUi
import app.dabbulu.quickadd.QuickPhase
import app.dabbulu.ui.components.CoinMotion
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.form.FormMode
import app.dabbulu.ui.form.FormState
import app.dabbulu.ui.form.buildFormUi
import app.dabbulu.ui.history.HistoryScreen
import app.dabbulu.ui.home.HomeScreen
import app.dabbulu.ui.settings.BudgetSheet
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
    private val cats = DefaultCategories.all
    private val noop = object : FormActions {
        override fun key(k: PadKey) {}
        override fun pick(id: String) {}
        override fun toggleAll() {}
        override fun setDate(d: LocalDate) {}
        override fun cyclePayment() {}
        override fun note(s: String) {}
        override fun submit() {}
    }

    // ₹20,000 budget, ₹12,320 spent before today => ₹640 for today.
    private fun stats(spentToday: Long = 0) = Allowance.stats(today, 20_000_00, 12_320_00, spentToday)

    private fun at(daysAgo: Long, h: Int, m: Int) =
        today.minusDays(daysAgo).atTime(LocalTime.of(h, m)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val sample = listOf(
        Expense(1, 250_00, "food", "Lunch at office canteen", PaymentMethod.UPI, at(0, 13, 42)),
        Expense(2, 40_00, "snacks", "Chai", PaymentMethod.CASH, at(0, 16, 5)),
        Expense(3, 180_00, "transport", "Auto to station", PaymentMethod.UPI, at(0, 9, 10)),
        Expense(4, 1_249_00, "shopping", "", PaymentMethod.CARD, at(1, 20, 30)),
        Expense(5, 612_50, "groceries", "Vegetables + milk", PaymentMethod.UPI, at(1, 18, 2)),
        Expense(6, 299_00, "subscriptions", "Music", PaymentMethod.CARD, at(2, 8, 0)),
        Expense(7, 2_000_00, "family", "Sent to Amma", PaymentMethod.UPI, at(2, 11, 15)),
    )

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { DabbuluTheme { content() } }
        compose.mainClock.advanceTimeBy(700)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun OtherApp() {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFECEEF2))
                .padding(20.dp),
        ) {
            Spacer(Modifier.height(50.dp))
            repeat(9) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFD5D9E0)))
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    private fun quickUi(state: FormState, phase: QuickPhase = QuickPhase.Entry, s: TodayStats = stats()) =
        QuickAddUi(buildFormUi(state, cats, emptyMap(), s, today, FormMode.Add), phase, s)

    @Test
    fun quickAddEmpty() = shoot("1_quick_add_empty") {
        Box(Modifier.fillMaxSize()) {
            OtherApp()
            QuickAddSheet(quickUi(FormState(date = today)), noop, {}, {}, animateIn = false, autoClose = false)
        }
    }

    @Test
    fun quickAddFilled() = shoot("2_quick_add_filled") {
        Box(Modifier.fillMaxSize()) {
            OtherApp()
            QuickAddSheet(
                quickUi(FormState(amount = AmountInput("250"), categoryId = "food", date = today, note = "Lunch")),
                noop, {}, {}, animateIn = false, autoClose = false,
            )
        }
    }

    @Test
    fun quickAddOverLimit() = shoot("3_quick_add_over_limit") {
        Box(Modifier.fillMaxSize()) {
            OtherApp()
            QuickAddSheet(
                quickUi(FormState(amount = AmountInput("1249"), categoryId = "shopping", date = today, payment = PaymentMethod.CARD), s = stats(470_00)),
                noop, {}, {}, animateIn = false, autoClose = false,
            )
        }
    }

    @Test
    fun quickAddAllCategories() = shoot("4_quick_add_all_categories") {
        Box(Modifier.fillMaxSize()) {
            OtherApp()
            QuickAddSheet(quickUi(FormState(date = today, showAllCategories = true, categoryId = "health")), noop, {}, {}, animateIn = false, autoClose = false)
        }
    }

    @Test
    fun quickAddSaved() = shoot("5_quick_add_saved") {
        Box(Modifier.fillMaxSize()) {
            OtherApp()
            QuickAddSheet(
                quickUi(
                    FormState(amount = AmountInput("250"), categoryId = "food", date = today),
                    QuickPhase.Saved(1, 250_00, "Food & Dining", PaymentMethod.UPI, forToday = true),
                    stats(250_00),
                ),
                noop, {}, {}, animateIn = false, autoClose = false,
            )
        }
    }

    @Test
    fun home() = shoot("6_home") {
        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            HomeScreen(
                HomeUi(stats(470_00), groupByDay(sample), cats.associateBy { it.id }, hasAny = true),
                PaddingValues(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 120.dp),
                {}, {}, {}, {},
                coinMotion = CoinMotion.Still,
            )
        }
    }

    @Test
    fun homeFirstRun() = shoot("7_home_first_run") {
        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            HomeScreen(
                HomeUi(Allowance.stats(today, 0, 0, 0), emptyList(), cats.associateBy { it.id }, hasAny = false),
                PaddingValues(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 120.dp),
                {}, {}, {}, {},
                coinMotion = CoinMotion.Still,
            )
        }
    }

    @Test
    fun history() = shoot("8_history") {
        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            HistoryScreen(
                HistoryUi(groupByDay(sample), sample.size, sample.sumOf { it.amount }, cats, cats.associateBy { it.id }, "", null, today),
                PaddingValues(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 120.dp),
                {}, {}, {},
            )
        }
    }

    @Test
    fun budget() = shoot("9_budget") {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Palette.Bg, Palette.Card)))) {
            BudgetSheet(current = 20_000_00, today = today, onSave = {}, onDismissed = {}, animateIn = false)
        }
    }
}
