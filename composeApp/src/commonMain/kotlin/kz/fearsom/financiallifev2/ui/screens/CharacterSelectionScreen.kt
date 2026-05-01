package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.presentation.NewGameUiState
import kz.fearsom.financiallifev2.ui.components.AppTopBar
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun CharacterSelectionScreen(
    uiState: NewGameUiState,
    onSelectPredefined: (String) -> Unit,   // characterId → sessionId returned from presenter
    onSelectBundle: (String) -> Unit,        // bundleId
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val selectedEra = uiState.eras.find { it.id == uiState.selectedEraId }

    var activeTab by remember { mutableStateOf(0) }  // 0 = characters, 1 = bundles

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(PurpleAccent.copy(alpha = 0.08f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = Strings.uiCharSelTitle,
                subtitle = if (selectedEra != null) "${selectedEra.emoji} ${selectedEra.name}" else null,
                onBack = onBack
            )

            Spacer(Modifier.height(12.dp))

            // ── Tab switcher ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.backgroundCard)
                    .padding(4.dp)
            ) {
                TabButton(
                    label    = Strings.uiCharSelTabCharacters,
                    selected = activeTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick  = { activeTab = 0 }
                )
                TabButton(
                    label    = Strings.uiCharSelTabBundles,
                    selected = activeTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick  = { activeTab = 1 }
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "tab_content"
            ) { tab ->
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tab == 0) {
                        itemsIndexed(uiState.availableCharacters) { index, char ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(index * 60L)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter   = fadeIn(tween(220)) + slideInVertically { it / 2 }
                            ) {
                                PredefinedCharacterCard(
                                    character = char,
                                    onClick   = { if (char.isUnlocked) onSelectPredefined(char.id) }
                                )
                            }
                        }
                    } else {
                        itemsIndexed(uiState.availableBundles) { index, bundle ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(index * 60L)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter   = fadeIn(tween(220)) + slideInVertically { it / 2 }
                            ) {
                                BundleCard(
                                    bundle  = bundle,
                                    onClick = { if (!bundle.isLocked) onSelectBundle(bundle.id) }
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ── Tab Button ────────────────────────────────────────────────────────────────

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.backgroundElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (selected) colors.textPrimary else colors.textSecondary
        )
    }
}

// ── Predefined Character Card ─────────────────────────────────────────────────

@Composable
private fun PredefinedCharacterCard(character: PredefinedCharacter, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label         = "char_scale"
    )

    val isLocked    = !character.isUnlocked
    val accentColor = difficultyColor(character.difficulty)
    val bgGradient  = if (isLocked)
        listOf(colors.backgroundCard, colors.backgroundCard)
    else
        listOf(accentColor.copy(alpha = 0.07f), colors.backgroundCard)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(bgGradient))
            .border(1.dp,
                if (isLocked) colors.textHint.copy(0.2f) else accentColor.copy(0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLocked) {
                pressed = true
                onClick()
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor.copy(0.2f), Color.Transparent)
                        ), CircleShape
                    )
                    .border(1.dp, accentColor.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = if (isLocked) "🔒" else character.emoji,
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = character.name,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isLocked) colors.textHint else colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    DifficultyBadge(character.difficulty)
                }
                Text(
                    text     = "${character.age} ${Strings.uiCharSelAgeSuffix} ${character.profession}",
                    fontSize = 12.sp,
                    color    = colors.textSecondary
                )
            }
            if (!isLocked) {
                Text("›", fontSize = 20.sp, color = accentColor)
            }
        }

        if (!isLocked) {
            Spacer(Modifier.height(10.dp))
            Text(
                text       = character.personality,
                fontSize   = 12.sp,
                color      = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            // Compact stats row
            CompactStatsRow(stats = character.initialStats)
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text     = Strings.uiCharSelLocked,
                fontSize = 11.sp,
                color    = colors.textHint
            )
        }
    }
}

// ── Bundle Card ───────────────────────────────────────────────────────────────

