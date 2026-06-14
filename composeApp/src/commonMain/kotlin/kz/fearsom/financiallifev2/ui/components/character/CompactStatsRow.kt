package kz.fearsom.financiallifev2.ui.components.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.CharacterStats
import kz.fearsom.financiallifev2.ui.components.shortFormat
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess

@Composable
fun CompactStatsRow(stats: CharacterStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PrimaryStatPill(
            emoji = "💰",
            value = stats.capital.shortFormat(),
            label = Strings.uiCharSelStatCapital,
            color = GoldPrimary,
            modifier = Modifier.weight(1f)
        )
        PrimaryStatPill(
            emoji = "📈",
            value = "${stats.income / 1000}k${Strings.uiCharSelPerMonth}",
            label = Strings.uiCharSelStatIncome,
            color = GreenSuccess,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PrimaryStatPill(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
        // Larger value (16sp was 12sp) — much easier to scan and compare
        Text(value, fontSize = 16.sp, color = color, fontWeight = FontWeight.Bold)
        Text(
            label,
            fontSize = 11.sp,
            color = color.copy(alpha = 0.70f),
            fontWeight = FontWeight.Normal
        )
    }
}