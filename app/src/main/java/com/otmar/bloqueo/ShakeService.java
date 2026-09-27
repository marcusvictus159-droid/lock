package com.otmar.bloqueo;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

public class ShakeService extends Service implements SensorEventListener {
    private static final String CH = "agitar";
    private SensorManager sm;
    private Sensor acc;
    private boolean listening = false;
    private long firstShake = 0, lastShake = 0, lastLock = 0;
    private int shakes = 0;
    private float threshold = 2.8f;

    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) stopListening();
            else startListening();
        }
    };

    static void start(Context c) {
        Intent i = new Intent(c, ShakeService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }
    static void stop(Context c) { c.stopService(new Intent(c, ShakeService.class)); }

    @Override public void onCreate() {
        super.onCreate();
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(screen, f);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Agitar para bloquear",
                    NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, SettingsActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        Notification n = b.setSmallIcon(R.drawable.ic_tile_lock)
                .setContentTitle("Agitar para bloquear activo")
                .setContentText("Agita el teléfono dos veces para bloquear")
                .setContentIntent(pi).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, n);
        }
        threshold = Prefs.threshold(this);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm.isInteractive()) startListening();
        return START_STICKY;
    }

    private void startListening() {
        if (!listening && acc != null) {
            sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI);
            listening = true;
        }
    }
    private void stopListening() {
        if (listening) { sm.unregisterListener(this); listening = false; }
        shakes = 0;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        float x = e.values[0], y = e.values[1], z = e.values[2];
        float g = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        if (g < threshold) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastShake < 250) return;          // mismo movimiento
        if (now - firstShake > 1200) { shakes = 0; firstShake = now; }
        lastShake = now;
        shakes++;
        if (shakes >= 2 && now - lastLock > 3000) {  // dos sacudidas en ~1 s
            shakes = 0;
            lastLock = now;
            lock();
        }
    }

    private void lock() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km.isKeyguardLocked()) return;
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm.isAdminActive(new ComponentName(this, AdminReceiver.class))) dpm.lockNow();
    }

    @Override public void onAccuracyChanged(Sensor s, int a) { }

    @Override public void onDestroy() {
        stopListening();
        try { unregisterReceiver(screen); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
