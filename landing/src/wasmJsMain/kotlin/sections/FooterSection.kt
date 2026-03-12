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
fun FooterSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1225),
                        Color(0xFF060A12)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CTA section
            Text(
                text = "Есть вопрос или предложение?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Пиши нам — мы отвечаем в течение 24 часов",
                fontSize = 15.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            FeedbackButton()

            Spacer(Modifier.height(56.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.divider)
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FinanceLifeLine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.gold
                )
                Text(
                    text = "© 2025 FinanceLifeLine. Все права защищены.",
                    fontSize = 12.sp,
                    color = AppColors.textSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Данные предоставлены FTC (Федеральная торговая комиссия США) и FBI IC3 (Центр жалоб на интернет-преступления). " +
                        "Приложение носит образовательный характер.",
                fontSize = 11.sp,
                color = AppColors.textSecondary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun FeedbackButton() {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .background(
                if (isHovered) AppColors.gold.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .border(
                2.dp,
                if (isHovered) AppColors.gold else AppColors.gold.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                openUrl("mailto:contact@financialsim.app")
            }
            .padding(horizontal = 40.dp, vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✉", fontSize = 18.sp)
            Text(
                text = "Написать нам",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.gold
            )
        }
    }
}
