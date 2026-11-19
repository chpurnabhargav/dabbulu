package app.dabbulu.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.dabbulu.domain.Expense
import app.dabbulu.domain.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ExpenseRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.build(ctx, name = null)
        repo = ExpenseRepository(db, SettingsRepository(ctx))
    }

    @After
    fun tearDown() = db.close()

    private fun lunch(amount: Long = 250_00) =
        Expense(amount = amount, categoryId = "food", note = "Lunch", payment = PaymentMethod.UPI, timestamp = System.currentTimeMillis())

    @Test
    fun seedsDefaultCategories() = runBlocking {
        val cats = repo.categories.first()
        assertEquals(13, cats.size)
        assertEquals("food", cats.first().id)
    }

    @Test
    fun addEditDeleteRestore() = runBlocking {
        val id = repo.add(lunch())
        assertEquals(listOf(25_000L), repo.expenses.first().map { it.amount })

        repo.update(repo.expenses.first().single().copy(amount = 300_00, payment = PaymentMethod.CASH))
        val edited = repo.expenses.first().single()
        assertEquals(300_00L, edited.amount)
        assertEquals(PaymentMethod.CASH, edited.payment)

        val removed = repo.delete(id)
        assertNotNull(removed)
        assertEquals(0, repo.expenses.first().size)
        assertNull(repo.delete(id))

        repo.restore(removed!!)
        assertEquals(300_00L, repo.expenses.first().single().amount)
    }

    @Test
    fun todayStatsFollowBudgetAndSpending() = runBlocking {
        repo.setMonthlyBudget(30_000_00)
        repo.add(lunch(250_00))
        repo.add(lunch(40_00))
        val stats = repo.todayStats.first { it.spentToday == 290_00L && it.budget != null }
        assertEquals(290_00L, stats.spentThisMonth)
        assertEquals(30_000_00L, stats.budget!!.monthlyBudget)
        assertEquals(2, repo.categoryUses.first()["food"])
    }
}
