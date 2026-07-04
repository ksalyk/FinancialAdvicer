package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ─── DARK COLOR SCHEME ────────────────────────────────────────────────────────
internal val DarkColorScheme = darkColorScheme(
    primary          = IndigoPrimary,
    onPrimary        = Color.White,
    primaryContainer = IndigoDark,
    secondary        = EmeraldOnDark,
    onSecondary      = BackgroundDeep,
    tertiary         = PurpleGradient,
    onTertiary       = Color.White,
    background       = BackgroundDeep,
    onBackground     = TextPrimary,
    surface          = BackgroundCard,
    onSurface        = TextPrimary,
    surfaceVariant   = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    error            = CoralOnDark,
    onError          = Color.White,
    outline          = BorderStrongDark
)

// ─── LIGHT COLOR SCHEME ───────────────────────────────────────────────────────
internal val LightColorScheme = lightColorScheme(
    primary          = IndigoPrimary,
    onPrimary        = Color.White,
    primaryContainer = IndigoLight,
    secondary        = EmeraldOnLight,
    onSecondary      = Color.White,
    tertiary         = PurpleGradient,
    onTertiary       = Color.White,
    background       = BackgroundLightDeep,
    onBackground     = TextPrimaryLight,
    surface          = BackgroundLightCard,
    onSurface        = TextPrimaryLight,
    surfaceVariant   = BackgroundLightElevated,
    onSurfaceVariant = TextSecondaryLight,
    error            = CoralOnLight,
    onError          = Color.White,
    outline          = BorderLight
)

/**
 * Main theme composable that automatically switches between light and dark
 * based on system preferences
 */
@Composable
fun FinanceLifeLineTheme(
    content: @Composable () -> Unit,
    colorScheme: ColorScheme,
    darkTheme: Boolean,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = AppShapes,
            content     = content
        )
    }
}
