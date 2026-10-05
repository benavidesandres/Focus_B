// wear/src/main/java/com/focus/wear/service/FocusForegroundService.kt
// ───────────────────────────────────────────────────────────────
// Foreground Service: mantiene el temporizador activo aunque la pantalla se apague.
// Sin este servicio, Android mataría la app para ahorrar batería.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.focus.shared.model.FocusDurations
import com.focus.wear.R
import com.focus.wear.domain.haptics.HapticPattern
import com.focus.wear.domain.haptics.VibrationController
import com.focus.wear.domain.sensor.InactivityDetector
import com.focus.wear.domain.sensor.InactivityEvent
import com.focus.wear.domain.timer.FocusTimerEngine
import com.focus.wear.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service que orquesta el temporizador, sensores y háptica.
 *
 * @AndroidEntryPoint: permite a Hilt inyectar dependencias en este servicio.
 *
 * Ciclo de vida del servicio:
 * 1. WearActivity llama a startForegroundService(intent) con el comando.
 * 2. onStartCommand() procesa el comando (START, PAUSE, RESUME, STOP).
 * 3. El servicio se mantiene vivo mientras hay una sesión activa.
 * 4. Cuando el timer termina o el usuario cancela, el servicio se autodestruye.
 */
@AndroidEntryPoint
class FocusForegroundService : Service() {

    companion object {
        // Identificadores del canal y notificación
        const val NOTIFICATION_CHANNEL_ID = "focus_session_channel"
        const val NOTIFICATION_ID = 1001

        // Extras del Intent para pasar datos al servicio
        const val EXTRA_COMMAND = "extra_command"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"

        // Comandos que acepta el servicio
        const val COMMAND_START = "command_start"
        const val COMMAND_PAUSE = "command_pause"
        const val COMMAND_RESUME = "command_resume"
        const val COMMAND_STOP = "command_stop"

        /** Helper para crear el Intent de inicio desde cualquier parte de la app. */
        fun buildStartIntent(
            context: Context,
            durationMs: Long,
            activityTypeName: String
        ): Intent = Intent(context, FocusForegroundService::class.java).apply {
            putExtra(EXTRA_COMMAND, COMMAND_START)
            putExtra(EXTRA_DURATION_MS, durationMs)
            putExtra(EXTRA_ACTIVITY_TYPE, activityTypeName)
        }

        /** Helper para crear el Intent de pausa. */
        fun buildPauseIntent(context: Context): Intent =
            Intent(context, FocusForegroundService::class.java).apply {
                putExtra(EXTRA_COMMAND, COMMAND_PAUSE)
            }

        /** Helper para crear el Intent de reanudación. */
        fun buildResumeIntent(context: Context): Intent =
            Intent(context, FocusForegroundService::class.java).apply {
                putExtra(EXTRA_COMMAND, COMMAND_RESUME)
            }

        /** Helper para crear el Intent de detención. */
        fun buildStopIntent(context: Context): Intent =
            Intent(context, FocusForegroundService::class.java).apply {
                putExtra(EXTRA_COMMAND, COMMAND_STOP)
            }
    }

    // Dependencias inyectadas por Hilt
    @Inject lateinit var timerEngine: FocusTimerEngine
    @Inject lateinit var inactivityDetector: InactivityDetector
    @Inject lateinit var vibrationController: VibrationController

