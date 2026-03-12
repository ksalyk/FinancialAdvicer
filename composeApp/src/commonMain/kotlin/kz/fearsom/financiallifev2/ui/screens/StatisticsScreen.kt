package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.presentation.StatisticsUiState
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(listOf(GreenSuccess.copy(0.08f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Назад", fontSize = 14.sp, color = colors.textSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text       = "Статистика",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary
                )
                Text(
                    text     = "Твои финансовые достижения",
                    fontSize = 13.sp,
                    color    = colors.textSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка...", color = colors.textSecondary)
                }
            } else if (!uiState.hasAnyGames || uiState.stats == null) {
                EmptyStatsPlaceholder()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val stats = uiState.stats

                    // ── Summary row ───────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryTile(
                            emoji  = "🎮",
                            label  = "Всего игр",
                            value  = stats.totalGamesPlayed.toString(),
                            color  = BlueAccent,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryTile(
                            emoji  = "✅",
                            label  = "Завершено",
                            value  = stats.gamesCompleted.toString(),
                            color  = GreenSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryTile(
                            emoji  = "💰",
                            label  = "Средний капитал",
                            value  = stats.averageCapitalAtEnd.compactFormat(),
                            color  = GoldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ── Best ending ───────────────────────────────────────────
                    stats.bestEnding?.let { BestEndingCard(ending = it) }

                    // ── Ending distribution ───────────────────────────────────
                    val nonZeroEndings = stats.endingDistribution.filter { it.value > 0 }
                    if (nonZeroEndings.isNotEmpty()) {
                        StatsSection(title = "📈 Финалы") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                nonZeroEndings.entries.sortedByDescending { it.value }.forEach { (ending, count) ->
                                    EndingRow(
                                        ending = ending,
                                        count  = count,
                                        total  = stats.gamesCompleted.coerceAtLeast(1)
                                    )
                                }
                            }
                        }
                    }

                    // ── Per character ─────────────────────────────────────────
                    if (stats.perCharacter.isNotEmpty()) {
                        StatsSection(title = "👥 По персонажам") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.perCharacter.sortedByDescending { it.timesPlayed }.forEach { charStat ->
                                    CharacterStatRow(stat = charStat)
                                }
                            }
                        }
                    }

                    // ── Per era ───────────────────────────────────────────────
                    if (stats.perEra.isNotEmpty()) {
                        StatsSection(title = "🗺️ По эпохам") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.perEra.sortedByDescending { it.timesPlayed }.forEach { eraStat ->
                                    EraStatRow(stat = eraStat)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Empty Placeholder ──────────────────────────────────────────────────────────

@Composable
private fun EmptyStatsPlaceholder() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📊", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "Статистика пуста",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Пройди хотя бы одну игру, чтобы увидеть свои результаты",
                fontSize  = 14.sp,
                color     = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Summary Tile ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryTile(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier           = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.08f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

// ── Best Ending Card ──────────────────────────────────────────────────────────

@Composable
private fun BestEndingCard(ending: GameEnding) {
    val colors = LocalAppColors.current
    val color  = endingAccentColor(ending)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(0.15f), color.copy(0.05f))))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(ending.emoji(), fontSize = 32.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text       = "Лучший финал",
                fontSize   = 11.sp,
                color      = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text       = ending.label(),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ── Ending Distribution Row ───────────────────────────────────────────────────

@Composable
private fun EndingRow(ending: GameEnding, count: Int, total: Int) {
    val colors   = LocalAppColors.current
    val color    = endingAccentColor(ending)
    val fraction = count.toFloat() / total.toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ending.emoji(), fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text     = ending.label(),
                fontSize = 13.sp,
                color    = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text     = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color    = color
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress          = { fraction },
            modifier          = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color             = color,
            trackColor        = color.copy(0.15f)
        )
    }
}

// ── Character Stat Row ────────────────────────────────────────────────────────

@Composable
private fun CharacterStatRow(stat: CharacterStatistics) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.backgroundCard)
            .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stat.characterEmoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stat.characterName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(
                "${stat.timesPlayed} игр · Ср. капитал: ${stat.averageCapital.compactFormat()}",
                fontSize = 11.sp, color = colors.textSecondary
            )
        }
        stat.bestEnding?.let { Text(it.emoji(), fontSize = 18.sp) }
    }
}

// ── Era Stat Row ──────────────────────────────────────────────────────────────

@Composable
private fun EraStatRow(stat: EraStatistics) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.backgroundCard)
            .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stat.eraName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text("${stat.timesPlayed} игр", fontSize = 11.sp, color = colors.textSecondary)
        }
        stat.bestEnding?.let { Text(it.emoji(), fontSize = 18.sp) }
    }
}

// ── Stats Section wrapper ─────────────────────────────────────────────────────

@Composable
private fun StatsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.backgroundCard)
            .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun endingAccentColor(ending: GameEnding): Color = when (ending) {
    GameEnding.BANKRUPTCY           -> RedDanger
    GameEnding.PAYCHECK_TO_PAYCHECK -> StatStress
    GameEnding.FINANCIAL_STABILITY  -> BlueAccent
    GameEnding.FINANCIAL_FREEDOM    -> GoldPrimary
    GameEnding.WEALTH               -> GreenSuccess
    GameEnding.PRISON               -> PurpleAccent
}

private fun Long.compactFormat(): String = when {
    this >= 1_000_000L -> "${this / 1_000_000}М₸"
    this >= 1_000L     -> "${this / 1_000}к₸"
    else               -> "$this₸"
}
