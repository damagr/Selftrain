package com.selftrain.app.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import com.selftrain.app.MainActivity

// ponytail: foreground service for rest timer, notification shows countdown.
// La cuenta es por deadline (RestTimerController): los ticks solo refrescan la notificación,
// el tiempo mostrado es correcto aunque Doze retrase un tick. Wake lock parcial para que el
// tick corra con pantalla apagada. START_STICKY + prefs: si el sistema mata el proceso,
// el servicio se recrea y la cuenta continúa desde el deadline persistido.
class RestTimerService : Service() {

    private var tickJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceScope: CoroutineScope? = null

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val CHANNEL_ID_DONE = "rest_timer_done_channel"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_ID_DONE = 1002
        const val EXTRA_END_TIMESTAMP = "end_timestamp"
        const val EXTRA_TOTAL = "total_seconds"
        const val EXTRA_ACTION = "action" // "start", "pause", "stop"

        private const val PREFS = "rest_timer_prefs"
        private const val KEY_END = "end_timestamp"
        private const val KEY_TOTAL = "total_seconds"
        private const val KEY_PAUSED = "paused_remaining"
        private const val KEY_RUNNING = "is_running"

        fun createStartIntent(ctx: Context, endTimestamp: Long, totalSecs: Int): Intent {
            return Intent(ctx, RestTimerService::class.java).apply {
                putExtra(EXTRA_END_TIMESTAMP, endTimestamp)
                putExtra(EXTRA_TOTAL, totalSecs)
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

        // ponytail: estado persistido para sobrevivir kills del sistema (START_STICKY)
        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        private fun persistState(ctx: Context) {
            prefs(ctx).edit()
                .putLong(KEY_END, RestTimerController.endTimestamp)
                .putInt(KEY_TOTAL, RestTimerController.totalSeconds)
                .putInt(KEY_PAUSED, RestTimerController.pausedRemaining)
                .putBoolean(KEY_RUNNING, RestTimerController.isRunning)
                .apply()
        }

        private fun clearState(ctx: Context) {
            prefs(ctx).edit().clear().apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(EXTRA_ACTION)) {
            "start" -> {
                val end = intent.getLongExtra(EXTRA_END_TIMESTAMP, 0L)
                val total = intent.getIntExtra(EXTRA_TOTAL, 90)
                if (end <= System.currentTimeMillis()) {
                    // deadline ya pasado → nada que contar
                    RestTimerController.stop()
                    clearState(this)
                    stopSelf()
                    return START_NOT_STICKY
                }
                RestTimerController.restoreRunning(total, end)
                persistState(this)
                goForeground()
                acquireLock()
                startTicking()
            }
            "pause" -> {
                // toggle: pausa ↔ reanuda (lo usan la notificación y la app — sincronizados)
                if (RestTimerController.isRunning) {
                    RestTimerController.pause()
                    stopTicking()
                    releaseLock()
                } else {
                    RestTimerController.resume()
                    if (RestTimerController.endTimestamp <= System.currentTimeMillis()) {
                        // agotado mientras estaba en pausa → fin
                        finishAndNotify()
                        return START_NOT_STICKY
                    }
                    goForeground()
                    acquireLock()
                    startTicking()
                }
                persistState(this)
                updateNotification()
            }
            "stop" -> {
                stopEverything()
                return START_NOT_STICKY
            }
            else -> {
                // START_STICKY: recreación tras kill del sistema → seguir desde prefs
                val p = prefs(this)
                val end = p.getLong(KEY_END, 0L)
                val total = p.getInt(KEY_TOTAL, 90)
                val paused = p.getInt(KEY_PAUSED, 0)
                val wasRunning = p.getBoolean(KEY_RUNNING, false)
                if (wasRunning && end > System.currentTimeMillis()) {
                    RestTimerController.restoreRunning(total, end)
                    goForeground()
                    acquireLock()
                    startTicking()
                } else if (wasRunning && end > 0L) {
                    // agotado mientras el proceso estuvo muerto → aviso de fin
                    finishAndNotify()
                } else if (!wasRunning && paused > 0) {
                    // estaba en pausa: recuperar notificación pausada
                    RestTimerController.restorePaused(total, paused)
                    goForeground()
                    updateNotification()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    // ponytail: startForeground siempre — sin notificaciones el FGS corre headless y sigue
    // contando (omítelo y el sistema mata el proceso a los 5s por FGS no iniciado)
    private fun goForeground() {
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

    private fun acquireLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "selftrain:RestTimer").apply {
            setReferenceCounted(false)
            // red de seguridad: auto-release si algo se cuelga (restante + margen)
            acquire((RestTimerController.remainingFromDeadline() + 5) * 1000L)
        }
    }

    private fun releaseLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startTicking() {
        stopTicking()
        tickJob = serviceScope?.launch {
            while (RestTimerController.isRunning) {
                delay(1000L)
                // recalcula desde el deadline: correcto aunque el tick se retrase
                RestTimerController.tick()
                updateNotification()
                if (RestTimerController.remaining <= 0) {
                    finishAndNotify()
                    return@launch
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun finishAndNotify() {
        releaseLock()
        RestTimerController.finish()
        clearState(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_DONE, buildDoneNotification())
        stopSelf()
    }

    private fun stopEverything() {
        stopTicking()
        releaseLock()
        RestTimerController.stop()
        clearState(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

        val rem = if (RestTimerController.isRunning) {
            RestTimerController.remainingFromDeadline()
        } else {
            RestTimerController.remaining
        }
        val mins = rem / 60
        val secs = rem % 60
        val timeStr = "${mins}:${secs.toString().padStart(2, '0')}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descanso: $timeStr")
            .setContentText(if (RestTimerController.isRunning) "Tiempo restante..." else "Pausado")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, if (RestTimerController.isRunning) "Pausar" else "Reanudar", pausedIntent)
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
        // NO limpiar prefs aquí: el sticky restart las necesita tras muerte del proceso.
        stopTicking()
        releaseLock()
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }
}
