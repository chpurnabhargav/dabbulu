package app.dabbulu.domain

enum class PaymentMethod(val label: String) {
    UPI("UPI"),
    CASH("Cash"),
    CARD("Card");

    fun next(): PaymentMethod = entries[(ordinal + 1) % entries.size]

    companion object {
        fun from(name: String): PaymentMethod = entries.firstOrNull { it.name == name } ?: UPI
    }
}

data class Category(
    val id: String,
    val name: String,
    val shortName: String,
    /** 0xAARRGGBB */
    val color: Long,
    /** Default need/want for expenses in this category. */
    val isWant: Boolean,
    val sortOrder: Int,
    /** Optional monthly limit for this category, in paise. */
    val budget: Long? = null,
)

object DefaultCategories {
    val all: List<Category> = listOf(
        Category("food", "Food & Dining", "Food", 0xFFFB923C, isWant = true, sortOrder = 0),
        Category("snacks", "Snacks & Chai", "Snacks", 0xFFE0A96D, isWant = true, sortOrder = 1),
        Category("transport", "Transport", "Transport", 0xFF60A5FA, isWant = false, sortOrder = 2),
        Category("groceries", "Groceries", "Groceries", 0xFF4ADE80, isWant = false, sortOrder = 3),
        Category("shopping", "Shopping", "Shopping", 0xFFF472B6, isWant = true, sortOrder = 4),
        Category("bills", "Bills & Recharge", "Bills", 0xFFFACC15, isWant = false, sortOrder = 5),
        Category("family", "Family & Friends", "Family", 0xFFFB7185, isWant = false, sortOrder = 6),
        Category("rent", "Rent", "Rent", 0xFFA78BFA, isWant = false, sortOrder = 7),
        Category("subscriptions", "Subscriptions", "Subs", 0xFFF87171, isWant = true, sortOrder = 8),
        Category("entertainment", "Entertainment", "Fun", 0xFFC084FC, isWant = true, sortOrder = 9),
        Category("health", "Health", "Health", 0xFF2DD4BF, isWant = false, sortOrder = 10),
        Category("personal", "Personal Care", "Care", 0xFFF9A8D4, isWant = false, sortOrder = 11),
        Category("other", "Other", "Other", 0xFF94A3B8, isWant = false, sortOrder = 12),
    )

    val fallback: Category = all.last()
}

/**
 * The 7 categories Quick Add shows before "More": the most used in recent weeks first,
 * then default order. A selected category that isn't in the top 7 takes the last slot.
 */
fun quickPicks(all: List<Category>, uses: Map<String, Int>, selectedId: String?, count: Int = 7): List<Category> {
    val ranked = all.sortedWith(compareByDescending<Category> { uses[it.id] ?: 0 }.thenBy { it.sortOrder })
    val top = ranked.take(count)
    val selected = all.firstOrNull { it.id == selectedId }
    return if (selected == null || selected in top) top else top.take(count - 1) + selected
}
