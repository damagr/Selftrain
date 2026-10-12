package com.selftrain.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.selftrain.app.MainActivity
import com.selftrain.app.R

// ponytail: atajos de long-press del icono (F4, opción A):
// - Historial / Estadísticas: fijos
// - "Repetir {rutina}": dinámico, se actualiza al finalizar cada entreno (AppPrefs)
// MainActivity lee EXTRA_ROUTE y navega a esa ruta del NavHost.
object Shortcuts {

    const val EXTRA_ROUTE = "shortcut_route"
    private const val ID_HISTORY = "shortcut_history"
    private const val ID_STATS = "shortcut_stats"
    private const val ID_REPEAT = "shortcut_repeat_last"

    private fun routeIntent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ROUTE, route)
        }

    /** Reconstruye la lista completa de atajos dinámicos (llamar en onCreate y tras finalizar). */
    fun setup(context: Context) {
        val shortcuts = buildList {
            add(
                ShortcutInfoCompat.Builder(context, ID_HISTORY)
                    .setShortLabel("Historial")
                    .setLongLabel("Ver historial")
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(routeIntent(context, "history"))
                    .build()
            )
            add(
                ShortcutInfoCompat.Builder(context, ID_STATS)
                    .setShortLabel("Estadísticas")
                    .setLongLabel("Ver estadísticas")
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(routeIntent(context, "stats"))
                    .build()
            )
            if (AppPrefs.lastRoutineId != 0L && AppPrefs.lastRoutineName.isNotBlank()) {
                add(repeatShortcut(context))
            }
        }
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) { }
    }

    /** Actualiza el atajo "Repetir" tras finalizar un entreno de esa rutina. */
    fun updateLastRoutine(context: Context, routineId: Long, routineName: String) {
        AppPrefs.setLastRoutine(context, routineId, routineName)
        try {
            ShortcutManagerCompat.pushDynamicShortcut(context, repeatShortcut(context))
        } catch (_: Exception) { }
    }

    private fun repeatShortcut(context: Context): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, ID_REPEAT)
            .setShortLabel("Repetir")
            .setLongLabel("Repetir ${AppPrefs.lastRoutineName}")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(routeIntent(context, "train/${AppPrefs.lastRoutineId}"))
            .build()
}
