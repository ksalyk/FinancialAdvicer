package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

@Composable
fun DiaryGameOverBar(endingType: EndingType?, onRestart: () -> Unit) {
    val colors = LocalAppColors.current
    val endingColor = Color(
        when (endingType) {
            EndingType.BANKRUPTCY -> 0xFFFF5252
            EndingType.PAYCHECK_TO_PAYCHECK -> 0xFFFF6E40
            EndingType.FINANCIAL_STABILITY -> 0xFF40C4FF
            EndingType.FINANCIAL_FREEDOM -> 0xFFFFD700
            EndingType.WEALTH -> 0xFF00E676
            null -> 0xFF8899BB
        }
    )

    // VFX state management


    Box(modifier = Modifier.fillMaxSize()) {
        GameOverConfetti(endingColor, modifier = Modifier.fillMaxSize())
        GameOverVignette(modifier = Modifier.fillMaxSize())
        GameOverInkBleed(endingColor, modifier = Modifier.fillMaxSize())

        // ── Restart action bar (appears after VFX) ──
        Surface(
            color = colors.backgroundDeep,
            shadowElevation = 16.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when (endingType) {
                        EndingType.BANKRUPTCY -> Strings.endingBankruptcy
                        EndingType.PAYCHECK_TO_PAYCHECK -> Strings.endingPaycheck
                        EndingType.FINANCIAL_STABILITY -> Strings.endingStability
                        EndingType.FINANCIAL_FREEDOM -> Strings.endingFreedom
                        EndingType.WEALTH -> Strings.endingWealth
                        null -> Strings.endingGameOver
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = endingColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = colors.backgroundDeep
                    )
                ) {
                    Text(Strings.uiChatRestart, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ── Game-Over VFX Components ──────────────────────────────────────────────────

/**
 * Falling confetti particles in ending-specific color.
 * Duration: 2.5 seconds with easing fall effect.
 */
@Composable
private fun GameOverConfetti(accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    // Create 8 confetti particles at different horizontal positions
    val confettiPositions = remember {
        listOf(0.1f, 0.25f, 0.4f, 0.55f, 0.7f, 0.85f, 0.15f, 0.65f)
    }

    Box(modifier = modifier) {
        confettiPositions.forEachIndexed { index, xPos ->
            val animDelay = (index * 100).toLong()
            val yAnimation by infiniteTransition.animateFloat(
                initialValue = -0.2f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        2500,
                        delayMillis = animDelay.toInt(),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "confetti_$index"
            )

            // Confetti particle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val centerX = size.width * xPos
                        val centerY = size.height * yAnimation
                        drawCircle(
                            color = accentColor.copy(alpha = 0.7f),
                            radius = 6.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                    }
            )
        }
    }
}

/**
 * Vignette effect: darkening around screen edges.
 * Fades in over 800ms.
 */
@Composable
private fun GameOverVignette(modifier: Modifier = Modifier) {
    val vignetteAlpha by animateFloatAsState(
        targetValue = 0.4f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "vignette"
    )

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteAlpha)),
                radius = 600f
            )
        )
    )
}

/**
 * Ink-bleed effect: animated glow/bloom from center.
 * Fades in from 200-1500ms with bloom effect.
 */
@Composable
private fun GameOverInkBleed(accentColor: Color, modifier: Modifier = Modifier) {
    val bloomAlpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = 1300,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "ink_bleed"
    )

    // Ink bloom grows and fades
    val bloomRadius by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = 1300,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "bloom_radius"
    )

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.3f * (1f - bloomAlpha)),
                    Color.Transparent
                ),
                radius = 400f + (bloomRadius * 200f)
            )
        )
    )
}