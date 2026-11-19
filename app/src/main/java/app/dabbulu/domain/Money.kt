package app.dabbulu.domain

import kotlin.math.abs

/** All amounts in the app are whole paise (₹12.50 == 1250) so totals never drift. */
object Money {

    /** Indian digit grouping: 1234567 -> "12,34,567". Expects only digits. */
    fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val parts = ArrayDeque<String>()
        while (rest.length > 2) {
            parts.addFirst(rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.addFirst(rest)
        return parts.joinToString(",") + "," + last3
    }

    /** "₹1,25,000" or "₹12.50". Paise are shown only when they are not zero. */
    fun format(paise: Long, symbol: Boolean = true): String {
        val a = abs(paise)
        val rupees = a / 100
        val p = a % 100
        val body = groupIndian(rupees.toString()) + if (p != 0L) "." + p.toString().padStart(2, '0') else ""
        return (if (paise < 0) "-" else "") + (if (symbol) "₹" else "") + body
    }
}
