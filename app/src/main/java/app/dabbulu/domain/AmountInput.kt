package app.dabbulu.domain

/** A key on the Quick Add keypad. */
sealed interface PadKey {
    data class Digit(val d: Char) : PadKey
    data object Dot : PadKey
    data object Back : PadKey
}

/**
 * What the person has typed on the keypad, kept as the raw string ("250", "12.5", "0.")
 * so the display can show exactly what was typed.
 */
data class AmountInput(val raw: String = "") {

    val isEmpty: Boolean get() = raw.isEmpty()

    val paise: Long
        get() {
            if (raw.isEmpty()) return 0
            val whole = raw.substringBefore('.').ifEmpty { "0" }.toLong()
            val frac = if ('.' in raw) raw.substringAfter('.').padEnd(2, '0').take(2).toLong() else 0L
            return whole * 100 + frac
        }

    /** Display text without the ₹ sign: "0", "1,25,000", "12.5". */
    val display: String
        get() {
            if (raw.isEmpty()) return "0"
            val whole = Money.groupIndian(raw.substringBefore('.').ifEmpty { "0" }.toLong().toString())
            return if ('.' in raw) whole + "." + raw.substringAfter('.') else whole
        }

    fun press(key: PadKey): AmountInput = when (key) {
        PadKey.Back -> copy(raw = raw.dropLast(1))
        PadKey.Dot -> if ('.' in raw) this else copy(raw = raw.ifEmpty { "0" } + ".")
        is PadKey.Digit -> typeDigit(key.d)
    }

    private fun typeDigit(d: Char): AmountInput {
        if (!d.isDigit()) return this
        if ('.' in raw) {
            return if (raw.substringAfter('.').length >= 2) this else copy(raw = raw + d)
        }
        if (raw == "0") return copy(raw = d.toString())
        if (raw.trimStart('0').length >= MAX_WHOLE_DIGITS) return this
        return copy(raw = raw + d)
    }

    companion object {
        /** Up to ₹99,99,999. */
        const val MAX_WHOLE_DIGITS = 7

        fun fromPaise(paise: Long): AmountInput {
            if (paise <= 0) return AmountInput()
            val whole = paise / 100
            val frac = paise % 100
            return AmountInput(if (frac == 0L) "$whole" else "$whole." + frac.toString().padStart(2, '0'))
        }
    }
}
