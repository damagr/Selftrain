package com.selftrain.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Font choices for Selftrain (fitness app):
 *
 * Oswald (display/headline):
 *   Condensed sans-serif with strong, athletic character — evokes scoreboards,
 *   sportswear branding, and gym posters. Gives display/headline text impact.
 *
 * Inter (body/label):
 *   Highly legible neo-grotesque with generous x-height and open apertures.
 *   Excellent for reading sets, reps, and weight data at small sizes.
 *   Pairs cleanly with Oswald as a neutral, functional body face.
 */

private val OswaldFont = FontFamily(
    Font(GoogleFont("Oswald"), FontWeight.Normal),
    Font(GoogleFont("Oswald"), FontWeight.Medium),
    Font(GoogleFont("Oswald"), FontWeight.SemiBold),
    Font(GoogleFont("Oswald"), FontWeight.Bold),
)

private val InterFont = FontFamily(
    Font(GoogleFont("Inter"), FontWeight.Normal),
    Font(GoogleFont("Inter"), FontWeight.Medium),
    Font(GoogleFont("Inter"), FontWeight.SemiBold),
    Font(GoogleFont("Inter"), FontWeight.Bold),
)

private val AppTypography = Typography(
    fontFamily = InterFont,

    // Display — Oswald, bold, expressive
    displayLarge = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp
    ),

    // Headline — Oswald, semibold
    headlineLarge = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),

    // Title — Inter semibold
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),

    // Body — Inter regular
    bodyLarge = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),

    // Label — Inter
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),

    // --- Emphasized variants (M3 Expressive scale) ---

    displayLargeEmphasized = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp
    ),
    displayMediumEmphasized = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp
    ),
    displaySmallEmphasized = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp
    ),
    headlineLargeEmphasized = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineMediumEmphasized = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp
    ),
    headlineSmallEmphasized = TextStyle(
        fontFamily = OswaldFont, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),
    titleLargeEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMediumEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    titleSmallEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodyLargeEmphasized = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMediumEmphasized = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    bodySmallEmphasized = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelLargeEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMediumEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
    labelSmallEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
)

/*
 * Fallback colors when dynamic color (Material You) is unavailable:
 * Android <12 or user disabled it.
 * Green replaced with pastel blue for a calmer, fitness-friendly palette.
 */
private val LightFallback = lightColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF003258),
    secondary = Color(0xFF1565C0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF001D36),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF42A5F5),
    onSecondary = Color(0xFF001D36),
    secondaryContainer = Color(0xFF003258),
    onSecondaryContainer = Color(0xFFBBDEFB),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFEF5350),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLargeIncreased = RoundedCornerShape(28.dp),
    extraExtraLarge = RoundedCornerShape(32.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelfTrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkFallback
        else -> expressiveLightColorScheme().copy(
            primary = LightFallback.primary,
            onPrimary = LightFallback.onPrimary,
            primaryContainer = LightFallback.primaryContainer,
            onPrimaryContainer = LightFallback.onPrimaryContainer,
            secondary = LightFallback.secondary,
            onSecondary = LightFallback.onSecondary,
            secondaryContainer = LightFallback.secondaryContainer,
            onSecondaryContainer = LightFallback.onSecondaryContainer,
            error = LightFallback.error,
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive()
    ) { content() }
}
