package components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import theme.AppColors

@Composable
fun AnimatedCounter(
    targetValue: Float,
    formatter: (Float) -> String,
    label: String,
    color: Color = AppColors.gold,
    delayMs: Long = 300L
) {
    val animated = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animated.snapTo(0f)
        delay(delayMs)
        animated.animateTo(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = 1500, easing = EaseOutQuart)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = formatter(animated.value),
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = AppColors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatMillions(value: Float): String {
    val whole = value.toInt()
    val frac = ((value - whole) * 10).toInt()
    return "${whole}.${frac}M"
}

fun formatBillionsDollar(value: Float): String {
    val whole = value.toInt()
    val frac = ((value - whole) * 10).toInt()
    return "\$${whole}.${frac}B"
}
