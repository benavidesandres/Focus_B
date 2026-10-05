// wear/src/main/java/com/focus/wear/ui/screens/DurationSelectScreen.kt
// ───────────────────────────────────────────────────────────────
// Pantalla 2: Selector de duración con scroll circular (Picker de Wear OS).
// ───────────────────────────────────────────────────────────────
package com.focus.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.Text
import com.focus.shared.model.FocusActivityType
import com.focus.shared.model.FocusDurations
import com.focus.wear.ui.theme.FocusColors
import com.focus.wear.ui.theme.FocusSpacing
import com.focus.wear.ui.theme.FocusTypography

/**
 * Pantalla de selección de duración.
 *
 * Usa InlineSlider de Wear OS — optimizado para la corona giratoria del reloj.
 * El usuario puede usar el giro físico del bezel/corona para ajustar el tiempo.
 *
 * @param activityType Actividad seleccionada (para mostrar el nombre y sugerir duración).
 * @param initialDurationMinutes Duración inicial sugerida.
 * @param onDurationSelected Callback cuando el usuario confirma la duración.
 * @param onBack Callback para volver a la pantalla anterior.
 */
@Composable
fun DurationSelectScreen(
    activityType: FocusActivityType,
    initialDurationMinutes: Int,
    onDurationSelected: (minutes: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durations = FocusDurations.presets

    // Estado local del selector — index de la lista de duraciones
    var selectedIndex by remember {
        mutableIntStateOf(
            durations.indexOfFirst { it == initialDurationMinutes }
                .takeIf { it >= 0 } ?: 1
        )
    }

    val accentColor = when (activityType) {
        FocusActivityType.STUDY -> FocusColors.StudyAccent
        FocusActivityType.WORK -> FocusColors.WorkAccent
        FocusActivityType.READ -> FocusColors.ReadAccent
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            .padding(horizontal = FocusSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Etiqueta de la actividad
        Text(
            text = activityType.displayName.uppercase(),
            style = FocusTypography.ActivityLabel,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(FocusSpacing.m))

        // Duración actual en grande
        Text(
            text = "${durations[selectedIndex]}",
            style = FocusTypography.TimerDisplay,
            color = FocusColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "minutos",
            style = FocusTypography.ActivityLabel,
            color = FocusColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(FocusSpacing.m))

        // InlineSlider: optimizado para corona giratoria del reloj
        InlineSlider(
            value = selectedIndex,
            onValueChange = { selectedIndex = it },
            valueProgression = 0 until durations.size,
            decreaseIcon = {
                Text("-", color = FocusColors.TextSecondary)
            },
            increaseIcon = {
                Text("+", color = FocusColors.TextSecondary)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = InlineSliderDefaults.colors(
                selectedBarColor = accentColor,
                unselectedBarColor = FocusColors.ArcBackground
            )
        )

        Spacer(modifier = Modifier.height(FocusSpacing.l))

        // Botón de inicio — circular y llamativo
        Button(
            onClick = { onDurationSelected(durations[selectedIndex]) },
            modifier = Modifier.size(56.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = accentColor)
        ) {
            Text(
                text = "▶",
                color = FocusColors.Background,
                style = FocusTypography.ButtonLabel
            )
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DurationSelectScreenPreview() {
    com.focus.wear.ui.theme.FocusTheme {
        DurationSelectScreen(
            activityType = FocusActivityType.STUDY,
            initialDurationMinutes = 25,
            onDurationSelected = {},
            onBack = {}
        )
    }
}
