package kz.fearsom.financiallifev2.ui.components.character

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.PredefinedCharacter
import kz.fearsom.financiallifev2.ui.components.difficultyColor
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

@Composable
fun PredefinedCharacterCard(
    character: PredefinedCharacter,
    isLocked: Boolean = !character.isUnlocked,
    lockedHint: String = Strings.uiCharSelLocked,
    onClick: () -> Unit,
    onLockedClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "char_scale"
    )

    val accentColor = character.difficulty.difficultyColor()
    val bgGradient = if (isLocked)
        listOf(colors.backgroundCard, colors.backgroundCard)
    else
        listOf(accentColor.copy(alpha = 0.07f), colors.backgroundCard)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(bgGradient))
            .border(
                1.dp,
                if (isLocked) colors.textHint.copy(0.2f) else accentColor.copy(0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLocked || onLockedClick != null) {
                pressed = true
                if (isLocked) onLockedClick?.invoke() else onClick()
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor.copy(0.2f), Color.Transparent)
                        ), CircleShape
                    )
                    .border(1.dp, accentColor.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLocked) "🔒" else character.emoji,
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = character.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked) colors.textHint else colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    CharacterDifficultyBadge(character.difficulty)
                }
                Text(
                    text = "${character.age} ${Strings.uiCharSelAgeSuffix} ${character.profession}",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            if (!isLocked) {
                Text("›", fontSize = 20.sp, color = accentColor)
            }
        }

        if (!isLocked) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = character.personality,
                fontSize = 12.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            // Compact stats row
            CompactStatsRow(stats = character.initialStats)
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text = lockedHint,
                fontSize = 11.sp,
                color = colors.textHint
            )
        }
    }
}
