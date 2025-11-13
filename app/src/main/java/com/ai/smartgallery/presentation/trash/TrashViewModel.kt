package com.ai.smartgallery.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Trash screen
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val deletedPhotos: StateFlow<List<Photo>> = mediaRepository
        .getDeletedPhotos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedPhotos = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotos: StateFlow<Set<Long>> = _selectedPhotos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showEmptyTrashDialog = MutableStateFlow(false)
    val showEmptyTrashDialog: StateFlow<Boolean> = _showEmptyTrashDialog.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedPhotos.map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Toggle photo selection
     */
    fun togglePhotoSelection(photoId: Long) {
        val current = _selectedPhotos.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedPhotos.value = current
    }

    /**
     * Select all photos
     */
    fun selectAll() {
        _selectedPhotos.value = deletedPhotos.value.map { it.id }.toSet()
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedPhotos.value = emptySet()
    }

    /**
     * Restore selected photos
     */
    fun restoreSelected() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val photoIds = _selectedPhotos.value.toList()
                mediaRepository.restoreFromTrashBatch(photoIds)
                clearSelection()
            } catch (e: Exception) {
                _error.value = "Failed to restore photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restore single photo
     */
    fun restorePhoto(photoId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                mediaRepository.restoreFromTrash(photoId)
            } catch (e: Exception) {
                _error.value = "Failed to restore photo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Permanently delete selected photos
     */
    fun deleteSelectedPermanently() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val photoIds = _selectedPhotos.value.toList()
                // Since photos are in trash, we need to delete them from database
                mediaRepository.deletePhotos(photoIds)
                clearSelection()
            } catch (e: Exception) {
                _error.value = "Failed to delete photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Show empty trash confirmation dialog
     */
    fun showEmptyTrashDialog() {
        _showEmptyTrashDialog.value = true
    }

    /**
     * Hide empty trash dialog
     */
    fun hideEmptyTrashDialog() {
        _showEmptyTrashDialog.value = false
    }

    /**
     * Empty entire trash
     */
    fun emptyTrash() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                mediaRepository.emptyTrash()
                _showEmptyTrashDialog.value = false
            } catch (e: Exception) {
                _error.value = "Failed to empty trash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Auto-delete old trash (30+ days)
     */
    fun cleanupOldTrash() {
        viewModelScope.launch {
            try {
                mediaRepository.permanentlyDeleteOldTrash(30)
            } catch (e: Exception) {
                // Silent fail - this is background cleanup
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    init {
        // Automatically cleanup old trash on init
        cleanupOldTrash()
    }
}
