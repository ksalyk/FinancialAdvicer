package kz.fearsom.financiallifev2.model

import kz.fearsom.financiallifev2.i18n.Strings

private fun CurrencyCode.suffix() = when (this) {
    CurrencyCode.KZT -> "₸"
    CurrencyCode.RUB -> Strings.currencyRub
    CurrencyCode.USD -> "$"
}

fun Long.moneyFormat(currency: CurrencyCode = CurrencyCode.KZT): String = when {
    currency == CurrencyCode.USD && this >= 1_000_000L -> "${this / 1_000_000L}M ${currency.suffix()}"
    currency == CurrencyCode.USD && this >= 1_000L -> "${this / 1_000L}k ${currency.suffix()}"
    currency == CurrencyCode.USD -> "$this ${currency.suffix()}"
    this >= 1_000_000L -> "${this / 1_000_000L} М ${currency.suffix()}"
    this >= 1_000L -> "${this / 1_000L}к ${currency.suffix()}"
    else -> "$this ${currency.suffix()}"
}
