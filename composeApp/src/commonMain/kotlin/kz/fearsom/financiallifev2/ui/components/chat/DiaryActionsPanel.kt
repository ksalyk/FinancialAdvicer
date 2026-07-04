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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.ui.icons.LineIcons
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

// ─── Actions Panel (redesign 2026-07) ─────────────────────────────────────────

/**
 * Bottom action panel: pencil + "ТВОЙ ХОД" header, option cards with a 4dp
 * risk-colored left bar and РИСК/НАДЁЖНО badges. Neutral options render with
 * a border-colored bar and no badge, per the mock's hints-off state.
 */
@Composable
fun DiaryActionsPanel(options: List<GameOption>, onSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    Surface(color = colors.backgroundPanel) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = colors.divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(
                        imageVector = LineIcons.Pencil,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        Strings.uiChatActionLabel.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = colors.textSecondary
                    )
                }
                // Height cap prevents the panel from swallowing the screen when
                // there are 3–4 options; overflow becomes scrollable.
                Column(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
}

@Composable
private fun DiaryActionItem(option: GameOption, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(11.dp)
    val risk = effectRisk(option)

    val barColor = when (risk) {
        OptionRisk.RISKY -> colors.accentNegative
        OptionRisk.SAFE -> colors.accentPositive
        OptionRisk.NEUTRAL -> colors.borderStrong
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),   // WCAG touch target minimum
        shape = shape,
        color = colors.bubbleCharacter,
        border = BorderStroke(1.dp, colors.borderStrong)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Colored left accent bar (full height) ────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    option.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
                when (risk) {
                    OptionRisk.RISKY -> RiskBadge(
                        text = Strings.uiChatOptionRisky,
                        textColor = colors.accentNegativeText,
                        background = colors.accentNegative.copy(alpha = 0.12f)
                    )

                    OptionRisk.SAFE -> RiskBadge(
                        text = Strings.uiChatOptionSafe,
                        textColor = colors.accentPositiveText,
                        background = colors.accentPositive.copy(alpha = 0.12f)
                    )

                    OptionRisk.NEUTRAL -> Unit
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(text: String, textColor: Color, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = textColor
        )
    }
}

/**
 * Risk level for a player choice, derived from its [Effect].
 * Drives the left-bar color and the РИСК/НАДЁЖНО badge in [DiaryActionItem].
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
