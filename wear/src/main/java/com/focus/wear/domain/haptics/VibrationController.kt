// wear/src/main/java/com/focus/wear/domain/haptics/VibrationController.kt
// ───────────────────────────────────────────────────────────────
// Controlador de vibración háptica.
// Diseñado para comunicar estados sin sonido — fundamental en un reloj de enfoque.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.domain.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tipos de patrones de vibración disponibles.
 * Cada patrón comunica un estado diferente de forma intuitiva.
 */
enum class HapticPattern {
    /** Sesión iniciada — 1 pulso firme. */
    SESSION_START,

    /** Sesión completada — 3 pulsos de celebración. */
    SESSION_COMPLETE,

    /** Recordatorio de movimiento/estiramiento — 2 pulsos suaves. */
    INACTIVITY_REMINDER,

    /** Punto de progreso (50%, 75%) — 1 pulso suave. */
    PROGRESS_MILESTONE,

    /** Advertencia antes de finalizar — pulsos rápidos. */
    COUNTDOWN_WARNING,

    /** Confirmación de acción del usuario — pulso corto. */
    CONFIRM_ACTION,

    /** Cancelación / error — 2 pulsos largos. */
    CANCEL_ACTION
}

/**
 * Controlador de vibración háptica para Wear OS.
 *
 * En Android 12+ (API 31), usamos [VibratorManager] para acceder al vibrador principal.
 * En versiones anteriores, usamos el [Vibrator] directamente.
 */
@Singleton
class VibrationController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ — VibratorManager API moderna
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        // Android 11 y anteriores
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Ejecuta un patrón de vibración según el tipo de evento.
     *
     * @param pattern El patrón háptico a ejecutar.
     */
    fun vibrate(pattern: HapticPattern) {
        if (!vibrator.hasVibrator()) return

        val effect = when (pattern) {
            HapticPattern.SESSION_START -> createWaveform(
                // Esperar 0ms, vibrar 200ms con intensidad alta
                timings = longArrayOf(0, 200),
                amplitudes = intArrayOf(0, 220)
            )

            HapticPattern.SESSION_COMPLETE -> createWaveform(
                // 3 pulsos rítmicos de celebración
                timings = longArrayOf(0, 150, 80, 150, 80, 150),
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            )

            HapticPattern.INACTIVITY_REMINDER -> createWaveform(
                // 2 pulsos suaves — como un toque en el hombro
                timings = longArrayOf(0, 100, 80, 100),
                amplitudes = intArrayOf(0, 120, 0, 120)
            )

            HapticPattern.PROGRESS_MILESTONE -> createWaveform(
                // 1 pulso corto y suave — sutil, no interrumpe
                timings = longArrayOf(0, 80),
                amplitudes = intArrayOf(0, 100)
            )

            HapticPattern.COUNTDOWN_WARNING -> createWaveform(
                // Pulsos acelerados — sensación de urgencia
                timings = longArrayOf(0, 60, 40, 60, 40, 60),
                amplitudes = intArrayOf(0, 180, 0, 180, 0, 180)
            )

            HapticPattern.CONFIRM_ACTION -> createWaveform(
                // Click corto de confirmación
                timings = longArrayOf(0, 50),
                amplitudes = intArrayOf(0, 150)
            )

            HapticPattern.CANCEL_ACTION -> createWaveform(
                // 2 pulsos largos — señal de fin / cancelación
                timings = longArrayOf(0, 200, 100, 200),
                amplitudes = intArrayOf(0, 180, 0, 150)
            )
        }

        vibrator.vibrate(effect)
    }

    /**
     * Cancela cualquier vibración en curso.
     */
    fun cancel() {
        vibrator.cancel()
    }

    /**
     * Crea un [VibrationEffect] a partir de arrays de tiempos e intensidades.
     * El parámetro -1 en repeat significa que NO se repite.
     */
    private fun createWaveform(
        timings: LongArray,
        amplitudes: IntArray
    ): VibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, -1)
}
