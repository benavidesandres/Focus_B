// wear/src/main/java/com/focus/wear/ui/theme/FocusTheme.kt
// ───────────────────────────────────────────────────────────────
// Sistema de diseño de Focus para Wear OS.
// Colores OLED (negro puro), tipografía minimalista, tokens de diseño.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme

// ─── COLORES ──────────────────────────────────────────────────

/**
 * Paleta de colores de Focus.
 * Negro puro (#000000) para OLED — ahorra hasta 40% de batería en pantallas AMOLED.
 * Acento: verde-azulado (#00E5C8) — energético pero no agresivo para la vista.
 */
object FocusColors {
    // Fondos
    val Background = Color(0xFF000000)       // Negro puro OLED
    val Surface = Color(0xFF0D0D0D)          // Superficie muy oscura
    val SurfaceVariant = Color(0xFF1A1A1A)   // Para cards y elementos elevados

    // Colores de acento por tipo de actividad
    val StudyAccent = Color(0xFF00E5C8)      // Cian/Verde — estudio, flujo mental
    val WorkAccent = Color(0xFF7C6FFF)       // Violeta — concentración profesional
    val ReadAccent = Color(0xFFFF9E4D)       // Ámbar — lectura, calidez

    // Colores de estado
    val Success = Color(0xFF00E5A0)          // Verde éxito — sesión completada
    val Warning = Color(0xFFFFB930)          // Ámbar — cuenta regresiva
    val Error = Color(0xFFFF5252)            // Rojo — cancelación

    // Texto
    val TextPrimary = Color(0xFFFFFFFF)      // Blanco puro para tiempo
    val TextSecondary = Color(0xFFB0B0B0)    // Gris medio para etiquetas
    val TextDisabled = Color(0xFF505050)     // Gris oscuro para texto inactivo

    // Arco de progreso
    val ArcBackground = Color(0xFF1E1E1E)    // Pista de fondo del arco
    val ArcTrackAlpha = 0.15f               // Opacidad del track de fondo
}

// ─── TIPOGRAFÍA ───────────────────────────────────────────────

/**
 * Familia tipográfica.
 * Nota: Para usar Roboto Mono, agrega el archivo de fuente en res/font/
 * o usa Google Fonts. Aquí usamos Default (sistema) como fallback.
 */
object FocusTypography {
    /** Tiempo principal — enorme, imposible de ignorar. */
    val TimerDisplay = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,      // Light para elegancia minimalista
        fontSize = 52.sp,
        letterSpacing = (-1).sp            // Tracking negativo para compactar
    )

    /** Nombre de la actividad — pequeño, sutil. */
    val ActivityLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 2.sp               // Tracking positivo para elegancia
    )

    /** Botones de selección de actividad. */
    val ButtonLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )

    /** Estadísticas secundarias. */
    val Stat = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    )

    /** Mensajes de estado (pausa, inactividad). */
    val StatusMessage = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    )
}

// ─── ESPACIADO ────────────────────────────────────────────────

/** Tokens de espaciado consistentes. */
object FocusSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

// ─── COMPOSITION LOCALS ───────────────────────────────────────
// Permiten acceder al tema desde cualquier Composable sin pasar parámetros.

val LocalFocusColors = staticCompositionLocalOf { FocusColors }
val LocalFocusTypography = staticCompositionLocalOf { FocusTypography }
val LocalFocusSpacing = staticCompositionLocalOf { FocusSpacing }

/**
 * Tema principal de Focus para Wear OS.
 * Envuelve toda la jerarquía de Composables.
 *
 * Uso:
 * ```kotlin
 * FocusTheme {
 *     // Tu UI aquí
 * }
 * ```
 */
@Composable
fun FocusTheme(
    content: @Composable () -> Unit
) {
    // Proveemos los tokens del design system a todos los Composables hijos
    CompositionLocalProvider(
        LocalFocusColors provides FocusColors,
        LocalFocusTypography provides FocusTypography,
        LocalFocusSpacing provides FocusSpacing,
    ) {
        // MaterialTheme de Wear OS con nuestros colores
        MaterialTheme(
            colors = androidx.wear.compose.material.Colors(
                primary = FocusColors.StudyAccent,
                primaryVariant = FocusColors.StudyAccent,
                secondary = FocusColors.WorkAccent,
                background = FocusColors.Background,
                surface = FocusColors.Surface,
                onPrimary = FocusColors.Background,
                onSecondary = FocusColors.TextPrimary,
                onBackground = FocusColors.TextPrimary,
                onSurface = FocusColors.TextPrimary,
                onSurfaceVariant = FocusColors.TextSecondary,
                error = FocusColors.Error,
                onError = FocusColors.TextPrimary
            ),
            content = content
        )
    }
}
