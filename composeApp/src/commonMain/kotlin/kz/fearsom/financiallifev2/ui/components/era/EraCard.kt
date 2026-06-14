package kz.fearsom.financiallifev2.ui.components.era

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.Era
import kz.fearsom.financiallifev2.ui.theme.BlueAccent
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.RedDanger

@Composable
fun EraCard(era: Era, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val borderColor = if (era.isLocked) colors.textHint.copy(alpha = 0.3f)
    else GoldPrimary.copy(alpha = 0.4f)
    val bgGradient  = if (era.isLocked)
        listOf(colors.backgroundCard, colors.backgroundCard)
    else
        listOf(GoldPrimary.copy(alpha = 0.08f), BlueAccent.copy(alpha = 0.04f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(bgGradient))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !era.isLocked) {
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
                text     = Strings.uiEraLockedHint,
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
                    label = Strings.uiEraInflation,
                    value = "${era.baseInflationRate}%",
                    color = if (era.baseInflationRate > 8) RedDanger else GreenSuccess
                )
                StatPill(
                    label = Strings.uiEraSalary,
                    value = "${era.baseSalaryMin / 1000}k–${era.baseSalaryMax / 1000}k ₸",
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