package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.achievements.AchievementCatalog
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.achievements.AchievementKind
import kz.fearsom.financiallifev2.achievements.AchievementRarity
import kz.fearsom.financiallifev2.achievements.AchievementVote
import kz.fearsom.financiallifev2.achievements.ScamTimelineEntry
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.presentation.AchievementsUiState
import kz.fearsom.financiallifev2.ui.components.core.AppTopBar
import kz.fearsom.financiallifev2.ui.theme.AmberOnDark
import kz.fearsom.financiallifev2.ui.theme.CoralOnDark
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.IndigoPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.PurpleGradient

// ── Rarity mapping ─────────────────────────────────────────────────────────────

private val AchievementRarity.accent: Color
    get() = when (this) {
        AchievementRarity.COMMON    -> IndigoPrimary
        AchievementRarity.RARE      -> PurpleGradient
        AchievementRarity.LEGENDARY -> AmberOnDark
    }

private val AchievementRarity.label: String
    get() = when (this) {
        AchievementRarity.COMMON    -> Strings.uiAchRarityCommon
        AchievementRarity.RARE      -> Strings.uiAchRarityRare
        AchievementRarity.LEGENDARY -> Strings.uiAchRarityLegend
    }

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    uiState: AchievementsUiState,
    onSelect: (String) -> Unit,
    onCloseDetail: () -> Unit,
    onVote: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = Strings.uiAchTitle,
                subtitle = Strings.uiAchSubtitle,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                ProgressCard(uiState)

                SectionHeader(
                    label = Strings.uiAchSectionGame,
                    count = "${uiState.gameUnlockedCount} / ${AchievementCatalog.gameAchievements.size}"
                )
                BadgeGrid(
                    definitions = AchievementCatalog.gameAchievements,
                    uiState = uiState,
                    onSelect = onSelect
                )

                SectionHeader(
                    label = Strings.uiAchSectionScams,
                    count = "${uiState.scamUnlockedCount} / ${AchievementCatalog.scamAchievements.size}"
                )
                Text(
                    text = Strings.uiAchScamHint,
                    fontSize = 13.sp,
                    color = colors.textHint,
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp)
                )
                BadgeGrid(
                    definitions = AchievementCatalog.scamAchievements,
                    uiState = uiState,
                    onSelect = onSelect
                )

                Spacer(Modifier.height(40.dp))
            }
        }

        // ── Detail bottom sheet ────────────────────────────────────────────────
        val selected = uiState.selectedId?.let { AchievementCatalog.byId[it] }
        if (selected != null) {
            ModalBottomSheet(
                onDismissRequest = onCloseDetail,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = colors.backgroundCard
            ) {
                AchievementDetail(
                    definition = selected,
                    uiState = uiState,
                    onVote = onVote
                )
            }
        }
    }
}

// ── Progress card ──────────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(uiState: AchievementsUiState) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.backgroundCard)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = Strings.uiAchUnlockedLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Text(
                text = Strings.uiAchCountPattern
                    .replaceFirst("%s", uiState.unlockedCount.toString())
                    .replaceFirst("%s", uiState.totalCount.toString()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AmberOnDark
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.backgroundElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(uiState.progressPct / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(listOf(IndigoPrimary, AmberOnDark))
                    )
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 26.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = colors.textSecondary
        )
        Text(
            text = count,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textHint
        )
    }
}

// ── Badge grid ─────────────────────────────────────────────────────────────────

@Composable
private fun BadgeGrid(
    definitions: List<AchievementDefinition>,
    uiState: AchievementsUiState,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        definitions.chunked(3).forEach { rowDefs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowDefs.forEach { def ->
                    AchievementBadge(
                        definition = def,
                        unlocked = def.id in uiState.unlocks,
                        onClick = { onSelect(def.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad incomplete rows so cells keep equal width
                repeat(3 - rowDefs.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    definition: AchievementDefinition,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val accent = definition.rarity.accent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        if (unlocked) {
                            Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent))
                        } else {
                            Brush.radialGradient(listOf(colors.backgroundElevated, colors.backgroundElevated))
                        },
                        CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (unlocked) accent else colors.borderStrong,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = definition.emoji,
                    fontSize = 30.sp,
                    modifier = Modifier.alpha(if (unlocked) 1f else 0.35f)
                )
            }
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .background(colors.backgroundElevated, CircleShape)
                        .border(1.dp, colors.borderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = 10.sp)
                }
            }
        }
        Text(
            text = Strings[definition.titleKey],
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            color = if (unlocked) colors.textPrimary else colors.textHint
        )
    }
}

// ── Detail sheet ───────────────────────────────────────────────────────────────

