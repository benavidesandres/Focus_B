// settings.gradle.kts — Configuración raíz del proyecto multi-módulo
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FocusSmartwatch"

// Módulos del proyecto:
// :wear           → App principal para el reloj Wear OS
// :shared         → Código compartido (modelos, base de datos)
include(":wear")
include(":shared")
