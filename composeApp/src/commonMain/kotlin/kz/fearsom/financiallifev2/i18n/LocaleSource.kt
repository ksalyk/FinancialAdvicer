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

private var _cachedDeviceLocale: String? = null

fun getCachedDeviceLocale(): String {
    val cached = _cachedDeviceLocale
    if (cached != null) return cached

    return deviceLocale().also { _cachedDeviceLocale = it }
}

fun initDeviceLocaleCache() {
    if (_cachedDeviceLocale == null) {
        _cachedDeviceLocale = deviceLocale()
    }
}
