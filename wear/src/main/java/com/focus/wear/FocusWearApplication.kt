// wear/src/main/java/com/focus/wear/FocusWearApplication.kt
// ───────────────────────────────────────────────────────────────
// Application class — punto de inicio del proceso de la app.
// @HiltAndroidApp genera el componente raíz de Hilt automáticamente.
// ───────────────────────────────────────────────────────────────
package com.focus.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class de Focus Wear OS.
 *
 * @HiltAndroidApp: Dispara la generación de código de Hilt en tiempo de compilación.
 * Crea el ApplicationComponent que provee todas las dependencias @Singleton.
 *
 * Debe declararse en AndroidManifest.xml bajo android:name=".FocusWearApplication"
 */
@HiltAndroidApp
class FocusWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar librerías de terceros si las necesitas
        // Ejemplo: Timber (logging), Firebase, etc.
    }
}
