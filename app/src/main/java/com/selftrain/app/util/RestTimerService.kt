package com.selftrain.app.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import com.selftrain.app.MainActivity

// ponytail: foreground service for rest timer, notification shows countdown
class RestTimerService : Service() {

    private var secondsRemaining: Int = 90
    private var isRunning: Boolean = false
    private var tickJob: Job? = null

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val CHANNEL_ID_DONE = "rest_timer_done_channel"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_ID_DONE = 1002
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_ACTION = "action" // "start", "pause", "stop"

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
                val nm = context.getSystemService(NotificationManager::class.java)
                // Running countdown: silent, low priority
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Temporizador",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Temporizador de descanso"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
                // Finished: high priority, sound + heads-up bubble
                val doneChannel = NotificationChannel(
                    CHANNEL_ID_DONE,
                    "Descanso terminado",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Aviso sonoro cuando termina el descanso"
                    enableVibration(true)
                    enableLights(true)
                }
                nm.createNotificationChannel(doneChannel)
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
                // ponytail: guard startForeground so a denied/revoked POST_NOTIFICATIONS
                // doesn't crash the service; countdown still runs without a visible notif.
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            startForeground(
                                NOTIFICATION_ID,
                                buildNotification(),
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            )
                        } else {
                            startForeground(NOTIFICATION_ID, buildNotification())
                        }
                    } catch (_: SecurityException) {
                        // FGS permission denied at runtime — timer runs silently
                    }
                }
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
                if (secondsRemaining <= 0) {
                    isRunning = false
                    // ponytail: post heads-up + sound notification on HIGH channel so it
                    // surfaces over other apps; remove the silent running notification.
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID_DONE, buildDoneNotification())
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
        val openIntent = PendingIntent.getActivity(
            this, 2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
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
            .setContentIntent(openIntent)
            .addAction(0, if (isRunning) "Pausar" else "Reanudar", pausedIntent)
            .addAction(0, "Parar", stopIntent)
            .build()
    }

    private fun buildDoneNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1, createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID_DONE)
            .setContentTitle("Descanso terminado")
            .setContentText("Ya puedes continuar")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setDeleteIntent(stopIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
