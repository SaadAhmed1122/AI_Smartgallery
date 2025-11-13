package com.ai.smartgallery.presentation.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.domain.model.Album
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.AlbumRepository
import com.ai.smartgallery.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Albums screen
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = albumRepository
        .getAllAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoritePhotos: StateFlow<List<Photo>> = mediaRepository
        .getFavoritePhotos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPhotos: StateFlow<List<Photo>> = mediaRepository
        .getAllPhotos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val videos: StateFlow<List<Photo>> = mediaRepository
        .getVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    init {
        loadSmartAlbums()
    }

    private fun loadSmartAlbums() {
        viewModelScope.launch {
            try {
                albumRepository.createSmartAlbums()
            } catch (e: Exception) {
                // Smart albums may already exist
            }
        }
    }

    fun createAlbum(name: String) {
        if (name.isBlank()) {
            _error.value = "Album name cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                albumRepository.createAlbum(name)
                _showCreateDialog.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create album"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch {
            try {
                albumRepository.deleteAlbum(albumId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete album"
            }
        }
    }

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
