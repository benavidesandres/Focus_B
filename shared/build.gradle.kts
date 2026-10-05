// shared/build.gradle.kts
// Módulo compartido entre :app (teléfono) y :wear (reloj)
// Contiene: modelos de datos, constantes y contratos de la Data Layer

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp) // Para Room
}

android {
    namespace = "com.focus.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26 // Wear OS 2.0+ | Android 8.0+ para el teléfono

        // Kotlin options
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Corrutinas para StateFlow y operaciones asíncronas
    implementation(libs.coroutines.android)

    // Room: base de datos local para historial de sesiones
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore para preferencias persistentes del usuario
    implementation(libs.datastore.preferences)

    // Servicios de Wear OS (Data Layer API para comunicación reloj ↔ teléfono)
    implementation(libs.play.services.wearable)

    // Javax Inject para Hilt / Dependency Injection
    implementation("javax.inject:javax.inject:1")
}
