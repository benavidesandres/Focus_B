# Reglas de ProGuard / R8 para la aplicación Wear OS en release

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Hilt
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.internal.GeneratedComponent

# Wear OS
-keep public class * extends android.app.Service
