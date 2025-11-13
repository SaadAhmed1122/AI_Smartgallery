package com.ai.smartgallery.presentation.photo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.model.Tag
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for PhotoDetail screen
 */
@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val photoId: Long = savedStateHandle.get<Long>("photoId") ?: 0L

    private val _photo = MutableStateFlow<Photo?>(null)
    val photo: StateFlow<Photo?> = _photo.asStateFlow()

    private val _photoTags = MutableStateFlow<List<Tag>>(emptyList())
    val photoTags: StateFlow<List<Tag>> = _photoTags.asStateFlow()

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showInfo = MutableStateFlow(false)
    val showInfo: StateFlow<Boolean> = _showInfo.asStateFlow()

    private val _showActions = MutableStateFlow(true)
    val showActions: StateFlow<Boolean> = _showActions.asStateFlow()

    private val _showTagDialog = MutableStateFlow(false)
    val showTagDialog: StateFlow<Boolean> = _showTagDialog.asStateFlow()

    init {
        loadPhoto()
        loadPhotoTags()
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val loadedPhoto = mediaRepository.getPhotoById(photoId)
                _photo.value = loadedPhoto
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load photo"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _photo.value?.let { currentPhoto ->
                try {
                    mediaRepository.toggleFavorite(currentPhoto.id, currentPhoto.isFavorite)
                    _photo.value = currentPhoto.copy(isFavorite = !currentPhoto.isFavorite)
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to update favorite"
                }
            }
        }
    }

    fun updateRating(rating: Int) {
        viewModelScope.launch {
            _photo.value?.let { currentPhoto ->
                try {
                    mediaRepository.updateRating(currentPhoto.id, rating)
                    _photo.value = currentPhoto.copy(rating = rating)
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to update rating"
                }
            }
        }
    }

    fun deletePhoto() {
        viewModelScope.launch {
            _photo.value?.let { currentPhoto ->
                try {
                    mediaRepository.deletePhoto(currentPhoto.id)
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to delete photo"
                }
            }
        }
    }

    fun toggleInfo() {
        _showInfo.value = !_showInfo.value
    }

    fun toggleActions() {
        _showActions.value = !_showActions.value
    }

    fun clearError() {
        _error.value = null
    }

    // Tag management
    private fun loadPhotoTags() {
        viewModelScope.launch {
            try {
                val tags = tagRepository.getTagsForPhoto(photoId)
                _photoTags.value = tags
            } catch (e: Exception) {
                // Silent fail for tags
            }
        }
    }

    fun showTagDialog() {
        _showTagDialog.value = true
    }

    fun hideTagDialog() {
        _showTagDialog.value = false
    }

    fun addTagToPhoto(tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.addTagToPhoto(photoId, tagId)
                loadPhotoTags() // Reload tags
            } catch (e: Exception) {
                _error.value = "Failed to add tag"
            }
        }
    }

    fun removeTagFromPhoto(tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.removeTagFromPhoto(photoId, tagId)
                loadPhotoTags() // Reload tags
            } catch (e: Exception) {
                _error.value = "Failed to remove tag"
            }
        }
    }

    fun createAndAddTag(name: String) {
        viewModelScope.launch {
            try {
                // Check if tag already exists
                val existingTag = tagRepository.getTagByName(name)
                val tagId = existingTag?.id ?: tagRepository.createTag(name)
                tagRepository.addTagToPhoto(photoId, tagId)
                loadPhotoTags() // Reload tags
            } catch (e: Exception) {
                _error.value = "Failed to create tag"
            }
        }
    }
}
