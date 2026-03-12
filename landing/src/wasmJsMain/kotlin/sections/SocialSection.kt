package sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openUrl
import theme.AppColors

private data class SocialLink(
    val name: String,
    val emoji: String,
    val url: String,
    val color: Color,
    val followers: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialSection() {
    val socials = listOf(
        SocialLink("Telegram", "✈", "#", Color(0xFF29B6F6), "Чат сообщества"),
        SocialLink("Instagram", "📸", "#", Color(0xFFE91E8C), "Советы и контент"),
        SocialLink("Twitter / X", "✕", "#", Color(0xFFF0F4FF), "Новости и статьи"),
        SocialLink("YouTube", "▶", "#", Color(0xFFFF3D3D), "Видео-разборы")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionLabel("СОЦСЕТИ", AppColors.purple)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Будь в курсе новых угроз",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Свежие разборы схем мошенничества, советы по защите и обновления приложения",
                fontSize = 15.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                socials.forEach { social ->
                    SocialCard(social)
                }
            }
        }
    }
}

@Composable
private fun SocialCard(social: SocialLink) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .widthIn(min = 160.dp, max = 200.dp)
            .background(
                if (isHovered) social.color.copy(alpha = 0.08f) else AppColors.elevated,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isHovered) social.color.copy(alpha = 0.4f) else AppColors.cardStroke,
                RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                openUrl(social.url)
            }
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(social.color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = social.emoji,
                    fontSize = 22.sp,
                    color = social.color
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = social.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = social.followers,
                fontSize = 12.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
