package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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

            // ── Hero tier ──────────────────────────────────────────────────────
            // Two-tier system: hero action first (Continue or New Game), then secondary actions

            if (uiState.canContinue && uiState.activeSession != null) {
                // Hero: Continue with filled gold button + session context
                uiState.activeSession?.let { session ->
                    HeroActionCard(
                        emoji       = "▶️",
                        label       = "Продолжить игру",
                        context     = "${session.characterEmoji} ${session.characterName} · ${session.eraName}",
                        onClick     = onContinue
                    )
                    Spacer(Modifier.height(20.dp))
                }
            } else {
                // Hero: New Game when no active session (promote this as primary action)
                HeroActionCard(
                    emoji       = "🎮",
                    label       = "Новая игра",
                    context     = "Выбери эпоху и персонажа",
                    onClick     = onNewGame
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── Secondary tier ────────────────────────────────────────────────

            // New Game (only shown when continuing from active session)
            if (uiState.canContinue) {
                OutlinedMenuButton(
                    emoji       = "🎮",
                    label       = "Новая игра",
                    description = "Выбери эпоху и персонажа",
                    accentColor = BlueAccent,
                    onClick     = onNewGame
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedMenuButton(
                emoji       = "👥",
                label       = "Персонажи",
                description = "Изучи предысторию и характеристики",
                accentColor = PurpleAccent,
                onClick     = onCharacters
            )
            Spacer(Modifier.height(10.dp))

            OutlinedMenuButton(
                emoji       = "📊",
                label       = "Статистика",
                description = uiState.quickStats?.let {
                    "Сыграно игр: ${it.totalGames}" +
                            (it.bestEnding?.let { e -> " · Лучший финал: ${e.emoji()} ${e.label()}" } ?: "")
                } ?: "Начни свою историю",
                accentColor = GreenSuccess,
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

// ── Hero Action Card (primary action) ──────────────────────────────────────────

@Composable
private fun HeroActionCard(
    emoji: String,
    label: String,
    context: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val scale = animateFloatAsState(
        targetValue = if (isPressed.value) 0.97f else 1f,
        animationSpec = tween(100),
        label = "hero_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(GoldPrimary.copy(alpha = 0.25f), GoldDark.copy(alpha = 0.12f))
                )
            )
            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 32.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text       = label,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = context,
                    fontSize = 13.sp,
                    color    = colors.textSecondary
                )
            }
            Text("▶", fontSize = 20.sp, color = GoldPrimary)
        }
    }
}

// ── Outlined Menu Button (secondary action) ────────────────────────────────────

@Composable
private fun OutlinedMenuButton(
    emoji: String,
    label: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val scale = animateFloatAsState(
        targetValue = if (isPressed.value) 0.97f else 1f,
        animationSpec = tween(100),
        label = "menu_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.08f), colors.backgroundCard)
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = label,
                fontSize   = 15.sp,
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
        Text("›", fontSize = 18.sp, color = accentColor)
    }
}
