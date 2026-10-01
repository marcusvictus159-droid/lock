package com.otmar.linternashake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reactiva el detector al reiniciar el teléfono si estaba encendido. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.enabled(context)) {
            ShakeService.start(context)
        }
    }
}
