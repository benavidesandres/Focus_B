// wear/src/main/java/com/focus/wear/ui/screens/ActivitySelectScreen.kt
// ───────────────────────────────────────────────────────────────
// Pantalla 1: Selección de tipo de actividad.
// UI minimalista — 3 opciones grandes y fáciles de tocar en la pantalla redonda.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.focus.shared.model.FocusActivityType
import com.focus.wear.ui.theme.FocusColors
import com.focus.wear.ui.theme.FocusSpacing
import com.focus.wear.ui.theme.FocusTheme
import com.focus.wear.ui.theme.FocusTypography

/**
 * Pantalla de selección de actividad.
 *
 * @param onActivitySelected Callback cuando el usuario elige una actividad.
 * @param completedSessionsToday Número de sesiones completadas hoy (se muestra en el encabezado).
 */
@Composable
fun ActivitySelectScreen(
    onActivitySelected: (FocusActivityType) -> Unit,
    completedSessionsToday: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FocusColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // ScalingLazyColumn: adaptado para pantallas redondas — escala items en los bordes.
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.s)
        ) {
            item {
                Spacer(modifier = Modifier.height(FocusSpacing.xl))
            }

            // Encabezado con estadísticas del día
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FOCUS",
                        style = FocusTypography.ActivityLabel,
                        color = FocusColors.TextSecondary
                    )
                    if (completedSessionsToday > 0) {
                        Spacer(modifier = Modifier.height(FocusSpacing.xs))
                        Text(
                            text = "$completedSessionsToday ${if (completedSessionsToday == 1) "sesión" else "sesiones"} hoy",
                            style = FocusTypography.Stat,
                            color = FocusColors.StudyAccent
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(FocusSpacing.s)) }

            // Tarjetas de selección de actividad
            item {
                ActivityCard(
                    activityType = FocusActivityType.STUDY,
                    accentColor = FocusColors.StudyAccent,
                    emoji = "📚",
                    onClick = { onActivitySelected(FocusActivityType.STUDY) }
                )
            }

            item {
                ActivityCard(
                    activityType = FocusActivityType.WORK,
                    accentColor = FocusColors.WorkAccent,
                    emoji = "💻",
                    onClick = { onActivitySelected(FocusActivityType.WORK) }
                )
            }

            item {
                ActivityCard(
                    activityType = FocusActivityType.READ,
                    accentColor = FocusColors.ReadAccent,
                    emoji = "📖",
                    onClick = { onActivitySelected(FocusActivityType.READ) }
                )
            }

            item { Spacer(modifier = Modifier.height(FocusSpacing.xl)) }
        }
    }
}

/**
 * Tarjeta de actividad individual — grande, clara y fácil de tocar con el dedo.
 */
@Composable
private fun ActivityCard(
    activityType: FocusActivityType,
    accentColor: Color,
    emoji: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animación de color al presionar
    val backgroundColor by animateColorAsState(
        targetValue = accentColor.copy(alpha = 0.12f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_bg_${activityType.name}"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = FocusSpacing.m, horizontal = FocusSpacing.l),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                style = FocusTypography.ButtonLabel.copy(fontSize = 22.sp)
            )
            Spacer(modifier = Modifier.height(FocusSpacing.xs))
            Text(
                text = activityType.displayName.uppercase(),
                style = FocusTypography.ActivityLabel,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${activityType.defaultDurationMinutes} min",
                style = FocusTypography.Stat,
                color = FocusColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun ActivitySelectScreenPreview() {
    FocusTheme {
        ActivitySelectScreen(
            onActivitySelected = {},
            completedSessionsToday = 2
        )
    }
}
