package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.presentation.MainMenuUiState
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun MainMenuScreen(
    uiState: MainMenuUiState,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onCharacters: () -> Unit,
    onStatistics: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalAppColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "menu_bg")
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logo_pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f, targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        // Background glow orbs
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(listOf(GoldPrimary.copy(alpha = glowAlpha), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(listOf(BlueAccent.copy(alpha = glowAlpha * 0.7f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(logoPulse)
                    .size(80.dp)
                    .background(
                        Brush.radialGradient(listOf(GoldPrimary.copy(alpha = 0.25f), Color.Transparent)),
                        CircleShape
                    )
                    .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("₸", fontSize = 38.sp, color = GoldPrimary)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text       = "FinancialLife",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary
            )
            Text(
                text     = "Стать богатым — это наука",
                fontSize = 13.sp,
                color    = colors.textSecondary
            )

            Spacer(Modifier.height(40.dp))

            // ── Active session card ───────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.canContinue && uiState.activeSession != null,
                enter   = fadeIn() + slideInVertically { -it / 2 },
                exit    = fadeOut()
            ) {
                uiState.activeSession?.let { session ->
                    ActiveSessionCard(session = session, onClick = onContinue)
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Main buttons ──────────────────────────────────────────────────

            // Continue (shown only when there's an active session)
            if (uiState.canContinue) {
                MenuButton(
                    emoji       = "▶️",
                    label       = "Продолжить игру",
                    description = uiState.activeSession?.let {
                        "${it.characterEmoji} ${it.characterName} · ${it.eraName}"
                    } ?: "",
                    gradient    = listOf(GoldPrimary.copy(alpha = 0.18f), GoldDark.copy(alpha = 0.08f)),
                    borderColor = GoldPrimary.copy(alpha = 0.6f),
                    onClick     = onContinue
                )
                Spacer(Modifier.height(10.dp))
            }

            MenuButton(
                emoji       = "🎮",
                label       = "Новая игра",
                description = "Выбери эпоху и персонажа",
                gradient    = listOf(BlueAccent.copy(alpha = 0.18f), BlueAccent.copy(alpha = 0.05f)),
                borderColor = BlueAccent.copy(alpha = 0.5f),
                onClick     = onNewGame
            )
            Spacer(Modifier.height(10.dp))

            MenuButton(
                emoji       = "👥",
                label       = "Персонажи",
                description = "Изучи предысторию и характеристики",
                gradient    = listOf(PurpleAccent.copy(alpha = 0.18f), PurpleAccent.copy(alpha = 0.05f)),
                borderColor = PurpleAccent.copy(alpha = 0.5f),
                onClick     = onCharacters
            )
            Spacer(Modifier.height(10.dp))

            MenuButton(
                emoji       = "📊",
                label       = "Статистика",
                description = uiState.quickStats?.let {
                    "Сыграно игр: ${it.totalGames}" +
                            (it.bestEnding?.let { e -> " · Лучший финал: ${e.emoji()} ${e.label()}" } ?: "")
                } ?: "Начни свою историю",
                gradient    = listOf(GreenSuccess.copy(alpha = 0.18f), GreenSuccess.copy(alpha = 0.05f)),
                borderColor = GreenSuccess.copy(alpha = 0.5f),
                onClick     = onStatistics
            )

            Spacer(Modifier.weight(1f))

            // ── Footer ────────────────────────────────────────────────────────
            TextButton(onClick = onLogout) {
                Text(
                    text     = "Выйти из аккаунта",
                    fontSize = 13.sp,
                    color    = colors.textHint
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Active Session Card ────────────────────────────────────────────────────────

@Composable
private fun ActiveSessionCard(session: GameSession, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary.copy(alpha = 0.07f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(session.characterEmoji, fontSize = 26.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = session.characterName,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary
            )
            Text(
                text     = "${session.eraName} · ${session.currentGameYear}/${session.currentGameMonth.toString().padStart(2, '0')}",
                fontSize = 11.sp,
                color    = colors.textSecondary
            )
        }
        Text("▶", fontSize = 16.sp, color = GoldPrimary)
    }
}

// ── Menu Button ───────────────────────────────────────────────────────────────

@Composable
private fun MenuButton(
    emoji: String,
    label: String,
    description: String,
    gradient: List<Color>,
    borderColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue    = if (pressed) 0.97f else 1f,
        animationSpec  = tween(100),
        label          = "btn_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(gradient))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable {
                pressed = true
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = label,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary
            )
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = description,
                    fontSize = 12.sp,
                    color    = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
        Text("›", fontSize = 22.sp, color = borderColor)
    }
}