    /**
     * Scope de corrutinas del servicio.
     * SupervisorJob: si una corrutina hija falla, no cancela las demás.
     * Se cancela en onDestroy() para limpiar todos los recursos.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // WakeLock para asegurar que el reloj procese el tiempo con la pantalla apagada
    private var wakeLock: PowerManager.WakeLock? = null

    // Flags de hitos para evitar vibraciones repetidas múltiples veces
    private var milestone50Triggered = false
    private var milestone75Triggered = false
    private var countdownWarningTriggered = false

    // Control de frecuencia para no spammear la notificación en cada tick de 100ms
    private var lastNotifiedSecond: Long = -1L
    private var lastNotifiedPausedState: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null // No usamos binding

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val initialNotification = buildNotification("Iniciando sesión...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        observeTimerForNotifications()
        observeInactivityEvents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(EXTRA_COMMAND)) {
            COMMAND_START -> {
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
                if (durationMs > 0) {
                    resetMilestoneFlags()
                    acquireWakeLock(durationMs)
                    timerEngine.start(scope = serviceScope, durationMs = durationMs)
                    inactivityDetector.startMonitoring()
                    vibrationController.vibrate(HapticPattern.SESSION_START)
                }
            }
            COMMAND_PAUSE -> {
                timerEngine.pause()
                vibrationController.vibrate(HapticPattern.CONFIRM_ACTION)
            }
            COMMAND_RESUME -> {
                timerEngine.resume()
                vibrationController.vibrate(HapticPattern.CONFIRM_ACTION)
            }
            COMMAND_STOP -> {
                stopSession()
            }
        }

        // START_NOT_STICKY: si el sistema mata el servicio, NO lo reinicia automáticamente.
        return START_NOT_STICKY
    }

    private fun resetMilestoneFlags() {
        milestone50Triggered = false
        milestone75Triggered = false
        countdownWarningTriggered = false
        lastNotifiedSecond = -1L
        lastNotifiedPausedState = null
    }

    /**
     * Observa el timer para actualizar la notificación de forma eficiente y detectar finalización.
     */
    private fun observeTimerForNotifications() {
        timerEngine.timerState
            .onEach { state ->
                when {
                    state.isFinished -> {
                        releaseWakeLock()
                        vibrationController.vibrate(HapticPattern.SESSION_COMPLETE)
                        inactivityDetector.stopMonitoring()
                        updateNotification("¡Sesión completada! 🎉")
                        stopSelf()
                    }
                    state.isRunning -> {
                        val currentSecond = state.remainingMs / 1000L
                        // Solo actualizar la notificación cuando cambie el segundo o el estado de pausa
                        if (currentSecond != lastNotifiedSecond || state.isPaused != lastNotifiedPausedState) {
                            lastNotifiedSecond = currentSecond
                            lastNotifiedPausedState = state.isPaused
                            val timeText = FocusDurations.msToDisplayTime(state.remainingMs)
                            val statusText = if (state.isPaused) "Pausado • $timeText" else timeText
                            updateNotification(statusText)
                        }

                        // Vibrar exactamente una vez al 50% y al 75%
                        val progress = state.progress
                        if (progress >= 0.50f && !milestone50Triggered) {
                            milestone50Triggered = true
                            vibrationController.vibrate(HapticPattern.PROGRESS_MILESTONE)
                        }
                        if (progress >= 0.75f && !milestone75Triggered) {
                            milestone75Triggered = true
                            vibrationController.vibrate(HapticPattern.PROGRESS_MILESTONE)
                        }

                        // Alerta de un solo disparo en el último minuto
                        if (state.remainingMs in 1..60_000L && !countdownWarningTriggered) {
                            countdownWarningTriggered = true
                            vibrationController.vibrate(HapticPattern.COUNTDOWN_WARNING)
                        }
                    }
                }
            }
            .launchIn(serviceScope)
    }

    /**
     * Observa los eventos del detector de inactividad y vibra apropiadamente.
     */
    private fun observeInactivityEvents() {
        serviceScope.launch {
            inactivityDetector.events.collectLatest { event ->
                when (event) {
                    is InactivityEvent.ProlongedInactivity -> {
                        vibrationController.vibrate(HapticPattern.INACTIVITY_REMINDER)
                        updateNotification("¡Muévete un poco! 🧘")
                    }
                    is InactivityEvent.ActivityResumed -> {
                        // Volvemos al estado normal
                    }
                }
            }
        }
    }

    /**
     * Detiene la sesión y limpia recursos.
     */
    private fun stopSession() {
        releaseWakeLock()
        timerEngine.stop()
        inactivityDetector.stopMonitoring()
        vibrationController.vibrate(HapticPattern.CANCEL_ACTION)
        stopSelf()
    }

    private fun acquireWakeLock(durationMs: Long) {
        releaseWakeLock()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocusWear:SessionWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(durationMs + 120_000L)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
    }

    // ─── Notificación de Foreground ────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Sesión de Enfoque",
            // IMPORTANCE_LOW: sin sonido, sin vibración propia — solo visual en la barra
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Muestra el progreso de tu sesión de enfoque"
            setShowBadge(false)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        // Intent para abrir la app al tocar la notificación
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle("Focus — Sesión Activa")
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)          // No se puede descartar deslizando
            .setSilent(true)           // Sin sonido en la notificación
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }
}
