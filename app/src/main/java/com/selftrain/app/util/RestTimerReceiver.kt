package com.selftrain.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// ponytail: red de seguridad del descanso — si el sistema mata el servicio (swipe de la
// notificación por el usuario, kill del proceso), la alarma dispara el aviso de fin a la
// hora correcta. Corre en el proceso de la app → puede tocar RestTimerController.
class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RestTimerService.ACTION_FINISH_ALARM) return
        val prefs = context.getSharedPreferences(RestTimerService.PREFS, Context.MODE_PRIVATE)
        val end = prefs.getLong(RestTimerService.KEY_END, 0L)
        val wasRunning = prefs.getBoolean(RestTimerService.KEY_RUNNING, false)
        if (!wasRunning || end <= 0L) return
        // alarma prematura (reschedule) → ignorar; margen de 500ms por redondeos
        if (System.currentTimeMillis() < end - 500L) return
        RestTimerService.finishFromReceiver(context)
    }
}
