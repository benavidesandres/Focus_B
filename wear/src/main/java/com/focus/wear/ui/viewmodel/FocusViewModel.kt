// wear/src/main/java/com/focus/wear/ui/viewmodel/FocusViewModel.kt
// ───────────────────────────────────────────────────────────────
// ViewModel: puente entre la lógica de negocio y la UI.
// La UI NUNCA accede directamente a servicios, sensores o la base de datos.
// Todo pasa por el ViewModel.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.ui.viewmodel

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focus.shared.model.FocusActivityType
import com.focus.shared.model.FocusDurations
import com.focus.shared.model.FocusSessionRecord
import com.focus.shared.repository.FocusSessionRepository
import com.focus.wear.domain.timer.FocusTimerEngine
import com.focus.wear.domain.timer.TimerState
import com.focus.wear.service.FocusForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado completo de la UI — un único objeto que describe todo lo que la UI necesita renderizar.
 * Este patrón se llama "Unidirectional Data Flow" (UDF).
 *
 * La UI solo lee este estado — nunca lo modifica directamente.
 * Los cambios se hacen a través de los métodos del ViewModel (eventos de la UI).
 */
data class FocusUiState(
    /** Pantalla actual que debe mostrarse. */
    val screen: FocusScreen = FocusScreen.ActivitySelect,

    /** Tipo de actividad seleccionada por el usuario. */
    val selectedActivity: FocusActivityType = FocusActivityType.STUDY,

    /** Duración seleccionada en minutos. */
    val selectedDurationMinutes: Int = 45,

    /** Estado actual del temporizador. */
    val timerState: TimerState = TimerState(),

    /** Si se está mostrando el diálogo de confirmación para cancelar. */
    val showCancelConfirmation: Boolean = false,

    /** Estadísticas del día para mostrar al usuario. */
    val todayStats: TodayStats = TodayStats()
)

/**
 * Pantallas disponibles en la app del reloj.
 * Usamos sealed class para garantizar que la UI maneje todos los casos.
 */
sealed class FocusScreen {
    /** Pantalla 1: Selección de actividad (Estudiar / Trabajar / Leer). */
    data object ActivitySelect : FocusScreen()

    /** Pantalla 2: Selección de duración. */
    data object DurationSelect : FocusScreen()

    /** Pantalla 3: Sesión activa con el temporizador. */
    data object ActiveSession : FocusScreen()

    /** Pantalla 4: Descanso guiado después de completar la sesión. */
    data class BreakTime(val breakDurationMinutes: Int = 5) : FocusScreen()

    /** Pantalla 5: Resumen de la sesión completada. */
    data object SessionSummary : FocusScreen()
}

/** Estadísticas del día actual. */
data class TodayStats(
    val completedSessions: Int = 0,
    val totalFocusMinutes: Int = 0
)

