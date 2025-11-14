package com.ai.smartgallery.presentation.album.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI Album Detail screen
 */
@HiltViewModel
class AIAlbumDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPhotosForLabel(label: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val photosForLabel = mediaRepository.getPhotosForLabel(label)
                _photos.value = photosForLabel
            } catch (e: Exception) {
                _photos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
