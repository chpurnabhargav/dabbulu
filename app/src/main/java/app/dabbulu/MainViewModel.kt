package app.dabbulu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.dabbulu.data.ExpenseRepository
import app.dabbulu.domain.Category
import app.dabbulu.domain.DayGroup
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Money
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUi(
    val stats: TodayStats,
    val recent: List<DayGroup>,
    val categories: Map<String, Category>,
    val hasAny: Boolean,
)

data class HistoryUi(
    val groups: List<DayGroup>,
    val count: Int,
    val total: Long,
    val categories: List<Category>,
    val byId: Map<String, Category>,
    val query: String,
    val filter: String?,
    val today: LocalDate,
)

sealed interface MainEvent {
    data class Deleted(val expense: Expense, val label: String) : MainEvent
}

class MainViewModel(private val repo: ExpenseRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<String?>(null)
    private val editing = MutableStateFlow<Expense?>(null)
    private val editForm = FormController(FormState(date = LocalDate.now()))
    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val editActions: FormActions = editForm.actions(::saveEdit)

    val home: StateFlow<HomeUi?> = combine(repo.todayStats, repo.expenses, repo.categories) { stats, all, cats ->
        HomeUi(stats, groupByDay(all.take(40)), cats.associateBy { it.id }, all.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<HistoryUi?> = combine(repo.expenses, repo.categories, query, filter, repo.today) { all, cats, q, f, today ->
        val byId = cats.associateBy { it.id }
        val needle = q.trim().lowercase()
        val digits = needle.filter { it.isDigit() }
        val list = all.filter { e ->
            (f == null || e.categoryId == f) && (
                needle.isEmpty() ||
                    e.note.lowercase().contains(needle) ||
                    (byId[e.categoryId]?.name?.lowercase()?.contains(needle) == true) ||
                    (digits.isNotEmpty() && Money.format(e.amount, symbol = false).replace(",", "").contains(digits))
                )
        }
        HistoryUi(groupByDay(list), list.size, list.sumOf { it.amount }, cats, byId, q, f, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val editUi: StateFlow<FormUi?> = combine(editing, editForm.state, repo.categories, repo.categoryUses, repo.todayStats) { e, s, cats, uses, stats ->
        if (e == null) null else buildFormUi(s, cats, uses, stats, stats.date, FormMode.Edit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val monthlyBudget: StateFlow<Long> = repo.monthlyBudget.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setQuery(q: String) { query.value = q }
    fun setFilter(id: String?) { filter.value = id }

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
        viewModelScope.launch {
            repo.update(s.toExpense(id = original.id, wantOverride = original.wantOverride))
        }
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

    fun setBudget(paise: Long) {
        viewModelScope.launch { repo.setMonthlyBudget(paise) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MainViewModel((this[APPLICATION_KEY] as DabbuluApp).repository) }
        }
    }
}
