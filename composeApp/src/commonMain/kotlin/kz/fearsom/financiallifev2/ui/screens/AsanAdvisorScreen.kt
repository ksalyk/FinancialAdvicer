package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.presentation.AsanMessage
import kz.fearsom.financiallifev2.presentation.AsanUiState
import kz.fearsom.financiallifev2.ui.icons.LineIcons
import kz.fearsom.financiallifev2.ui.theme.IndigoPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.MonoFontFamily
import kz.fearsom.financiallifev2.ui.theme.PurpleGradient

// ─── Shapes ───────────────────────────────────────────────────────────────────

private val AiBubbleShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
private val UserBubbleShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)

/**
 * Asan AI advisor (redesign 2026-07). UI stub: canned analysis of the current
 * scam event, red-flag chips, text composer with a visual-only voice mode.
 */
@Composable
fun AsanAdvisorScreen(
    uiState: AsanUiState,
    onSend: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onCancelVoice: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    // Keep the newest message in view.
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val count = uiState.messages.size + if (uiState.isTyping) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundChat)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = LineIcons.ChevronLeft,
                    contentDescription = Strings.uiAppbarBack,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            AsanAvatar(size = 38.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Strings.uiAsanTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(colors.accentPositive, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Strings.uiAsanStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.divider)

        // ── Messages ──────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                AsanMessageItem(message)
            }
            if (uiState.isTyping) {
                item(key = "asan-typing") { AsanTypingBubble() }
            }
        }

        // ── Composer ──────────────────────────────────────────────────────────
        HorizontalDivider(thickness = 1.dp, color = colors.divider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.backgroundPanel)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            AnimatedContent(
                targetState = uiState.isRecording,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "composer"
            ) { recording ->
                if (recording) {
                    VoiceRecordingComposer(
                        seconds = uiState.recordingSeconds,
                        onStop = onToggleVoice,
                        onCancel = onCancelVoice
                    )
                } else {
                    IdleComposer(
                        onSend = onSend,
                        onVoice = onToggleVoice
                    )
                }
            }
        }
    }
}

// ─── Avatar ───────────────────────────────────────────────────────────────────

@Composable
private fun AsanAvatar(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(IndigoPrimary, PurpleGradient)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = LineIcons.Sparkle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

// ─── Messages ─────────────────────────────────────────────────────────────────

@Composable
private fun AsanMessageItem(message: AsanMessage) {
    val colors = LocalAppColors.current
    if (message.fromUser) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(colors.bubblePlayer, UserBubbleShape)
                    .padding(horizontal = 15.dp, vertical = 12.dp)
            ) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.5.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentWidth(Alignment.Start)
                    .background(colors.bubbleAi, AiBubbleShape)
                    .border(1.dp, colors.bubbleAiBorder, AiBubbleShape)
                    .padding(horizontal = 15.dp, vertical = 13.dp)
            ) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.5.sp,
                    color = colors.textBody,
                    lineHeight = 21.sp
                )
            }
            if (message.showRedFlags) {
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RedFlagChip(Strings.uiAsanFlagGuarantee)
                    RedFlagChip(Strings.uiAsanFlagUpfront)
                    RedFlagChip(Strings.uiAsanFlagUrgency)
                }
            }
        }
    }
}

@Composable
private fun RedFlagChip(text: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .background(colors.accentNegative.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .border(1.dp, colors.accentNegative.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = LineIcons.Flag,
            contentDescription = null,
            tint = colors.accentNegativeText,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accentNegativeText,
            maxLines = 1
        )
    }
}

@Composable
private fun AsanTypingBubble() {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "asanTyping")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "asanTypingPhase"
    )
    Row(
        modifier = Modifier
            .background(colors.bubbleAi, AiBubbleShape)
            .border(1.dp, colors.bubbleAiBorder, AiBubbleShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val local = ((phase - index * 0.15f + 1f) % 1f)
            val lift = when {
                local < 0.3f -> local / 0.3f
                local < 0.6f -> 1f - (local - 0.3f) / 0.3f
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .padding(top = (4.dp * (1f - lift)))
                    .alpha(0.4f + 0.6f * lift)
                    .size(7.dp)
                    .background(colors.textSecondary, CircleShape)
            )
        }
    }
}

// ─── Composer states ──────────────────────────────────────────────────────────

@Composable
private fun IdleComposer(
    onSend: (String) -> Unit,
    onVoice: () -> Unit
) {
    val colors = LocalAppColors.current
    var input by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundCard, RoundedCornerShape(24.dp))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(24.dp))
            .padding(start = 17.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = 13.5.sp,
                color = colors.textPrimary,
                lineHeight = 18.sp
            ),
            cursorBrush = SolidColor(IndigoPrimary),
            maxLines = 3,
            decorationBox = { innerTextField ->
                Box {
                    if (input.isEmpty()) {
                        Text(
                            Strings.uiAsanPlaceholder,
                            fontSize = 13.5.sp,
                            color = colors.textHint
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(Modifier.width(10.dp))
        val hasText = input.isNotBlank()
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(IndigoPrimary, CircleShape)
                .clickable {
                    if (hasText) {
                        onSend(input)
                        input = ""
                    } else {
                        onVoice()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasText) LineIcons.Send else LineIcons.Mic,
                contentDescription = if (hasText) Strings.uiAsanCdSend else Strings.uiAsanCdVoice,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun VoiceRecordingComposer(
    seconds: Int,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "rec")

    // Blinking record dot (mock: flrec 1.2s).
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "recDot"
    )
    // Waveform phase for gentle bar oscillation.
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "wavePhase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundCard, RoundedCornerShape(24.dp))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(24.dp))
            .padding(start = 15.dp, top = 6.dp, bottom = 6.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dotAlpha)
                .background(colors.accentNegative, CircleShape)
        )
        Text(
            text = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MonoFontFamily,
            color = colors.textPrimary
        )
        // Waveform bars (visual stub — heights oscillate around mock values)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val base = listOf(8, 15, 6, 18, 10, 16, 7, 13)
            base.forEachIndexed { i, h ->
                val local = ((wavePhase + i * 0.125f) % 1f)
                val osc = if (local < 0.5f) local * 2f else (1f - local) * 2f
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((h * (0.6f + 0.4f * osc)).dp)
                        .background(IndigoPrimary, RoundedCornerShape(2.dp))
                )
            }
        }
        Text(
            Strings.uiChatCancel,
            fontSize = 11.5.sp,
            color = colors.textHint,
            maxLines = 1,
            modifier = Modifier.clickable(onClick = onCancel)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.accentNegative, CircleShape)
                .clickable(onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LineIcons.Mic,
                contentDescription = Strings.uiAsanCdVoice,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
