package kz.fearsom.financiallifev2.i18n

/**
 * Returns the BCP-47 language tag of the device's current locale
 * (e.g. "ru", "kk", "en").
 *
 * Implemented per-platform:
 *   - Android: Locale.getDefault().language
 *   - iOS:     NSLocale.currentLocale.languageCode ?: "ru"
 */
expect fun deviceLocale(): String
