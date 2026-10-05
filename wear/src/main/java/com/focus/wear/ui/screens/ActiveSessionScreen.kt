// wear/src/main/java/com/focus/wear/ui/screens/ActiveSessionScreen.kt
// ───────────────────────────────────────────────────────────────
// Pantalla 3: Sesión de enfoque activa.
// El centro de la experiencia — minimalista y sin distracciones.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.focus.shared.model.FocusActivityType
import com.focus.shared.model.FocusDurations
import com.focus.wear.domain.timer.TimerState
import com.focus.wear.ui.theme.FocusColors
import com.focus.wear.ui.theme.FocusSpacing
import com.focus.wear.ui.theme.FocusTypography

/**
 * Pantalla principal de la sesión de enfoque activa.
 *
 * Diseño:
 * - Arco de progreso circular fino (fondo oscuro + arco de color)
 * - Tiempo restante en grande en el centro
 * - Nombre de la actividad en pequeño debajo
 * - Tap para pausar/reanudar
 * - Hold (mantener presionado) para cancelar — anticancelación impulsiva
 *
 * @param timerState Estado actual del temporizador.
 * @param activityType Tipo de actividad en curso.
 * @param onPause Callback al pausar.
 * @param onResume Callback al reanudar.
 * @param onCancelRequested Callback cuando el usuario pide cancelar (mostrará confirmación).
 */
@Composable
fun ActiveSessionScreen(
    timerState: TimerState,
    activityType: FocusActivityType,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancelRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Color de acento según el tipo de actividad
    val accentColor = when (activityType) {
        FocusActivityType.STUDY -> FocusColors.StudyAccent
        FocusActivityType.WORK -> FocusColors.WorkAccent
        FocusActivityType.READ -> FocusColors.ReadAccent
    }

    // Progreso animado del arco — suaviza los saltos entre ticks
    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "arc_progress"
    )

    // Animación de pulso cuando está en cuenta regresiva final
    val isCountingDown = timerState.remainingMs <= 60_000 && timerState.isRunning
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCountingDown) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            // Tap simple → pausa/reanuda | Long press → cancelar
            .pointerInput(timerState.isPaused) {
                detectTapGestures(
                    onTap = {
                        if (timerState.isPaused) onResume() else onPause()
                    },
                    onLongPress = {
                        onCancelRequested()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ── Arco de progreso circular ──────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            drawProgressArc(
                progress = animatedProgress,
                accentColor = accentColor,
                isPaused = timerState.isPaused
            )
        }

        // ── Contenido central ──────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Indicador de pausa
            AnimatedVisibility(
                visible = timerState.isPaused,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Text(
                    text = "PAUSADO",
                    style = FocusTypography.ActivityLabel,
                    color = FocusColors.TextSecondary
                )
            }

            // Tiempo restante — el corazón de la pantalla
            Text(
                text = FocusDurations.msToDisplayTime(timerState.remainingMs),
                style = FocusTypography.TimerDisplay.copy(
                    color = if (isCountingDown)
                        FocusColors.Warning.copy(alpha = pulseAlpha)
                    else
                        FocusColors.TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            // Nombre de la actividad — discreto, no distrae
            Text(
                text = activityType.displayName.uppercase(),
                style = FocusTypography.ActivityLabel,
                color = accentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // ── Hint de interacción (solo al inicio) ──────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (timerState.progress < 0.05f) {
                // Solo mostramos el hint al inicio de la sesión
                Text(
                    text = "Toca para pausar · Mantén para salir",
                    style = FocusTypography.Stat,
                    color = FocusColors.TextDisabled,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = FocusSpacing.xl)
                )
            }
        }
    }
}

/**
 * Dibuja el arco de progreso circular en el Canvas.
 *
 * Estructura:
 * 1. Arco de fondo (track) — siempre visible, muy oscuro.
 * 2. Arco de progreso — avanza según el tiempo transcurrido.
 *
 * @param progress Progreso de 0f a 1f.
 * @param accentColor Color del arco de progreso.
 * @param isPaused Si está pausado, el arco se dibuja más opaco.
 */
private fun DrawScope.drawProgressArc(
    progress: Float,
    accentColor: Color,
    isPaused: Boolean
) {
    val strokeWidth = 6.dp.toPx()      // Arco fino y elegante
    val padding = strokeWidth / 2f
    val arcSize = Size(
        width = size.width - padding * 2,
        height = size.height - padding * 2
    )
    val topLeft = Offset(x = padding, y = padding)

    // El arco empieza desde arriba (-90°) y gira en sentido horario
    val startAngle = -90f
    val sweepAngle = 360f * progress

    // 1. Arco de fondo (track)
    drawArc(
        color = accentColor.copy(alpha = FocusColors.ArcTrackAlpha),
        startAngle = startAngle,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // 2. Arco de progreso (solo si hay progreso)
    if (progress > 0.001f) {
        drawArc(
            color = accentColor.copy(alpha = if (isPaused) 0.5f else 1f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun ActiveSessionScreenPreview() {
    com.focus.wear.ui.theme.FocusTheme {
        ActiveSessionScreen(
            timerState = TimerState(
                totalDurationMs = 25 * 60 * 1000L,
                remainingMs = 15 * 60 * 1000L,
                isRunning = true,
                isPaused = false
            ),
            activityType = FocusActivityType.STUDY,
            onPause = {},
            onResume = {},
            onCancelRequested = {}
        )
    }
}