/**
 * ViewModel de la pantalla de enfoque.
 *
 * @HiltViewModel: Hilt sabe cómo crear este ViewModel con sus dependencias.
 * @Inject constructor: Hilt inyecta FocusTimerEngine y FocusSessionRepository.
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    application: Application,
    private val timerEngine: FocusTimerEngine,
    private val sessionRepository: FocusSessionRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    /**
     * StateFlow del tiempo restante para la UI del temporizador.
     * stateIn: convierte el Flow frío del engine en un StateFlow caliente
     * que se comparte entre múltiples observadores.
     */
    val timerState: StateFlow<TimerState> = timerEngine.timerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerState()
        )

    init {
        // Observar el timer para actualizar la pantalla cuando cambia de estado
        observeTimerState()
        // Cargar estadísticas del día
        loadTodayStats()
    }

    // ─── Eventos de la UI (UI Events) ──────────────────────────

    /** El usuario seleccionó un tipo de actividad. */
    fun onActivitySelected(activityType: FocusActivityType) {
        _uiState.update {
            it.copy(
                selectedActivity = activityType,
                selectedDurationMinutes = activityType.defaultDurationMinutes,
                screen = FocusScreen.DurationSelect
            )
        }
    }

    /** El usuario cambió la duración en el selector. */
    fun onDurationChanged(minutes: Int) {
        _uiState.update { it.copy(selectedDurationMinutes = minutes) }
    }

    /** El usuario presionó "Iniciar sesión". */
    fun onStartSession() {
        val durationMs = FocusDurations.minutesToMs(_uiState.value.selectedDurationMinutes)
        val activityType = _uiState.value.selectedActivity

        // Iniciar el ForegroundService — este es el punto de entrada al timer
        val intent = FocusForegroundService.buildStartIntent(
            context = getApplication(),
            durationMs = durationMs,
            activityTypeName = activityType.name
        )
        ContextCompat.startForegroundService(getApplication(), intent)

        _uiState.update { it.copy(screen = FocusScreen.ActiveSession) }
    }

    /** El usuario presionó el botón de pausa. */
    fun onPauseSession() {
        val intent = FocusForegroundService.buildPauseIntent(getApplication())
        getApplication<Application>().startService(intent)
    }

    /** El usuario presionó el botón de reanudar. */
    fun onResumeSession() {
        val intent = FocusForegroundService.buildResumeIntent(getApplication())
        getApplication<Application>().startService(intent)
    }

    /** El usuario quiere cancelar — mostrar confirmación primero (anticancelación impulsiva). */
    fun onCancelSessionRequested() {
        _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    /** El usuario confirmó la cancelación. */
    fun onCancelSessionConfirmed() {
        // Guardar el registro de sesión interrumpida
        saveInterruptedSession()

        // Detener el servicio
        val stopIntent = FocusForegroundService.buildStopIntent(getApplication())
        getApplication<Application>().startService(stopIntent)

        _uiState.update {
            it.copy(
                screen = FocusScreen.ActivitySelect,
                showCancelConfirmation = false
            )
        }
    }

    /** El usuario descartó el diálogo de cancelación — continuar la sesión. */
    fun onCancelDismissed() {
        _uiState.update { it.copy(showCancelConfirmation = false) }
    }

    /** El usuario terminó el descanso y quiere volver al inicio. */
    fun onBreakFinished() {
        _uiState.update { it.copy(screen = FocusScreen.ActivitySelect) }
    }

    /** El usuario quiere volver a la pantalla anterior. */
    fun onNavigateBack() {
        val currentScreen = _uiState.value.screen
        val previousScreen = when (currentScreen) {
            is FocusScreen.DurationSelect -> FocusScreen.ActivitySelect
            else -> return // No navegamos atrás desde otras pantallas
        }
        _uiState.update { it.copy(screen = previousScreen) }
    }

    // ─── Lógica Interna ────────────────────────────────────────

    /**
     * Observa el estado del timer para hacer transiciones automáticas de pantalla.
     */
    private fun observeTimerState() {
        viewModelScope.launch {
            timerEngine.timerState.collect { timerState ->
                _uiState.update { it.copy(timerState = timerState) }

                if (timerState.isFinished && _uiState.value.screen == FocusScreen.ActiveSession) {
                    // Sesión completada — guardar y navegar al descanso
                    saveCompletedSession()
                    _uiState.update { it.copy(screen = FocusScreen.BreakTime()) }
                }
            }
        }
    }

    /**
     * Guarda la sesión completada en la base de datos.
     */
    private fun saveCompletedSession() {
        viewModelScope.launch {
            val state = _uiState.value
            sessionRepository.saveSession(
                FocusSessionRecord(
                    activityType = state.selectedActivity,
                    plannedDurationMs = FocusDurations.minutesToMs(state.selectedDurationMinutes),
                    actualDurationMs = FocusDurations.minutesToMs(state.selectedDurationMinutes),
                    wasCompleted = true
                )
            )
        }
    }

    /**
     * Guarda una sesión interrumpida con el tiempo real transcurrido.
     */
    private fun saveInterruptedSession() {
        viewModelScope.launch {
            val state = _uiState.value
            val plannedMs = FocusDurations.minutesToMs(state.selectedDurationMinutes)
            val remainingMs = timerEngine.timerState.value.remainingMs
            val actualMs = plannedMs - remainingMs

            if (actualMs > 60_000) { // Solo guardar si duró más de 1 minuto
                sessionRepository.saveSession(
                    FocusSessionRecord(
                        activityType = state.selectedActivity,
                        plannedDurationMs = plannedMs,
                        actualDurationMs = actualMs,
                        wasCompleted = false
                    )
                )
            }
        }
    }

    /**
     * Carga las estadísticas del día actual para mostrar en la pantalla de inicio.
     */
    private fun loadTodayStats() {
        viewModelScope.launch {
            combine(
                sessionRepository.completedSessionsCount,
                sessionRepository.totalFocusTimeMs
            ) { count, totalMs ->
                TodayStats(
                    completedSessions = count,
                    totalFocusMinutes = (totalMs / 60_000L).toInt()
                )
            }.collect { stats ->
                _uiState.update { it.copy(todayStats = stats) }
            }
        }
    }
}
