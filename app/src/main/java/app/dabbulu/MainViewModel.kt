package app.dabbulu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.dabbulu.data.AppSettings
import app.dabbulu.data.ExpenseRepository
import app.dabbulu.domain.Category
import app.dabbulu.domain.Csv
import app.dabbulu.domain.DayGroup
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Insights
import app.dabbulu.domain.InsightsCalc
import app.dabbulu.domain.Money
import app.dabbulu.domain.Period
import app.dabbulu.domain.PeriodKind
import app.dabbulu.domain.TodayStats
import app.dabbulu.domain.groupByDay
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.form.FormController
import app.dabbulu.ui.form.FormMode
import app.dabbulu.ui.form.FormState
import app.dabbulu.ui.form.FormUi
import app.dabbulu.ui.form.actions
import app.dabbulu.ui.form.buildFormUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class BudgetUse(val category: Category, val spent: Long, val budget: Long) {
    val fraction: Float get() = if (budget <= 0) 0f else spent.toFloat() / budget
}

data class HomeUi(
    val stats: TodayStats,
    val recent: List<DayGroup>,
    val categories: Map<String, Category>,
    val hasAny: Boolean,
    val weekTotal: Long,
    val dailyAverage: Long,
    val lastCycleTotal: Long,
    val lastCycleLabel: String,
    val monthlyBudget: Long,
    val monthlyIncome: Long,
    val categoryBudgets: List<BudgetUse>,
)

data class InsightsUi(val data: Insights, val today: LocalDate, val categories: Map<String, Category>)

data class HistoryUi(
    val groups: List<DayGroup>,
    val count: Int,
    val total: Long,
    val categories: List<Category>,
    val byId: Map<String, Category>,
    val query: String,
    val filter: String?,
    val today: LocalDate,
    /** null = all time. */
    val period: Period?,
    val monthStartDay: Int,
)

data class SettingsUi(val settings: AppSettings, val categories: List<Category>, val categorySpent: Map<String, Long>)

sealed interface MainEvent {
    data class Deleted(val expense: Expense, val label: String) : MainEvent
    data class Message(val text: String) : MainEvent
}

