package com.otmar.bloqueo;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_ADMIN = 1;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);

        if (dpm.isAdminActive(admin)) {
            bloquear();
        } else {
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_explicacion));
            startActivityForResult(i, REQ_ADMIN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADMIN && dpm.isAdminActive(admin)) {
            bloquear();
        } else {
            Toast.makeText(this, "Permiso no concedido", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void bloquear() {
        // lockNow() desde un administrador obliga a usar PIN/contraseña
        // en el siguiente desbloqueo (la huella queda deshabilitada).
        dpm.lockNow();
        finish();
    }
}
