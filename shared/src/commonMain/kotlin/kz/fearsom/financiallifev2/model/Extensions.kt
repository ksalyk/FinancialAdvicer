package kz.fearsom.financiallifev2.model

/**
 * Format Long monetary values in Tenge (KZT) for display.
 * Examples:
 *   200_000L → "200к тг"
 *   1_500_000L → "1.5М тг"
 *   500L → "500 тг"
 */
fun Long.moneyFormat(): String = when {
    this >= 1_000_000L -> {
        val a = this / 1_000_000L
        "$a М тг"
    }
    this >= 1_000L -> {
        "${this / 1_000}к тг"
    }
    else -> "$this тг"
}
