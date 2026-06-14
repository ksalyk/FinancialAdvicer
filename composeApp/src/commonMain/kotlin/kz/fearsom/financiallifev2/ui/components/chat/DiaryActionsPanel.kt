package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.ui.theme.DiaryHeaderStyle
import kz.fearsom.financiallifev2.ui.theme.DiaryTextStyle
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.RedDanger

// ─── Actions Panel ────────────────────────────────────────────────────────────

@Composable
fun DiaryActionsPanel(options: List<GameOption>, onSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    Surface(color = colors.backgroundDeep, shadowElevation = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("✍️", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                Text(
                    Strings.uiChatActionLabel,
                    style = DiaryHeaderStyle.copy(fontSize = 14.sp, color = colors.textSecondary),
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Height cap prevents the panel from swallowing the screen when there are
            // 3–4 options. Options become vertically scrollable if they overflow.
            Column(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEachIndexed { index, option ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(option.id) {
                        delay(index * 80L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally { it / 2 } + fadeIn(tween(250))
                    ) {
                        DiaryActionItem(option, onClick = { onSelected(option.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryActionItem(option: GameOption, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(8.dp)
    val risk = effectRisk(option)
    val riskColor = when (risk) {
        OptionRisk.SAFE -> GreenSuccess
        OptionRisk.RISKY -> RedDanger
        OptionRisk.NEUTRAL -> GoldPrimary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),   // WCAG touch target minimum
        shape = shape,
        color = colors.backgroundElevated,
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Colored left accent bar ──────────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 48.dp)
                    .fillMaxHeight()
                    .background(
                        color = riskColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (option.emoji.isNotEmpty()) {
                    Text(option.emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                }
                Text(
                    option.text,
                    style = DiaryTextStyle.copy(
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Risk level for a player choice, derived from its [Effect].
 * Drives the left-border accent color in [DiaryActionItem].
 */
private enum class OptionRisk { SAFE, NEUTRAL, RISKY }

private fun effectRisk(option: GameOption): OptionRisk {
    val e = option.effects
    val risky = e.debtDelta > 0 ||
            e.stressDelta > 15 ||
            e.riskDelta > 25 ||
            (e.capitalDelta < -50_000L)
    val safe = !risky && (
            e.stressDelta < -5 ||
                    e.knowledgeDelta > 0 ||
                    (e.capitalDelta > 0 && e.debtDelta <= 0)
            )
    return when {
        risky -> OptionRisk.RISKY
        safe -> OptionRisk.SAFE
        else -> OptionRisk.NEUTRAL
    }
}