# Linterna Shake

Sacude el teléfono **dos veces seguidas** y la linterna se enciende o apaga,
aunque la app esté cerrada o la pantalla apagada.

## Compilar

**Opción A – GitHub Actions (sin instalar nada):**
1. Sube esta carpeta a un repositorio nuevo de GitHub.
2. Pestaña *Actions* → "Compilar APK" se ejecuta solo.
3. Al terminar, descarga `LinternaShake-apk` (zip con el APK).

**Opción B – Android Studio:** abre la carpeta y presiona ▶ Run.

## Primer uso
1. Acepta el permiso de notificaciones.
2. Activa "Detector de sacudida activo".
3. Toca "Permitir funcionar en segundo plano" (importante en Xiaomi/Samsung/Huawei).
4. En Xiaomi además: Ajustes → Apps → Linterna Shake → Inicio automático ✔.

## Funciones
- Detección de doble sacudida (evita activaciones al caminar).
- Sensibilidad ajustable 1–10.
- Vibración al activar.
- Apagado automático a los 10 min (opcional).
- Botones Encender/Desactivar en la notificación.
- Se reactiva sola al reiniciar el teléfono.
- No requiere permiso de cámara.

## Notas
- El detector mantiene el CPU despierto: consume algo de batería extra (poca).
- Para Play Store: cambia el `signingConfig` por tu keystore y justifica el
  servicio `specialUse` en la consola.
