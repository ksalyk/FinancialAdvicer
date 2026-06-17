package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import financiallifev2.composeapp.generated.resources.Res
import financiallifev2.composeapp.generated.resources.scene_career_v2
import financiallifev2.composeapp.generated.resources.scene_crisis_v2
import financiallifev2.composeapp.generated.resources.scene_family_v2
import financiallifev2.composeapp.generated.resources.scene_investment_v2
import financiallifev2.composeapp.generated.resources.scene_mortgage_v2
import financiallifev2.composeapp.generated.resources.scene_scam_v2
import financiallifev2.composeapp.generated.resources.scene_windfall_v2
import financiallifev2.composeapp.generated.resources.scene_world_v2
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.ui.theme.DiaryChoiceBorder
import kz.fearsom.financiallifev2.ui.theme.DiaryHeaderStyle
import kz.fearsom.financiallifev2.ui.theme.DiaryTextStyle
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// ─── Message router ───────────────────────────────────────────────────────────

@Composable
fun DiaryMessageItem(
    message: ChatMessage,
    playerState: PlayerState?,
    isLatestChar: Boolean = false,
    displayedLength: Int = message.text.length
) {
    when (message.sender) {
        MessageSender.CHARACTER -> DiaryEntryCard(
            message = message,
            playerState = playerState,
            isLatestChar = isLatestChar,
            displayedLength = displayedLength
        )

        MessageSender.PLAYER -> DiaryChoiceNote(message)
        MessageSender.SYSTEM -> DiarySectionLabel(message)
        MessageSender.MONTHLY_REPORT -> DiaryFinancialEntry(message)
    }
}

// ─── Diary Entry Card (CHARACTER messages) ────────────────────────────────────

@Composable
private fun DiaryEntryCard(
    message: ChatMessage,
    playerState: PlayerState?,
    isLatestChar: Boolean = false,
    displayedLength: Int = message.text.length
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)
    val lineColor = colors.diaryLine
    val sceneRes = sceneDrawableFor(message.sceneTag)
    var showExplanation by remember(message.id) { mutableStateOf(false) }
    val displayedText = message.text.take(displayedLength)
    val cursorVisible = isLatestChar && displayedLength < message.text.length
    val cursorAlpha by rememberInfiniteTransition(label = "cursor")
        .animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
            label = "cursorAlpha"
        )

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
                    painter = painterResource(sceneRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                    "scam" -> Strings.uiChatSceneScam
                    "crisis" -> Strings.uiChatSceneCrisis
                    "career" -> Strings.uiChatSceneCareer
                    "family" -> Strings.uiChatSceneFamily
                    "investment" -> Strings.uiChatSceneInvestment
                    "mortgage" -> Strings.uiChatSceneMortgage
                    "windfall" -> Strings.uiChatSceneWindfall
                    "world" -> Strings.uiChatSceneWorld
                    else -> null
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
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 10.sp,
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
                            color = lineColor,
                            start = Offset(16.dp.toPx(), y),
                            end = Offset(size.width - 16.dp.toPx(), y),
                            strokeWidth = 1f
                        )
                        y += lineSpacing
                    }
                }
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                // Date header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val snapshotState = message.sourcePlayerState ?: playerState
                    val dateText = if (snapshotState != null) {
                        "${snapshotState.month} ${monthNameFull(snapshotState.month)} ${snapshotState.year}"
                    } else {
                        message.emoji.ifEmpty { "📅" }
                    }
                    Text(
                        dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.diaryInkSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    if (message.emoji.isNotEmpty() && snapshotState != null) {
                        Text(message.emoji, fontSize = 16.sp)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    thickness = 1.dp,
                    color = colors.diaryLine
                )

                // Main diary text — uses animated displayedText when this is the
                // latest character message; otherwise shows the full text instantly.
                val annotatedText = remember(displayedText, cursorVisible, cursorAlpha) {
                    buildAnnotatedString {
                        append(displayedText)
                        if (cursorVisible) {
                            // Blinking block cursor appended after revealed text
                            withStyle(SpanStyle(color = colors.diaryInk.copy(alpha = cursorAlpha))) {
                                append("▌")
                            }
                        }
                    }
                }
                Text(
                    text = annotatedText,
                    style = DiaryTextStyle.copy(color = colors.diaryInk),
                    fontStyle = FontStyle.Normal
                )

                val explanation = message.schemeExplanation
                if (explanation != null && displayedLength >= message.text.length && !isLatestChar) {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showExplanation = !showExplanation },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.backgroundElevated.copy(alpha = 0.92f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (showExplanation) "Скрыть разбор" else "Разобрать схему",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    AnimatedVisibility(
                        visible = showExplanation,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 4 },
                        exit = fadeOut(tween(120))
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.backgroundElevated.copy(alpha = 0.82f))
                                .border(
                                    1.dp,
                                    GoldPrimary.copy(alpha = 0.28f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
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
    val shape = RoundedCornerShape(2.dp, 10.dp, 10.dp, 10.dp)

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
                    style = DiaryHeaderStyle.copy(
                        fontSize = 13.sp,
                        color = colors.diaryInkSecondary
                    ),
                    color = colors.diaryInkSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = DiaryTextStyle.copy(
                        fontWeight = FontWeight.Medium,
                        color = colors.diaryInk
                    ),
                    color = colors.diaryInk,
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = colors.diaryLine
        )
        Text(
            text = "  ${message.text}  ",
            style = DiaryHeaderStyle.copy(fontSize = 12.sp, color = colors.diaryInkSecondary),
            color = colors.diaryInkSecondary,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = colors.diaryLine
        )
    }
}

// ─── Financial Entry (MONTHLY_REPORT messages) ────────────────────────────────

@Composable
private fun DiaryFinancialEntry(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📒", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                Strings.uiChatMonthlyReport,
                style = MaterialTheme.typography.labelSmall,
                color = GreenSuccess.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
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
                        listOf(
                            GreenSuccess.copy(alpha = 0.4f),
                            DiaryChoiceBorder.copy(alpha = 0.2f)
                        )
                    ),
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.diaryInk,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Maps a semantic scene tag to a compiled drawable resource.
 * Returns null when no image should be shown (routine/consequence events).
 * Add new entries here as scene assets are created.
 */
@Composable
private fun sceneDrawableFor(tag: String?): DrawableResource? = when (tag) {
    "scam" -> Res.drawable.scene_scam_v2
    "crisis" -> Res.drawable.scene_crisis_v2
    "career" -> Res.drawable.scene_career_v2
    "family" -> Res.drawable.scene_family_v2
    "investment" -> Res.drawable.scene_investment_v2
    "mortgage" -> Res.drawable.scene_mortgage_v2
    "windfall" -> Res.drawable.scene_windfall_v2
    "world" -> Res.drawable.scene_world_v2
    else -> null
}

private fun monthNameFull(month: Int): String =
    Strings.uiChatMonthsGenitive.getOrNull(month) ?: ""
