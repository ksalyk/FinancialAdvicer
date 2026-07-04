package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

// ─── Writing Indicator (redesign 2026-07) ─────────────────────────────────────

/**
 * Typing indicator: character bubble with three bouncing dots, mirroring the
 * mock's `flldot` keyframes (staggered translateY + alpha).
 */
@Composable
fun DiaryWritingIndicator(characterName: String = "") {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "typing")
    // One master phase 0..1 over 900ms; each dot samples it with an offset.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "typingPhase"
    )

    Row(verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(colors.backgroundElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = characterName.firstOrNull()?.uppercase() ?: "•",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .background(
                    colors.bubbleCharacter,
                    RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                )
                .border(
                    1.dp,
                    colors.border,
                    RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                // flldot: 0%,60%,100% → rest (alpha .4); 30% → lifted (alpha 1)
                val local = ((phase - index * 0.15f + 1f) % 1f)
                val lift = when {
                    local < 0.3f -> local / 0.3f              // rising
                    local < 0.6f -> 1f - (local - 0.3f) / 0.3f // falling
                    else -> 0f                                  // resting
                }
                Box(
                    modifier = Modifier
                        .offset(y = (-4).dp * lift)
                        .alpha(0.4f + 0.6f * lift)
                        .size(7.dp)
                        .background(colors.textSecondary, CircleShape)
                )
            }
        }
    }
}
