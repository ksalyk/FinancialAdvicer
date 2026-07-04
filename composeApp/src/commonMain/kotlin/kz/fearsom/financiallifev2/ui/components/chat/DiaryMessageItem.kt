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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import kz.fearsom.financiallifev2.ui.icons.LineIcons
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.MonoFontFamily
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// ─── Shapes (redesign 2026-07) ────────────────────────────────────────────────

private val CharacterBubbleShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
private val PlayerBubbleShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)

// ─── Message router ───────────────────────────────────────────────────────────

@Composable
fun DiaryMessageItem(
    message: ChatMessage,
    playerState: PlayerState?,
    characterName: String = "",
    isLatestChar: Boolean = false,
    displayedLength: Int = message.text.length
) {
    when (message.sender) {
        MessageSender.CHARACTER -> CharacterBubble(
            message = message,
            characterName = characterName,
            isLatestChar = isLatestChar,
            displayedLength = displayedLength
        )

        MessageSender.PLAYER -> PlayerBubble(message)
        MessageSender.SYSTEM -> SystemChip(message)
        MessageSender.MONTHLY_REPORT -> MonthlyReportCard(message)
    }
}

// ─── Character avatar ─────────────────────────────────────────────────────────

@Composable
private fun CharacterAvatar(characterName: String) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(colors.backgroundElevated, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = characterName.firstOrNull()?.uppercase() ?: "•",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary
        )
    }
}

// ─── Character bubble (left, with avatar) ─────────────────────────────────────

@Composable
private fun CharacterBubble(
    message: ChatMessage,
    characterName: String,
    isLatestChar: Boolean = false,
    displayedLength: Int = message.text.length
) {
    val colors = LocalAppColors.current
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

    Row(modifier = Modifier.fillMaxWidth()) {
        CharacterAvatar(characterName)
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .clip(CharacterBubbleShape)
                    .background(colors.bubbleCharacter)
                    .border(1.dp, colors.border, CharacterBubbleShape)
            ) {
                // ── Scene image (tagged events only) ──────────────────────────
                if (sceneRes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Image(
                            painter = painterResource(sceneRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Bottom fade into the bubble surface
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, colors.bubbleCharacter)
                                    )
                                )
                        )
                        val tagLabel = sceneTagLabel(message.sceneTag)
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

                // ── Message text ──────────────────────────────────────────────
                val annotatedText = remember(displayedText, cursorVisible, cursorAlpha) {
                    buildAnnotatedString {
                        append(displayedText)
                        if (cursorVisible) {
                            withStyle(SpanStyle(color = colors.textBody.copy(alpha = cursorAlpha))) {
                                append("▌")
                            }
                        }
                    }
                }
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textBody,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)
                )
            }

            // ── Scheme breakdown toggle ("Разобрать схему") ───────────────────
            val explanation = message.schemeExplanation
            if (explanation != null && displayedLength >= message.text.length && !isLatestChar) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = { showExplanation = !showExplanation },
                    shape = RoundedCornerShape(8.dp),
                    color = colors.bubbleAi,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.bubbleAiBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LineIcons.Flag,
                            contentDescription = null,
                            tint = colors.accentNegativeText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (showExplanation) "Скрыть разбор" else "Разобрать схему",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showExplanation,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 4 },
                    exit = fadeOut(tween(120))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.bubbleAi)
                            .border(1.dp, colors.bubbleAiBorder, RoundedCornerShape(12.dp))
                            .padding(13.dp)
                    ) {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textBody,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Player bubble (right, indigo) ────────────────────────────────────────────

@Composable
private fun PlayerBubble(message: ChatMessage) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(PlayerBubbleShape)
                .background(colors.bubblePlayer)
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── System chip (centered date / section marker) ─────────────────────────────

@Composable
private fun SystemChip(message: ChatMessage) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(colors.bubbleSystem)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = colors.textSecondary
            )
        }
    }
}

// ─── Monthly report card ──────────────────────────────────────────────────────

@Composable
private fun MonthlyReportCard(message: ChatMessage) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = LineIcons.Bars,
                contentDescription = null,
                tint = colors.accentPositiveText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                Strings.uiChatMonthlyReport,
                style = MaterialTheme.typography.labelSmall,
                color = colors.accentPositiveText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.bubbleReport)
                .border(1.dp, colors.accentPositive.copy(alpha = 0.32f), shape)
                .padding(horizontal = 15.dp, vertical = 13.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                color = colors.textBody,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun sceneTagLabel(tag: String?): String? = when (tag) {
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
