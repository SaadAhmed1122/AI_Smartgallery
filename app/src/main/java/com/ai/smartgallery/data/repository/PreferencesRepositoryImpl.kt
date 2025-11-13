package com.ai.smartgallery.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ai.smartgallery.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using DataStore
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val GRID_COLUMN_COUNT_KEY = intPreferencesKey("grid_column_count")
        private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val AI_PROCESSING_ENABLED_KEY = booleanPreferencesKey("ai_processing_enabled")

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    override fun getThemeMode(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[THEME_MODE_KEY] ?: THEME_SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    override fun getGridColumnCount(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[GRID_COLUMN_COUNT_KEY] ?: 3
        }
    }

    override suspend fun setGridColumnCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[GRID_COLUMN_COUNT_KEY] = count
        }
    }

    override fun isAppLockEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[APP_LOCK_ENABLED_KEY] ?: false
        }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED_KEY] = enabled
        }
    }

    override fun isBiometricEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    override fun isAiProcessingEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[AI_PROCESSING_ENABLED_KEY] ?: true
        }
    }

    override suspend fun setAiProcessingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AI_PROCESSING_ENABLED_KEY] = enabled
        }
    }
}
