package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.moneyFormat
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.RedDanger
import kz.fearsom.financiallifev2.ui.theme.StatCapital
import kz.fearsom.financiallifev2.ui.theme.StatDebt
import kz.fearsom.financiallifev2.ui.theme.StatKnowledge
import kz.fearsom.financiallifev2.ui.theme.StatRisk
import kz.fearsom.financiallifev2.ui.theme.StatStress
import kotlin.math.abs
import kotlin.math.sin

/**
 * Sentiment classification of an ending — drives caption + VFX tone.
 */
private enum class EndingSentiment { WIN, NEUTRAL, LOSS }

/**
 * Accent color for an ending. Mirrors the original DiaryGameOverBar palette so
 * existing visual cues (green/gold/red/orange/blue) remain intact.
 */
private fun endingAccentColor(endingType: EndingType?): Color = Color(
    when (endingType) {
        EndingType.BANKRUPTCY            -> 0xFFFF5252
        EndingType.PAYCHECK_TO_PAYCHECK  -> 0xFFFF6E40
        EndingType.FINANCIAL_STABILITY   -> 0xFF40C4FF
        EndingType.FINANCIAL_FREEDOM     -> 0xFFFFD700
        EndingType.WEALTH                -> 0xFF00E676
        null                             -> 0xFF8899BB
    }
)

/** Hero emoji for the ending badge. Independent of the localized title string. */
private fun endingEmojiChar(endingType: EndingType?): String = when (endingType) {
    EndingType.BANKRUPTCY            -> "💔"
    EndingType.PAYCHECK_TO_PAYCHECK  -> "😰"
    EndingType.FINANCIAL_STABILITY   -> "😊"
    EndingType.FINANCIAL_FREEDOM     -> "🎯"
    EndingType.WEALTH                -> "🤑"
    null                             -> "🏁"
}

private fun endingSentiment(endingType: EndingType?): EndingSentiment = when (endingType) {
    EndingType.WEALTH, EndingType.FINANCIAL_FREEDOM -> EndingSentiment.WIN
    EndingType.FINANCIAL_STABILITY                  -> EndingSentiment.NEUTRAL
    EndingType.BANKRUPTCY, EndingType.PAYCHECK_TO_PAYCHECK, null -> EndingSentiment.LOSS
}

/**
 * Localized ending title text — same source as the original component. Returned
 * WITHOUT the leading emoji so the title and the hero badge don't duplicate the
 * same glyph. The original strings always start with `emoji + ' '` (or chained
 * emoji + ' '), so a single `substringAfter(' ')` is sufficient.
 */
private fun endingTitleText(endingType: EndingType?): String {
    val raw = when (endingType) {
        EndingType.BANKRUPTCY            -> Strings.endingBankruptcy
        EndingType.PAYCHECK_TO_PAYCHECK  -> Strings.endingPaycheck
        EndingType.FINANCIAL_STABILITY   -> Strings.endingStability
        EndingType.FINANCIAL_FREEDOM     -> Strings.endingFreedom
        EndingType.WEALTH                -> Strings.endingWealth
        null                             -> Strings.endingGameOver
    }
    return raw.substringAfter(' ', missingDelimiterValue = raw).trim().ifBlank { raw }
}

private fun endingCaption(sentiment: EndingSentiment): String = when (sentiment) {
    EndingSentiment.WIN     -> Strings.endingCaptionWin
    EndingSentiment.NEUTRAL -> Strings.endingCaptionNeutral
    EndingSentiment.LOSS    -> Strings.endingCaptionLoss
}

/**
 * Full-screen "game over" takeover rendered on top of the chat when the engine
 * flips `gameState.gameOver = true`. Shows the journey outcome: ending badge,
 * sentiment caption, character recap, and a stats grid so the player walks
 * away with a clear picture of what they built (or lost).
 *
 * @param endingType       Outcome tag chosen by the engine (drives accent + emoji).
 * @param playerState      Final player snapshot for the stats grid (nullable to
 *                         stay resilient if the engine has already torn down).
 * @param gameStartYear    Scenario start year, used for elapsed-month recap.
 * @param gameStartMonth   Scenario start month, used for elapsed-month recap.
 * @param characterName    Display name for the recap row.
 * @param characterEmoji   Avatar emoji for the recap row.
 * @param characterTitle   Optional subtitle (e.g. "Менеджер маркетплейса").
 * @param onRestart        Primary CTA — restart the same session.
 * @param onNavigateToMenu Secondary CTA — return to the main menu.
 */
