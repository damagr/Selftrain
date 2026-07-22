package com.selftrain.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
 * High-energy dark palette for Selftrain.
 *
 *   background  #0B0B0F — near-black foundation
 *   surface     #17171D — dark card/sheet
 *   primary     #D4FF3D — lime accent, used ONLY on key interactive elements
 *                         (CTA buttons, active progress, selected tab)
 *   secondary   #FF4B4B — red for alerts / high-intensity states
 *
 * No Material You dynamic color. This palette is fixed.
 * Default is always dark — no light scheme.
 */
private val FitnessDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4FF3D),
    onPrimary = Color(0xFF0B0B0F),
    primaryContainer = Color(0xFF1A2A00),
    onPrimaryContainer = Color(0xFFD4FF3D),
    secondary = Color(0xFFD4FF3D),
    onSecondary = Color(0xFF0B0B0F),
    secondaryContainer = Color(0xFF1A2A00),
    onSecondaryContainer = Color(0xFFD4FF3D),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF17171D),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1E1E28),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFD4FF3D),
    onError = Color(0xFF0B0B0F),
    errorContainer = Color(0xFF1A2A00),
    onErrorContainer = Color(0xFFD4FF3D),
    outline = Color(0xFF3D3D45),
    outlineVariant = Color(0xFF2A2A34),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF0B0B0F),
    inversePrimary = Color(0xFF1A2A00),
    surfaceTint = Color(0xFFD4FF3D),
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
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = FitnessDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive()
    ) { content() }
}
