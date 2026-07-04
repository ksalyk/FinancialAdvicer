package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color set that switches between dark and light palettes.
 * Theme-independent brand accents (Indigo, Purple) stay as top-level vals in Color.kt.
 *
 * Redesign 2026-07: diary/paper fields removed — the game chat is now a fintech
 * chat (bubbles + avatar). Positive/negative/warning accents are theme-aware here
 * because the mock uses different emerald/coral/amber tones per theme.
 */
data class AppColors(
    val backgroundDeep: Color,
    val backgroundCard: Color,
    val backgroundElevated: Color,
    val backgroundChat: Color,
    val backgroundPanel: Color,     // bottom action panel / composer area
    val backgroundSubCard: Color,   // tertiary metric cells
    val surfaceGlass: Color,
    val surfaceGlassBorder: Color,
    val divider: Color,             // hairline separators (top bar, panel top)
    val border: Color,              // card borders
    val borderStrong: Color,        // option cards, composer
    val textPrimary: Color,
    val textBody: Color,            // message copy
    val textEmphasis: Color,        // card labels
    val textSecondary: Color,       // meta, subtitles
    val textHint: Color,            // placeholders
    val bubbleCharacter: Color,
    val bubblePlayer: Color,
    val bubbleReport: Color,
    val bubbleSystem: Color,
    val bubbleAi: Color,            // Asan advisor bubble
    val bubbleAiBorder: Color,
    // Theme-aware semantic accents
    val accentPositive: Color,      // emerald fills / values
    val accentPositiveText: Color,  // emerald badge text
    val accentNegative: Color,      // coral fills / values
    val accentNegativeText: Color,  // coral badge text
    val accentWarning: Color,       // amber fills
    val accentWarningText: Color,   // amber values
)

internal val DarkAppColors = AppColors(
    backgroundDeep     = BackgroundDeep,
    backgroundCard     = BackgroundCard,
    backgroundElevated = BackgroundElevated,
    backgroundChat     = BackgroundChat,
    backgroundPanel    = BackgroundPanel,
    backgroundSubCard  = BackgroundSubCard,
    surfaceGlass       = SurfaceGlass,
    surfaceGlassBorder = SurfaceGlassBorder,
    divider            = DividerDark,
    border             = BorderDark,
    borderStrong       = BorderStrongDark,
    textPrimary        = TextPrimary,
    textBody           = TextBody,
    textEmphasis       = TextEmphasis,
    textSecondary      = TextSecondary,
    textHint           = TextHint,
    bubbleCharacter    = BubbleCharacter,
    bubblePlayer       = BubblePlayer,
    bubbleReport       = BubbleReport,
    bubbleSystem       = BubbleSystem,
    bubbleAi           = BubbleAiDark,
    bubbleAiBorder     = BubbleAiBorderDark,
    accentPositive     = EmeraldOnDark,
    accentPositiveText = EmeraldOnDark,
    accentNegative     = CoralOnDark,
    accentNegativeText = CoralSoftDark,
    accentWarning      = AmberOnDark,
    accentWarningText  = AmberOnDark,
)

internal val LightAppColors = AppColors(
    backgroundDeep     = BackgroundLightDeep,
    backgroundCard     = BackgroundLightCard,
    backgroundElevated = BackgroundLightElevated,
    backgroundChat     = BackgroundLightChat,
    backgroundPanel    = BackgroundLightPanel,
    backgroundSubCard  = BackgroundLightSubCard,
    surfaceGlass       = SurfaceGlassLight,
    surfaceGlassBorder = SurfaceGlassBorderLight,
    divider            = DividerLight,
    border             = BorderLight,
    borderStrong       = BorderStrongLight,
    textPrimary        = TextPrimaryLight,
    textBody           = TextBodyLight,
    textEmphasis       = TextEmphasisLight,
    textSecondary      = TextSecondaryLight,
    textHint           = TextHintLight,
    bubbleCharacter    = BubbleCharacterLight,
    bubblePlayer       = BubblePlayerLight,
    bubbleReport       = BubbleReportLight,
    bubbleSystem       = BubbleSystemLight,
    bubbleAi           = BubbleAiLight,
    bubbleAiBorder     = BubbleAiBorderLight,
    accentPositive     = EmeraldOnLight,
    accentPositiveText = EmeraldTextLight,
    accentNegative     = CoralOnLight,
    accentNegativeText = CoralTextLight,
    accentWarning      = AmberOnLight,
    accentWarningText  = AmberTextLight,
)

/** Provides the current [AppColors] set. Default to dark so previews don't crash. */
val LocalAppColors = compositionLocalOf<AppColors> { DarkAppColors }
