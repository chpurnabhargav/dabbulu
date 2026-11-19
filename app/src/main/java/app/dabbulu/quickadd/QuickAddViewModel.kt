package app.dabbulu.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.dabbulu.DabbuluApp
import app.dabbulu.data.ExpenseRepository
import app.dabbulu.domain.PaymentMethod
import app.dabbulu.domain.TodayStats
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.form.FormController
import app.dabbulu.ui.form.FormMode
import app.dabbulu.ui.form.FormState
import app.dabbulu.ui.form.FormUi
import app.dabbulu.ui.form.actions
import app.dabbulu.ui.form.buildFormUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface QuickPhase {
    data object Entry : QuickPhase
    data class Saved(
        val id: Long,
        val amount: Long,
        val categoryName: String,
        val payment: PaymentMethod,
        val forToday: Boolean,
    ) : QuickPhase
}

data class QuickAddUi(val form: FormUi, val phase: QuickPhase, val stats: TodayStats?)

class QuickAddViewModel(private val repo: ExpenseRepository) : ViewModel() {

    private val form = FormController(FormState(date = LocalDate.now()))
    private val phase = MutableStateFlow<QuickPhase>(QuickPhase.Entry)
    private var saving = false

    val actions: FormActions = form.actions(::submit)

    val ui: StateFlow<QuickAddUi?> = combine(
        form.state, repo.categories, repo.categoryUses, repo.todayStats, phase,
    ) { s, cats, uses, stats, ph ->
        QuickAddUi(buildFormUi(s, cats, uses, stats, stats.date, FormMode.Add), ph, stats)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Start over with an empty form (used when the in-app sheet opens again). */
    fun reset() {
        form.reset(FormState(date = LocalDate.now()))
        phase.value = QuickPhase.Entry
    }

    private fun submit() {
        val s = form.value
        val ui = ui.value ?: return
        if (!s.ready || saving) return
        val category = ui.form.all.firstOrNull { it.id == s.categoryId } ?: return
        saving = true
        viewModelScope.launch {
            try {
                val id = repo.add(s.toExpense())
                phase.value = QuickPhase.Saved(
                    id = id,
                    amount = s.amount.paise,
                    categoryName = category.name,
                    payment = s.payment,
                    forToday = s.date == ui.form.today,
                )
            } finally {
                saving = false
            }
        }
    }

    /** Removes what was just saved and goes back to the form with the same values, so it can be fixed. */
    fun undo() {
        val saved = phase.value as? QuickPhase.Saved ?: return
        viewModelScope.launch {
            repo.delete(saved.id)
            phase.value = QuickPhase.Entry
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { QuickAddViewModel((this[APPLICATION_KEY] as DabbuluApp).repository) }
        }
    }
}
