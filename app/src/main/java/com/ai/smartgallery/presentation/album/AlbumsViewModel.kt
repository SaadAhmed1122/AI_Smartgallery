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

    val aiGeneratedAlbums: StateFlow<List<Triple<String, Int, List<String>>>> = mediaRepository
        .getAIGeneratedAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _peopleCount = MutableStateFlow(0)
    val peopleCount: StateFlow<Int> = _peopleCount.asStateFlow()

    private val _documentsCount = MutableStateFlow(0)
    val documentsCount: StateFlow<Int> = _documentsCount.asStateFlow()

    private val _duplicatesCount = MutableStateFlow(0)
    val duplicatesCount: StateFlow<Int> = _duplicatesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    init {
        // Ensure photos are synced from MediaStore
        syncPhotos()
        // Load AI feature counts
        loadAIFeatureCounts()
    }

    private fun syncPhotos() {
        viewModelScope.launch {
            try {
                mediaRepository.syncPhotosFromMediaStore()
            } catch (e: Exception) {
                // Sync error - user may see empty albums
            }
        }
    }

    private fun loadAIFeatureCounts() {
        viewModelScope.launch {
            try {
                _peopleCount.value = mediaRepository.getPhotosWithFacesCount()
                _documentsCount.value = mediaRepository.getPhotosWithTextCount()
                val duplicateGroups = mediaRepository.getDuplicateGroups()
                _duplicatesCount.value = duplicateGroups.sumOf { it.second.size + 1 }
            } catch (e: Exception) {
                android.util.Log.e("AlbumsViewModel", "Failed to load AI feature counts", e)
            }
        }
    }

    fun refreshAIFeatureCounts() {
        loadAIFeatureCounts()
    }

    fun triggerAIProcessing() {
        android.util.Log.d("AlbumsViewModel", "triggerAIProcessing() called")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("AlbumsViewModel", "Calling mediaRepository.scheduleAIProcessing()")
                mediaRepository.scheduleAIProcessing()
                android.util.Log.d("AlbumsViewModel", "AI Processing scheduled successfully")
                _error.value = "AI Processing started! Refreshing in 60 seconds..."

                // Refresh counts after a delay to allow processing to complete
                // TODO: Use WorkManager observer instead of delay
                kotlinx.coroutines.delay(60000) // Wait 60 seconds
                android.util.Log.d("AlbumsViewModel", "Refreshing AI feature counts...")
                refreshAIFeatureCounts()
                _error.value = "AI Processing complete! Check your albums."
            } catch (e: Exception) {
                android.util.Log.e("AlbumsViewModel", "Failed to start AI processing", e)
                _error.value = "Failed to start AI processing: ${e.message}"
            } finally {
                _isLoading.value = false
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
