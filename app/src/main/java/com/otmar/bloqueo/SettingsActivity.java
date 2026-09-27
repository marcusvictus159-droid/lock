package com.otmar.bloqueo;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private Button adminBtn;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);
        int p = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(p, p * 2, p, p);

        TextView title = new TextView(this);
        title.setText("Bloqueo Seguro");
        title.setTextSize(26);
        l.addView(title);

        adminBtn = new Button(this);
        adminBtn.setOnClickListener(v -> {
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explicacion));
            startActivity(i);
        });
        l.addView(adminBtn);

        Switch sw = new Switch(this);
        sw.setText("Agitar para bloquear");
        sw.setTextSize(18);
        sw.setPadding(0, p, 0, p / 2);
        sw.setChecked(Prefs.shakeOn(this));
        sw.setOnCheckedChangeListener((v, on) -> {
            Prefs.setShake(this, on);
            if (on) {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                }
                ShakeService.start(this);
            } else {
                ShakeService.stop(this);
            }
        });
        l.addView(sw);

        TextView info = new TextView(this);
        info.setText("Agita el teléfono dos veces rápido (con la pantalla encendida) para bloquearlo y exigir PIN.");
        l.addView(info);

        TextView sensLbl = new TextView(this);
        sensLbl.setPadding(0, p, 0, 0);
        l.addView(sensLbl);
        SeekBar sb = new SeekBar(this);
        sb.setMax(10);
        sb.setProgress(10 - Prefs.sensitivity(this));
        sensLbl.setText(label(sb.getProgress()));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) { sensLbl.setText(label(v)); }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) {
                Prefs.setSensitivity(SettingsActivity.this, 10 - s.getProgress());
                if (Prefs.shakeOn(SettingsActivity.this)) {
                    ShakeService.stop(SettingsActivity.this);
                    ShakeService.start(SettingsActivity.this);
                }
            }
        });
        l.addView(sb);

        TextView xi = new TextView(this);
        xi.setPadding(0, p, 0, 0);
        xi.setText("En Xiaomi: activa \"Inicio automático\" y pon Ahorro de batería en \"Sin restricciones\" para que no se apague la función.");
        l.addView(xi);
        Button appInfo = new Button(this);
        appInfo.setText("Abrir ajustes de la app");
        appInfo.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        l.addView(appInfo);

        Button lockNow = new Button(this);
        lockNow.setText("Bloquear ahora");
        lockNow.setOnClickListener(v -> { if (dpm.isAdminActive(admin)) dpm.lockNow(); });
        l.addView(lockNow);

        ScrollView sv = new ScrollView(this);
        sv.addView(l);
        setContentView(sv);
    }

    private String label(int v) {
        return "Sensibilidad: " + v + "/10" + (v >= 7 ? " (muy sensible)" : v <= 3 ? " (agitar fuerte)" : "");
    }

    @Override protected void onResume() {
        super.onResume();
        boolean on = dpm.isAdminActive(admin);
        adminBtn.setText(on ? "✔ Permiso de administrador activo" : "Activar permiso de administrador");
        adminBtn.setEnabled(!on);
        if (Prefs.shakeOn(this)) ShakeService.start(this);
    }
}
