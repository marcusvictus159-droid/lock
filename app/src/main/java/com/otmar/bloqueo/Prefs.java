package com.otmar.bloqueo;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("bloqueo", Context.MODE_PRIVATE);
    }
    static boolean shakeOn(Context c) { return get(c).getBoolean("shake", false); }
    static void setShake(Context c, boolean v) { get(c).edit().putBoolean("shake", v).apply(); }
    /** 0 = muy sensible ... 10 = poco sensible */
    static int sensitivity(Context c) { return get(c).getInt("sens", 5); }
    static void setSensitivity(Context c, int v) { get(c).edit().putInt("sens", v).apply(); }
    /** umbral en g según la sensibilidad (1.8g a 3.8g) */
    static float threshold(Context c) { return 1.8f + sensitivity(c) * 0.2f; }
}
