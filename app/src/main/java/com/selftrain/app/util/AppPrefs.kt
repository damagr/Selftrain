package com.selftrain.app.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ponytail: preferencias de app (nivel proceso + persistidas) — patrón igual que AppThemePrefs
object AppPrefs {
    var onboardingDone by mutableStateOf(false)
        private set

    private const val PREFS = "settings_prefs"
    private const val KEY_ONBOARDING = "onboarding_done"

    fun load(context: Context) {
        onboardingDone = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING, false)
    }

    fun setOnboardingDone(context: Context) {
        onboardingDone = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING, true).apply()
    }
}
