package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.presentation.GameUiState
import kz.fearsom.financiallifev2.ui.components.StatsPanelOverlay
import kz.fearsom.financiallifev2.ui.theme.*
import kotlinx.coroutines.delay

// ─── Character constants ──────────────────────────────────────────────────────
// Asan's profile is fixed in this game; kept here rather than in PlayerState
// since it's purely presentational (no effect on economic simulation).
private const val CHAR_NAME  = "Асан"
private const val CHAR_EMOJI = "👨‍💻"
private const val CHAR_TITLE = "Инженер-программист"

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    uiState: GameUiState,
    onChoiceSelected: (String) -> Unit,
    onToggleStats: () -> Unit,
    onRestart: () -> Unit
) {
    val colors      = LocalAppColors.current
    val listState   = rememberLazyListState()
    val messages    = uiState.gameState?.messages ?: emptyList()
    val playerState = uiState.gameState?.playerState

    // Auto-scroll to bottom on every new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(80)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundChat)
    ) {
        // Top gradient vignette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.verticalGradient(listOf(colors.backgroundDeep, colors.backgroundChat)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                playerState    = playerState,
                onStatsClick   = onToggleStats,
                onRestartClick = onRestart
            )

            LazyColumn(
                state               = listState,
                modifier            = Modifier.weight(1f).fillMaxWidth(),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = slideInVertically { it / 2 } + fadeIn(tween(350))
                    ) {
                        MessageBubble(message)
                    }
                }
                if (uiState.isTyping) {
                    item(key = "typing") { TypingIndicator() }
                }
            }

            AnimatedVisibility(
                visible = uiState.currentOptions.isNotEmpty() && !uiState.isTyping,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                OptionsPanel(uiState.currentOptions, onChoiceSelected)
            }

            if (uiState.gameState?.gameOver == true) {
                GameOverBar(uiState.gameState.endingType, onRestart)
            }
        }

        // Stats overlay — shown when playerState is available
        if (uiState.showStats && playerState != null) {
            StatsPanelOverlay(
                playerState    = playerState,
                characterName  = CHAR_NAME,
                characterEmoji = CHAR_EMOJI,
                onDismiss      = onToggleStats
            )
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar(
    playerState: PlayerState?,
    onStatsClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "online")
    val onlinePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "onlineDot"
    )

    Surface(color = colors.backgroundDeep, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.backgroundElevated),
                    contentAlignment = Alignment.Center
                ) { Text(CHAR_EMOJI, fontSize = 22.sp) }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(GreenSuccess.copy(alpha = onlinePulse))
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    CHAR_NAME,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                val subtitle = if (playerState != null) {
                    "$CHAR_TITLE · ${monthName(playerState.month)} ${playerState.year}"
                } else {
                    CHAR_TITLE
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            IconButton(onClick = onStatsClick) {
                Text("📊", fontSize = 24.sp)
            }
            IconButton(onClick = onRestartClick) {
                Text("🔄", fontSize = 24.sp)
            }
        }
    }
}

// ─── Message Bubble factory ───────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessage) {
    when (message.sender) {
        MessageSender.CHARACTER      -> CharacterBubble(message)
        MessageSender.PLAYER         -> PlayerBubble(message)
        MessageSender.SYSTEM         -> SystemBubble(message)
        MessageSender.MONTHLY_REPORT -> MonthlyReportCard(message)
    }
}

