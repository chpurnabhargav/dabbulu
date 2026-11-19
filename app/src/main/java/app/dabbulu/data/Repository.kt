package app.dabbulu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.dabbulu.domain.Allowance
import app.dabbulu.domain.Category
import app.dabbulu.domain.Dates
import app.dabbulu.domain.Expense
import app.dabbulu.domain.TodayStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val budgetKey = longPreferencesKey("monthly_budget_paise")

    /** 0 = no budget set. */
    val monthlyBudget: Flow<Long> = context.settingsStore.data.map { it[budgetKey] ?: 0L }.distinctUntilChanged()

    suspend fun setMonthlyBudget(paise: Long) {
        context.settingsStore.edit { it[budgetKey] = paise.coerceAtLeast(0) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    val categories: Flow<List<Category>> = db.categories().observeAll().map { list -> list.map { it.toDomain() } }

    val expenses: Flow<List<Expense>> = db.expenses().observeAll().map { list -> list.map { it.toDomain() } }

    val monthlyBudget: Flow<Long> get() = settings.monthlyBudget

    /** Emits the current date, and again right after each midnight. */
    val today: Flow<LocalDate> = flow {
        while (true) {
            val now = ZonedDateTime.now(zone())
            emit(now.toLocalDate())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis() + 500)
        }
    }.distinctUntilChanged()

    /** How often each category was used in the last 60 days, for ordering Quick Add. */
    val categoryUses: Flow<Map<String, Int>> = today.flatMapLatest { date ->
        db.expenses().observeUses(Dates.startOfDayMillis(date.minusDays(60), zone()))
            .map { rows -> rows.associate { it.categoryId to it.uses } }
    }

    val todayStats: Flow<TodayStats> = today.flatMapLatest { date ->
        val z = zone()
        val monthStart = Dates.startOfDayMillis(date.withDayOfMonth(1), z)
        val dayStart = Dates.startOfDayMillis(date, z)
        val dayEnd = Dates.startOfDayMillis(date.plusDays(1), z)
        combine(
            settings.monthlyBudget,
            db.expenses().observeSum(monthStart, dayStart),
            db.expenses().observeSum(dayStart, dayEnd),
        ) { budget, before, spentToday -> Allowance.stats(date, budget, before, spentToday) }
    }

    suspend fun add(e: Expense): Long = db.expenses().insert(ExpenseEntity.from(e.copy(id = 0), System.currentTimeMillis()))

    suspend fun update(e: Expense) {
        val existing = db.expenses().get(e.id) ?: return
        db.expenses().update(ExpenseEntity.from(e, existing.createdAt))
    }

    /** Returns what was deleted so it can be restored with [restore]. */
    suspend fun delete(id: Long): Expense? {
        val existing = db.expenses().get(id) ?: return null
        db.expenses().delete(id)
        return existing.toDomain()
    }

    suspend fun restore(e: Expense): Long = db.expenses().insert(ExpenseEntity.from(e, System.currentTimeMillis()))

    suspend fun setMonthlyBudget(paise: Long) = settings.setMonthlyBudget(paise)
}