@Composable
private fun BundleCard(bundle: CharacterBundle, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label         = "bundle_scale"
    )

    val accentColor  = difficultyColor(bundle.difficulty)
    val isLocked     = bundle.isLocked
    val bundleGradient = if (isLocked)
        listOf(colors.backgroundCard, colors.backgroundCard)
    else
        listOf(accentColor.copy(alpha = 0.07f), colors.backgroundCard)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(bundleGradient))
            .border(1.dp,
                if (isLocked) colors.textHint.copy(0.2f) else accentColor.copy(0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLocked) {
                pressed = true
                onClick()
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = if (isLocked) "🔒" else bundle.emoji,
                fontSize = 28.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = bundle.label,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isLocked) colors.textHint else colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    DifficultyBadge(bundle.difficulty)
                }
                Text(
                    text     = bundle.profession,
                    fontSize = 11.sp,
                    color    = colors.textSecondary
                )
            }
            if (!isLocked) {
                Text("›", fontSize = 20.sp, color = accentColor)
            }
        }

        if (!isLocked) {
            Spacer(Modifier.height(8.dp))
            Text(
                text       = bundle.description,
                fontSize   = 12.sp,
                color      = colors.textSecondary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bundle.traits.forEach { trait ->
                    Text(
                        text     = trait,
                        fontSize = 10.sp,
                        color    = accentColor,
                        modifier = Modifier
                            .background(accentColor.copy(0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CompactStatsRow(stats = bundle.stats)
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text     = bundle.unlockCondition?.unlockHint() ?: Strings.uiCharSelLocked,
                fontSize = 11.sp,
                color    = colors.textHint
            )
        }
    }
}

// ── Compact Stats Row ─────────────────────────────────────────────────────────

@Composable
private fun CompactStatsRow(stats: CharacterStats) {
    val colors = LocalAppColors.current
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniStat("💰", stats.capital.shortFormat(),     GoldPrimary,    Modifier.weight(1f))
        MiniStat("📈", "${stats.income / 1000}k${Strings.uiCharSelPerMonth}",  GreenSuccess,   Modifier.weight(1f))
        MiniStat("😰", "${stats.stress}%",              StatStress,     Modifier.weight(1f))
        MiniStat("🎓", "${stats.financialKnowledge}",   StatKnowledge,  Modifier.weight(1f))
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, color: Color, modifier: Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier           = modifier
            .background(color.copy(0.07f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 13.sp)
        // Raised from 10sp to 12sp for accessibility (WCAG AA normal text minimum)
        Text(value, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Difficulty Badge ──────────────────────────────────────────────────────────

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val color = difficultyColor(difficulty)
    Text(
        text     = difficulty.label(),
        // Raised from 9sp to 11sp for accessibility (below 12sp comfort floor)
        fontSize = 11.sp,
        color    = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color.copy(0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun difficultyColor(d: Difficulty): Color = when (d) {
    Difficulty.EASY      -> GreenSuccess
    Difficulty.MEDIUM    -> GoldPrimary
    Difficulty.HARD      -> RedDanger
    Difficulty.NIGHTMARE -> PurpleAccent
}

fun Difficulty.label(): String = when (this) {
    Difficulty.EASY      -> Strings.uiCharSelDiffEasy
    Difficulty.MEDIUM    -> Strings.uiCharSelDiffMedium
    Difficulty.HARD      -> Strings.uiCharSelDiffHard
    Difficulty.NIGHTMARE -> Strings.uiCharSelDiffNightmare
}

private fun Long.shortFormat(): String = when {
    this >= 1_000_000L -> "${this / 1_000_000}M₸"
    this >= 1_000L     -> "${this / 1_000}k₸"
    else               -> "$this₸"
}

private fun UnlockCondition.unlockHint(): String = when (this) {
    is UnlockCondition.FinishGameWith -> "${Strings.uiCharSelUnlockComplete} ${ending.emoji()} ${ending.label()}"
    is UnlockCondition.ReachCapital   -> "${Strings.uiCharSelUnlockReach} ${amount.shortFormat()}"
    is UnlockCondition.PlayEra        -> "${Strings.uiCharSelUnlockEra} $eraId"
    is UnlockCondition.CompleteGames  -> "${Strings.uiCharSelUnlockCompleteN} $count"
}
