package com.selftrain.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// ponytail: máquina de estados del temporizador (objeto puro, sin Android) — F5
class RestTimerControllerTest {

    @Test
    fun `start activa sesion con deadline futuro`() {
        RestTimerController.stop()
        RestTimerController.start(90)

        assertTrue(RestTimerController.isActive)
        assertTrue(RestTimerController.isRunning)
        assertEquals(90, RestTimerController.remaining)
        assertEquals(90, RestTimerController.totalSeconds)
        assertTrue(RestTimerController.endTimestamp > System.currentTimeMillis())
        assertTrue(RestTimerController.sessionStarted)
        assertFalse(RestTimerController.finishedFlash)
    }

    @Test
    fun `pause congela remaining y resume recalcula deadline`() {
        RestTimerController.stop()
        RestTimerController.start(60)
        RestTimerController.pause()

        assertFalse(RestTimerController.isRunning)
        assertTrue(RestTimerController.isActive)
        assertEquals(RestTimerController.remaining, RestTimerController.pausedRemaining)

        RestTimerController.resume()
        assertTrue(RestTimerController.isRunning)
        assertTrue(RestTimerController.endTimestamp > System.currentTimeMillis())
        assertEquals(0, RestTimerController.pausedRemaining)
    }

    @Test
    fun `finish activa flash y limpia estado`() {
        RestTimerController.stop()
        RestTimerController.start(30)
        RestTimerController.finish()

        assertFalse(RestTimerController.isActive)
        assertFalse(RestTimerController.isRunning)
        assertTrue(RestTimerController.finishedFlash)
        assertEquals(0, RestTimerController.remaining)

        RestTimerController.clearFlash()
        assertFalse(RestTimerController.finishedFlash)
    }

    @Test
    fun `stop limpia todo el estado`() {
        RestTimerController.start(45)
        RestTimerController.stop()

        assertFalse(RestTimerController.isActive)
        assertFalse(RestTimerController.isRunning)
        assertEquals(0, RestTimerController.remaining)
        assertEquals(0L, RestTimerController.endTimestamp)
        assertFalse(RestTimerController.finishedFlash)
    }

    @Test
    fun `restoreRunning recalcula remaining desde el deadline`() {
        RestTimerController.stop()
        val deadline = System.currentTimeMillis() + 45_000L

        RestTimerController.restoreRunning(totalSecs = 100, deadline = deadline)

        assertTrue(RestTimerController.isRunning)
        assertTrue(RestTimerController.sessionStarted)
        assertEquals(45, RestTimerController.remaining)
        assertEquals(deadline, RestTimerController.endTimestamp)
    }

    @Test
    fun `restoreRunning con deadline pasado deja remaining en cero`() {
        RestTimerController.stop()
        val deadline = System.currentTimeMillis() - 10_000L

        RestTimerController.restoreRunning(totalSecs = 100, deadline = deadline)

        assertEquals(0, RestTimerController.remaining)
    }

    @Test
    fun `restorePaused restaura tarjeta sin correr`() {
        RestTimerController.stop()

        RestTimerController.restorePaused(totalSecs = 90, paused = 33)

        assertTrue(RestTimerController.isActive)
        assertFalse(RestTimerController.isRunning)
        assertEquals(33, RestTimerController.remaining)
        assertEquals(33, RestTimerController.pausedRemaining)
        assertEquals(0L, RestTimerController.endTimestamp)
    }

    @Test
    fun `remainingFromDeadline con deadline cero devuelve cero`() {
        RestTimerController.stop()
        assertEquals(0, RestTimerController.remainingFromDeadline())
    }

    @Test
    fun `pause sin sesion activa no hace nada`() {
        RestTimerController.stop()
        RestTimerController.pause()
        assertFalse(RestTimerController.isActive)
    }

    @Test
    fun `resume sin sesion activa no hace nada`() {
        RestTimerController.stop()
        RestTimerController.resume()
        assertFalse(RestTimerController.isActive)
    }
}