class MainViewModel(private val repo: ExpenseRepository) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<String?>(null)
    private val historyPeriod = MutableStateFlow<Period?>(null)
    private val insightsPeriod = MutableStateFlow<Period?>(null)
    private val editing = MutableStateFlow<Expense?>(null)
    private val editForm = FormController(FormState(date = LocalDate.now()))
    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val editActions: FormActions = editForm.actions(::saveEdit)

    private fun cycleSpend(all: List<Expense>, cycle: Period): Map<String, Long> =
        all.filter { it.localDate(zone) in cycle }.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }

    val home: StateFlow<HomeUi?> = combine(repo.todayStats, repo.expenses, repo.categories, repo.settings) { stats, all, cats, settings ->
        val today = stats.date
        val week = Period.of(PeriodKind.Week, today, settings.monthStartDay)
        val last = stats.cycle.previous()
        val byCycle = cycleSpend(all, stats.cycle)
        HomeUi(
            stats = stats,
            recent = groupByDay(all.take(30), zone),
            categories = cats.associateBy { it.id },
            hasAny = all.isNotEmpty(),
            weekTotal = all.filter { it.localDate(zone) in week }.sumOf { it.amount },
            dailyAverage = (stats.spentThisMonth / stats.cycle.elapsedDays(today)) / 100 * 100,
            lastCycleTotal = all.filter { it.localDate(zone) in last }.sumOf { it.amount },
            lastCycleLabel = last.label(today),
            monthlyBudget = settings.monthlyBudget,
            monthlyIncome = settings.monthlyIncome,
            categoryBudgets = cats.mapNotNull { c -> c.budget?.let { BudgetUse(c, byCycle[c.id] ?: 0L, it) } }
                .sortedByDescending { it.fraction },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val insights: StateFlow<InsightsUi?> = combine(repo.expenses, repo.categories, repo.settings, repo.today, insightsPeriod) { all, cats, settings, today, p ->
        val period = p ?: Period.of(PeriodKind.Month, today, settings.monthStartDay)
        val byId = cats.associateBy { it.id }
        InsightsUi(InsightsCalc.compute(all, period, today, byId, settings.monthlyBudget, settings.monthlyIncome, zone), today, byId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val historyInputs = combine(query, filter, historyPeriod) { q, f, p -> Triple(q, f, p) }

    val history: StateFlow<HistoryUi?> = combine(repo.expenses, repo.categories, historyInputs, repo.today, repo.settings) { all, cats, (q, f, p), today, settings ->
        val byId = cats.associateBy { it.id }
        val needle = q.trim().lowercase()
        val digits = needle.filter { it.isDigit() }
        val list = all.filter { e ->
            (f == null || e.categoryId == f) &&
                (p == null || e.localDate(zone) in p) &&
                (
                    needle.isEmpty() ||
                        e.note.lowercase().contains(needle) ||
                        (byId[e.categoryId]?.name?.lowercase()?.contains(needle) == true) ||
                        e.payment.label.lowercase() == needle ||
                        (digits.isNotEmpty() && Money.format(e.amount, symbol = false).replace(",", "").contains(digits))
                    )
        }
        HistoryUi(groupByDay(list, zone), list.size, list.sumOf { it.amount }, cats, byId, q, f, today, p, settings.monthStartDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<SettingsUi?> = combine(repo.settings, repo.categories, repo.todayStats, repo.expenses) { s, cats, stats, all ->
        SettingsUi(s, cats, cycleSpend(all, stats.cycle))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val editUi: StateFlow<FormUi?> = combine(editing, editForm.state, repo.categories, repo.categoryUses, repo.todayStats) { e, s, cats, uses, stats ->
        if (e == null) null else buildFormUi(s, cats, uses, stats, stats.date, FormMode.Edit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // History filters
    fun setQuery(q: String) { query.value = q }
    fun setFilter(id: String?) { filter.value = id }
    fun setHistoryPeriod(p: Period?) { historyPeriod.value = p }

    fun setInsightsPeriod(p: Period) { insightsPeriod.value = p }

    /** Switches Day/Week/Month/Year, keeping roughly the same point in time. */
    fun periodOfKind(current: Period?, kind: PeriodKind, today: LocalDate, startDay: Int): Period {
        val anchor = if (current == null || current.isCurrent(today)) today else current.start
        return Period.of(kind, anchor, startDay)
    }

    // Editing
    fun startEdit(e: Expense) {
        editForm.reset(FormState.from(e))
        editing.value = e
    }

    fun endEdit() { editing.value = null }

    fun canSaveEdit(): Boolean = editing.value != null && editForm.value.ready

    private fun saveEdit() {
        val original = editing.value ?: return
        val s = editForm.value
        if (!s.ready) return
        viewModelScope.launch { repo.update(s.toExpense(id = original.id)) }
    }

    fun deleteEditing() {
        val original = editing.value ?: return
        viewModelScope.launch {
            val removed = repo.delete(original.id) ?: return@launch
            val label = home.value?.categories?.get(removed.categoryId)?.shortName ?: "expense"
            _events.send(MainEvent.Deleted(removed, "Deleted ${Money.format(removed.amount)} · $label"))
        }
    }

    fun restore(e: Expense) {
        viewModelScope.launch { repo.restore(e) }
    }

    // Settings
    fun setBudget(paise: Long) = viewModelScope.launch { repo.setMonthlyBudget(paise) }
    fun setIncome(paise: Long) = viewModelScope.launch { repo.setMonthlyIncome(paise) }
    fun setStartDay(day: Int) = viewModelScope.launch { repo.setMonthStartDay(day) }
    fun setCategoryBudget(id: String, paise: Long?) = viewModelScope.launch { repo.setCategoryBudget(id, paise) }
    fun setReminder(on: Boolean) = viewModelScope.launch { repo.setDailyReminder(on) }

    suspend fun csv(): Pair<String, Int> {
        val all = repo.expenses.first()
        val cats = repo.categories.first().associateBy { it.id }
        return Csv.export(all, cats, zone) to all.size
    }

    fun message(text: String) {
        viewModelScope.launch { _events.send(MainEvent.Message(text)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MainViewModel((this[APPLICATION_KEY] as DabbuluApp).repository) }
        }
    }
}
