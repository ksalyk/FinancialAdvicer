package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.ui.theme.DiaryTextStyle
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

// ─── Writing Indicator ────────────────────────────────────────────────────────

/**
 * "Character is writing..." indicator with handwritten aesthetic (Kalam).
 * Uses DiaryTextStyle for visual continuity with character messages.
 */
@Composable
fun DiaryWritingIndicator() {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "writing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "writingAlpha"
    )

    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✍️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
        Text(
            Strings.uiChatWriting,
            style = DiaryTextStyle.copy(
                fontSize = 14.sp,
                color = colors.diaryInkSecondary.copy(alpha = alpha)
            ),
            color = colors.diaryInkSecondary.copy(alpha = alpha),
            fontStyle = FontStyle.Italic
        )
    }
}