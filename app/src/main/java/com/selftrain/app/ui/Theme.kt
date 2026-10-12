package com.selftrain.app.ui

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ponytail: preferencia de tema (modo claro) — estado Compose + persistencia en prefs.
// MainActivity hace load() al arrancar; Ajustes llama a set().
object AppThemePrefs {
    var lightMode by mutableStateOf(false)
        private set

    private const val PREFS = "settings_prefs"
    private const val KEY = "light_mode"

    fun load(context: Context) {
        lightMode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    }

    fun set(context: Context, light: Boolean) {
        lightMode = light
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, light).apply()
    }
}

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
    // ponytail: familia dorada para récords/logros (tertiary) — antes default M3 (morado)
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF2E2000),
    tertiaryContainer = Color(0xFF4A3A00),
    onTertiaryContainer = Color(0xFFFFD54F),
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

/*
 * Light palette for Selftrain — same lime accent family, adapted for light backgrounds.
 * Activable desde Ajustes ("Modo claro").
 */
private val FitnessLightColorScheme = lightColorScheme(
    primary = Color(0xFF556B00),
    onPrimary = Color(0xFFF8FDF2),
    primaryContainer = Color(0xFFD4FF3D),
    onPrimaryContainer = Color(0xFF1A2A00),
    secondary = Color(0xFF556B00),
    onSecondary = Color(0xFFF8FDF2),
    secondaryContainer = Color(0xFFE2F2B8),
    onSecondaryContainer = Color(0xFF1A2A00),
    // ponytail: familia dorada para récords/logros (tertiary) — antes default M3 (morado)
    tertiary = Color(0xFF6B5E00),
    onTertiary = Color(0xFFFDFDF6),
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFAFAF2),
    onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFFDFDF6),
    onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE6E8DC),
    onSurfaceVariant = Color(0xFF45483C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFDFDF6),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF76796C),
    outlineVariant = Color(0xFFD7DACD),
    inverseSurface = Color(0xFF2F312A),
    inverseOnSurface = Color(0xFFF1F2E7),
    inversePrimary = Color(0xFFD4FF3D),
    surfaceTint = Color(0xFF556B00),
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
    lightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = if (lightMode) FitnessLightColorScheme else FitnessDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive()
    ) { content() }
}
