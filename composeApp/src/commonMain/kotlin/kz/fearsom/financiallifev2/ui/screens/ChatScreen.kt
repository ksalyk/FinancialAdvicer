package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import financiallifev2.composeapp.generated.resources.*
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.presentation.GameUiState
import kz.fearsom.financiallifev2.ui.components.StatsPanelOverlay
import kz.fearsom.financiallifev2.ui.theme.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    uiState: GameUiState,
    onChoiceSelected: (String) -> Unit,
    onToggleStats: () -> Unit,
    onRestart: () -> Unit,
    onNavigateToMenu: () -> Unit = {}
) {
    val colors      = LocalAppColors.current
    val listState   = rememberLazyListState()
    val messages    = uiState.gameState?.messages ?: emptyList()
    val playerState = uiState.gameState?.playerState

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
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(colors.backgroundDeep, Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            DiaryTopBar(
                playerState      = playerState,
                characterName    = uiState.characterName,
                characterEmoji   = uiState.characterEmoji,
                characterTitle   = uiState.characterTitle,
                onStatsClick     = onToggleStats,
                onRestartClick   = onRestart,
                onMenuClick      = onNavigateToMenu
            )

            LazyColumn(
                state               = listState,
                modifier            = Modifier.weight(1f).fillMaxWidth(),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn(tween(400)) + slideInVertically(tween(350)) { it / 3 }
                    ) {
                        DiaryMessageItem(message, playerState)
                    }
                }
                if (uiState.isTyping) {
                    item(key = "typing") {
                        DiaryWritingIndicator()
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.currentOptions.isNotEmpty() && !uiState.isTyping,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                DiaryActionsPanel(uiState.currentOptions, onChoiceSelected)
            }

            if (uiState.gameState?.gameOver == true) {
                DiaryGameOverBar(uiState.gameState.endingType, onRestart)
            }
        }

        if (uiState.showStats && playerState != null) {
            StatsPanelOverlay(
                playerState    = playerState,
                characterName  = uiState.characterName,
                characterEmoji = uiState.characterEmoji,
                onDismiss      = onToggleStats
            )
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun DiaryTopBar(
    playerState: PlayerState?,
    characterName: String,
    characterEmoji: String,
    characterTitle: String,
    onStatsClick: () -> Unit,
    onRestartClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Surface(color = colors.backgroundDeep, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Text("🏠", fontSize = 20.sp)
            }

            Text("📓", fontSize = 26.sp, modifier = Modifier.padding(end = 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Дневник · $characterName",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                val subtitle = if (playerState != null) {
                    "$characterTitle · ${monthName(playerState.month)} ${playerState.year}"
                } else {
                    characterTitle
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            IconButton(onClick = onStatsClick) {
                Text("📊", fontSize = 22.sp)
            }
            IconButton(onClick = onRestartClick) {
                Text("🔄", fontSize = 22.sp)
            }
        }
    }
}

// ─── Message router ───────────────────────────────────────────────────────────

@Composable
private fun DiaryMessageItem(message: ChatMessage, playerState: PlayerState?) {
    when (message.sender) {
        MessageSender.CHARACTER      -> DiaryEntryCard(message, playerState)
        MessageSender.PLAYER         -> DiaryChoiceNote(message)
        MessageSender.SYSTEM         -> DiarySectionLabel(message)
        MessageSender.MONTHLY_REPORT -> DiaryFinancialEntry(message)
    }
}

// ─── Scene Image ──────────────────────────────────────────────────────────────

/**
 * Maps a semantic scene tag to a compiled drawable resource.
 * Returns null when no image should be shown (routine/consequence events).
 * Add new entries here as scene assets are created.
 */
@Composable
private fun sceneDrawableFor(tag: String?): DrawableResource? = when (tag) {
    "scam"       -> Res.drawable.scene_scam
    "crisis"     -> Res.drawable.scene_crisis
    "career"     -> Res.drawable.scene_career
    "family"     -> Res.drawable.scene_family
    "investment" -> Res.drawable.scene_investment
    "mortgage"   -> Res.drawable.scene_mortgage
    "windfall"   -> Res.drawable.scene_windfall
    "world"      -> Res.drawable.scene_world
    else         -> null
}

// ─── Diary Entry Card (CHARACTER messages) ────────────────────────────────────

@Composable
private fun DiaryEntryCard(message: ChatMessage, playerState: PlayerState?) {
    val colors   = LocalAppColors.current
    val shape    = RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)
    val lineColor = colors.diaryLine
    val sceneRes = sceneDrawableFor(message.sceneTag)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Scene image (shown only for tagged events) ────────────────────────
        if (sceneRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp))
            ) {
                Image(
                    painter             = painterResource(sceneRes),
                    contentDescription  = null,
                    contentScale        = ContentScale.Crop,
                    modifier            = Modifier.fillMaxSize()
                )
                // Bottom fade-out so scene bleeds into the diary card below
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.diaryPage)
                            )
                        )
                )
                // Scene tag label (top-left corner pill)
                val tagLabel = when (message.sceneTag) {
                    "scam"       -> "⚠️ Осторожно"
                    "crisis"     -> "📉 Кризис"
                    "career"     -> "💼 Карьера"
                    "family"     -> "🏠 Семья"
                    "investment" -> "📈 Инвестиции"
                    "mortgage"   -> "🔑 Ипотека"
                    "windfall"   -> "🎉 Удача"
                    "world"      -> "🌙 Размышление"
                    else         -> null
                }
                if (tagLabel != null) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            tagLabel,
                            style     = MaterialTheme.typography.labelSmall,
                            color     = Color.White,
                            fontSize  = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Diary text card ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    if (sceneRes != null)
                        RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 12.dp)
                    else
                        shape
                )
                .background(colors.diaryPage)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(DiaryChoiceBorder.copy(alpha = 0.30f), colors.diaryLine)
                    ),
                    shape = if (sceneRes != null)
                        RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 12.dp)
                    else
                        shape
                )
                .drawBehind {
                    val lineSpacing = 28.dp.toPx()
                    val startY = 72.dp.toPx()
                    var y = startY
                    while (y < size.height - 12.dp.toPx()) {
                        drawLine(
                            color       = lineColor,
                            start       = Offset(16.dp.toPx(), y),
                            end         = Offset(size.width - 16.dp.toPx(), y),
                            strokeWidth = 1f
                        )
                        y += lineSpacing
                    }
                }
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                // Date header row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val dateText = if (playerState != null) {
                        "${playerState.month} ${monthNameFull(playerState.month)} ${playerState.year}"
                    } else {
                        message.emoji.ifEmpty { "📅" }
                    }
                    Text(
                        dateText,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = colors.diaryInkSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 11.sp
                    )
                    if (message.emoji.isNotEmpty() && playerState != null) {
                        Text(message.emoji, fontSize = 16.sp)
                    }
                }

                HorizontalDivider(
                    modifier  = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    thickness = 1.dp,
                    color     = colors.diaryLine
                )

                // Main diary text
                Text(
                    text       = message.text,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = colors.diaryInk,
                    lineHeight = 26.sp,
                    fontStyle  = FontStyle.Normal
                )
            }
        }
    }
}

