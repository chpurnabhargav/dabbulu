package app.dabbulu.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import app.dabbulu.domain.Category
import app.dabbulu.domain.DefaultCategories
import app.dabbulu.domain.Expense
import app.dabbulu.domain.PaymentMethod
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val color: Long,
    val isWant: Boolean,
    val sortOrder: Int,
    val monthlyBudget: Long? = null,
    val archived: Boolean = false,
) {
    fun toDomain() = Category(id, name, shortName, color, isWant, sortOrder)
}

@Entity(tableName = "expenses", indices = [Index("timestamp"), Index("categoryId")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val categoryId: String,
    val note: String,
    val payment: String,
    val timestamp: Long,
    val wantOverride: Boolean? = null,
    val createdAt: Long,
) {
    fun toDomain() = Expense(id, amount, categoryId, note, PaymentMethod.from(payment), timestamp, wantOverride)

    companion object {
        fun from(e: Expense, createdAt: Long) = ExpenseEntity(
            id = e.id,
            amount = e.amount,
            categoryId = e.categoryId,
            note = e.note,
            payment = e.payment.name,
            timestamp = e.timestamp,
            wantOverride = e.wantOverride,
            createdAt = createdAt,
        )
    }
}

data class CategoryUse(val categoryId: String, val uses: Int)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder")
    fun observeAll(): Flow<List<CategoryEntity>>
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(e: ExpenseEntity): Long

    @Update
    suspend fun update(e: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE timestamp >= :start AND timestamp < :end")
    fun observeSum(start: Long, end: Long): Flow<Long>

    @Query("SELECT categoryId, COUNT(*) AS uses FROM expenses WHERE timestamp >= :since GROUP BY categoryId")
    fun observeUses(since: Long): Flow<List<CategoryUse>>
}

@Database(entities = [CategoryEntity::class, ExpenseEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categories(): CategoryDao
    abstract fun expenses(): ExpenseDao

    companion object {
        fun build(context: Context, name: String? = "dabbulu.db"): AppDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, name)
            }
            return builder.addCallback(SeedCategories).build()
        }
    }
}

/** Puts the default categories in when the database is first created. */
private object SeedCategories : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultCategories.all.forEach { c ->
            db.execSQL(
                "INSERT INTO categories (id, name, shortName, color, isWant, sortOrder, monthlyBudget, archived) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NULL, 0)",
                arrayOf<Any>(c.id, c.name, c.shortName, c.color, if (c.isWant) 1 else 0, c.sortOrder),
            )
        }
    }
}
