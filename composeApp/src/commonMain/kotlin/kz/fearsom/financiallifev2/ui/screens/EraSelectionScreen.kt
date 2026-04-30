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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.Era
import kz.fearsom.financiallifev2.presentation.NewGameUiState
import kz.fearsom.financiallifev2.ui.components.AppTopBar
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun EraSelectionScreen(
    uiState: NewGameUiState,
    onEraSelected: (String) -> Unit,   // eraId
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        // Decorative top glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(listOf(GoldPrimary.copy(alpha = 0.10f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Выбери эпоху",
                subtitle = "Каждая эпоха — уникальные экономические события",
                onBack = onBack
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(uiState.eras) { index, era ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 80L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 }
                    ) {
                        EraCard(era = era, onClick = { if (!era.isLocked) onEraSelected(era.id) })
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun EraCard(era: Era, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label         = "era_card_scale"
    )

    val borderColor = if (era.isLocked) colors.textHint.copy(alpha = 0.3f)
                      else GoldPrimary.copy(alpha = 0.4f)
    val bgGradient  = if (era.isLocked)
        listOf(colors.backgroundCard, colors.backgroundCard)
    else
        listOf(GoldPrimary.copy(alpha = 0.08f), BlueAccent.copy(alpha = 0.04f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(bgGradient))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !era.isLocked) {
                pressed = true
                onClick()
            }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = if (era.isLocked) "🔒" else era.emoji,
                fontSize = 28.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text       = era.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (era.isLocked) colors.textHint else colors.textPrimary
                )
                Text(
                    text     = "${era.startYear}–${era.endYear}",
                    fontSize = 12.sp,
                    color    = colors.textSecondary
                )
            }
            if (!era.isLocked) {
                Text("›", fontSize = 20.sp, color = GoldPrimary)
            }
        }

        if (era.isLocked) {
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "Разблокируйте, пройдя любую другую эпоху",
                fontSize = 11.sp,
                color    = colors.textHint
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                text     = era.description,
                fontSize = 13.sp,
                color    = colors.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))

            // Key events chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                era.keyEconomicEvents.take(3).forEach { event ->
                    Text(
                        text     = event,
                        fontSize = 10.sp,
                        color    = GoldPrimary,
                        modifier = Modifier
                            .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Inflation + salary range
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatPill(
                    label = "Инфляция",
                    value = "${era.baseInflationRate}%",
                    color = if (era.baseInflationRate > 8) RedDanger else GreenSuccess
                )
                StatPill(
                    label = "Зарплаты",
                    value = "${era.baseSalaryMin / 1000}к–${era.baseSalaryMax / 1000}к ₸",
                    color = BlueAccent
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    val colors = LocalAppColors.current
    Row(
        modifier          = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = colors.textSecondary)
        Spacer(Modifier.width(4.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
