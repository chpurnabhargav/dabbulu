package app.dabbulu.ui.form

import app.dabbulu.domain.AmountInput
import app.dabbulu.domain.BudgetLine
import app.dabbulu.domain.BudgetLines
import app.dabbulu.domain.Category
import app.dabbulu.domain.Dates
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Money
import app.dabbulu.domain.PadKey
import app.dabbulu.domain.PaymentMethod
import app.dabbulu.domain.TodayStats
import app.dabbulu.domain.quickPicks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class FormState(
    val amount: AmountInput = AmountInput(),
    val categoryId: String? = null,
    val date: LocalDate,
    val payment: PaymentMethod = PaymentMethod.UPI,
    val note: String = "",
    val showAllCategories: Boolean = false,
    /** For edits: keep the original time of day when the date changes. */
    val keepTime: LocalTime? = null,
    /** null = use the category's need/want default. */
    val wantOverride: Boolean? = null,
) {
    val ready: Boolean get() = amount.paise > 0 && categoryId != null

    fun toExpense(id: Long = 0, now: ZonedDateTime = ZonedDateTime.now()): Expense {
        val timestamp = if (keepTime == null && date == now.toLocalDate()) {
            now.toInstant().toEpochMilli()
        } else {
            Dates.timestampFor(date, now, keepTime)
        }
        return Expense(
            id = id,
            amount = amount.paise,
            categoryId = requireNotNull(categoryId),
            note = note.trim(),
            payment = payment,
            timestamp = timestamp,
            wantOverride = wantOverride,
        )
    }

    companion object {
        fun from(e: Expense) = FormState(
            amount = AmountInput.fromPaise(e.amount),
            categoryId = e.categoryId,
            date = e.localDate(),
            payment = e.payment,
            note = e.note,
            keepTime = e.localTime(),
            wantOverride = e.wantOverride,
        )
    }
}

/** Holds the form while it's being filled in. Shared by Quick Add and the edit sheet. */
class FormController(initial: FormState) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<FormState> = _state.asStateFlow()

    val value: FormState get() = _state.value

    fun key(k: PadKey) = _state.update { it.copy(amount = it.amount.press(k)) }
    fun pick(id: String) = _state.update { it.copy(categoryId = id, showAllCategories = false) }
    fun toggleAll() = _state.update { it.copy(showAllCategories = !it.showAllCategories) }
    fun setDate(d: LocalDate) = _state.update { it.copy(date = d) }
    fun setPayment(p: PaymentMethod) = _state.update { it.copy(payment = p) }
    fun setWant(want: Boolean) = _state.update { it.copy(wantOverride = want) }
    fun note(s: String) = _state.update { it.copy(note = s.take(80)) }
    fun reset(s: FormState) { _state.value = s }
}

/** Everything the form needs to draw itself. */
data class FormUi(
    val state: FormState,
    val today: LocalDate,
    val quick: List<Category>,
    val all: List<Category>,
    val line: BudgetLine,
    val submitLabel: String,
    val isWant: Boolean = false,
) {
    val dateLabel: String get() = when (state.date) {
        today -> "Today"
        else -> Dates.relative(state.date, today)
    }
}

enum class FormMode { Add, Edit }

fun buildFormUi(
    state: FormState,
    categories: List<Category>,
    uses: Map<String, Int>,
    stats: TodayStats?,
    today: LocalDate,
    mode: FormMode,
): FormUi {
    val cat = categories.firstOrNull { it.id == state.categoryId }
    val paise = state.amount.paise
    val verb = if (mode == FormMode.Add) "Add" else "Save"
    val label = when {
        paise <= 0 -> "Enter an amount"
        cat == null -> "Pick a category"
        else -> "$verb ${Money.format(paise)} · ${cat.shortName}"
    }
    val dateLabel = Dates.relative(state.date, today)
    // While editing, the amount is already counted in today's total, so only show the line for new entries.
    val line = if (mode == FormMode.Add) BudgetLines.forEntry(stats, paise, state.date, dateLabel) else BudgetLine("", false)
    return FormUi(
        state = state,
        today = today,
        quick = quickPicks(categories, uses, state.categoryId),
        all = categories,
        line = line,
        submitLabel = label,
        isWant = state.wantOverride ?: cat?.isWant ?: false,
    )
}

interface FormActions {
    fun key(k: PadKey)
    fun pick(id: String)
    fun toggleAll()
    fun setDate(d: LocalDate)
    fun setPayment(p: PaymentMethod)
    fun setWant(want: Boolean)
    fun note(s: String)
    fun submit()
}

fun FormController.actions(onSubmit: () -> Unit): FormActions = object : FormActions {
    override fun key(k: PadKey) = this@actions.key(k)
    override fun pick(id: String) = this@actions.pick(id)
    override fun toggleAll() = this@actions.toggleAll()
    override fun setDate(d: LocalDate) = this@actions.setDate(d)
    override fun setPayment(p: PaymentMethod) = this@actions.setPayment(p)
    override fun setWant(want: Boolean) = this@actions.setWant(want)
    override fun note(s: String) = this@actions.note(s)
    override fun submit() = onSubmit()
}
