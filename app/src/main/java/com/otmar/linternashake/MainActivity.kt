package com.otmar.linternashake

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {

    private lateinit var torch: Torch
    private lateinit var swEnabled: Switch
    private lateinit var btnTorch: Button
    private lateinit var txtSens: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        torch = Torch(this)
        swEnabled = findViewById(R.id.swEnabled)
        btnTorch = findViewById(R.id.btnTorch)
        txtSens = findViewById(R.id.txtSens)
        val seek = findViewById<SeekBar>(R.id.seekSens)
        val swVib = findViewById<Switch>(R.id.swVibrate)
        val swAuto = findViewById<Switch>(R.id.swAutoOff)
        val btnBattery = findViewById<Button>(R.id.btnBattery)

        if (!torch.available) {
            Toast.makeText(this, "Este teléfono no tiene flash", Toast.LENGTH_LONG).show()
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }

        // Detector on/off
        swEnabled.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
            if (checked) ShakeService.start(this)
            else stopService(Intent(this, ShakeService::class.java))
        }

        // Botón manual
        btnTorch.setOnClickListener { torch.toggle() }

        // Sensibilidad
        seek.max = 9
        seek.progress = Prefs.sensitivity(this) - 1
        showSens(seek.progress + 1)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) = showSens(p + 1)
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                Prefs.setSensitivity(this@MainActivity, s.progress + 1)
                ShakeService.send(this@MainActivity, ShakeService.ACTION_RELOAD)
            }
        })

        swVib.isChecked = Prefs.vibrate(this)
        swVib.setOnCheckedChangeListener { _, c -> Prefs.setVibrate(this, c) }

        swAuto.isChecked = Prefs.autoOff(this)
        swAuto.setOnCheckedChangeListener { _, c -> Prefs.setAutoOff(this, c) }

        // Muchos teléfonos (Xiaomi, Samsung, Huawei) matan servicios en segundo plano
        btnBattery.setOnClickListener {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "Ya está excluida del ahorro de batería ✔", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:$packageName"))
                )
            }
        }

        torch.onChange = { updateTorchButton() }
    }

    override fun onResume() {
        super.onResume()
        torch.start()
        swEnabled.isChecked = ShakeService.running || Prefs.enabled(this)
        if (swEnabled.isChecked && !ShakeService.running) ShakeService.start(this)
        updateTorchButton()
    }

    override fun onPause() {
        torch.stop()
        super.onPause()
    }

    private fun showSens(v: Int) {
        txtSens.text = "Sensibilidad: $v / 10"
    }

    private fun updateTorchButton() {
        btnTorch.text = if (torch.isOn) "APAGAR" else "ENCENDER"
        btnTorch.setBackgroundResource(if (torch.isOn) R.drawable.btn_on else R.drawable.btn_off)
    }
}
