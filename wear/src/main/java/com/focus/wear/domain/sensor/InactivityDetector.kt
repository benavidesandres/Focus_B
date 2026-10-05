// wear/src/main/java/com/focus/wear/domain/sensor/InactivityDetector.kt
// ───────────────────────────────────────────────────────────────
// Detector de inactividad física prolongada.
// Monitorea el acelerómetro y alerta si el usuario lleva demasiado tiempo quieto.
// ───────────────────────────────────────────────────────────────
package com.focus.wear.domain.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Tipos de alertas de inactividad que puede emitir el detector.
 */
sealed interface InactivityEvent {
    /** El usuario lleva mucho tiempo sin moverse — recordar estirarse. */
    data object ProlongedInactivity : InactivityEvent

    /** El usuario volvió a moverse después de un período de inactividad. */
    data object ActivityResumed : InactivityEvent
}

/**
 * Detector de inactividad usando el acelerómetro.
 *
 * Implementa [SensorEventListener] para recibir datos del sensor directamente.
 *
 * Lógica:
 * 1. Registra el sensor de acelerómetro cuando la sesión está activa.
 * 2. Calcula la magnitud del vector de aceleración (excluyendo la gravedad).
 * 3. Si la magnitud se mantiene baja por más de [INACTIVITY_THRESHOLD_MS], emite un evento.
 */
@Singleton
class InactivityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private companion object {
        /** Tiempo en ms sin movimiento para considerar inactividad. */
        const val INACTIVITY_THRESHOLD_MS = 30 * 60 * 1000L  // 30 minutos

        /** Aceleración mínima en m/s² para considerar que hay movimiento. */
        const val MOVEMENT_THRESHOLD_MS2 = 0.5f

        /** Constante de filtro paso bajo para eliminar el ruido del sensor. */
        const val LOW_PASS_ALPHA = 0.1f
    }

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Channel: similar a un canal de eventos — emite y consume de forma segura entre corrutinas.
    private val _events = Channel<InactivityEvent>(capacity = Channel.BUFFERED)

    /** Flow público de eventos de inactividad — la UI/ViewModel lo observa. */
    val events: Flow<InactivityEvent> = _events.receiveAsFlow()

    // Estado del filtro paso bajo (para suavizar lecturas del sensor)
    private var gravity = FloatArray(3)

    // Timestamp del último movimiento detectado
    private var lastMovementMs: Long = System.currentTimeMillis()

    // Si ya emitimos la alerta de inactividad para este período
    private var inactivityAlertSent: Boolean = false

    /**
     * Inicia el monitoreo de movimiento.
     * Llamar cuando empieza la sesión de enfoque.
     */
    fun startMonitoring() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return // El dispositivo no tiene acelerómetro — no hacemos nada

        lastMovementMs = System.currentTimeMillis()
        inactivityAlertSent = false

        // SENSOR_DELAY_NORMAL: 5 actualizaciones/segundo — suficiente para detectar postura.
        // No usamos FASTEST porque gastaría demasiada batería.
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Detiene el monitoreo. Llamar cuando la sesión termina o se cancela.
     */
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Callback del sensor — llamado cada vez que hay una nueva lectura.
     * IMPORTANTE: Este callback llega en el hilo del sensor, NO en el hilo principal.
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Filtro paso bajo: separa la gravedad de la aceleración del movimiento.
        // Esto evita que la orientación del reloj cause falsas alarmas.
        gravity[0] = LOW_PASS_ALPHA * event.values[0] + (1 - LOW_PASS_ALPHA) * gravity[0]
        gravity[1] = LOW_PASS_ALPHA * event.values[1] + (1 - LOW_PASS_ALPHA) * gravity[1]
        gravity[2] = LOW_PASS_ALPHA * event.values[2] + (1 - LOW_PASS_ALPHA) * gravity[2]

        // Aceleración lineal = aceleración total - gravedad
        val linearX = event.values[0] - gravity[0]
        val linearY = event.values[1] - gravity[1]
        val linearZ = event.values[2] - gravity[2]

        // Magnitud del vector de aceleración lineal
        val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        val now = System.currentTimeMillis()

        if (magnitude > MOVEMENT_THRESHOLD_MS2) {
            // Movimiento detectado
            if (inactivityAlertSent) {
                // El usuario retomó actividad después de estar quieto
                _events.trySend(InactivityEvent.ActivityResumed)
                inactivityAlertSent = false
            }
            lastMovementMs = now
        } else {
            // Sin movimiento significativo — verificar si ya pasó el umbral
            val inactiveDurationMs = now - lastMovementMs
            if (inactiveDurationMs >= INACTIVITY_THRESHOLD_MS && !inactivityAlertSent) {
                _events.trySend(InactivityEvent.ProlongedInactivity)
                inactivityAlertSent = true
                // Reiniciamos para no enviar alertas repetidas hasta que se mueva
                lastMovementMs = now
            }
        }
    }

    /** Llamado cuando cambia la precisión del sensor — no necesitamos manejarlo. */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
