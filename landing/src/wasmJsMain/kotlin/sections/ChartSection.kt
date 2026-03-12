package sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.BarChart
import components.BarData
import theme.AppColors

@Composable
fun ChartSection() {
    var selectedTab by remember { mutableStateOf(0) }

    val victimsData = listOf(
        BarData("2022", 2.4f, "2.4M", AppColors.gold),
        BarData("2023", 2.6f, "2.6M", AppColors.gold),
        BarData("2024", 2.9f, "2.9M", AppColors.gold),
        BarData("2025", 3.2f, "3.2M", AppColors.gold)
    )

    val lossesData = listOf(
        BarData("2022", 8.8f, "\$8.8B", AppColors.red),
        BarData("2023", 10.2f, "\$10.2B", AppColors.red),
        BarData("2024", 12.5f, "\$12.5B", AppColors.red),
        BarData("2025", 15.8f, "\$15.8B", AppColors.red)
    )

    val currentData = if (selectedTab == 0) victimsData else lossesData

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionLabel("4 ГОДА ДАННЫХ", AppColors.purple)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Рост мошенничества год за годом",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Анимированная статистика по количеству жертв и финансовым потерям",
                fontSize = 15.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Tab switcher
            Row(
                modifier = Modifier
                    .background(AppColors.card, RoundedCornerShape(12.dp))
                    .border(1.dp, AppColors.cardStroke, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartTab(
                    text = "Жертвы",
                    selected = selectedTab == 0,
                    color = AppColors.gold,
                    onClick = { selectedTab = 0 }
                )
                ChartTab(
                    text = "Потери (USD)",
                    selected = selectedTab == 1,
                    color = AppColors.red,
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Chart card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.card, RoundedCornerShape(16.dp))
                    .border(1.dp, AppColors.cardStroke, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Число жертв (млн)" else "Финансовые потери (млрд \$)",
                            fontSize = 14.sp,
                            color = AppColors.textSecondary
                        )
                        Text(
                            text = "Источник: FTC / FBI IC3",
                            fontSize = 11.sp,
                            color = AppColors.textSecondary.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    BarChart(
                        data = currentData,
                        chartHeight = 220
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "* Данные за 2025 год основаны на промежуточных отчётах",
                fontSize = 12.sp,
                color = AppColors.textSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChartTab(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .background(
                when {
                    selected -> color.copy(alpha = 0.15f)
                    isHovered -> AppColors.elevated
                    else -> Color.Transparent
                },
                RoundedCornerShape(8.dp)
            )
            .then(
                if (selected) Modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) color else AppColors.textSecondary
        )
    }
}
