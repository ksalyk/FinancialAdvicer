package kz.fearsom.financiallifev2.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kz.fearsom.financiallifev2.presentation.NewGameUiState
import kz.fearsom.financiallifev2.ui.screens.EraSelectionScreen

@Preview
@Composable
private fun EraSelectionScreenPreview() {
    EraSelectionScreen(
        uiState = NewGameUiState(),
        isAuthenticated = false,
        onEraSelected = {},
        onLoginRequired = {},
        onBack = {}
    )

}
