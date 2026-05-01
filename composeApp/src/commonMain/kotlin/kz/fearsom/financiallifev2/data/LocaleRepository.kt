package kz.fearsom.financiallifev2.data

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.i18n.deviceLocale

class LocaleRepository(
    private val secureStorage: SecureStorage
) {
    fun restoreLocale(): String {
        val locale = secureStorage.get(KEY_SELECTED_LOCALE)
            ?.let(::normalize)
            ?: normalize(deviceLocale())

        Strings.currentLocale = locale
        return locale
    }

    fun setLocale(locale: String): String {
        val normalized = normalize(locale)
        Strings.currentLocale = normalized
        secureStorage.save(KEY_SELECTED_LOCALE, normalized)
        return normalized
    }

    private fun normalize(locale: String): String {
        val language = locale.substringBefore('-').substringBefore('_').lowercase()
        return if (language in SUPPORTED_LOCALES) language else DEFAULT_LOCALE
    }

    companion object {
        private const val KEY_SELECTED_LOCALE = "selected_locale"
        const val DEFAULT_LOCALE = "ru"
        val SUPPORTED_LOCALES = setOf("ru", "kk", "en")
    }
}
