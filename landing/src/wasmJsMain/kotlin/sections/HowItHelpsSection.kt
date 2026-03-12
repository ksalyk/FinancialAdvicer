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
import androidx.compose.foundation.layout.size
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
import theme.AppColors

private data class Feature(
    val emoji: String,
    val title: String,
    val description: String,
    val accentColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowItHelpsSection() {
    val features = listOf(
        Feature(
            emoji = "🎮",
            title = "Реальные симуляции",
            description = "Проходи интерактивные сценарии с настоящими схемами мошенничества: фишинг, пирамиды, романтические скамы, инвестиционные ловушки.",
            accentColor = AppColors.gold
        ),
        Feature(
            emoji = "🤖",
            title = "AI-советник Asan",
            description = "Персонализированный ИИ-помощник разбирает каждое твоё решение, объясняет красные флаги и учит мыслить критически в финансовых ситуациях.",
            accentColor = AppColors.blue
        ),
        Feature(
            emoji = "📊",
            title = "Твои финансовые метрики",
            description = "Отслеживай Капитал, Долг, Стресс, Знания и Риск в реальном времени. Видь, как каждое решение влияет на твоё финансовое здоровье.",
            accentColor = AppColors.purple
        )
    )

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
            SectionLabel("КАК ЭТО РАБОТАЕТ", AppColors.blue)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Учись защищаясь в игре",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Три ключевых инструмента, которые делают тебя неуязвимым для мошенников",
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
                features.forEach { feature ->
                    FeatureCard(feature)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature) {
    Box(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 320.dp)
            .background(AppColors.elevated, RoundedCornerShape(20.dp))
            .border(1.dp, feature.accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(28.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(feature.accentColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, feature.accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(feature.emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = feature.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = feature.description,
                fontSize = 14.sp,
                color = AppColors.textSecondary,
                lineHeight = 22.sp
            )
        }
    }
}
