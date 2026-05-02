package kz.fearsom.financiallifev2.i18n

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun deviceLocale(): String = NSLocale.currentLocale.languageCode
