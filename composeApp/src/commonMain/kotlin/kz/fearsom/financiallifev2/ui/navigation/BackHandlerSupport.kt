package kz.fearsom.financiallifev2.ui.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
