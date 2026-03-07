package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.runtime.Composable

@Composable
actual fun FinanceLifeLineThemeWithDynamicColors(
    useDynamicColors: Boolean,
    content: @Composable () -> Unit
) {
    // iOS has no Material You — use static light/dark scheme
    val darkTheme   = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    FinanceLifeLineTheme(
        colorScheme = colorScheme,
        darkTheme   = darkTheme,
        content     = content
    )
}
