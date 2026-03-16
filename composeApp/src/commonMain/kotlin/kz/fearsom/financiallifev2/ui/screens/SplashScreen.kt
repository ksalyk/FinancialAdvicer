package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.ui.theme.*

@Composable
fun SplashScreen() {
    val colors = LocalAppColors.current

    // ── Entry animation trigger ────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    // Logo: spring scale + fade
    val logoScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0.35f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(450),
        label         = "logoAlpha"
    )

    // Title: fade + slide up
    val titleAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(450, delayMillis = 250),
        label         = "titleAlpha"
    )
    val titleOffset by animateDpAsState(
        targetValue   = if (started) 0.dp else 18.dp,
        animationSpec = tween(450, delayMillis = 250, easing = FastOutSlowInEasing),
        label         = "titleOffset"
    )

    // Subtitle: fade
    val subtitleAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(450, delayMillis = 500),
        label         = "subtitleAlpha"
    )

    // ── Infinite animations ────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splashInfinite")

    // Glow ring pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Loading dots wave (0 → 3 linear, each dot [i, i+1) is its active window)
    val dotPhase by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotPhase"
    )

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors    = listOf(BackgroundDeep, Color(0xFF0D1630), BackgroundDeep),
                    startY    = 0f,
                    endY      = Float.POSITIVE_INFINITY
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Logo + title ───────────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Pulsing glow ring + logo coin
            Box(contentAlignment = Alignment.Center) {

                // Outer glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .alpha(0.28f * logoAlpha)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GoldPrimary, Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                // Coin circle
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                        .background(
                            Brush.linearGradient(listOf(GoldLight, GoldPrimary, GoldDark)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "$",
                        fontSize   = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color      = BackgroundDeep
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text       = "Financial Life",
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary,
                modifier   = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text     = "Master your financial story",
                fontSize = 14.sp,
                color    = colors.textSecondary,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }

        // ── Loading dots ───────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                // Each dot occupies a 1-unit window of dotPhase (0–3 cycling)
                val phase = ((dotPhase - i.toFloat() + 3f) % 3f)
                val dotAlpha = when {
                    phase < 0.5f -> 0.2f + (phase / 0.5f) * 0.8f         // fade in
                    phase < 1.0f -> 1.0f - ((phase - 0.5f) / 0.5f) * 0.8f // fade out
                    else         -> 0.2f                                    // idle
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(dotAlpha)
                        .background(GoldPrimary, CircleShape)
                )
            }
        }
    }
}
