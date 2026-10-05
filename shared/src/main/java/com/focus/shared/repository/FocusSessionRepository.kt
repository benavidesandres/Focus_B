// shared/src/main/java/com/focus/shared/repository/FocusSessionRepository.kt
// ───────────────────────────────────────────────────────────────
// Repositorio: capa intermedia entre la UI y las fuentes de datos.
// Patrón Repository → oculta de dónde vienen los datos (Room, API, etc.)
// ───────────────────────────────────────────────────────────────
package com.focus.shared.repository

import com.focus.shared.database.FocusSessionDao
import com.focus.shared.database.toDomain
import com.focus.shared.database.toEntity
import com.focus.shared.model.FocusSessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio de sesiones de enfoque.
 *
 * @Singleton: Hilt garantiza que solo exista una instancia en toda la app.
 * @Inject constructor: Hilt inyecta las dependencias automáticamente.
 */
@Singleton
class FocusSessionRepository @Inject constructor(
    private val dao: FocusSessionDao
) {
    /**
     * Flow reactivo con todas las sesiones.
     * La UI observa este Flow y se actualiza automáticamente.
     */
    val allSessions: Flow<List<FocusSessionRecord>> =
        dao.observeAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }

    /** Flow con el total de sesiones completadas. */
    val completedSessionsCount: Flow<Int> = dao.observeCompletedCount()

    /** Flow con el tiempo total de enfoque en ms (puede ser null si no hay sesiones). */
    val totalFocusTimeMs: Flow<Long> =
        dao.observeTotalFocusTimeMs().map { it ?: 0L }

    /**
     * Flow con las sesiones de los últimos 7 días.
     * Calcula el inicio de la semana actual (lunes a las 00:00:00).
     */
    val sessionsThisWeek: Flow<List<FocusSessionRecord>> = dao
        .observeSessionsThisWeek(weekStartMs = getWeekStartMs())
        .map { entities -> entities.map { it.toDomain() } }

    /**
     * Guarda un registro de sesión en la base de datos.
     * suspend: debe llamarse desde una corrutina.
     */
    suspend fun saveSession(session: FocusSessionRecord) {
        dao.insertSession(session.toEntity())
    }

    /**
     * Elimina todos los datos (para la opción de reset en ajustes).
     */
    suspend fun clearAllData() {
        dao.deleteAllSessions()
    }

    /**
     * Calcula el timestamp del inicio del lunes de la semana actual.
     */
    private fun getWeekStartMs(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
