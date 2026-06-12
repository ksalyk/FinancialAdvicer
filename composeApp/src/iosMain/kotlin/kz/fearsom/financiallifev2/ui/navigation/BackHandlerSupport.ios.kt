package kz.fearsom.financiallifev2.ui.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS handles back navigation natively via swipe gesture; no system back key exists.
}