// Asan's left-aligned chat bubble
@Composable
private fun CharacterBubble(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp, bottom = 4.dp)
        ) {
            if (message.emoji.isNotEmpty()) {
                Text(message.emoji, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                CHAR_NAME,
                style      = MaterialTheme.typography.bodySmall,
                color      = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(shape)
                .background(colors.bubbleCharacter)
                .border(1.dp, GoldPrimary.copy(alpha = 0.28f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text       = message.text,
                style      = MaterialTheme.typography.bodyMedium,
                color      = colors.textPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

// Player's right-aligned choice bubble
@Composable
private fun PlayerBubble(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            "Вы",
            style      = MaterialTheme.typography.bodySmall,
            color      = BlueAccent.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(end = 4.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(shape)
                .background(colors.bubblePlayer)
                .border(1.dp, BlueAccent.copy(alpha = 0.30f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text       = message.text,
                style      = MaterialTheme.typography.bodyMedium,
                color      = colors.textPrimary,
                lineHeight = 22.sp,
                textAlign  = TextAlign.End,
                modifier   = Modifier.fillMaxWidth()
            )
        }
    }
}

// Centered system announcement
@Composable
private fun SystemBubble(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier         = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(colors.bubbleSystem)
                .border(1.dp, PurpleAccent.copy(alpha = 0.28f), shape)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text       = message.text,
                style      = MaterialTheme.typography.bodySmall,
                color      = PurpleAccent.copy(alpha = 0.85f),
                textAlign  = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Full-width monthly financial report card with monospace layout
@Composable
private fun MonthlyReportCard(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp, bottom = 6.dp)
        ) {
            Text("📊", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "Финансовый отчёт",
                style      = MaterialTheme.typography.bodySmall,
                color      = GreenSuccess.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.bubbleReport)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(GreenSuccess.copy(alpha = 0.45f), GoldPrimary.copy(alpha = 0.25f))
                    ),
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text       = message.text,
                style      = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color      = colors.textPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Typing Indicator ─────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val bubbleShape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Column {
        Row(
            modifier          = Modifier.padding(start = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(CHAR_EMOJI, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Text(CHAR_NAME, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(colors.bubbleCharacter)
                .border(1.dp, GoldPrimary.copy(alpha = 0.20f), bubbleShape)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation          = tween(550, easing = FastOutSlowInEasing),
                            repeatMode         = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 180)
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = dotAlpha))
                    )
                }
            }
        }
    }
}

// ─── Options Panel ────────────────────────────────────────────────────────────

@Composable
private fun OptionsPanel(options: List<GameOption>, onSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    Surface(color = colors.backgroundDeep, shadowElevation = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Выберите решение:",
                style    = MaterialTheme.typography.bodySmall,
                color    = colors.textSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            options.forEachIndexed { index, option ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(option.id) {
                    delay(index * 90L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter   = slideInHorizontally { it } + fadeIn(tween(250))
                ) {
                    OptionButton(option, onClick = { onSelected(option.id) })
                }
            }
        }
    }
}

@Composable
private fun OptionButton(option: GameOption, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = colors.backgroundElevated,
        border   = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.22f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(option.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
            Text(
                option.text,
                style      = MaterialTheme.typography.bodyMedium,
                color      = colors.textPrimary,
                modifier   = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Game Over ────────────────────────────────────────────────────────────────

@Composable
private fun GameOverBar(endingType: EndingType?, onRestart: () -> Unit) {
    val colors = LocalAppColors.current
    val endingColor = Color(
        when (endingType) {
            EndingType.BANKRUPTCY            -> 0xFFFF5252
            EndingType.PAYCHECK_TO_PAYCHECK  -> 0xFFFF6E40
            EndingType.FINANCIAL_STABILITY   -> 0xFF40C4FF
            EndingType.FINANCIAL_FREEDOM     -> 0xFFFFD700
            EndingType.WEALTH                -> 0xFF00E676
            null                             -> 0xFF8899BB
        }
    )
    Surface(color = colors.backgroundDeep, shadowElevation = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when (endingType) {
                    EndingType.BANKRUPTCY            -> "💔 Банкротство"
                    EndingType.PAYCHECK_TO_PAYCHECK  -> "😰 Без накоплений"
                    EndingType.FINANCIAL_STABILITY   -> "😊 Стабильность достигнута"
                    EndingType.FINANCIAL_FREEDOM     -> "🎯 Финансовая свобода!"
                    EndingType.WEALTH                -> "🤑 Богатство!"
                    null                             -> "🏁 Конец"
                },
                style      = MaterialTheme.typography.titleMedium,
                color      = endingColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onRestart,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor   = colors.backgroundDeep
                )
            ) {
                Text("🔄 Играть снова", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun monthName(month: Int): String = when (month) {
    1  -> "Янв"; 2  -> "Фев"; 3  -> "Мар"; 4  -> "Апр"
    5  -> "Май"; 6  -> "Июн"; 7  -> "Июл"; 8  -> "Авг"
    9  -> "Сен"; 10 -> "Окт"; 11 -> "Ноя"; 12 -> "Дек"
    else -> "?"
}