@Composable
fun DiaryGameOverBar(
    endingType: EndingType?,
    playerState: PlayerState?,
    gameStartYear: Int?,
    gameStartMonth: Int?,
    characterName: String,
    characterEmoji: String,
    characterTitle: String,
    onRestart: () -> Unit,
    onNavigateToMenu: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val accent = endingAccentColor(endingType)
    val sentiment = endingSentiment(endingType)
    val title = endingTitleText(endingType)
    val heroEmoji = endingEmojiChar(endingType)
    val caption = endingCaption(sentiment)

    // ── Entry choreography: slide up + scale + fade ───────────────────
    val entry = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entry.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 220f)
        )
    }
    val entryProgress = entry.value
    val cardScale = 0.94f + entryProgress * 0.06f

    // Per-row stagger: each child fades + slides in with a 10% cascade.
    @Composable
    fun stagedChild(index: Int, content: @Composable () -> Unit) {
        val raw = ((entryProgress - index * 0.10f) / 0.6f).coerceIn(0f, 1f)
        val alpha = raw
        val offsetY = (1f - alpha) * 18f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = alpha, translationY = offsetY)
        ) { content() }
    }

    // ── Continuous halo pulse behind the badge ────────────────────────
    val halo = rememberInfiniteTransition(label = "halo")
    val haloPulse by halo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep.copy(alpha = 0.92f))
    ) {
        // ── Particle VFX behind everything else ───────────────────────
        EndingParticles(
            sentiment = sentiment,
            accent = accent,
            haloPulse = haloPulse,
            modifier = Modifier.fillMaxSize()
        )

        // ── Scrollable card column, centered vertically ──────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Hero badge with radial halo ──────────────────────────
            stagedChild(index = 0) {
                HeroBadge(
                    emoji = heroEmoji,
                    accent = accent,
                    haloPulse = haloPulse,
                    scale = cardScale
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Title + caption ──────────────────────────────────────
            stagedChild(index = 1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Character recap row ──────────────────────────────────
            stagedChild(index = 2) {
                CharacterRecap(
                    characterEmoji = characterEmoji,
                    characterName = characterName,
                    characterTitle = characterTitle,
                    playerState = playerState,
                    gameStartYear = gameStartYear,
                    gameStartMonth = gameStartMonth,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats grid 2×3 ───────────────────────────────────────
            if (playerState != null) {
                stagedChild(index = 3) {
                    StatsGrid(
                        playerState = playerState,
                        accent = accent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Primary CTA: restart ─────────────────────────────────
            stagedChild(index = 4) {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = colors.backgroundDeep
                    )
                ) {
                    Text(
                        text = Strings.uiChatRestart,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Secondary CTA: back to menu ──────────────────────────
            stagedChild(index = 5) {
                OutlinedButton(
                    onClick = onNavigateToMenu,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.textSecondary
                    )
                ) {
                    Text(
                        text = Strings.uiChatCdHome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Hero badge ──────────────────────────────────────────────────────────────

@Composable
private fun HeroBadge(
    emoji: String,
    accent: Color,
    haloPulse: Float,
    scale: Float
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing halo (largest, lowest alpha)
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.28f + haloPulse * 0.18f),
                                accent.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = with(density) { 72.dp.toPx() }
                        )
                    )
                }
        )
        // Inner gradient disk (slightly tinted, holds the emoji)
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            accent.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(
                            x = with(density) { 52.dp.toPx() } * 0.6f,
                            y = with(density) { 52.dp.toPx() } * 0.6f
                        ),
                        radius = with(density) { 56.dp.toPx() }
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = accent.copy(alpha = 0.55f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 52.sp
            )
        }
    }
}

// ─── Character recap row ─────────────────────────────────────────────────────

@Composable
private fun CharacterRecap(
    characterEmoji: String,
    characterName: String,
    characterTitle: String,
    playerState: PlayerState?,
    gameStartYear: Int?,
    gameStartMonth: Int?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val monthLabel = playerState?.let { ps ->
        val m = Strings.uiChatShortMonths.getOrElse(ps.month) { "?" }
        "$m ${ps.year}"
    } ?: ""
    val monthsPlayed = playerState?.let { ps ->
        val startYear = gameStartYear ?: return@let null
        val startMonth = gameStartMonth ?: return@let null
        val startAbsoluteMonth = startYear * 12 + startMonth
        val currentAbsoluteMonth = ps.year * 12 + ps.month
        maxOf(0, currentAbsoluteMonth - startAbsoluteMonth)
    }
    val monthsPlayedText = monthsPlayed
        ?.takeIf { it > 0 }
        ?.let { Strings.uiGameoverMonthsPlayed.replace("%d", it.toString()) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.backgroundElevated)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(characterEmoji, fontSize = 26.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = Strings.uiGameoverJourney,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textHint
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = characterName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            if (characterTitle.isNotBlank()) {
                Text(
                    text = characterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            if (monthLabel.isNotBlank() || monthsPlayedText != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅 $monthLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    if (monthsPlayedText != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textHint
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = monthsPlayedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─── Stats grid ──────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid(
    playerState: PlayerState,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val cells = listOf(
        StatCell(
            label = Strings.uiStatsPanelCapital,
            value = playerState.capital.moneyFormat(playerState.currency),
            accentColor = StatCapital,
            valueColor = StatCapital
        ),
        StatCell(
            label = Strings.uiGameoverNetWorth,
            value = playerState.netWorth.moneyFormat(playerState.currency),
            accentColor = accent,
            valueColor = if (playerState.netWorth >= 0) GreenSuccess else RedDanger
        ),
        StatCell(
            label = Strings.uiStatsPanelDebt,
            value = playerState.debt.moneyFormat(playerState.currency),
            accentColor = StatDebt,
            valueColor = if (playerState.debt > 0) StatDebt else GreenSuccess
        ),
        StatCell(
            label = Strings.uiStatsPanelKnowledge,
            value = "${playerState.financialKnowledge}/100",
            accentColor = StatKnowledge,
            valueColor = StatKnowledge
        ),
        StatCell(
            label = Strings.uiStatsPanelStress,
            value = "${playerState.stress}/100",
            accentColor = StatStress,
            valueColor = if (playerState.stress <= 40) GreenSuccess else StatStress
        ),
        StatCell(
            label = Strings.uiStatsPanelRisk,
            value = "${playerState.riskLevel}/100",
            accentColor = StatRisk,
            valueColor = if (playerState.riskLevel <= 40) GreenSuccess else StatRisk
        ),
    )

    // 2 columns × 3 rows
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cell ->
                    Box(modifier = Modifier.weight(1f)) {
                        StatCellCard(cell = cell)
                    }
                }
                // Pad incomplete final row with empty weight
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class StatCell(
    val label: String,
    val value: String,
    val accentColor: Color,
    val valueColor: Color
)

@Composable
private fun StatCellCard(cell: StatCell) {
    val colors = LocalAppColors.current
    val animAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animAlpha.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.backgroundElevated)
            .border(1.dp, cell.accentColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.graphicsLayer(alpha = animAlpha.value)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(cell.valueColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = cell.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = cell.value,
                style = MaterialTheme.typography.titleMedium,
                color = cell.valueColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Particle VFX ────────────────────────────────────────────────────────────

/**
 * Falling particles whose tone depends on the ending sentiment:
 *  - WIN     → bright confetti (8–14 particles) in accent + gold
 *  - NEUTRAL → light blue/cyan specks
 *  - LOSS    → slow red/dark ash falling
 *
 * All particles use the same continuous infiniteTransition so animation phase
 * stays coherent across recompositions.
 */
@Composable
private fun EndingParticles(
    sentiment: EndingSentiment,
    accent: Color,
    haloPulse: Float,
    modifier: Modifier = Modifier
) {
    val particles = remember(sentiment) {
        when (sentiment) {
            EndingSentiment.WIN -> listOf(
                Particle(0.08f, 0.00f,  6f, 1.0f),
                Particle(0.22f, 0.08f,  5f, 1.0f),
                Particle(0.40f, 0.16f,  7f, 1.0f),
                Particle(0.55f, 0.24f,  5f, 1.0f),
                Particle(0.72f, 0.32f,  6f, 1.0f),
                Particle(0.88f, 0.40f,  7f, 1.0f),
                Particle(0.15f, 0.48f,  5f, 1.0f),
                Particle(0.65f, 0.56f,  6f, 1.0f),
                Particle(0.32f, 0.64f,  5f, 1.0f),
                Particle(0.48f, 0.72f,  6f, 1.0f),
                Particle(0.05f, 0.80f,  5f, 1.0f),
                Particle(0.78f, 0.88f,  6f, 1.0f),
            )
            EndingSentiment.NEUTRAL -> listOf(
                Particle(0.20f, 0.00f, 4f, 0.7f),
                Particle(0.50f, 0.17f, 4f, 0.7f),
                Particle(0.75f, 0.34f, 3f, 0.7f),
                Particle(0.30f, 0.51f, 4f, 0.7f),
                Particle(0.60f, 0.68f, 3f, 0.7f),
                Particle(0.85f, 0.85f, 4f, 0.7f),
            )
            EndingSentiment.LOSS -> listOf(
                Particle(0.10f, 0.00f, 4f, 0.55f),
                Particle(0.30f, 0.17f, 3f, 0.55f),
                Particle(0.55f, 0.34f, 4f, 0.55f),
                Particle(0.78f, 0.51f, 3f, 0.55f),
                Particle(0.45f, 0.68f, 4f, 0.55f),
                Particle(0.65f, 0.85f, 3f, 0.55f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val density = LocalDensity.current
    Box(
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height
            particles.forEachIndexed { idx, p ->
                val cyclePos = (phase + p.phaseOffset) % 1f
                val y = h * (cyclePos * 1.4f - 0.2f) // enter above, exit below
                val x = w * p.xPos + normalizedSin((cyclePos + idx * 0.17f) % 1f) * 14f
                val baseAlpha = (1f - abs(cyclePos - 0.5f) * 2f).coerceIn(0f, 1f) * p.alpha
                val color = when (sentiment) {
                    EndingSentiment.WIN -> if (idx % 3 == 0) GoldPrimary else accent
                    EndingSentiment.NEUTRAL -> accent.copy(alpha = 0.7f)
                    EndingSentiment.LOSS -> if (idx % 2 == 0) RedDanger.copy(alpha = 0.55f) else accent.copy(alpha = 0.4f)
                }
                drawCircle(
                    color = color.copy(alpha = baseAlpha.coerceIn(0f, 1f)),
                    radius = with(density) { p.radiusDp.dp.toPx() },
                    center = Offset(x, y)
                )
            }
        }
    ) {
        // Soft outer halo glow that follows the hero badge — driven by the
        // haloPulse phase (0..1) and centered on the screen.
        val haloAlpha by animateFloatAsState(
            targetValue = 0.32f + haloPulse * 0.18f,
            animationSpec = tween(900, easing = FastOutSlowInEasing),
            label = "halo_alpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height * 0.18f
                    val r = with(density) { 220.dp.toPx() }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = haloAlpha),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
        )
    }
}

private data class Particle(
    val xPos: Float,
    val phaseOffset: Float,
    val radiusDp: Float,
    val alpha: Float
)

private fun normalizedSin(x: Float): Float {
    // x is normalized to 0..1 by the caller.
    val twoPi = 6.2831855f
    return sin((x * twoPi).toDouble()).toFloat()
}
