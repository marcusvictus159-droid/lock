package com.otmar.linternashake

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*

/**
 * Servicio en primer plano: escucha el acelerómetro aunque la app esté
 * cerrada o la pantalla apagada, y alterna la linterna al sacudir.
 */
class ShakeService : Service() {

    companion object {
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_STOP = "stop"
        const val ACTION_RELOAD = "reload"
        private const val CHANNEL = "shake"
        private const val NOTIF_ID = 1
        private const val AUTO_OFF_MS = 10 * 60 * 1000L   // 10 minutos

        var running = false
            private set

        fun start(c: Context) {
            c.startForegroundService(Intent(c, ShakeService::class.java))
        }

        fun send(c: Context, action: String) {
            if (running) c.startService(Intent(c, ShakeService::class.java).setAction(action))
        }
    }

    private lateinit var sm: SensorManager
    private lateinit var torch: Torch
    private lateinit var detector: ShakeDetector
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoOff = Runnable { torch.set(false) }

    override fun onCreate() {
        super.onCreate()
        running = true
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        torch = Torch(this).apply {
            onChange = { on ->
                updateNotification()
                handler.removeCallbacks(autoOff)
                if (on && Prefs.autoOff(this@ShakeService)) handler.postDelayed(autoOff, AUTO_OFF_MS)
            }
            start()
        }
        detector = ShakeDetector(Prefs.thresholdFor(Prefs.sensitivity(this))) {
            torch.toggle()
            buzz()
        }

        createChannel()
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(detector, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Mantiene el CPU despierto para que el sensor siga funcionando con la pantalla apagada
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LinternaShake::sensor")
            .apply { acquire() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> torch.toggle()
            ACTION_RELOAD -> detector.thresholdG = Prefs.thresholdFor(Prefs.sensitivity(this))
            ACTION_STOP -> {
                Prefs.setEnabled(this, false)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(autoOff)
        sm.unregisterListener(detector)
        torch.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buzz() {
        if (!Prefs.vibrate(this)) return
        val effect = VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(effect)
        }
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL, "Detector de sacudida", NotificationManager.IMPORTANCE_LOW)
        ch.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun pending(action: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, ShakeService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val on = torch.isOn
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_flash)
            .setContentTitle(if (on) "Linterna ENCENDIDA" else "Sacude para encender la linterna")
            .setContentText("Detector activo")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, if (on) "Apagar" else "Encender", pending(ACTION_TOGGLE, 1)).build())
            .addAction(Notification.Action.Builder(null, "Desactivar", pending(ACTION_STOP, 2)).build())
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }
}
