package com.selftrain.app.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

// ponytail: foreground service for rest timer, notification shows countdown
class RestTimerService : Service() {

    private var secondsRemaining: Int = 90
    private var isRunning: Boolean = false
    private var tickJob: Job? = null

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_ACTION = "action" // "start", "pause", "stop"
        const val ACTION_PAUSE = "com.selftrain.app.PAUSE_TIMER"
        const val ACTION_STOP = "com.selftrain.app.STOP_TIMER"
        const val BROADCAST_TICK = "com.selftrain.app.TIMER_TICK"
        const val BROADCAST_FINISHED = "com.selftrain.app.TIMER_FINISHED"

        fun createStartIntent(ctx: Context, seconds: Int): Intent {
            return Intent(ctx, RestTimerService::class.java).apply {
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_ACTION, "start")
            }
        }

        fun createPauseIntent(ctx: Context): Intent {
            return Intent(ctx, RestTimerService::class.java).apply {
                putExtra(EXTRA_ACTION, "pause")
            }
        }

        fun createStopIntent(ctx: Context): Intent {
            return Intent(ctx, RestTimerService::class.java).apply {
                putExtra(EXTRA_ACTION, "stop")
            }
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Temporizador",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Temporizador de descanso"
                    setShowBadge(false)
                }
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: "start"
        when (action) {
            "start" -> {
                val secs = intent?.getIntExtra(EXTRA_SECONDS, 90) ?: 90
                secondsRemaining = secs
                isRunning = true
                startForeground(NOTIFICATION_ID, buildNotification())
                startTicking()
            }
            "pause" -> {
                isRunning = !isRunning
                if (isRunning) startTicking() else tickJob?.cancel()
                updateNotification()
            }
            "stop" -> {
                stopTicking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = CoroutineScope(Dispatchers.Default).launch {
            while (isRunning && secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
                updateNotification()
                sendBroadcast(Intent(BROADCAST_TICK).putExtra("remaining", secondsRemaining))
                if (secondsRemaining <= 0) {
                    isRunning = false
                    sendBroadcast(Intent(BROADCAST_FINISHED))
                    stopForeground(STOP_FOREGROUND_DETACH)
                    updateNotification()
                    stopSelf()
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val pausedIntent = PendingIntent.getService(
            this, 0, createPauseIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mins = secondsRemaining / 60
        val secs = secondsRemaining % 60
        val timeStr = "${mins}:${secs.toString().padStart(2, '0')}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descanso: $timeStr")
            .setContentText(if (isRunning) "Tiempo restante..." else "Pausado")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .addAction(0, if (isRunning) "Pausar" else "Reanudar", pausedIntent)
            .addAction(0, "Parar", stopIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
