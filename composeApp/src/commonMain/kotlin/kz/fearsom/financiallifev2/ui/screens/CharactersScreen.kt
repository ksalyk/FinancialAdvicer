package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.presentation.CharactersUiState
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun CharactersScreen(
    uiState: CharactersUiState,
    onCharacterClick: (String) -> Unit,    // navigate to CharacterDetail
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(PurpleAccent.copy(alpha = 0.10f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Назад", fontSize = 14.sp, color = colors.textSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text       = "Персонажи",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary
                )
                Text(
                    text     = "Нажми на персонажа, чтобы узнать его историю",
                    fontSize = 13.sp,
                    color    = colors.textSecondary
                )
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(uiState.characters) { index, character ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 80L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(250)) + slideInVertically { it / 2 }
                    ) {
                        CharacterRosterCard(
                            character = character,
                            onClick   = { onCharacterClick(character.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun CharacterRosterCard(character: PredefinedCharacter, onClick: () -> Unit) {
    val colors      = LocalAppColors.current
    val accentColor = when (character.difficulty) {
        Difficulty.EASY      -> GreenSuccess
        Difficulty.MEDIUM    -> GoldPrimary
        Difficulty.HARD      -> RedDanger
        Difficulty.NIGHTMARE -> PurpleAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentColor.copy(0.08f), colors.backgroundCard)
                )
            )
            .border(1.dp, accentColor.copy(0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.radialGradient(listOf(accentColor.copy(0.20f), Color.Transparent)),
                    CircleShape
                )
                .border(1.5.dp, accentColor.copy(0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(character.emoji, fontSize = 26.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = character.name,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = "${character.age} лет",
                    fontSize = 11.sp,
                    color    = colors.textHint,
                    modifier = Modifier
                        .background(colors.backgroundElevated, RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text     = character.profession,
                fontSize = 12.sp,
                color    = accentColor
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = character.personality,
                fontSize   = 12.sp,
                color      = colors.textSecondary,
                maxLines   = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("›", fontSize = 20.sp, color = accentColor)
            Spacer(Modifier.height(4.dp))
            Text(
                text     = when (character.difficulty) {
                    Difficulty.EASY      -> "Лёгкий"
                    Difficulty.MEDIUM    -> "Средний"
                    Difficulty.HARD      -> "Сложный"
                    Difficulty.NIGHTMARE -> "Кошмар"
                },
                fontSize = 9.sp,
                color    = accentColor,
                modifier = Modifier
                    .background(accentColor.copy(0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}
