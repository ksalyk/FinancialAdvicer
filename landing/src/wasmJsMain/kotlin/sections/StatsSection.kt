package sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.AnimatedCounter
import components.formatBillionsDollar
import components.formatMillions
import theme.AppColors

private data class StatItem(
    val year: String,
    val victims: Float,
    val losses: Float,
    val victimDelay: Long,
    val lossDelay: Long
)

private val stats = listOf(
    StatItem("2022", 2.4f, 8.8f, 300, 500),
    StatItem("2023", 2.6f, 10.2f, 400, 600),
    StatItem("2024", 2.9f, 12.5f, 500, 700),
    StatItem("2025", 3.2f, 15.8f, 600, 800)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionLabel("МАСШТАБ ПРОБЛЕМЫ")

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Статистика мошенничества по годам",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "По данным FTC и FBI IC3 — крупнейших агентств по борьбе с мошенничеством",
                fontSize = 15.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                stats.forEach { stat ->
                    YearStatCard(stat)
                }
            }
        }
    }
}

@Composable
private fun YearStatCard(stat: StatItem) {
    Box(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 260.dp)
            .background(AppColors.elevated, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.cardStroke, RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stat.year,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.gold.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            AnimatedCounter(
                targetValue = stat.victims,
                formatter = ::formatMillions,
                label = "жертв скама",
                color = AppColors.gold,
                delayMs = stat.victimDelay
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.divider)
            )

            Spacer(Modifier.height(20.dp))

            AnimatedCounter(
                targetValue = stat.losses,
                formatter = ::formatBillionsDollar,
                label = "потери в долларах",
                color = AppColors.red,
                delayMs = stat.lossDelay
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, color: Color = AppColors.gold) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 2.sp
        )
    }
}
