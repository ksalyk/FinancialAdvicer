package kz.fearsom.financiallifev2.ui.screens.character

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.presentation.NewGameUiState
import kz.fearsom.financiallifev2.ui.components.character.PredefinedCharacterCard
import kz.fearsom.financiallifev2.ui.components.core.AppTopBar
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.PurpleAccent

@Composable
fun CharacterSelectionScreen(
    uiState: NewGameUiState,
    onSelectPredefined: (String) -> Unit,   // characterId → sessionId returned from presenter
    onSelectBundle: (String) -> Unit,        // bundleId
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val selectedEra = uiState.eras.find { it.id == uiState.selectedEraId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(PurpleAccent.copy(alpha = 0.08f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = Strings.uiCharSelTitle,
                subtitle = if (selectedEra != null) "${selectedEra.emoji} ${selectedEra.name}" else null,
                onBack = onBack
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.availableCharacters) { index, char ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 60L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(220)) + slideInVertically { it / 2 }
                    ) {
                        PredefinedCharacterCard(
                            character = char,
                            onClick   = { if (char.isUnlocked) onSelectPredefined(char.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
