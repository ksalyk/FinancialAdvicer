package kz.fearsom.financiallifev2.ui.components.character

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.CharacterBundle
import kz.fearsom.financiallifev2.ui.components.difficultyColor
import kz.fearsom.financiallifev2.ui.components.unlockHint
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

@Composable
private fun BundleCard(bundle: CharacterBundle, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label         = "bundle_scale"
    )

    val accentColor  = bundle.difficulty.difficultyColor()
    val isLocked     = bundle.isLocked
    val bundleGradient = if (isLocked)
        listOf(colors.backgroundCard, colors.backgroundCard)
    else
        listOf(accentColor.copy(alpha = 0.07f), colors.backgroundCard)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(bundleGradient))
            .border(1.dp,
                if (isLocked) colors.textHint.copy(0.2f) else accentColor.copy(0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLocked) {
                pressed = true
                onClick()
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = if (isLocked) "🔒" else bundle.emoji,
                fontSize = 28.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = bundle.label,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isLocked) colors.textHint else colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    CharacterDifficultyBadge(bundle.difficulty)
                }
                Text(
                    text     = bundle.profession,
                    fontSize = 11.sp,
                    color    = colors.textSecondary
                )
            }
            if (!isLocked) {
                Text("›", fontSize = 20.sp, color = accentColor)
            }
        }

        if (!isLocked) {
            Spacer(Modifier.height(8.dp))
            Text(
                text       = bundle.description,
                fontSize   = 12.sp,
                color      = colors.textSecondary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bundle.traits.forEach { trait ->
                    Text(
                        text     = trait,
                        fontSize = 10.sp,
                        color    = accentColor,
                        modifier = Modifier
                            .background(accentColor.copy(0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CompactStatsRow(stats = bundle.stats)
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text     = bundle.unlockCondition?.unlockHint() ?: Strings.uiCharSelLocked,
                fontSize = 11.sp,
                color    = colors.textHint
            )
        }
    }
}

