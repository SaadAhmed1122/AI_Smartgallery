package com.ai.smartgallery.presentation.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ai.smartgallery.domain.model.Album
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.AlbumRepository
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.workers.AIProcessingWorker
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
    private val mediaRepository: MediaRepository,
    private val workManager: WorkManager
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

    private val _processingProgress = MutableStateFlow<String?>(null)
    val processingProgress: StateFlow<String?> = _processingProgress.asStateFlow()

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
                _processingProgress.value = "Starting AI processing..."

                android.util.Log.d("AlbumsViewModel", "Calling mediaRepository.scheduleAIProcessing()")
                mediaRepository.scheduleAIProcessing()
                android.util.Log.d("AlbumsViewModel", "AI Processing scheduled successfully")

                // Observe WorkManager progress
                observeWorkProgress()
            } catch (e: Exception) {
                android.util.Log.e("AlbumsViewModel", "Failed to start AI processing", e)
                _error.value = "Failed to start AI processing: ${e.message}"
                _isLoading.value = false
                _processingProgress.value = null
            }
        }
    }

    private fun observeWorkProgress() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData(AIProcessingWorker.WORK_NAME)
                .asFlow()
                .collect { workInfoList ->
                    val workInfo = workInfoList.firstOrNull()

                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            // Extract progress data
                            val progress = workInfo.progress.getInt("progress", 0)
                            val total = workInfo.progress.getInt("total", 0)
                            val batch = workInfo.progress.getInt("batch", 0)

                            if (total > 0) {
                                val percentage = (progress * 100) / total
                                _processingProgress.value = "Processing photos: $progress/$total ($percentage%)"
                                android.util.Log.d("AlbumsViewModel", "Progress: $progress/$total")
                            } else {
                                _processingProgress.value = "AI Processing in progress..."
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            android.util.Log.d("AlbumsViewModel", "AI Processing completed successfully")
                            _processingProgress.value = "Processing complete! Refreshing..."

                            // Refresh counts
                            refreshAIFeatureCounts()

                            // Show success message briefly
                            kotlinx.coroutines.delay(2000)
                            _processingProgress.value = null
                            _isLoading.value = false
                            _error.value = "AI Processing complete! Check your albums."
                        }
                        WorkInfo.State.FAILED -> {
                            android.util.Log.e("AlbumsViewModel", "AI Processing failed")
                            _processingProgress.value = null
                            _isLoading.value = false
                            _error.value = "AI Processing failed. Please try again."
                        }
                        WorkInfo.State.CANCELLED -> {
                            android.util.Log.w("AlbumsViewModel", "AI Processing cancelled")
                            _processingProgress.value = null
                            _isLoading.value = false
                            _error.value = "AI Processing cancelled."
                        }
                        else -> {
                            // ENQUEUED or BLOCKED state
                            _processingProgress.value = "Waiting to start processing..."
                        }
                    }
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
