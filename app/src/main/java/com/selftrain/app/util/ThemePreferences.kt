package com.selftrain.app.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { CLASSIC, MODERN }

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(
        if (prefs.getBoolean("modern_ui", false)) ThemeMode.MODERN else ThemeMode.CLASSIC
    )
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(newMode: ThemeMode) {
        _mode.value = newMode
        prefs.edit().putBoolean("modern_ui", newMode == ThemeMode.MODERN).apply()
    }

    fun toggle() {
        setMode(if (_mode.value == ThemeMode.CLASSIC) ThemeMode.MODERN else ThemeMode.CLASSIC)
    }
}

@Composable
fun rememberThemePreferences(): ThemePreferences {
    val ctx = LocalContext.current
    return androidx.compose.runtime.remember(ctx) { ThemePreferences(ctx) }
}

@Composable
fun rememberThemeMode(): State<ThemeMode> {
    val prefs = rememberThemePreferences()
    return prefs.mode.collectAsState()
}
