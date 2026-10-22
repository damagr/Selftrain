package com.selftrain.app.ui.train

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.selftrain.app.util.RestTimerController
import com.selftrain.app.util.RestTimerService
import kotlinx.coroutines.delay

// ponytail: temporizador de descanso — única fuente de verdad = RestTimerController
// (lo escribe el servicio, la UI lee). Ver util/RestTimerController.kt para el diseño.

@Composable
fun RestTimer(category: String? = null, restTrigger: Int = 0, defaultSeconds: Int = 90) {
    val context = LocalContext.current

    // Sin estado local duplicado: la UI siempre muestra el valor del controller — el servicio y
    // la app quedan sincronizados (pausa por notificación incluida), y la cuenta sobrevive a
    // navegación y rotación porque el controller vive a nivel de proceso.
    val isActive = RestTimerController.isActive
    val isRunning = RestTimerController.isRunning
    val remaining = RestTimerController.remaining

    // Payload para restaurar tras muerte de proceso: guarda el DEADLINE (no los segundos),
    // así el remaining real se recalcula aunque el proceso haya estado muerto un rato.
    var savedActive by rememberSaveable { mutableStateOf(false) }
    var savedRunning by rememberSaveable { mutableStateOf(false) }
    var savedDeadline by rememberSaveable { mutableLongStateOf(0L) }
    var savedTotal by rememberSaveable { mutableIntStateOf(0) }
    var savedPaused by rememberSaveable { mutableIntStateOf(0) }

    // Restore-once: SOLO si el controller no ha tenido sesión en este proceso (proceso fresco
    // tras muerte) y el payload guardado dice que había timer activo. Si el controller ya
    // vivió una sesión, su estado es la verdad y un restore stale re-disparaba el flash.
    LaunchedEffect(Unit) {
        if (!RestTimerController.sessionStarted && savedActive) {
            if (savedRunning) {
                val rem = ((savedDeadline - System.currentTimeMillis()) / 1000L).toInt()
                if (rem > 0) {
                    // rearranca servicio + notificación con el tiempo real restante
                    RestTimerController.restoreRunning(savedTotal, savedDeadline)
                    context.startForegroundService(
                        RestTimerService.createStartIntent(context, savedDeadline, savedTotal)
                    )
                } else {
                    // expiró mientras el proceso estuvo muerto → flash, sin servicio
                    RestTimerController.finish()
                }
            } else {
                // estaba en pausa: restaura la tarjeta; la notificación la recupera el sticky
                // restart si lo hubo, y reanudar in-app rearraanca el servicio
                RestTimerController.restorePaused(savedTotal, savedPaused)
            }
            savedActive = false
        }
    }

    // Sync del payload tras cada composición
    SideEffect {
        savedActive = RestTimerController.isActive
        savedRunning = RestTimerController.isRunning
        savedDeadline = RestTimerController.endTimestamp
        savedTotal = RestTimerController.totalSeconds
        savedPaused = RestTimerController.pausedRemaining
    }

    // Flash "terminado": háptico + se auto-limpia a los 2s
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(RestTimerController.finishedFlash) {
        if (RestTimerController.finishedFlash) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2000)
            RestTimerController.clearFlash()
        }
    }

    // Reset del default al cambiar de ejercicio, solo si no hay descanso activo
    // (para no cortar un descanso en marcha). El ±30s manual sigue disponible.
    var customSeconds by remember(defaultSeconds) { mutableIntStateOf(defaultSeconds) }
    LaunchedEffect(defaultSeconds) {
        if (!RestTimerController.isActive) customSeconds = defaultSeconds
    }

    // ponytail: latido local — la cuenta en pantalla sigue viva aunque el servicio haya muerto
    // (swipe de la notificación por el usuario); tick() recalcula desde el deadline, así que
    // latir aquí y en el servicio a la vez es idempotente
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            RestTimerController.tick()
        }
    }

    // ponytail: descanso automático — al registrar una serie (trigger del ViewModel) arranca
    // el descanso con los customSeconds del usuario (respeta sus ±30s ajustados).
    // El último trigger atendido vive en RestTimerController (nivel proceso): el remember local
    // se perdía al desecharse el item (scroll) o rotar y se REINICIABA el descanso en marcha.
    LaunchedEffect(restTrigger) {
        if (restTrigger > RestTimerController.lastHandledTrigger) {
            RestTimerController.lastHandledTrigger = restTrigger
            if (restTrigger > 0) {
                RestTimerController.start(customSeconds)
                context.startForegroundService(
                    RestTimerService.createStartIntent(context, RestTimerController.endTimestamp, customSeconds)
                )
            }
        }
    }

    if (!isActive) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // +/- 30s buttons
            IconButton(onClick = {
                if (customSeconds > 30) customSeconds -= 30
            }) {
                Text("−30s", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                RestTimerController.start(customSeconds)
                context.startForegroundService(
                    RestTimerService.createStartIntent(context, RestTimerController.endTimestamp, customSeconds)
                )
            }) {
                Icon(Icons.Default.Timer, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Iniciar descanso (${customSeconds}s)")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (customSeconds < 300) customSeconds += 30
            }) {
                Text("+30s", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, "Descanso")
                Text(
                    "${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row {
                    TextButton(onClick = {
                        // mismo intent toggle que la notificación → sincronizados
                        context.startService(RestTimerService.createPauseIntent(context))
                    }) {
                        Text(if (isRunning) "Pausa" else "Reanudar")
                    }
                    TextButton(onClick = {
                        context.startService(RestTimerService.createStopIntent(context))
                    }) {
                        Text("Reset")
                    }
                }
            }
        }
    }

    // Flash "finished" message
    if (RestTimerController.finishedFlash) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                "¡Descanso terminado!",
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Default de descanso según tipo de ejercicio: compuestos 2-3min, aislamiento 90-120s */
internal fun defaultRestSeconds(category: String?): Int = when (category) {
    "compound" -> 150
    "isolation" -> 105
    else -> 90
}

/** ponytail: para el descanso activo y limpia el controller al terminar/descartar el entreno.
 *  Usa el intent "stop" (no stopService) para que el servicio limpie también las prefs. */
internal fun stopRestTimer(context: android.content.Context) {
    if (RestTimerController.isActive) {
        context.startService(RestTimerService.createStopIntent(context))
    }
    RestTimerController.stop()
}
