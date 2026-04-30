package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import financiallifev2.composeapp.generated.resources.Res
import financiallifev2.composeapp.generated.resources.kalam_bold
import financiallifev2.composeapp.generated.resources.kalam_regular
import financiallifev2.composeapp.generated.resources.manrope_bold
import financiallifev2.composeapp.generated.resources.manrope_medium
import financiallifev2.composeapp.generated.resources.manrope_regular
import financiallifev2.composeapp.generated.resources.manrope_semibold

/**
 * Phase 2 Typography System: Option B "Fintech World Wins"
 * Updated to Manrope + Kalam (Decision locked: 2026-04-28)
 *
 * - **Manrope** (system UI, all non-diary content)
 *   - Warm sans-serif, slightly curved, trustworthy
 *   - Weights: Regular (400), Medium (500), SemiBold (600), Bold (700)
 *   - Used for: Buttons, labels, stats, form fields, titles
 *
 * - **Kalam** (diary entries, handwritten flavor)
 *   - Bold handwritten style, playful, intimate
 *   - Weights: Regular (400), Bold (700)
 *   - Used for: Diary headers, scene labels, choice notes, "пишет в дневник..." indicator
 *
 * This keeps the dark glassmorphism fintech world intact while scoping diary
 * handwriting to gameplay only (ChatScreen, DiaryCard).
 */

/**
 * Professional UI font family (Manrope)
 * Weights: Regular (400), Medium (500), SemiBold (600), Bold (700)
 * Location: composeApp/src/commonMain/composeResources/font/
 *
 * Downloads (Google Fonts):
 * 1. Regular (400): https://fonts.google.com/download?family=Manrope
 * 2. Medium (500): Same download, extract manrope-medium
 * 3. SemiBold (600): Same download, extract manrope-semibold
 * 4. Bold (700): Same download, extract manrope-bold
 *
 * Placement: composeApp/src/commonMain/composeResources/font/
 * - manrope_regular.ttf (weight: 400)
 * - manrope_medium.ttf (weight: 500)
 * - manrope_semibold.ttf (weight: 600)
 * - manrope_bold.ttf (weight: 700)
 */
val ManropeFontFamily: FontFamily = FontFamily(
    Font(Res.font.manrope_regular.hashCode(), FontWeight.Normal),
    Font(Res.font.manrope_medium.hashCode(), FontWeight.Medium),
    Font(Res.font.manrope_semibold.hashCode(), FontWeight.SemiBold),
    Font(Res.font.manrope_bold.hashCode(), FontWeight.Bold)
)

/**
 * Handwritten diary font family (Kalam)
 * Weights: Regular (400), Bold (700)
 * Location: composeApp/src/commonMain/composeResources/font/
 *
 * Downloads (Google Fonts):
 * https://fonts.google.com/download?family=Kalam
 *
 * Placement: composeApp/src/commonMain/composeResources/font/
 * - kalam_regular.ttf (weight: 400)
 * - kalam_bold.ttf (weight: 700)
 */
val KalamFontFamily: FontFamily = FontFamily(
    Font(Res.font.kalam_regular.hashCode(), FontWeight.Normal),
    Font(Res.font.kalam_bold.hashCode(), FontWeight.Bold)
)

/**
 * Updated Typography with:
 * - Body/UI text → Inter (professional)
 * - Headline/Display → Inter with optional Caveat override for diary titles
 * - Diary flavor → Uses KalamFontFamily
 */
val AppTypography = Typography(
    // Display fonts (hero titles)
    headlineLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // Title fonts (section headers, card titles)
    titleLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // Body fonts (main content, readable text)
    bodyLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Label fonts (buttons, tabs, badges)
    labelLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Diary-specific text style (ChatScreen, DiaryCard, game narrative)
 * Uses Caveat for handwritten flavor while maintaining readability
 */
val DiaryTextStyle = TextStyle(
    fontFamily = KalamFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.25.sp
)

/**
 * Diary header (title of diary entry, character name, date)
 * Handwritten but with more prominence
 */
val DiaryHeaderStyle = TextStyle(
    fontFamily = KalamFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)
