package com.ai.smartgallery.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app preferences
 */
interface PreferencesRepository {

    /**
     * Get theme mode (light/dark/system)
     */
    fun getThemeMode(): Flow<String>

    /**
     * Set theme mode
     */
    suspend fun setThemeMode(mode: String)

    /**
     * Get grid column count
     */
    fun getGridColumnCount(): Flow<Int>

    /**
     * Set grid column count
     */
    suspend fun setGridColumnCount(count: Int)

    /**
     * Check if app lock is enabled
     */
    fun isAppLockEnabled(): Flow<Boolean>

    /**
     * Enable/disable app lock
     */
    suspend fun setAppLockEnabled(enabled: Boolean)

    /**
     * Check if biometric is enabled
     */
    fun isBiometricEnabled(): Flow<Boolean>

    /**
     * Enable/disable biometric
     */
    suspend fun setBiometricEnabled(enabled: Boolean)

    /**
     * Check if AI processing is enabled
     */
    fun isAiProcessingEnabled(): Flow<Boolean>

    /**
     * Enable/disable AI processing
     */
    suspend fun setAiProcessingEnabled(enabled: Boolean)
}
