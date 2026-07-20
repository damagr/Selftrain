package com.selftrain.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selftrain.app.util.ThemeMode
import com.selftrain.app.util.rememberThemePreferences

// ── Classic palette (current) ──────────────────────────────────────────────

private val ClassicLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF1565C0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    tertiary = Color(0xFFFF8F00),
    tertiaryContainer = Color(0xFFFFE082),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    error = Color(0xFFC62828),
    onError = Color.White,
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    secondary = Color(0xFF42A5F5),
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1565C0),
    tertiary = Color(0xFFFFAB40),
    tertiaryContainer = Color(0xFFCC6B00),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    error = Color(0xFFEF5350),
    onError = Color(0xFF690005),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

// ── Modern palette ─────────────────────────────────────────────────────────

private val ModernLight = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF4A6741),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8C4),
    tertiary = Color(0xFF7D5260),
    tertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFFFFFBFE),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3EDF7),
    error = Color(0xFFB3261E),
    onError = Color.White,
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val ModernDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    secondary = Color(0xFFA0D5A1),
    onSecondary = Color(0xFF1F3A1A),
    secondaryContainer = Color(0xFF36512D),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF2B2930),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

// ── Typography ─────────────────────────────────────────────────────────────

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ── Shapes ─────────────────────────────────────────────────────────────────

private val ClassicShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)

private val ModernShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

// ── Dynamic Colors (Material You) for Modern mode on Android 12+ ───────────

@Composable
private fun modernColorScheme(darkTheme: Boolean): ColorScheme {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val dynamic = dynamicLightColorScheme(context)
        val dynamicDark = dynamicDarkColorScheme(context)
        // Use dynamic if available, fall back to custom palette
        return if (darkTheme) dynamicDark else dynamic
    }
    return if (darkTheme) ModernDark else ModernLight
}

// ── Theme composable ──────────────────────────────────────────────────────

@Composable
fun SelfTrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val prefs = rememberThemePreferences()
    val themeMode by prefs.mode.collectAsState()

    val colorScheme = when (themeMode) {
        com.selftrain.app.util.ThemeMode.CLASSIC -> if (darkTheme) ClassicDark else ClassicLight
        com.selftrain.app.util.ThemeMode.MODERN -> modernColorScheme(darkTheme)
    }

    val shapes = when (themeMode) {
        com.selftrain.app.util.ThemeMode.CLASSIC -> ClassicShapes
        com.selftrain.app.util.ThemeMode.MODERN -> ModernShapes
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = shapes,
        content = content
    )
}
