package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Design system tokens for consistent spacing across the app.
 * Based on 4-point scale: xs=4, sm=8, md=12, lg=16, xl=24, xxl=32, hero=48
 */
object Spacing {
    val xs = 4.dp      // extra small: dividers, minimal gaps
    val sm = 8.dp      // small: spacing within components
    val md = 12.dp     // medium: standard spacing
    val lg = 16.dp     // large: card padding, section gaps
    val xl = 24.dp     // extra large: major section gaps
    val xxl = 32.dp    // 2x large: screen padding
    val hero = 48.dp   // hero: major screen spacing
}

/**
 * Design system tokens for border radius.
 * Semantic radii: xs=4, sm=8, md=14, lg=20, xl=28
 * Plus special shapes for diary metaphor.
 */
object Radius {
    val xs = 4.dp      // minimal curves: chips, small buttons
    val sm = 8.dp      // small components
    val md = 14.dp     // medium cards, standard components
    val lg = 20.dp     // large cards, bottom sheets
    val xl = 28.dp     // extra large, full-screen dialogs

    // Named shapes for diary metaphor
    val diaryEntry = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 4.dp)
    val diaryChoice = RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
}

/**
 * Design system tokens for elevation and shadows.
 */
object Elevation {
    val card = 2.dp      // subtle elevation for cards
    val sheet = 16.dp    // bottom sheet elevation
    val topBar = 6.dp    // top app bar elevation
}

/**
 * Design system tokens for motion and animations.
 * Defines timing and easing for consistent animation feel.
 */
object Motion {
    // Timing durations
    val fast = tween<Float>(150, easing = FastOutSlowInEasing)
    val medium = tween<Float>(280, easing = FastOutSlowInEasing)
    val slow = tween<Float>(450, easing = FastOutSlowInEasing)

    // Press/interaction animation (bouncy, responsive feel)
    val pressScale = spring<Float>(stiffness = Spring.StiffnessHigh)

    // Visibility/alpha transitions
    val fadeIn = tween<Float>(200, easing = FastOutSlowInEasing)
    val fadeOut = tween<Float>(150, easing = FastOutSlowInEasing)
}
