# Focus Smartwatch (Wear OS) ⌚

Aplicación nativa e independiente (*standalone*) de temporizador de enfoque y productividad diseñada específicamente para relojes inteligentes con **Wear OS**.

## 🌟 Características Principales

- **Diseño OLED Minimalista**: Interfaz oscura de contraste óptimo (#000000) diseñada para reducir el consumo energético en pantallas AMOLED y eliminar distracciones.
- **Temporizador Circular Preciso**: Visualización fluida del tiempo restante con arco de progreso circular reactivo.
- **Retroalimentación Háptica Silenciosa**: Notificaciones basadas exclusivamente en patrones de vibración intuitivos (inicio, hitos al 50% y 75%, cuenta regresiva final y finalización) sin emitir ruidos molestos.
- **Detector de Inactividad**: Monitoreo inteligente mediante acelerómetro para sugerir pausas y estiramientos ante períodos prolongados de sedentarismo.
- **Servicio en Primer Plano (Foreground Service)**: Garantiza la continuidad del temporizador con la pantalla apagada cumpliendo las directrices de salud y batería de Android 14 y 15.
- **Almacenamiento Local Seguro**: Historial de sesiones y estadísticas diarias persistidas con Room Database.

## 🏗️ Arquitectura y Tecnologías

El proyecto sigue los principios de **Clean Architecture** y arquitectura reactiva **Unidirectional Data Flow (UDF)**:

- **Jetpack Compose for Wear OS**: Interfaz declarativa optimizada para pantallas redondas con componentes `ScalingLazyColumn`, `InlineSlider` y `Chip`.
- **Kotlin 2.0 & Corrutinas / Flow**: Gestión asíncrona eficiente con `StateFlow` reactivo.
- **Dagger Hilt**: Inyección de dependencias modular (`@Singleton`, `@AndroidEntryPoint`, `@HiltViewModel`).
- **Room Database**: Capa de persistencia local con soporte para migraciones y operaciones no bloqueantes.
- **AndroidX Lifecycle & ViewModel**: Separación clara de responsabilidades entre la capa de presentación y dominio.

## 📁 Estructura del Proyecto

```text
FocusSmartwatch/
├── gradle/
│   └── libs.versions.toml     # Version Catalog centralizado
├── shared/                     # Módulo compartido (:shared)
│   └── src/main/java/com/focus/shared/
│       ├── database/          # Entidades Room y DAO
│       ├── model/             # Modelos de dominio y enums
│       └── repository/        # Repositorio de sesiones de enfoque
├── wear/                       # Módulo principal Wear OS (:wear)
│   └── src/main/java/com/focus/wear/
│       ├── di/                # Módulos de inyección de dependencias (Hilt)
│       ├── domain/
│       │   ├── haptics/       # Controlador de vibración y patrones
│       │   ├── sensor/        # Detector de inactividad física
│       │   └── timer/         # Motor de temporización reactivo
│       ├── service/           # FocusForegroundService con gestión de WakeLock
│       └── ui/                # Pantallas Compose, Tema y ViewModel
└── README.md
```

## 📋 Requisitos de Entorno

- **Android Studio**: Jellyfish | Koala | Ladybug o superior
- **JDK**: Java 17
- **Min SDK**: API 30 (Wear OS 3.0+)
- **Target SDK / Compile SDK**: API 35 (Android 15)

## 🚀 Compilación y Ejecución

1. Clona el repositorio:
   ```bash
   git clone https://github.com/benavidesandres/Focus_B.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza Gradle con los archivos del proyecto.
4. Selecciona un emulador de **Wear OS (Round)** o un smartwatch físico con depuración activada.
5. Ejecuta la variante `:wear` en modo Debug.
