package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.DialogProperties
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
import kz.fearsom.financiallifev2.i18n.Strings
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
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmRestart by remember { mutableStateOf(false) }

    // Confirmation dialog for restart (destructive action)
    if (showConfirmRestart) {
        AlertDialog(
            onDismissRequest = { showConfirmRestart = false },
            title = { Text(Strings.uiChatResetTitle) },
            text = { Text(Strings.uiChatResetMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestart = false
                        showMenu = false
                        onRestartClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDanger
                    )
                ) {
                    Text(Strings.uiChatResetConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestart = false }) {
                    Text(Strings.uiChatCancel)
                }
            }
        )
    }

    Surface(color = colors.backgroundDeep, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = Strings.uiChatCdHome,
                    tint = colors.textPrimary
                )
            }

            Text("📓", fontSize = 26.sp, modifier = Modifier.padding(end = 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${Strings.uiChatDiary} · $characterName",
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

            IconButton(
                onClick = onStatsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = Strings.uiChatCdStats,
                    tint = colors.textPrimary
                )
            }

            // Overflow menu (three-dot menu)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = Strings.uiChatCdOptions,
                        tint = colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.uiChatRestartGame) },
                        onClick = {
                            showMenu = false
                            showConfirmRestart = true
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                        }
                    )
                }
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
                    "scam"       -> Strings.uiChatSceneScam
                    "crisis"     -> Strings.uiChatSceneCrisis
                    "career"     -> Strings.uiChatSceneCareer
                    "family"     -> Strings.uiChatSceneFamily
                    "investment" -> Strings.uiChatSceneInvestment
                    "mortgage"   -> Strings.uiChatSceneMortgage
                    "windfall"   -> Strings.uiChatSceneWindfall
                    "world"      -> Strings.uiChatSceneWorld
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

                // Main diary text (Phase 2: Updated to use DiaryTextStyle with Caveat font)
                Text(
                    text       = message.text,
                    style      = DiaryTextStyle.copy(color = colors.diaryInk),
                    color      = colors.diaryInk,
                    fontStyle  = FontStyle.Normal
                )
            }
        }
    }
}

// ─── Choice Note (PLAYER messages) ────────────────────────────────────────────

/**
 * Player choice rendered as a handwritten note (right-aligned).
 * Both label and choice text use Kalam for consistent handwritten aesthetic.
 */
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
                    "✍️  ${Strings.uiChatPlayerPrefix}",
                    style      = DiaryHeaderStyle.copy(fontSize = 13.sp, color = colors.diaryInkSecondary),
                    color      = colors.diaryInkSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = message.text,
                    style      = DiaryTextStyle.copy(fontWeight = FontWeight.Medium, color = colors.diaryInk),
                    color      = colors.diaryInk,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Section Label (SYSTEM messages) ──────────────────────────────────────────

/**
 * Scene/section header with handwritten aesthetic (Kalam).
 * Divider lines create visual hierarchy between diary scenes.
 */
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
            style    = DiaryHeaderStyle.copy(fontSize = 12.sp, color = colors.diaryInkSecondary),
            color    = colors.diaryInkSecondary,
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
                Strings.uiChatMonthlyReport,
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

/**
 * "Character is writing..." indicator with handwritten aesthetic (Kalam).
 * Uses DiaryTextStyle for visual continuity with character messages.
 */
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
            Strings.uiChatWriting,
            style    = DiaryTextStyle.copy(fontSize = 14.sp, color = colors.diaryInkSecondary.copy(alpha = alpha)),
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
                    Strings.uiChatActionLabel,
                    style    = DiaryHeaderStyle.copy(fontSize = 14.sp, color = colors.textSecondary),
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
                style      = DiaryTextStyle.copy(fontWeight = FontWeight.Medium, color = colors.textPrimary),
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

    // VFX state management
    var showVFX by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000)  // VFX duration: 2 seconds
        showVFX = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── VFX Layer (confetti + vignette + ink-bleed) ──
        if (showVFX) {
            GameOverConfetti(endingColor, modifier = Modifier.fillMaxSize())
            GameOverVignette(modifier = Modifier.fillMaxSize())
            GameOverInkBleed(endingColor, modifier = Modifier.fillMaxSize())
        }

        // ── Restart action bar (appears after VFX) ──
        Surface(
            color = colors.backgroundDeep,
            shadowElevation = 16.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when (endingType) {
                        EndingType.BANKRUPTCY            -> Strings.endingBankruptcy
                        EndingType.PAYCHECK_TO_PAYCHECK  -> Strings.endingPaycheck
                        EndingType.FINANCIAL_STABILITY   -> Strings.endingStability
                        EndingType.FINANCIAL_FREEDOM     -> Strings.endingFreedom
                        EndingType.WEALTH                -> Strings.endingWealth
                        null                             -> Strings.endingGameOver
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
                    Text(Strings.uiChatRestart, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Game-Over VFX Components ──────────────────────────────────────────────────

/**
 * Falling confetti particles in ending-specific color.
 * Duration: 2.5 seconds with easing fall effect.
 */
@Composable
private fun GameOverConfetti(accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    // Create 8 confetti particles at different horizontal positions
    val confettiPositions = remember {
        listOf(0.1f, 0.25f, 0.4f, 0.55f, 0.7f, 0.85f, 0.15f, 0.65f)
    }

    Box(modifier = modifier) {
        confettiPositions.forEachIndexed { index, xPos ->
            val animDelay = (index * 100).toLong()
            val yAnimation by infiniteTransition.animateFloat(
                initialValue = -0.2f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, delayMillis = animDelay.toInt(), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "confetti_$index"
            )

            // Confetti particle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val centerX = size.width * xPos
                        val centerY = size.height * yAnimation
                        drawCircle(
                            color = accentColor.copy(alpha = 0.7f),
                            radius = 6.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                    }
            )
        }
    }
}

/**
 * Vignette effect: darkening around screen edges.
 * Fades in over 800ms.
 */
@Composable
private fun GameOverVignette(modifier: Modifier = Modifier) {
    val vignetteAlpha by animateFloatAsState(
        targetValue = 0.4f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "vignette"
    )

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteAlpha)),
                radius = 600f
            )
        )
    )
}

/**
 * Ink-bleed effect: animated glow/bloom from center.
 * Fades in from 200-1500ms with bloom effect.
 */
@Composable
private fun GameOverInkBleed(accentColor: Color, modifier: Modifier = Modifier) {
    val bloomAlpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = 1300,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "ink_bleed"
    )

    // Ink bloom grows and fades
    val bloomRadius by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = 1300,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "bloom_radius"
    )

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.3f * (1f - bloomAlpha)),
                    Color.Transparent
                ),
                radius = 400f + (bloomRadius * 200f)
            )
        )
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun monthName(month: Int): String =
    Strings.uiChatShortMonths.getOrNull(month) ?: "?"

private fun monthNameFull(month: Int): String =
    Strings.uiChatMonthsGenitive.getOrNull(month) ?: ""
