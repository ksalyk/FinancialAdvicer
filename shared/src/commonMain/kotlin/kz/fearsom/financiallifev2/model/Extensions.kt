package kz.fearsom.financiallifev2.model

import kz.fearsom.financiallifev2.i18n.Strings

private fun CurrencyCode.suffix() = when (this) {
    CurrencyCode.KZT -> "₸"
    CurrencyCode.RUB -> Strings.currencyRub
    CurrencyCode.USD -> "$"
}

private fun Long.compactStr(divisor: Long): String {
    val whole = this / divisor
    val dec   = (this % divisor) * 10 / divisor
    return if (dec == 0L) "$whole" else "$whole.$dec"
}

fun Long.moneyFormat(currency: CurrencyCode = CurrencyCode.KZT): String {
    val s = currency.suffix()
    return when {
        currency == CurrencyCode.USD && this >= 1_000_000L -> "${compactStr(1_000_000L)}M $s"
        currency == CurrencyCode.USD && this >= 1_000L     -> "${compactStr(1_000L)}k $s"
        currency == CurrencyCode.USD                       -> "$this $s"
        this >= 1_000_000L                                 -> "${compactStr(1_000_000L)}M $s"
        this >= 1_000L                                     -> "${compactStr(1_000L)}k $s"
        else                                               -> "$this $s"
    }
}
