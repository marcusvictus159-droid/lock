package com.otmar.linternashake

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detecta una sacudida "intencional": se necesitan [requiredShakes] picos de
 * aceleración por encima del umbral dentro de una ventana corta. Así no se
 * activa al caminar o al dejar el teléfono sobre la mesa.
 *
 * @param thresholdG fuerza en g (gravedad = 1.0). Menor = más sensible.
 */
class ShakeDetector(
    var thresholdG: Float = 2.5f,
    private val requiredShakes: Int = 2,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val windowMs = 700L      // tiempo máximo para juntar los picos
    private val peakGapMs = 120L     // separación mínima entre picos
    private val cooldownMs = 1200L   // espera tras activar para no rebotar

    private var count = 0
    private var firstPeak = 0L
    private var lastPeak = 0L
    private var lastTrigger = 0L

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g < thresholdG) return

        val now = System.currentTimeMillis()
        if (now - lastTrigger < cooldownMs) return
        if (now - lastPeak < peakGapMs) return

        if (count == 0 || now - firstPeak > windowMs) {
            count = 0
            firstPeak = now
        }
        lastPeak = now
        count++

        if (count >= requiredShakes) {
            count = 0
            lastTrigger = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
