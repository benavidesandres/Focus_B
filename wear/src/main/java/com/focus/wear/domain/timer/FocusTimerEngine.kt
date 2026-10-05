// wear/src/main/java/com/focus/wear/domain/timer/FocusTimerEngine.kt
// ───────────────────────────────────────────────────────────────
// Motor del temporizador de enfoque — el corazón de la aplicación.
// Usa corrutinas para actualizar el tiempo sin bloquear el hilo principal.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.domain.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado interno del temporizador.
 *
 * @param totalDurationMs Duración total de la sesión.
 * @param remainingMs Tiempo restante en ms.
 * @param isRunning Si el timer está contando activamente.
 * @param isPaused Si está temporalmente pausado.
 * @param isFinished Si llegó a 0.
 */
data class TimerState(
    val totalDurationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false
) {
    /** Progreso de 0.0f (inicio) a 1.0f (completado), útil para el arco circular. */
    val progress: Float
        get() = if (totalDurationMs == 0L) 0f
                else 1f - (remainingMs.toFloat() / totalDurationMs.toFloat())
}

/**
 * Motor del temporizador.
 * Es @Singleton: vive mientras el proceso esté activo.
 * El ForegroundService lo mantiene vivo incluso con la pantalla apagada.
 *
 * Arquitectura:
 * ForegroundService ──── controla ────► FocusTimerEngine
 *                                           │
 *                                    StateFlow<TimerState>
 *                                           │
 *                               ViewModel observa y expone a la UI
 */
@Singleton
class FocusTimerEngine @Inject constructor() {

    // Intervalo de actualización: cada 100ms para suavidad visual.
    // En Ambient Mode se puede reducir a 1000ms para ahorrar batería.
    private companion object {
        const val TICK_INTERVAL_MS = 100L
    }

    // _timerState es privado y mutable — solo el Engine lo modifica.
    // timerState es público y de solo lectura — la UI solo puede observar.
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Job de la corrutina activa — guardamos la referencia para poder cancelarla.
    private var timerJob: Job? = null

    /**
     * Inicia una nueva sesión de temporizador.
     *
     * @param scope CoroutineScope del servicio en foreground — garantiza que el
     *              temporizador siga corriendo aunque la pantalla esté apagada.
     * @param durationMs Duración total en milisegundos.
     */
    fun start(scope: CoroutineScope, durationMs: Long) {
        // Cancelamos cualquier timer anterior antes de iniciar
        stop()

        _timerState.update {
            TimerState(
                totalDurationMs = durationMs,
                remainingMs = durationMs,
                isRunning = true,
                isPaused = false,
                isFinished = false
            )
        }

        timerJob = scope.launch {
            var lastTickMs = System.currentTimeMillis()

            while (_timerState.value.remainingMs > 0) {
                val isPaused = _timerState.value.isPaused
                // Durante pausa aumentamos el delay para no consumir CPU en el reloj
                delay(if (isPaused) 500L else TICK_INTERVAL_MS)

                val now = System.currentTimeMillis()
                val elapsed = now - lastTickMs
                lastTickMs = now

                _timerState.update { state ->
                    if (state.isPaused) {
                        state
                    } else {
                        val newRemaining = (state.remainingMs - elapsed).coerceAtLeast(0L)
                        state.copy(
                            remainingMs = newRemaining,
                            isFinished = newRemaining == 0L
                        )
                    }
                }
            }

            // Sesión completada
            _timerState.update { it.copy(isRunning = false, isFinished = true) }
        }
    }

    /**
     * Pausa el temporizador (el tiempo deja de correr pero la sesión sigue abierta).
     */
    fun pause() {
        _timerState.update { it.copy(isPaused = true) }
    }

    /**
     * Reanuda el temporizador después de una pausa.
     */
    fun resume() {
        _timerState.update { it.copy(isPaused = false) }
    }

    /**
     * Detiene y reinicia el temporizador (cancelación de sesión).
     */
    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _timerState.update { TimerState() }
    }
}
