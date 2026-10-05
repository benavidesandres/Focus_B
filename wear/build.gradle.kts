// wear/build.gradle.kts — Módulo principal de la app para Wear OS

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.focus.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.focus.wear"
        minSdk = 30          // Wear OS 3.0 (API 30) — soporta Compose nativo
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true       // Habilitar Jetpack Compose
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Habilitar opciones experimentales de Compose for Wear OS
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material.ExperimentalMaterialApi")
    }
}

dependencies {
    // Módulo compartido
    implementation(project(":shared"))

    // Core de Android
    implementation(libs.core.ktx)
    implementation(libs.splash.screen)

    // ─── Wear OS Compose ────────────────────────────────────────
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.wear.compose.material)       // Material 3 adaptado para relojes
    implementation(libs.wear.compose.foundation)     // Layouts y scrolling para relojes
    implementation(libs.wear.compose.navigation)     // Navegación con SwipeDismiss integrado
    implementation(libs.wear.input)                  // Input físico del reloj (botón lateral)

    // ─── Comunicación Wear ↔ Teléfono ───────────────────────────
    implementation(libs.play.services.wearable)

    // ─── Lifecycle y ViewModel ──────────────────────────────────
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.viewmodel.compose)
    implementation(libs.viewmodel.ktx)

    // ─── Corrutinas ─────────────────────────────────────────────
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // ─── Hilt (Inyección de dependencias) ───────────────────────
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ─── Room ───────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // ─── WorkManager + Hilt (tareas en background) ──────────────
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)
}
