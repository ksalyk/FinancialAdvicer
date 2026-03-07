package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.runtime.Composable

@Composable
expect fun FinanceLifeLineThemeWithDynamicColors(
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit
)
