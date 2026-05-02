package kz.fearsom.financiallifev2.data

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.i18n.getCachedDeviceLocale

class LocaleRepository(
    private val secureStorage: SecureStorage
) {
    private var cachedLocale: String? = null
    private var isInitialized = false

    fun restoreLocale(): String {
        cachedLocale?.takeIf { isInitialized }?.let { return it }

        val locale = secureStorage.get(KEY_SELECTED_LOCALE)
            ?.let(::normalize)
            ?: normalize(getCachedDeviceLocale())

        cachedLocale = locale
        isInitialized = true
        Strings.currentLocale = locale
        return locale
    }

    fun setLocale(locale: String): String {
        val normalized = normalize(locale)
        cachedLocale = normalized
        isInitialized = true
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
