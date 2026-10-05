// shared/src/main/java/com/focus/shared/database/FocusDatabase.kt
// ───────────────────────────────────────────────────────────────
// Capa de persistencia local usando Room.
// Room es el ORM oficial de Android — abstrae SQLite con seguridad de tipos.
// ───────────────────────────────────────────────────────────────
package com.focus.shared.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.focus.shared.model.FocusActivityType
import com.focus.shared.model.FocusSessionRecord
import kotlinx.coroutines.flow.Flow

// ─── 1. ENTIDAD (tabla en la base de datos) ────────────────────

/**
 * Entidad Room que mapea a la tabla "focus_sessions".
 * Cada propiedad es una columna de la tabla.
 *
 * Nota: Room no soporta tipos personalizados directamente,
 * por lo que FocusActivityType se guarda como String.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "activity_type")
    val activityType: String,          // Guardamos el .name del enum

    @ColumnInfo(name = "planned_duration_ms")
    val plannedDurationMs: Long,

    @ColumnInfo(name = "actual_duration_ms")
    val actualDurationMs: Long,

    @ColumnInfo(name = "was_completed")
    val wasCompleted: Boolean,

    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long,

    @ColumnInfo(name = "inactivity_alerts_count")
    val inactivityAlertsCount: Int
)

// Funciones de extensión para convertir entre Entity y modelo de dominio:

/** Convierte el modelo de dominio [FocusSessionRecord] a entidad de base de datos. */
fun FocusSessionRecord.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    activityType = activityType.name,
    plannedDurationMs = plannedDurationMs,
    actualDurationMs = actualDurationMs,
    wasCompleted = wasCompleted,
    startTimestamp = startTimestamp,
    inactivityAlertsCount = inactivityAlertsCount
)

/** Convierte la entidad de base de datos al modelo de dominio [FocusSessionRecord]. */
fun FocusSessionEntity.toDomain(): FocusSessionRecord = FocusSessionRecord(
    id = id,
    activityType = FocusActivityType.fromName(activityType),
    plannedDurationMs = plannedDurationMs,
    actualDurationMs = actualDurationMs,
    wasCompleted = wasCompleted,
    startTimestamp = startTimestamp,
    inactivityAlertsCount = inactivityAlertsCount
)

// ─── 2. DAO (Data Access Object — consultas SQL typesafe) ──────

/**
 * Interfaz DAO: define las operaciones de la base de datos.
 * Room genera automáticamente la implementación en tiempo de compilación.
 */
@Dao
interface FocusSessionDao {

    /**
     * Inserta una sesión. OnConflictStrategy.REPLACE sobreescribe si existe el mismo ID.
     * Es una 'suspend fun' porque es una operación de I/O — no debe bloquear el hilo principal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    /**
     * Retorna todas las sesiones ordenadas por fecha descendente.
     * Flow<List<>> emite automáticamente cuando cambian los datos — reactivo.
     */
    @Query("SELECT * FROM focus_sessions ORDER BY start_timestamp DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    /**
     * Total de sesiones completadas (para estadísticas).
     */
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE was_completed = 1")
    fun observeCompletedCount(): Flow<Int>

    /**
     * Tiempo total enfocado en milisegundos (solo sesiones completadas).
     */
    @Query("SELECT SUM(actual_duration_ms) FROM focus_sessions WHERE was_completed = 1")
    fun observeTotalFocusTimeMs(): Flow<Long?>

    /**
     * Sesiones de los últimos 7 días para el historial semanal.
     */
    @Query("""
        SELECT * FROM focus_sessions 
        WHERE start_timestamp >= :weekStartMs 
        ORDER BY start_timestamp DESC
    """)
    fun observeSessionsThisWeek(weekStartMs: Long): Flow<List<FocusSessionEntity>>

    /**
     * Elimina todas las sesiones (para reset completo).
     */
    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}

// ─── 3. BASE DE DATOS ROOM ─────────────────────────────────────

/**
 * Base de datos Room.
 *
 * @param entities Lista de entidades (tablas).
 * @param version Número de versión — si cambias el esquema, incrementa esto
 *                y provee una Migración o usa fallbackToDestructiveMigration().
 * @param exportSchema Guardar el esquema como JSON para control de versiones.
 */
@Database(
    entities = [FocusSessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class FocusDatabase : RoomDatabase() {

    /** Expone el DAO para acceder a las sesiones. */
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        private const val DATABASE_NAME = "focus_database"

        /**
         * Crea la base de datos.
         * Nota: La instancia la gestiona Hilt (ver DatabaseModule.kt),
         * NO uses getInstance() directamente en el código.
         */
        fun create(context: Context): FocusDatabase =
            Room.databaseBuilder(
                context = context.applicationContext,
                klass = FocusDatabase::class.java,
                name = DATABASE_NAME
            )
            // Solo en desarrollo — en producción usa migraciones
            .fallbackToDestructiveMigration()
            .build()
    }
}
