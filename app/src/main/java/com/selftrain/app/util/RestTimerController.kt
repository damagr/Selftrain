package com.selftrain.app.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ponytail: única fuente de verdad del temporizador de descanso — el servicio escribe, la UI lee
// (mismo proceso, sincronización directa). Cuenta por deadline (reloj de pared), no por ticks:
// aunque Doze retrase un tick, el tiempo mostrado se corrige solo al recalcular desde el deadline.
object RestTimerController {

    var isActive by mutableStateOf(false)   // hay sesión de descanso (corriendo o en pausa)
        private set
    var isRunning by mutableStateOf(false)
        private set
    var totalSeconds by mutableIntStateOf(0)
        private set
    var endTimestamp by mutableLongStateOf(0L)  // deadline wall-clock mientras corre
        private set
    var pausedRemaining by mutableIntStateOf(0)
        private set
    var remaining by mutableIntStateOf(0)   // restante vivo (lo escribe el servicio cada tick)
        private set
    var finishedFlash by mutableStateOf(false)
        private set

    fun remainingFromDeadline(): Int =
        if (endTimestamp == 0L) 0
        else ((endTimestamp - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)

    fun start(totalSecs: Int) {
        totalSeconds = totalSecs
        endTimestamp = System.currentTimeMillis() + totalSecs * 1000L
        remaining = totalSecs
        isRunning = true
        isActive = true
        pausedRemaining = 0
        finishedFlash = false
    }

    // ponytail: restaurar tras muerte de proceso, desde el deadline guardado (no desde ticks)
    fun restoreRunning(totalSecs: Int, deadline: Long) {
        totalSeconds = totalSecs
        endTimestamp = deadline
        remaining = ((deadline - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        isRunning = true
        isActive = true
        pausedRemaining = 0
        finishedFlash = false
    }

    fun restorePaused(totalSecs: Int, paused: Int) {
        totalSeconds = totalSecs
        endTimestamp = 0L
        pausedRemaining = paused
        remaining = paused
        isRunning = false
        isActive = true
        finishedFlash = false
    }

    fun pause() {
        if (!isActive || !isRunning) return
        pausedRemaining = remainingFromDeadline()
        remaining = pausedRemaining
        isRunning = false
    }

    fun resume() {
        if (!isActive || isRunning) return
        val rem = if (pausedRemaining > 0) pausedRemaining else remaining
        endTimestamp = System.currentTimeMillis() + rem * 1000L
        remaining = rem
        isRunning = true
        pausedRemaining = 0
    }

    // ponytail: llamado por el servicio cada tick — recalcula desde el deadline
    fun tick() {
        remaining = remainingFromDeadline()
    }

    fun finish() {
        isRunning = false
        isActive = false
        endTimestamp = 0L
        remaining = 0
        pausedRemaining = 0
        finishedFlash = true
    }

    fun stop() {
        isRunning = false
        isActive = false
        endTimestamp = 0L
        remaining = 0
        pausedRemaining = 0
        finishedFlash = false
    }

    fun clearFlash() {
        finishedFlash = false
    }
}