// ─── Choice Note (PLAYER messages) ────────────────────────────────────────────

@Composable
private fun DiaryChoiceNote(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape  = RoundedCornerShape(2.dp, 10.dp, 10.dp, 10.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(shape)
                .background(colors.diaryChoice)
                .border(1.dp, DiaryChoiceBorder.copy(alpha = 0.55f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    "✍️  Я решил:",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = colors.diaryInkSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 11.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = message.text,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = colors.diaryInk,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Section Label (SYSTEM messages) ──────────────────────────────────────────

@Composable
private fun DiarySectionLabel(message: ChatMessage) {
    val colors = LocalAppColors.current
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier  = Modifier.weight(1f),
            thickness = 1.dp,
            color     = colors.diaryLine
        )
        Text(
            text     = "  ${message.text}  ",
            style    = MaterialTheme.typography.labelSmall,
            color    = colors.diaryInkSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(
            modifier  = Modifier.weight(1f),
            thickness = 1.dp,
            color     = colors.diaryLine
        )
    }
}

// ─── Financial Entry (MONTHLY_REPORT messages) ────────────────────────────────

@Composable
private fun DiaryFinancialEntry(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape  = RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(start = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📒", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "Итоги месяца",
                style      = MaterialTheme.typography.labelSmall,
                color      = GreenSuccess.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.diaryPage)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(GreenSuccess.copy(alpha = 0.4f), DiaryChoiceBorder.copy(alpha = 0.2f))
                    ),
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text       = message.text,
                style      = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color      = colors.diaryInk,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Writing Indicator ────────────────────────────────────────────────────────

@Composable
private fun DiaryWritingIndicator() {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "writing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "writingAlpha"
    )

    Row(
        modifier          = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✍️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
        Text(
            "пишет в дневник...",
            style    = MaterialTheme.typography.bodySmall,
            color    = colors.diaryInkSecondary.copy(alpha = alpha),
            fontStyle = FontStyle.Italic
        )
    }
}

// ─── Actions Panel ────────────────────────────────────────────────────────────

@Composable
private fun DiaryActionsPanel(options: List<GameOption>, onSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    Surface(color = colors.backgroundDeep, shadowElevation = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✍️", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                Text(
                    "Что я сделаю:",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            options.forEachIndexed { index, option ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(option.id) {
                    delay(index * 80L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter   = slideInHorizontally { it / 2 } + fadeIn(tween(250))
                ) {
                    DiaryActionItem(option, onClick = { onSelected(option.id) })
                }
            }
        }
    }
}

@Composable
private fun DiaryActionItem(option: GameOption, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape  = RoundedCornerShape(8.dp)
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = shape,
        color    = colors.backgroundElevated,
        border   = BorderStroke(1.dp, DiaryChoiceBorder.copy(alpha = 0.20f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "☐",
                fontSize  = 16.sp,
                color     = DiaryChoiceBorder.copy(alpha = 0.70f),
                modifier  = Modifier.padding(end = 10.dp)
            )
            if (option.emoji.isNotEmpty()) {
                Text(option.emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            }
            Text(
                option.text,
                style      = MaterialTheme.typography.bodyMedium,
                color      = colors.textPrimary,
                modifier   = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Game Over ────────────────────────────────────────────────────────────────

@Composable
private fun DiaryGameOverBar(endingType: EndingType?, onRestart: () -> Unit) {
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
                Text("🔄 Начать заново", fontWeight = FontWeight.Bold)
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

private fun monthNameFull(month: Int): String = when (month) {
    1  -> "января"; 2  -> "февраля"; 3  -> "марта";   4  -> "апреля"
    5  -> "мая";    6  -> "июня";    7  -> "июля";     8  -> "августа"
    9  -> "сентября"; 10 -> "октября"; 11 -> "ноября"; 12 -> "декабря"
    else -> ""
}
