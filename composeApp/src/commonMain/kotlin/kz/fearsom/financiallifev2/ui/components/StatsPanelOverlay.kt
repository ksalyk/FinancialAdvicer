package kz.fearsom.financiallifev2.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kz.fearsom.financiallifev2.ui.theme.GoldDark
import kz.fearsom.financiallifev2.ui.theme.GoldLight
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.RedDanger
import kz.fearsom.financiallifev2.ui.theme.StatCapital
import kz.fearsom.financiallifev2.ui.theme.StatDebt
import kz.fearsom.financiallifev2.ui.theme.StatKnowledge
import kz.fearsom.financiallifev2.ui.theme.StatRisk
import kz.fearsom.financiallifev2.ui.theme.StatStress

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
                .background(colors.backgroundCard)
                .border(
                    1.dp, colors.surfaceGlassBorder,
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(enabled = false) {}
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(colors.textHint)
            )
            Spacer(Modifier.height(20.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.backgroundElevated),
                    contentAlignment = Alignment.Center
                ) { Text(characterEmoji, fontSize = 24.sp) }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${Strings.uiStatsPanelTitle} $characterName",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${Strings.uiStatsPanelMonths.getOrElse(playerState.month) { "?" }} ${playerState.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 24.sp, color = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── HERO TIER: Financial Freedom ──────────────────────────────────
            val freedomPct = calculateFreedom(playerState)
            val animPct by animateFloatAsState(
                targetValue = freedomPct,
                animationSpec = tween(900, easing = FastOutSlowInEasing),
                label = "freedom"
            )
            Text(
                Strings.uiStatsPanelFreedom,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Freedom progress bar (hero size: 12dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(12.dp)
                    .clip(CircleShape)
                    .background(colors.backgroundElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animPct)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                        )
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Strings.uiStatsPanelStart, style = MaterialTheme.typography.bodySmall, color = colors.textHint)
                Text(
                    "${(animPct * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(Strings.uiStatsPanelFreedomLabel, style = MaterialTheme.typography.bodySmall, color = colors.textHint)
            }

            Spacer(Modifier.height(24.dp))

            // ── PRIMARY: Net Cash Flow (full width) ────────────────────────────
            val netCashFlow =
                playerState.income - playerState.expenses - playerState.debtPaymentMonthly
            Text(
                Strings.uiStatsPanelFlow,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.backgroundElevated)
                    .border(1.5.dp,
                        if (netCashFlow >= 0) GreenSuccess.copy(0.4f) else RedDanger.copy(0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (netCashFlow >= 0) Strings.uiStatsPanelProfit else Strings.uiStatsPanelDeficit,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Text(
                        (if (netCashFlow >= 0) "+" else "") + formatMoney(netCashFlow),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (netCashFlow >= 0) GreenSuccess else RedDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── SECONDARY: Financial Position (balance sheet row) ───────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard(
                    label = Strings.uiStatsPanelCapital,
                    value = formatMoney(playerState.capital),
                    color = StatCapital,
                    modifier = Modifier.weight(1f)
                )
                MoneyCard(
                    label = Strings.uiStatsPanelDebt,
                    value = formatMoney(playerState.debt),
                    color = StatDebt,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))

            // ── DETAIL: Income, Expenses, Investments ──────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard(
                    label = Strings.uiStatsPanelIncome,
                    value = formatMoney(playerState.income) + Strings.uiStatsPanelPerMonth,
                    color = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                MoneyCard(
                    label = Strings.uiStatsPanelExpenses,
                    value = formatMoney(playerState.expenses) + Strings.uiStatsPanelPerMonth,
                    color = StatStress,
                    modifier = Modifier.weight(1f)
                )
                MoneyCard(
                    label = Strings.uiStatsPanelInvestments,
                    value = formatMoney(playerState.investments),
                    color = StatKnowledge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── Soft metrics ──────────────────────────────────────────────────
            Text(
                Strings.uiStatsPanelIndicators,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            StatBar(Strings.uiStatsPanelStress, playerState.stress, StatStress)
            Spacer(Modifier.height(10.dp))
            StatBar(Strings.uiStatsPanelKnowledge, playerState.financialKnowledge, StatKnowledge)
            Spacer(Modifier.height(10.dp))
            StatBar(Strings.uiStatsPanelRisk, playerState.riskLevel, StatRisk)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun MoneyCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.backgroundElevated)
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, color: Color) {
    val colors = LocalAppColors.current
    val animValue by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "statBar"
    )
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text(
                "$value / 100",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(6.dp)
                .clip(CircleShape)
                .background(colors.backgroundElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animValue)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)))
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatMoney(amount: Long): String {
    return when {
        amount >= 1_000_000L -> "${amount / 1_000_000L} М ₸"
        amount >= 1_000L     -> "${amount / 1_000}к ₸"
        else                 -> "$amount ₸"
    }
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

// monthName() removed — replaced inline with Strings.uiStatsPanelMonths.getOrElse()
