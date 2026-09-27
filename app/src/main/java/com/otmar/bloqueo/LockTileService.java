package com.otmar.bloqueo;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class LockTileService extends TileService {

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t != null) {
            t.setState(Tile.STATE_INACTIVE);
            t.updateTile();
        }
    }

    @Override
    public void onClick() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);

        if (dpm.isAdminActive(admin)) {
            // Bloquea: el siguiente desbloqueo exige PIN/contraseña (sin huella)
            dpm.lockNow();
        } else {
            // Aún no hay permiso: abrir la app para concederlo
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 34) {
                PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                        PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pi);
            } else {
                startActivityAndCollapse(i);
            }
        }
    }
}
