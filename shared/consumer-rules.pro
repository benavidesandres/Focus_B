# Reglas específicas de ProGuard / R8 para el módulo shared
# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
