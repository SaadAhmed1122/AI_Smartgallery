package com.ai.smartgallery.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val mediaRepository: MediaRepository,
    private val imageLoader: ImageLoader,
    private val application: Application
) : ViewModel() {

    val themeMode: StateFlow<String> = preferencesRepository
        .getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )

    val gridColumnCount: StateFlow<Int> = preferencesRepository
        .getGridColumnCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3
        )

    val isAppLockEnabled: StateFlow<Boolean> = preferencesRepository
        .isAppLockEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isBiometricEnabled: StateFlow<Boolean> = preferencesRepository
        .isBiometricEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isAiProcessingEnabled: StateFlow<Boolean> = preferencesRepository
        .isAiProcessingEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setGridColumnCount(count: Int) {
        viewModelScope.launch {
            preferencesRepository.setGridColumnCount(count)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAppLockEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
        }
    }

    fun setAiProcessingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAiProcessingEnabled(enabled)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch {
            try {
                // Clear Coil image cache
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()

                // Clear app cache directory
                application.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try {
                mediaRepository.emptyTrash()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
