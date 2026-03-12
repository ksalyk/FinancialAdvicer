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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openUrl
import theme.AppColors

@Composable
fun DownloadSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.background,
                        Color(0xFF0D1225)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛡️",
                fontSize = 48.sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Скачай FinanceLifeLine",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Доступно бесплатно на Android и iOS.\nНачни учиться защищаться уже сейчас.",
                fontSize = 16.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadButton(
                    platform = "Google Play",
                    icon = "▶",
                    description = "Android",
                    primaryColor = AppColors.green,
                    onClick = { openUrl("https://play.google.com/store") }
                )
                DownloadButton(
                    platform = "App Store",
                    icon = "◆",
                    description = "iOS",
                    primaryColor = AppColors.blue,
                    onClick = { openUrl("https://apps.apple.com") }
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Бесплатно · Без рекламы · Только обучение",
                fontSize = 13.sp,
                color = AppColors.textSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DownloadButton(
    platform: String,
    icon: String,
    description: String,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .background(
                if (isHovered) primaryColor else primaryColor.copy(alpha = 0.85f),
                RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                color = Color.White
            )
            Column {
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = platform,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
