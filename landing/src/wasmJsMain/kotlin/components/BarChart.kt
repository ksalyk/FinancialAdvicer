package components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import theme.AppColors

data class BarData(
    val label: String,
    val value: Float,
    val displayValue: String,
    val color: Color = AppColors.gold
)

@Composable
fun BarChart(
    data: List<BarData>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 200
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        delay(400)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight.dp)
        ) {
            val maxValue = data.maxOf { it.value }
            val barGapPx = 12.dp.toPx()
            val totalGaps = barGapPx * (data.size - 1)
            val barWidth = (size.width - totalGaps) / data.size
            val topPadding = 16.dp.toPx()
            val availableHeight = size.height - topPadding

            data.forEachIndexed { index, bar ->
                val heightFraction = (bar.value / maxValue) * animProgress.value
                val barHeight = availableHeight * heightFraction
                val x = index * (barWidth + barGapPx)
                val y = size.height - barHeight

                // Background column (subtle)
                drawRect(
                    color = bar.color.copy(alpha = 0.06f),
                    topLeft = Offset(x, topPadding),
                    size = Size(barWidth, availableHeight)
                )

                // Gradient bar
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(bar.color, bar.color.copy(alpha = 0.6f)),
                        startY = y,
                        endY = size.height
                    ),
                    topLeft = Offset(x + 4.dp.toPx(), y),
                    size = Size(barWidth - 8.dp.toPx(), barHeight)
                )

                // Top highlight line
                if (barHeight > 4.dp.toPx()) {
                    drawRect(
                        color = bar.color.copy(alpha = 0.9f),
                        topLeft = Offset(x + 4.dp.toPx(), y),
                        size = Size(barWidth - 8.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { bar ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = bar.displayValue,
                        fontSize = 13.sp,
                        color = bar.color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = bar.label,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary
                    )
                }
            }
        }
    }
}
