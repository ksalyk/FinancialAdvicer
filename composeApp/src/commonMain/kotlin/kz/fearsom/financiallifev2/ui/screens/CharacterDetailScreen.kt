package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.ui.components.AppTopBar
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun CharacterDetailScreen(
    characterId: String,
    onBack: () -> Unit,
    onStartGame: (characterId: String) -> Unit   // quick-start with this character
) {
    val colors    = LocalAppColors.current
    val character = SeedData.predefinedCharacters.find { it.id == characterId }

    if (character == null) {
        Box(Modifier.fillMaxSize().background(colors.backgroundDeep), Alignment.Center) {
            Text(Strings.uiCharDetailNotFound, color = colors.textSecondary)
        }
        return
    }

    val accentColor = when (character.difficulty) {
        Difficulty.EASY      -> GreenSuccess
        Difficulty.MEDIUM    -> GoldPrimary
        Difficulty.HARD      -> RedDanger
        Difficulty.NIGHTMARE -> PurpleAccent
    }

    val infiniteTransition = rememberInfiniteTransition(label = "detail")
    val avatarPulse by infiniteTransition.animateFloat(
        initialValue  = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "avatar"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .background(
                    Brush.radialGradient(listOf(accentColor.copy(0.12f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = character.name,
                subtitle = "${character.profession} · ${character.difficulty.label()}",
                onBack = onBack
            )

            Column(
                modifier       = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Avatar ────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .scale(avatarPulse)
                        .size(100.dp)
                        .background(
                            Brush.radialGradient(listOf(accentColor.copy(0.25f), Color.Transparent)),
                            CircleShape
                        )
                        .border(2.dp, accentColor.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(character.emoji, fontSize = 46.sp)
                }

                Spacer(Modifier.height(16.dp))

                // ── Name & profession ─────────────────────────────────────────
                Text(
                    text       = character.name,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary
                )
                Text(
                    text     = "${character.age} ${Strings.uiCharDetailAgeEra} ${character.profession}",
                    fontSize = 14.sp,
                    color    = accentColor
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = character.personality,
                    fontSize = 13.sp,
                    color    = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // ── Backstory ─────────────────────────────────────────────────
                SectionCard(title = Strings.uiCharDetailBackstory, accentColor = accentColor) {
                    Text(
                        text       = character.backstory,
                        fontSize   = 14.sp,
                        color      = colors.textPrimary,
                        lineHeight = 21.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Starting Stats ────────────────────────────────────────────
                SectionCard(title = Strings.uiCharDetailStats, accentColor = accentColor) {
                    StartingStatsGrid(stats = character.initialStats)
                }

                Spacer(Modifier.height(12.dp))

                // ── Compatible Eras ───────────────────────────────────────────
                SectionCard(title = Strings.uiCharDetailEras, accentColor = accentColor) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        character.compatibleEraIds.forEach { eraId ->
                            val era = SeedData.eras.find { it.id == eraId }
                            if (era != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(era.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            era.name,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = if (era.isLocked) colors.textHint else colors.textPrimary
                                        )
                                        if (era.isLocked) {
                                            Text(Strings.uiCharDetailLockedEra, fontSize = 10.sp, color = colors.textHint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Difficulty Info ───────────────────────────────────────────
                SectionCard(title = Strings.uiCharDetailDifficulty, accentColor = accentColor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text     = character.difficulty.label(),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text     = when (character.difficulty) {
                                Difficulty.EASY      -> Strings.uiCharDetailDiffEasyDesc
                                Difficulty.MEDIUM    -> Strings.uiCharDetailDiffMediumDesc
                                Difficulty.HARD      -> Strings.uiCharDetailDiffHardDesc
                                Difficulty.NIGHTMARE -> Strings.uiCharDetailDiffNmDesc
                            },
                            fontSize = 12.sp,
                            color    = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── CTA Button ────────────────────────────────────────────────
                if (character.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor.copy(0.3f), accentColor.copy(0.15f))
                                )
                            )
                            .border(1.dp, accentColor.copy(0.6f), RoundedCornerShape(14.dp))
                            .clickable { onStartGame(character.id) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = "${Strings.uiCharDetailPlay} ${character.name}",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = colors.textPrimary
                        )
                    }
                } else {
                    Text(
                        text     = Strings.uiCharDetailLockedChar,
                        fontSize = 14.sp,
                        color    = colors.textHint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.backgroundCard)
                            .border(1.dp, colors.textHint.copy(0.2f), RoundedCornerShape(14.dp))
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Section Card ──────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(listOf(accentColor.copy(0.05f), colors.backgroundCard))
            )
            .border(1.dp, accentColor.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text       = title,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = accentColor
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

// ── Starting Stats Grid ───────────────────────────────────────────────────────

@Composable
private fun StartingStatsGrid(stats: CharacterStats) {
    val items = listOf(
        Triple(Strings.uiCharDetailStatCapital,   stats.capital.longFormat(),              GoldPrimary),
        Triple(Strings.uiCharDetailStatIncome,    stats.income.longFormat(),               GreenSuccess),
        Triple(Strings.uiCharDetailStatExpenses,  stats.monthlyExpenses.longFormat(),      RedDanger.copy(0.8f)),
        Triple(Strings.uiCharDetailStatDebt,      if (stats.debt > 0) stats.debt.longFormat() else "0",
                                                                           if (stats.debt > 0) RedDanger else GreenSuccess),
        Triple(Strings.uiCharDetailStatStress,    "${stats.stress}%",                      StatStress),
        Triple(Strings.uiCharDetailStatKnowledge, "${stats.financialKnowledge}/100",        StatKnowledge)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value, color) ->
                    StatCell(label = label, value = value, color = color, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color, modifier: Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier           = modifier
            .background(color.copy(0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Number format ─────────────────────────────────────────────────────────────

private fun Long.longFormat(): String = when {
    this >= 1_000_000L -> "${this / 1_000_000}.${(this % 1_000_000) / 100_000}М ₸"
    this >= 1_000L     -> "${this / 1_000}к ₸"
    else               -> "$this ₸"
}
