package com.otmar.linternashake

import android.content.Context

object Prefs {
    private const val FILE = "ajustes"

    /** Sensibilidad 1..10 (10 = más sensible). */
    fun sensitivity(c: Context) = c.getSharedPreferences(FILE, 0).getInt("sens", 5)
    fun setSensitivity(c: Context, v: Int) =
        c.getSharedPreferences(FILE, 0).edit().putInt("sens", v).apply()

    fun enabled(c: Context) = c.getSharedPreferences(FILE, 0).getBoolean("on", false)
    fun setEnabled(c: Context, v: Boolean) =
        c.getSharedPreferences(FILE, 0).edit().putBoolean("on", v).apply()

    fun vibrate(c: Context) = c.getSharedPreferences(FILE, 0).getBoolean("vib", true)
    fun setVibrate(c: Context, v: Boolean) =
        c.getSharedPreferences(FILE, 0).edit().putBoolean("vib", v).apply()

    fun autoOff(c: Context) = c.getSharedPreferences(FILE, 0).getBoolean("auto", true)
    fun setAutoOff(c: Context, v: Boolean) =
        c.getSharedPreferences(FILE, 0).edit().putBoolean("auto", v).apply()

    /** Sensibilidad 1..10 → umbral en g (1 → 3.6g, 10 → 1.8g). */
    fun thresholdFor(sens: Int) = 3.8f - sens * 0.2f
}
