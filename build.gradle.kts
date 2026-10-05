// build.gradle.kts (raíz) — Plugins declarados para todos los módulos
plugins {
    // Plugin de Android para aplicaciones (se aplica en :app y :wear)
    alias(libs.plugins.android.application) apply false

    // Plugin de Android para librerías (se aplica en :shared)
    alias(libs.plugins.android.library) apply false

    // Plugin de Kotlin para Android
    alias(libs.plugins.kotlin.android) apply false

    // Plugin de Compose Compiler para Kotlin 2.0+
    alias(libs.plugins.kotlin.compose) apply false

    // Kapt: procesa anotaciones de Kotlin (usado por Hilt)
    alias(libs.plugins.kotlin.kapt) apply false

    // Hilt: inyección de dependencias de Google
    alias(libs.plugins.hilt.android) apply false

    // KSP: alternativa moderna a Kapt (usada por Room)
    alias(libs.plugins.ksp) apply false
}
