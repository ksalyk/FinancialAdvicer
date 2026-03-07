package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape system with consistent border radius
 */
val AppShapes = Shapes(
    // Extra small - for small components like chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),

    // Small - for medium components
    small = RoundedCornerShape(8.dp),

    // Medium - for standard components like cards, dialogs
    medium = RoundedCornerShape(12.dp),

    // Large - for larger components
    large = RoundedCornerShape(16.dp),

    // Extra large - for full-screen components
    extraLarge = RoundedCornerShape(28.dp)
)
