package com.selftrain.app.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ponytail: preferencias de app (nivel proceso + persistidas) — patrón igual que AppThemePrefs
object AppPrefs {
    var onboardingDone by mutableStateOf(false)
        private set

    // última rutina entrenada (para el shortcut "Repetir" y la tarjeta "Continuar")
    var lastRoutineId: Long = 0
        private set
    var lastRoutineName: String = ""
        private set

    private const val PREFS = "settings_prefs"
    private const val KEY_ONBOARDING = "onboarding_done"
    private const val KEY_LAST_ROUTINE_ID = "last_routine_id"
    private const val KEY_LAST_ROUTINE_NAME = "last_routine_name"

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false)
        lastRoutineId = prefs.getLong(KEY_LAST_ROUTINE_ID, 0L)
        lastRoutineName = prefs.getString(KEY_LAST_ROUTINE_NAME, "") ?: ""
    }

    fun setOnboardingDone(context: Context) {
        onboardingDone = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun setLastRoutine(context: Context, routineId: Long, routineName: String) {
        lastRoutineId = routineId
        lastRoutineName = routineName
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_ROUTINE_ID, routineId)
            .putString(KEY_LAST_ROUTINE_NAME, routineName)
            .apply()
    }
}
