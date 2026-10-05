// shared/src/main/java/com/focus/shared/model/FocusSession.kt
// ───────────────────────────────────────────────────────────────
// Modelos de dominio compartidos entre el reloj y el teléfono.
// Usamos 'data class' para inmutabilidad y comparación automática.
// ───────────────────────────────────────────────────────────────
package com.focus.shared.model

import java.util.UUID

/**
 * Tipos de actividad disponibles al iniciar una sesión de enfoque.
 *
 * @property displayName Nombre en español para mostrar en la UI.
 * @property icon Nombre del recurso de icono (Material Icons).
 * @property defaultDurationMinutes Duración predeterminada sugerida.
 */
enum class FocusActivityType(
    val displayName: String,
    val icon: String,
    val defaultDurationMinutes: Int
) {
    STUDY(displayName = "Estudiar", icon = "school", defaultDurationMinutes = 45),
    WORK(displayName = "Trabajar", icon = "work", defaultDurationMinutes = 50),
    READ(displayName = "Leer", icon = "menu_book", defaultDurationMinutes = 30);

    companion object {
        /** Retorna el tipo por su nombre, o STUDY si no se encuentra. */
        fun fromName(name: String): FocusActivityType =
            entries.firstOrNull { it.name == name } ?: STUDY
    }
}

/**
 * Estados posibles de una sesión de enfoque.
 * Usamos un sealed interface para garantizar exhaustividad en los 'when'.
 */
sealed interface SessionState {
    /** No hay sesión activa — pantalla inicial. */
    data object Idle : SessionState

    /** El usuario está seleccionando la duración. */
    data class SelectingDuration(val activityType: FocusActivityType) : SessionState

    /** Sesión en curso. */
    data class Active(
        val sessionId: String,
        val activityType: FocusActivityType,
        val totalDurationMs: Long,
        val remainingMs: Long,
        val isPaused: Boolean = false
    ) : SessionState {
        /** Progreso de 0.0f (inicio) a 1.0f (completado). */
        val progress: Float
            get() = if (totalDurationMs == 0L) 0f
                    else 1f - (remainingMs.toFloat() / totalDurationMs.toFloat())
    }

    /** Sesión completada — mostrando pantalla de descanso. */
    data class Break(
        val completedSessionId: String,
        val breakDurationMs: Long,
        val breakRemainingMs: Long
    ) : SessionState

    /** Sesión interrumpida por el usuario antes de completarse. */
    data class Interrupted(val reason: InterruptionReason) : SessionState
}

/** Razones por las que una sesión puede ser interrumpida. */
enum class InterruptionReason {
    USER_CANCELLED,     // El usuario canceló manualmente
    BATTERY_LOW,        // Batería insuficiente para continuar
    SYSTEM_KILL         // El sistema operativo mató el servicio
}

/**
 * Registro histórico de una sesión completada — se guarda en Room.
 *
 * @param id UUID único generado automáticamente.
 * @param activityType Tipo de actividad realizada.
 * @param plannedDurationMs Duración planeada en milisegundos.
 * @param actualDurationMs Duración real (puede ser menor si fue interrumpida).
 * @param wasCompleted Si true, la sesión llegó a 0:00 sin cancelarse.
 * @param startTimestamp Marca de tiempo de inicio (epoch ms).
 * @param inactivityAlertsCount Cuántas veces se detectó inactividad prolongada.
 */
data class FocusSessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val activityType: FocusActivityType,
    val plannedDurationMs: Long,
    val actualDurationMs: Long,
    val wasCompleted: Boolean,
    val startTimestamp: Long = System.currentTimeMillis(),
    val inactivityAlertsCount: Int = 0
)

/**
 * Opciones de duración disponibles en el selector.
 * Centralizado aquí para que reloj y teléfono muestren las mismas opciones.
 */
object FocusDurations {
    /** Lista de duraciones predefinidas en minutos. */
    val presets: List<Int> = listOf(15, 25, 30, 45, 60, 90)

    /** Convierte minutos a milisegundos. */
    fun minutesToMs(minutes: Int): Long = minutes * 60 * 1000L

    /** Convierte milisegundos a texto "MM:SS". */
    fun msToDisplayTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
