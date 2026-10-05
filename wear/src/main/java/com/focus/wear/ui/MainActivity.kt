// wear/src/main/java/com/focus/wear/ui/MainActivity.kt
// ───────────────────────────────────────────────────────────────
// Activity principal del reloj — punto de entrada de la app.
// Aquí se conecta el ViewModel con las pantallas Compose.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.Text
import com.focus.wear.ui.screens.ActiveSessionScreen
import com.focus.wear.ui.screens.ActivitySelectScreen
import com.focus.wear.ui.screens.DurationSelectScreen
import com.focus.wear.ui.theme.FocusColors
import com.focus.wear.ui.theme.FocusTheme
import com.focus.wear.ui.theme.FocusTypography
import com.focus.wear.ui.viewmodel.FocusScreen
import com.focus.wear.ui.viewmodel.FocusViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity del reloj.
 *
 * @AndroidEntryPoint: permite inyección de dependencias con Hilt.
 *
 * Responsabilidades:
 * 1. Iniciar la Splash Screen.
 * 2. Solicitar permisos si son necesarios.
 * 3. Renderizar el contenido Compose.
 * 4. El NavHost actúa como router entre las pantallas.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // viewModels(): delegate de Jetpack que crea el ViewModel lazy y lo vincula al ciclo de vida
    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen debe instalarse ANTES de super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            FocusTheme {
                FocusApp(viewModel = viewModel)
            }
        }
    }
}

/**
 * Composable raíz de la aplicación.
 * Observa el estado del ViewModel y renderiza la pantalla correcta.
 *
 * Usamos AnimatedContent para transiciones suaves entre pantallas.
 */
@Composable
private fun FocusApp(viewModel: FocusViewModel) {
    // collectAsState() convierte el StateFlow en un State de Compose.
    // La UI se recompone automáticamente cuando el estado cambia.
    val uiState by viewModel.uiState.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // AnimatedContent maneja las transiciones entre pantallas
        AnimatedContent(
            targetState = uiState.screen,
            transitionSpec = {
                // Transición simple fade — no distrae en un reloj
                fadeIn(initialAlpha = 0f) togetherWith fadeOut(targetAlpha = 0f)
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is FocusScreen.ActivitySelect -> {
                    ActivitySelectScreen(
                        onActivitySelected = viewModel::onActivitySelected,
                        completedSessionsToday = uiState.todayStats.completedSessions
                    )
                }

                is FocusScreen.DurationSelect -> {
                    DurationSelectScreen(
                        activityType = uiState.selectedActivity,
                        initialDurationMinutes = uiState.selectedDurationMinutes,
                        onDurationSelected = { minutes ->
                            viewModel.onDurationChanged(minutes)
                            viewModel.onStartSession()
                        },
                        onBack = viewModel::onNavigateBack
                    )
                }

                is FocusScreen.ActiveSession -> {
                    ActiveSessionScreen(
                        timerState = timerState,
                        activityType = uiState.selectedActivity,
                        onPause = viewModel::onPauseSession,
                        onResume = viewModel::onResumeSession,
                        onCancelRequested = viewModel::onCancelSessionRequested
                    )
                }

                is FocusScreen.BreakTime -> {
                    BreakTimeContent(
                        onFinish = viewModel::onBreakFinished
                    )
                }

                is FocusScreen.SessionSummary -> {
                    // TODO: Pantalla de resumen de sesión
                }
            }
        }

        // Diálogo de confirmación de cancelación (anticancelación impulsiva)
        if (uiState.showCancelConfirmation) {
            CancelConfirmationDialog(
                onConfirm = viewModel::onCancelSessionConfirmed,
                onDismiss = viewModel::onCancelDismissed
            )
        }
    }
}

/**
 * Pantalla de descanso — simple y tranquilizante.
 */
@Composable
private fun BreakTimeContent(onFinish: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusColors.Background),
        contentAlignment = Alignment.Center
    ) {
        androidx.wear.compose.material.Chip(
            onClick = onFinish,
            label = {
                Text(
                    text = "Descanso",
                    style = FocusTypography.ButtonLabel,
                    color = FocusColors.TextPrimary
                )
            }
        )
    }
}

/**
 * Diálogo de confirmación de cancelación.
 *
 * Este diálogo aparece cuando el usuario hace long-press para cancelar.
 * El usuario debe confirmar explícitamente — evitamos cancelaciones accidentales.
 */
@Composable
private fun CancelConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Alert(
        title = {
            Text(
                text = "¿Salir?",
                style = FocusTypography.ButtonLabel,
                color = FocusColors.TextPrimary
            )
        },
        negativeButton = {
            androidx.wear.compose.material.Button(
                onClick = onDismiss,
                colors = androidx.wear.compose.material.ButtonDefaults.secondaryButtonColors()
            ) {
                Text(
                    text = "Continuar",
                    color = FocusColors.StudyAccent,
                    style = FocusTypography.Stat
                )
            }
        },
        positiveButton = {
            androidx.wear.compose.material.Button(
                onClick = onConfirm,
                colors = androidx.wear.compose.material.ButtonDefaults.primaryButtonColors()
            ) {
                Text(
                    text = "Salir",
                    color = FocusColors.Error,
                    style = FocusTypography.Stat
                )
            }
        },
        content = {
            Text(
                text = "La sesión no se contará como completada.",
                style = FocusTypography.Stat,
                color = FocusColors.TextSecondary
            )
        }
    )
}
