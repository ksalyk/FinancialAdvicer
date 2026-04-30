package kz.fearsom.financiallifev2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kz.fearsom.financiallifev2.ui.theme.Spacing
import kz.fearsom.financiallifev2.ui.theme.Radius

/**
 * Reusable accent card component that consolidates common card styling.
 * Fixes the "pressed" state bug by using MutableInteractionSource.
 *
 * Usage:
 * ```
 * AccentCard(
 *   gradient = listOf(GoldPrimary.copy(0.08f), BlueAccent.copy(0.04f)),
 *   borderColor = GoldPrimary.copy(0.4f),
 *   onClick = { ... },
 *   modifier = Modifier.fillMaxWidth(),
 *   enabled = true,
 *   content = { /* Your card content */ }
 * )
 * ```
 */
@Composable
fun AccentCard(
    gradient: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: RoundedCornerShape = RoundedCornerShape(Radius.md),
    padding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    // Proper scale animation that resets when interaction ends
    val scale = animateFloatAsState(
        targetValue = if (isPressed.value && enabled) 0.97f else 1f,
        animationSpec = tween(100),
        label = "card_scale"
    )

    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(cornerRadius)
            .background(Brush.horizontalGradient(gradient))
            .border(1.dp, borderColor, cornerRadius)
            .clickable(
                enabled = enabled,
                indication = ripple(),
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(padding)
    ) {
        content()
    }
}

/**
 * Lightweight card without interaction effects, for static display.
 * Used for disabled states or when no click interaction needed.
 */
@Composable
fun StaticCard(
    gradient: List<Color>,
    borderColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: RoundedCornerShape = RoundedCornerShape(Radius.md),
    padding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(cornerRadius)
            .background(Brush.horizontalGradient(gradient))
            .border(1.dp, borderColor, cornerRadius)
            .padding(padding)
    ) {
        content()
    }
}
