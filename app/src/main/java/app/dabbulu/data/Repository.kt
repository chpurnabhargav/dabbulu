package app.dabbulu.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.dabbulu.domain.Allowance
import app.dabbulu.domain.Category
import app.dabbulu.domain.Dates
import app.dabbulu.domain.Expense
import app.dabbulu.domain.Period
import app.dabbulu.domain.PeriodKind
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

data class AppSettings(
    /** 0 = no budget set. */
    val monthlyBudget: Long = 0,
    /** Day of the month the budget month starts on (1–28). */
    val monthStartDay: Int = 1,
    /** 0 = not set. */
    val monthlyIncome: Long = 0,
    val dailyReminder: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private val budgetKey = longPreferencesKey("monthly_budget_paise")
    private val startDayKey = intPreferencesKey("month_start_day")
    private val incomeKey = longPreferencesKey("monthly_income_paise")
    private val reminderKey = booleanPreferencesKey("daily_reminder")

    val settings: Flow<AppSettings> = context.settingsStore.data.map {
        AppSettings(
            monthlyBudget = it[budgetKey] ?: 0L,
            monthStartDay = (it[startDayKey] ?: 1).coerceIn(1, 28),
            monthlyIncome = it[incomeKey] ?: 0L,
            dailyReminder = it[reminderKey] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun setMonthlyBudget(paise: Long) = context.settingsStore.edit { it[budgetKey] = paise.coerceAtLeast(0) }
    suspend fun setMonthStartDay(day: Int) = context.settingsStore.edit { it[startDayKey] = day.coerceIn(1, 28) }
    suspend fun setMonthlyIncome(paise: Long) = context.settingsStore.edit { it[incomeKey] = paise.coerceAtLeast(0) }
    suspend fun setDailyReminder(on: Boolean) = context.settingsStore.edit { it[reminderKey] = on }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseRepository(
    private val db: AppDatabase,
    private val settingsRepo: SettingsRepository,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    val categories: Flow<List<Category>> = db.categories().observeAll().map { list -> list.map { it.toDomain() } }

    val expenses: Flow<List<Expense>> = db.expenses().observeAll().map { list -> list.map { it.toDomain() } }

    val settings: Flow<AppSettings> get() = settingsRepo.settings

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

    /** Today's spending against the budget month (which starts on the chosen day and resets automatically). */
    val todayStats: Flow<TodayStats> = combine(today, settings.map { it.monthStartDay }.distinctUntilChanged()) { d, sd -> d to sd }
        .flatMapLatest { (date, startDay) ->
            val z = zone()
            val cycle = Period.of(PeriodKind.Month, date, startDay)
            val cycleStart = Dates.startOfDayMillis(cycle.start, z)
            val dayStart = Dates.startOfDayMillis(date, z)
            val dayEnd = Dates.startOfDayMillis(date.plusDays(1), z)
            combine(
                settings.map { it.monthlyBudget }.distinctUntilChanged(),
                db.expenses().observeSum(cycleStart, dayStart),
                db.expenses().observeSum(dayStart, dayEnd),
            ) { budget, before, spentToday -> Allowance.stats(date, budget, before, spentToday, cycle) }
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

    suspend fun setMonthlyBudget(paise: Long) = settingsRepo.setMonthlyBudget(paise)
    suspend fun setMonthStartDay(day: Int) = settingsRepo.setMonthStartDay(day)
    suspend fun setMonthlyIncome(paise: Long) = settingsRepo.setMonthlyIncome(paise)
    suspend fun setDailyReminder(on: Boolean) = settingsRepo.setDailyReminder(on)
    suspend fun setCategoryBudget(id: String, paise: Long?) = db.categories().setBudget(id, paise?.takeIf { it > 0 })
}
