package kz.fearsom.financiallifev2.ui.components.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import kz.fearsom.financiallifev2.model.Difficulty
import kz.fearsom.financiallifev2.model.PredefinedCharacter
import kz.fearsom.financiallifev2.ui.components.label
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.PurpleAccent
import kz.fearsom.financiallifev2.ui.theme.RedDanger

@Composable
fun CharacterRosterCard(character: PredefinedCharacter, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val accentColor = when (character.difficulty) {
        Difficulty.EASY -> GreenSuccess
        Difficulty.MEDIUM -> GoldPrimary
        Difficulty.HARD -> RedDanger
        Difficulty.NIGHTMARE -> PurpleAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentColor.copy(0.08f), colors.backgroundCard)
                )
            )
            .border(1.dp, accentColor.copy(0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.radialGradient(listOf(accentColor.copy(0.20f), Color.Transparent)),
                    CircleShape
                )
                .border(1.5.dp, accentColor.copy(0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(character.emoji, fontSize = 26.sp)
        }

        Spacer(Modifier.width(14.dp))

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.profession,
                    fontSize = 12.sp,
                    color = accentColor
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = character.personality,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 2
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("›", fontSize = 20.sp, color = accentColor)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = character.difficulty.label(),
                    fontSize = 9.sp,
                    color = accentColor,
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}