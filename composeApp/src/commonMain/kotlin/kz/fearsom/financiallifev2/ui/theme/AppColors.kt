package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color set that switches between dark and light palettes.
 * Brand/accent colors (Gold, Green, Red, Blue, Purple) are theme-independent
 * and stay as top-level vals in Color.kt.
 */
data class AppColors(
    val backgroundDeep: Color,
    val backgroundCard: Color,
    val backgroundElevated: Color,
    val backgroundChat: Color,
    val surfaceGlass: Color,
    val surfaceGlassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val bubbleCharacter: Color,
    val bubblePlayer: Color,
    val bubbleReport: Color,
    val bubbleSystem: Color,
)

internal val DarkAppColors = AppColors(
    backgroundDeep     = BackgroundDeep,
    backgroundCard     = BackgroundCard,
    backgroundElevated = BackgroundElevated,
    backgroundChat     = BackgroundChat,
    surfaceGlass       = SurfaceGlass,
    surfaceGlassBorder = SurfaceGlassBorder,
    textPrimary        = TextPrimary,
    textSecondary      = TextSecondary,
    textHint           = TextHint,
    bubbleCharacter    = BubbleCharacter,
    bubblePlayer       = BubblePlayer,
    bubbleReport       = BubbleReport,
    bubbleSystem       = BubbleSystem,
)

internal val LightAppColors = AppColors(
    backgroundDeep     = BackgroundLightDeep,
    backgroundCard     = BackgroundLightCard,
    backgroundElevated = BackgroundLightElevated,
    backgroundChat     = BackgroundLightChat,
    surfaceGlass       = SurfaceGlassLight,
    surfaceGlassBorder = SurfaceGlassBorderLight,
    textPrimary        = TextPrimaryLight,
    textSecondary      = TextSecondaryLight,
    textHint           = TextHintLight,
    bubbleCharacter    = BubbleCharacterLight,
    bubblePlayer       = BubblePlayerLight,
    bubbleReport       = BubbleReportLight,
    bubbleSystem       = BubbleSystemLight,
)

/** Provides the current [AppColors] set. Default to dark so previews don't crash. */
val LocalAppColors = compositionLocalOf<AppColors> { DarkAppColors }
