package kz.fearsom.financiallifev2.ui.components.core

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.ui.icons.LineIcons
import kz.fearsom.financiallifev2.ui.theme.AppColors
import kz.fearsom.financiallifev2.ui.theme.IndigoPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.MoneyLargeStyle
import kz.fearsom.financiallifev2.ui.theme.MoneyMediumStyle
import kz.fearsom.financiallifev2.ui.theme.MoneySmallStyle

/**
 * Financial health bottom sheet (redesign 2026-07, "Метрики" screen):
 * indigo→emerald freedom gradient, mono numerals, bordered metric cards.
 */
@Composable
fun StatsPanelOverlay(
    playerState: PlayerState,
    characterName: String,
    characterEmoji: String,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep.copy(alpha = 0.88f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.backgroundDeep)
                .border(
                    1.dp, colors.border,
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(enabled = false) {}
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(colors.borderStrong)
            )
            Spacer(Modifier.height(14.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Strings.uiStatsPanelTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$characterName · ${Strings.uiStatsPanelMonths.getOrElse(playerState.month) { "?" }} ${playerState.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = LineIcons.Close,
                        contentDescription = Strings.uiChatCancel,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Financial Freedom (hero card) ─────────────────────────────────
            val freedomPct = calculateFreedom(playerState)
            val animPct by animateFloatAsState(
                targetValue = freedomPct,
                animationSpec = tween(900, easing = FastOutSlowInEasing),
                label = "freedom"
            )
            MetricCard(colors) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        Strings.uiStatsPanelFreedom,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textEmphasis
                    )
                    Text(
                        "${(animPct * 100).toInt()}%",
                        style = MoneyLargeStyle,
                        color = colors.textPrimary
                    )
                }
                Spacer(Modifier.height(11.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(9.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.backgroundElevated.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animPct)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(IndigoPrimary, colors.accentPositive)
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Net Cash Flow ─────────────────────────────────────────────────
            val netCashFlow =
                playerState.income - playerState.expenses - playerState.debtPaymentMonthly
            val flowPositive = netCashFlow >= 0
            val flowAccent = if (flowPositive) colors.accentPositive else colors.accentNegative
            val flowText = if (flowPositive) colors.accentPositiveText else colors.accentNegativeText
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.backgroundCard)
                    .border(1.dp, flowAccent.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        Strings.uiStatsPanelFlow,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (flowPositive) Strings.uiStatsPanelProfit else Strings.uiStatsPanelDeficit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textEmphasis
                    )
                }
                Text(
                    (if (flowPositive) "+" else "") + formatMoney(netCashFlow),
                    style = MoneyLargeStyle.copy(fontSize = 21.sp),
                    color = flowText
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Capital / Debt ────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                MoneyCard(
                    label = Strings.uiStatsPanelCapital,
                    value = formatMoney(playerState.capital),
                    valueColor = colors.accentPositiveText,
                    modifier = Modifier.weight(1f)
                )
                MoneyCard(
                    label = Strings.uiStatsPanelDebt,
                    value = formatMoney(playerState.debt),
                    valueColor = if (playerState.debt > 0) colors.accentNegativeText else colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Income / Expenses / Investments ───────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                SubMetricCard(
                    label = Strings.uiStatsPanelIncome,
                    value = formatAmount(playerState.income),
                    modifier = Modifier.weight(1f)
                )
                SubMetricCard(
                    label = Strings.uiStatsPanelExpenses,
                    value = formatAmount(playerState.expenses),
                    modifier = Modifier.weight(1f)
                )
                SubMetricCard(
                    label = Strings.uiStatsPanelInvestments,
                    value = formatAmount(playerState.investments),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Soft metrics ("Показатели") ───────────────────────────────────
            MetricCard(colors) {
                Text(
                    Strings.uiStatsPanelIndicators.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(13.dp))
                StatBar(
                    label = Strings.uiStatsPanelStress,
                    value = playerState.stress,
                    barColor = colors.accentWarning,
                    valueColor = colors.accentWarningText
                )
                Spacer(Modifier.height(13.dp))
                StatBar(
                    label = Strings.uiStatsPanelKnowledge,
                    value = playerState.financialKnowledge,
                    barColor = IndigoPrimary,
                    valueColor = IndigoPrimary
                )
                Spacer(Modifier.height(13.dp))
                StatBar(
                    label = Strings.uiStatsPanelRisk,
                    value = playerState.riskLevel,
                    barColor = colors.accentNegative,
                    valueColor = colors.accentNegativeText
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun MetricCard(
    colors: AppColors,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.backgroundCard)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        content = content
    )
}

@Composable
private fun MoneyCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.backgroundCard)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Text(label, fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MoneyMediumStyle, color = valueColor)
    }
}

@Composable
private fun SubMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(colors.backgroundSubCard)
            .border(1.dp, colors.border, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style = MoneySmallStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = colors.textBody,
            maxLines = 1
        )
    }
}

@Composable
private fun StatBar(label: String, value: Int, barColor: Color, valueColor: Color) {
    val colors = LocalAppColors.current
    val animValue by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "statBar"
    )
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = colors.textEmphasis)
            Text("$value / 100", style = MoneySmallStyle, color = valueColor)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.backgroundElevated.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animValue)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** "1 500 ₸" — space-grouped, full precision (mock style). */
internal fun formatMoney(amount: Long): String = "${formatAmount(amount)} ₸"

internal fun formatAmount(amount: Long): String {
    val negative = amount < 0
    val digits = (if (negative) -amount else amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    return if (negative) "-$grouped" else grouped
}

/**
 * Composite 0–1 score: weighted blend of capital cushion, debt clearance,
 * financial knowledge, and stress management.
 *
 * Weights:
 *  40% — capital (target: 10M ₸)
 *  20% — debt-free ratio (target: 0 debt)
 *  25% — financial knowledge (0–100)
 *  15% — low stress (100 − stress)
 */
private fun calculateFreedom(ps: PlayerState): Float {
    val capitalScore   = (ps.capital.coerceAtMost(10_000_000L) / 10_000_000f) * 0.40f
    val debtScore      = (1f - (ps.debt.coerceAtMost(5_000_000L) / 5_000_000f)) * 0.20f
    val knowledgeScore = (ps.financialKnowledge / 100f) * 0.25f
    val stressScore    = ((100 - ps.stress) / 100f) * 0.15f
    return (capitalScore + debtScore + knowledgeScore + stressScore).coerceIn(0f, 1f)
}
