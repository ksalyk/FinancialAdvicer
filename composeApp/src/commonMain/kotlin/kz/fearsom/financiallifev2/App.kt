package kz.fearsom.financiallifev2

import androidx.compose.runtime.Composable
import kz.fearsom.financiallifev2.ui.navigation.AppNavigation
import kz.fearsom.financiallifev2.ui.theme.FinanceLifeLineThemeWithDynamicColors

/**
 * Root Composable — entry point for BOTH Android and iOS.
 *
 * Android: called from MainActivity.setContent { App() }
 * iOS:     called from MainViewController via ComposeUIViewController { App() }
 *
 * Features:
 * - Automatic light/dark theme detection from system settings
 * - Material You dynamic colors (Android 12+)
 * - Static colors fallback for older devices and iOS
 */
@Composable
fun App() {
    FinanceLifeLineThemeWithDynamicColors(useDynamicColors = false) {
        AppNavigation()
    }
}