@Composable
private fun AchievementDetail(
    definition: AchievementDefinition,
    uiState: AchievementsUiState,
    onVote: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val accent = definition.rarity.accent
    val unlock = uiState.unlocks[definition.id]
    val unlocked = unlock != null
    val isScam = definition.kind == AchievementKind.SCAM

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Badge + title + chips ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(104.dp)
                .background(
                    if (unlocked) {
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent))
                    } else {
                        Brush.radialGradient(listOf(colors.backgroundElevated, colors.backgroundElevated))
                    },
                    CircleShape
                )
                .border(3.dp, if (unlocked) accent else colors.borderStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = definition.emoji,
                fontSize = 48.sp,
                modifier = Modifier.alpha(if (unlocked) 1f else 0.35f)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = Strings[definition.titleKey],
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(text = definition.rarity.label, color = accent)
            val ageKey = definition.dossier?.ageKey
            if (isScam && ageKey != null) {
                Chip(
                    text = "⏳ " + Strings.uiAchAgePattern.replaceFirst("%s", Strings[ageKey]),
                    color = AmberOnDark
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = statusText(definition, unlocked, unlock?.sourceCharacterId),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) GreenSuccess else colors.textHint
        )

        // ── Body ──────────────────────────────────────────────────────────────
        if (!isScam) {
            GameAchievementBody(definition)
        } else if (unlocked) {
            ScamStoryBody(definition)
        } else {
            LockedDossierTeaser()
        }

        // ── Feedback ──────────────────────────────────────────────────────────
        FeedbackRow(
            vote = uiState.votes[definition.id],
            onVote = onVote
        )
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

private fun statusText(
    definition: AchievementDefinition,
    unlocked: Boolean,
    sourceCharacterId: String?
): String = when {
    unlocked -> {
        val characterName = sourceCharacterId
            ?.let { id -> SeedData.predefinedCharacters.find { it.id == id }?.name }
        if (characterName != null) {
            "✓ " + Strings.uiAchUnlockedVia.replaceFirst("%s", characterName)
        } else {
            "✓ " + Strings.uiAchStatusUnlocked
        }
    }
    definition.kind == AchievementKind.SCAM -> "🔒 " + Strings.uiAchStatusLockedScam
    else -> "🔒 " + Strings.uiAchStatusLockedGame
}

// ── Game achievement body: task + hint ─────────────────────────────────────────

@Composable
private fun GameAchievementBody(definition: AchievementDefinition) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        definition.descKey?.let { key ->
            InfoCard(
                label = Strings.uiAchTaskLabel,
                labelColor = colors.textSecondary,
                text = Strings[key],
                background = colors.backgroundSubCard,
                borderColor = colors.border
            )
        }
        definition.hintKey?.let { key ->
            InfoCard(
                label = Strings.uiAchHintLabel,
                labelColor = IndigoPrimary,
                text = Strings[key],
                background = IndigoPrimary.copy(alpha = 0.06f),
                borderColor = IndigoPrimary.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun InfoCard(
    label: String,
    labelColor: Color,
    text: String,
    background: Color,
    borderColor: Color
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = labelColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = colors.textBody
        )
    }
}

// ── Locked scam teaser ─────────────────────────────────────────────────────────

@Composable
private fun LockedDossierTeaser() {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.backgroundSubCard)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔒", fontSize = 28.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = Strings.uiAchTeaserTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = Strings.uiAchTeaserText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            color = colors.textSecondary
        )
    }
}

// ── Scam story timeline ────────────────────────────────────────────────────────

@Composable
private fun ScamStoryBody(definition: AchievementDefinition) {
    val colors = LocalAppColors.current
    val dossier = definition.dossier ?: return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            text = Strings.uiAchStoryTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AmberOnDark,
            modifier = Modifier.padding(start = 2.dp, bottom = 14.dp)
        )

        TimelineEntry(
            entry = dossier.origin,
            sectionLabel = Strings.uiAchOriginLabel,
            isCurrent = false
        )
        dossier.variants.forEach { variant ->
            TimelineEntry(
                entry = variant,
                sectionLabel = Strings.uiAchVariantLabel,
                isCurrent = false
            )
        }
        TimelineEntry(
            entry = dossier.modern,
            sectionLabel = Strings.uiAchModernLabel,
            isCurrent = true
        )

        // ── Red flags ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.backgroundSubCard)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = Strings.uiAchSignsTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(12.dp))
            dossier.signKeys.forEach { signKey ->
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("🚩", fontSize = 13.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = Strings[signKey],
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = colors.textBody
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry(
    entry: ScamTimelineEntry,
    sectionLabel: String,
    isCurrent: Boolean
) {
    val colors = LocalAppColors.current
    val amber = AmberOnDark

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Year pill + connector line
        Column(
            modifier = Modifier.width(62.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isCurrent) Strings.uiAchNow else Strings[entry.yearKey],
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isCurrent) colors.backgroundDeep else amber,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) amber else amber.copy(alpha = 0.10f))
                    .border(1.dp, if (isCurrent) amber else amber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            if (!isCurrent) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(amber.copy(alpha = 0.4f), amber.copy(alpha = 0.1f))
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp))
                .background(colors.backgroundSubCard)
                .border(
                    width = 1.dp,
                    color = if (isCurrent) amber.copy(alpha = 0.45f) else colors.border,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = sectionLabel.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isCurrent) amber else colors.textSecondary
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = Strings[entry.titleKey],
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Strings[entry.textKey],
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = colors.textBody
            )
        }
    }
}

// ── Feedback row ───────────────────────────────────────────────────────────────

@Composable
private fun FeedbackRow(
    vote: String?,
    onVote: (String) -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (vote != null) Strings.uiAchFbThanks else Strings.uiAchFbQuestion,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (vote != null) GreenSuccess else colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VoteButton(
                emoji = "👍",
                active = vote == AchievementVote.UP,
                dimmed = vote == AchievementVote.DOWN,
                activeColor = GreenSuccess,
                onClick = { onVote(AchievementVote.UP) }
            )
            VoteButton(
                emoji = "👎",
                active = vote == AchievementVote.DOWN,
                dimmed = vote == AchievementVote.UP,
                activeColor = CoralOnDark,
                onClick = { onVote(AchievementVote.DOWN) }
            )
        }
    }
}

@Composable
private fun VoteButton(
    emoji: String,
    active: Boolean,
    dimmed: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(52.dp)
            .alpha(if (dimmed) 0.35f else 1f)
            .background(
                if (active) activeColor.copy(alpha = 0.18f) else colors.backgroundElevated,
                CircleShape
            )
            .border(
                1.dp,
                if (active) activeColor else colors.borderStrong,
                CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}
