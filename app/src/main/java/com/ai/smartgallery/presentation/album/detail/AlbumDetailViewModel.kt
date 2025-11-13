package com.ai.smartgallery.presentation.album.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.domain.model.Album
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Album Detail screen
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: 0L

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    val photos: StateFlow<List<Photo>> = albumRepository
        .getPhotosInAlbum(albumId)
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

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedPhotos.map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        loadAlbum()
    }

    private fun loadAlbum() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val albumData = albumRepository.getAlbumById(albumId)
                _album.value = albumData
            } catch (e: Exception) {
                _error.value = "Failed to load album: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

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
        _selectedPhotos.value = photos.value.map { it.id }.toSet()
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedPhotos.value = emptySet()
    }

    /**
     * Remove selected photos from album
     */
    fun removeSelectedFromAlbum() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val photoIds = _selectedPhotos.value.toList()
                photoIds.forEach { photoId ->
                    albumRepository.removePhotoFromAlbum(photoId, albumId)
                }
                clearSelection()
            } catch (e: Exception) {
                _error.value = "Failed to remove photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Show delete album dialog
     */
    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    /**
     * Hide delete album dialog
     */
    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
    }

    /**
     * Show rename album dialog
     */
    fun showRenameDialog() {
        _showRenameDialog.value = true
    }

    /**
     * Hide rename album dialog
     */
    fun hideRenameDialog() {
        _showRenameDialog.value = false
    }

    /**
     * Delete album
     */
    fun deleteAlbum(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                albumRepository.deleteAlbum(albumId)
                _showDeleteDialog.value = false
                onDeleted()
            } catch (e: Exception) {
                _error.value = "Failed to delete album: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Rename album
     */
    fun renameAlbum(newName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentAlbum = _album.value
                if (currentAlbum != null && newName.isNotBlank()) {
                    val updatedAlbum = currentAlbum.copy(name = newName)
                    albumRepository.updateAlbum(updatedAlbum)
                    _album.value = updatedAlbum
                    _showRenameDialog.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to rename album: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
