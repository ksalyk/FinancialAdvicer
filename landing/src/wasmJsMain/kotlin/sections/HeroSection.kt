package sections

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openUrl
import theme.AppColors

@Composable
fun HeroSection() {
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(900, easing = EaseOutCubic))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1225),
                        AppColors.background,
                        AppColors.background
                    )
                )
            )
            .alpha(alphaAnim.value)
    ) {
        // Background decorative circles
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopStart)
                .alpha(0.04f)
                .background(AppColors.gold, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .alpha(0.04f)
                .background(AppColors.purple, CircleShape)
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .background(
                            AppColors.gold.copy(alpha = 0.12f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            AppColors.gold.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Финансовая грамотность через симуляцию",
                        fontSize = 13.sp,
                        color = AppColors.gold,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(24.dp))

                // App name with gradient
                Text(
                    text = "FinanceLifeLine",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(AppColors.goldLight, AppColors.gold, AppColors.goldDark)
                        ),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Миллионы людей теряют деньги из-за мошенников.\nНаучись распознавать угрозы через реальные симуляции.",
                    fontSize = 18.sp,
                    color = AppColors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(48.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroStoreButton(
                        text = "Google Play",
                        subText = "Скачать для Android",
                        color = AppColors.green,
                        onClick = { openUrl("https://play.google.com/store") }
                    )
                    HeroStoreButton(
                        text = "App Store",
                        subText = "Скачать для iOS",
                        color = AppColors.blue,
                        onClick = { openUrl("https://apps.apple.com") }
                    )
                }

                Spacer(Modifier.height(60.dp))

                // Stats strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeroStat("3.2M+", "жертв в 2025")
                    HeroStat("$15.8B", "потерь в 2025")
                    HeroStat("4 года", "статистики")
                }
            }
        }
    }
}

@Composable
private fun HeroStoreButton(
    text: String,
    subText: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .background(
                if (isHovered) color.copy(alpha = 0.15f) else color.copy(alpha = 0.1f),
                RoundedCornerShape(14.dp)
            )
            .border(
                1.5.dp,
                if (isHovered) color else color.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(subText, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
        Text(label, fontSize = 12.sp, color = AppColors.textSecondary)
    }
}
