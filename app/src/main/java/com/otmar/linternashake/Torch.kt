package com.otmar.linternashake

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/** Controla la linterna y sigue su estado real (aunque otra app la cambie). */
class Torch(context: Context) {
    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = cm.cameraIdList.firstOrNull { id ->
        val c = cm.getCameraCharacteristics(id)
        c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    } ?: cm.cameraIdList.firstOrNull { id ->
        cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    var isOn = false
        private set

    var onChange: ((Boolean) -> Unit)? = null

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) {
                isOn = enabled
                onChange?.invoke(enabled)
            }
        }
    }

    val available get() = cameraId != null

    fun start() = cm.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    fun stop() = cm.unregisterTorchCallback(callback)

    fun set(on: Boolean) {
        val id = cameraId ?: return
        try {
            cm.setTorchMode(id, on)
            isOn = on
        } catch (_: Exception) {
            // La cámara puede estar ocupada por otra app
        }
    }

    fun toggle() = set(!isOn)
}
