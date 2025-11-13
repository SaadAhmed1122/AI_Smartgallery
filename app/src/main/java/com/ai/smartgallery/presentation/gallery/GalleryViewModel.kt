package com.ai.smartgallery.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Gallery screen
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // Paged photos flow
    val photosFlow: Flow<PagingData<Photo>> = mediaRepository
        .getAllPhotosPaged()
        .cachedIn(viewModelScope)

    // Grid column count from preferences
    val gridColumnCount: StateFlow<Int> = preferencesRepository
        .getGridColumnCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3
        )

    // Selected photos for batch operations
    private val _selectedPhotos = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotos: StateFlow<Set<Long>> = _selectedPhotos.asStateFlow()

    // Selection mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        syncPhotos()
    }

    /**
     * Sync photos from MediaStore
     */
    fun syncPhotos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                mediaRepository.syncPhotosFromMediaStore()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sync photos"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle photo selection
     */
    fun togglePhotoSelection(photoId: Long) {
        val currentSelection = _selectedPhotos.value.toMutableSet()
        if (currentSelection.contains(photoId)) {
            currentSelection.remove(photoId)
        } else {
            currentSelection.add(photoId)
        }
        _selectedPhotos.value = currentSelection

        // Exit selection mode if no photos selected
        if (currentSelection.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    /**
     * Enter selection mode
     */
    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    /**
     * Exit selection mode
     */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPhotos.value = emptySet()
    }

    /**
     * Select all photos
     */
    fun selectAll(photoIds: List<Long>) {
        _selectedPhotos.value = photoIds.toSet()
        _isSelectionMode.value = true
    }

    /**
     * Delete selected photos
     */
    fun deleteSelectedPhotos() {
        viewModelScope.launch {
            try {
                mediaRepository.deletePhotos(_selectedPhotos.value.toList())
                exitSelectionMode()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete photos"
            }
        }
    }

    /**
     * Toggle favorite for a photo
     */
    fun toggleFavorite(photoId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                mediaRepository.toggleFavorite(photoId, !isFavorite)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update favorite"
            }
        }
    }

    /**
     * Update grid column count
     */
    fun updateGridColumnCount(count: Int) {
        viewModelScope.launch {
            preferencesRepository.setGridColumnCount(count)
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}
