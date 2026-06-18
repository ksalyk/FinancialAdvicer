package kz.fearsom.financiallifev2.ui.screens.character

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kz.fearsom.financiallifev2.data.CatalogRepository
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.CharacterStats
import kz.fearsom.financiallifev2.model.Difficulty
import kz.fearsom.financiallifev2.ui.components.character.SectionCard
import kz.fearsom.financiallifev2.ui.components.core.AppTopBar
import kz.fearsom.financiallifev2.ui.components.label
import kz.fearsom.financiallifev2.ui.components.longFormat
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.PurpleAccent
import kz.fearsom.financiallifev2.ui.theme.RedDanger
import kz.fearsom.financiallifev2.ui.theme.StatKnowledge
import kz.fearsom.financiallifev2.ui.theme.StatStress
import org.koin.compose.koinInject

@Composable
fun CharacterDetailScreen(
    characterId: String,
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    onLoginRequired: () -> Unit,
    onStartGame: (characterId: String) -> Unit   // quick-start with this character
) {
    val colors = LocalAppColors.current
    val catalogRepo: CatalogRepository = koinInject()
    val character = catalogRepo.predefinedCharacters().find { it.id == characterId }
    val canStartGame = character?.isUnlocked == true || isAuthenticated

    if (character == null) {
        Box(Modifier.fillMaxSize().background(colors.backgroundDeep), Alignment.Center) {
            Text(Strings.uiCharDetailNotFound, color = colors.textSecondary)
        }
        return
    }

    val accentColor = when (character.difficulty) {
        Difficulty.EASY -> GreenSuccess
        Difficulty.MEDIUM -> GoldPrimary
        Difficulty.HARD -> RedDanger
        Difficulty.NIGHTMARE -> PurpleAccent
    }

    val infiniteTransition = rememberInfiniteTransition(label = "detail")
    val avatarPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "avatar"
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
                modifier = Modifier
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
                            Brush.radialGradient(
                                listOf(
                                    accentColor.copy(0.25f),
                                    Color.Transparent
                                )
                            ),
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
                    text = character.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${character.age} ${Strings.uiCharDetailAgeEra} ${character.profession}",
                    fontSize = 14.sp,
                    color = accentColor
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = character.personality,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // ── Backstory ─────────────────────────────────────────────────
                SectionCard(title = Strings.uiCharDetailBackstory, accentColor = accentColor) {
                    Text(
                        text = character.backstory,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
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
                            val era = catalogRepo.eras().find { it.id == eraId }
                            if (era != null) {
                                val isLockedForUser = era.isLocked && !isAuthenticated
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(era.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            era.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isLockedForUser) colors.textHint else colors.textPrimary
                                        )
                                        if (isLockedForUser) {
                                            Text(
                                                Strings.uiAuthRequired,
                                                fontSize = 10.sp,
                                                color = colors.textHint
                                            )
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
                            text = character.difficulty.label(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when (character.difficulty) {
                                Difficulty.EASY -> Strings.uiCharDetailDiffEasyDesc
                                Difficulty.MEDIUM -> Strings.uiCharDetailDiffMediumDesc
                                Difficulty.HARD -> Strings.uiCharDetailDiffHardDesc
                                Difficulty.NIGHTMARE -> Strings.uiCharDetailDiffNmDesc
                            },
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── CTA Button ────────────────────────────────────────────────
                if (canStartGame) {
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
                            text = "${Strings.uiCharDetailPlay} ${character.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                } else {
                    Text(
                        text = Strings.uiAuthRequired,
                        fontSize = 14.sp,
                        color = colors.textHint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.backgroundCard)
                            .border(1.dp, colors.textHint.copy(0.2f), RoundedCornerShape(14.dp))
                            .clickable { onLoginRequired() }
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StartingStatsGrid(stats: CharacterStats) {
    val items = listOf(
        Triple(
            Strings.uiCharDetailStatCapital,
            stats.capital.longFormat(),
            GoldPrimary
        ),
        Triple(
            Strings.uiCharDetailStatIncome,
            stats.income.longFormat(),
            GreenSuccess
        ),
        Triple(
            Strings.uiCharDetailStatExpenses,
            stats.monthlyExpenses.longFormat(),
            RedDanger.copy(0.8f)
        ),
        Triple(
            Strings.uiCharDetailStatDebt,
            if (stats.debt > 0) stats.debt.longFormat() else "0",
            if (stats.debt > 0) RedDanger else GreenSuccess
        ),
        Triple(
            Strings.uiCharDetailStatStress,
            "${stats.stress}%",
            StatStress
        ),
        Triple(
            Strings.uiCharDetailStatKnowledge,
            "${stats.financialKnowledge}/100",
            StatKnowledge
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value, color) ->
                    StatCell(
                        label = label,
                        value = value,
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
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
        modifier = modifier
            .background(color.copy(0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
