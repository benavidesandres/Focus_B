// wear/src/main/java/com/focus/wear/di/AppModule.kt
// ───────────────────────────────────────────────────────────────
// Módulos de Hilt — aquí le decimos a Hilt cómo crear cada dependencia.
// Hilt se encarga de inyectarlas donde las necesitemos (@Inject).
// ───────────────────────────────────────────────────────────────
package com.focus.wear.di

import android.content.Context
import com.focus.shared.database.FocusDatabase
import com.focus.shared.database.FocusSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de base de datos.
 *
 * @Module: Marca esta clase como módulo de Hilt.
 * @InstallIn(SingletonComponent): Las dependencias aquí viven toda la vida de la app.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provee la instancia de la base de datos Room.
     *
     * @Provides: Hilt llama a esta función cuando alguien necesita un FocusDatabase.
     * @Singleton: Solo se crea una instancia en toda la app.
     */
    @Provides
    @Singleton
    fun provideFocusDatabase(
        @ApplicationContext context: Context
    ): FocusDatabase = FocusDatabase.create(context)

    /**
     * Provee el DAO de sesiones a partir de la base de datos.
     * Hilt inyecta automáticamente el FocusDatabase provisto arriba.
     */
    @Provides
    @Singleton
    fun provideFocusSessionDao(database: FocusDatabase): FocusSessionDao =
        database.focusSessionDao()
}
