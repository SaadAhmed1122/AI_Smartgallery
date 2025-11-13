package com.ai.smartgallery.presentation.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.utils.AspectRatio
import com.ai.smartgallery.utils.ImageProcessor
import com.ai.smartgallery.utils.PhotoFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Photo Editor
 */
@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val imageProcessor: ImageProcessor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val photoId: Long = savedStateHandle.get<Long>("photoId") ?: 0L

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _editedBitmap = MutableStateFlow<Bitmap?>(null)
    val editedBitmap: StateFlow<Bitmap?> = _editedBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Edit state
    private val _selectedFilter = MutableStateFlow(PhotoFilter.NONE)
    val selectedFilter: StateFlow<PhotoFilter> = _selectedFilter.asStateFlow()

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(1f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(1f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(AspectRatio.FREE)
    val selectedAspectRatio: StateFlow<AspectRatio> = _selectedAspectRatio.asStateFlow()

    private val _cropRect = MutableStateFlow<Rect?>(null)
    val cropRect: StateFlow<Rect?> = _cropRect.asStateFlow()

    private val _isCropMode = MutableStateFlow(false)
    val isCropMode: StateFlow<Boolean> = _isCropMode.asStateFlow()

    init {
        loadPhoto()
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val photo = mediaRepository.getPhotoById(photoId)
                if (photo != null) {
                    val bitmap = BitmapFactory.decodeFile(photo.path)
                    _originalBitmap.value = bitmap
                    _editedBitmap.value = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load photo"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Apply filter to image
     */
    fun applyFilter(filter: PhotoFilter) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedFilter.value = filter

                val original = _originalBitmap.value ?: return@launch
                val result = when (filter) {
                    PhotoFilter.NONE -> original.copy(original.config ?: Bitmap.Config.ARGB_8888, true)
                    PhotoFilter.GRAYSCALE -> imageProcessor.applyGrayscale(original)
                    PhotoFilter.SEPIA -> imageProcessor.applySepia(original)
                    PhotoFilter.VINTAGE -> imageProcessor.applyVintage(original)
                }

                _editedBitmap.value = result
            } catch (e: Exception) {
                _error.value = "Failed to apply filter"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Adjust brightness (-255 to 255)
     */
    fun adjustBrightness(value: Float) {
        viewModelScope.launch {
            try {
                _brightness.value = value
                applyAdjustments()
            } catch (e: Exception) {
                _error.value = "Failed to adjust brightness"
            }
        }
    }

    /**
     * Adjust contrast (0.5 to 2.0)
     */
    fun adjustContrast(value: Float) {
        viewModelScope.launch {
            try {
                _contrast.value = value
                applyAdjustments()
            } catch (e: Exception) {
                _error.value = "Failed to adjust contrast"
            }
        }
    }

    /**
     * Adjust saturation (0 to 2)
     */
    fun adjustSaturation(value: Float) {
        viewModelScope.launch {
            try {
                _saturation.value = value
                applyAdjustments()
            } catch (e: Exception) {
                _error.value = "Failed to adjust saturation"
            }
        }
    }

    private suspend fun applyAdjustments() {
        val original = _originalBitmap.value ?: return

        var result = original.copy(original.config ?: Bitmap.Config.ARGB_8888, true)

        if (_brightness.value != 0f) {
            result = imageProcessor.adjustBrightness(result, _brightness.value)
        }

        if (_contrast.value != 1f) {
            result = imageProcessor.adjustContrast(result, _contrast.value)
        }

        if (_saturation.value != 1f) {
            result = imageProcessor.adjustSaturation(result, _saturation.value)
        }

        _editedBitmap.value = result
    }

    /**
     * Rotate image by 90 degrees
     */
    fun rotate90() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val current = _editedBitmap.value ?: return@launch
                val newDegrees = (_rotationDegrees.value + 90f) % 360f
                _rotationDegrees.value = newDegrees

                val rotated = imageProcessor.rotateImage(current, 90f)
                _editedBitmap.value = rotated
            } catch (e: Exception) {
                _error.value = "Failed to rotate image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Flip image horizontally
     */
    fun flipHorizontal() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val current = _editedBitmap.value ?: return@launch
                val flipped = imageProcessor.flipHorizontal(current)
                _editedBitmap.value = flipped
            } catch (e: Exception) {
                _error.value = "Failed to flip image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Flip image vertically
     */
    fun flipVertical() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val current = _editedBitmap.value ?: return@launch
                val flipped = imageProcessor.flipVertical(current)
                _editedBitmap.value = flipped
            } catch (e: Exception) {
                _error.value = "Failed to flip image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set crop aspect ratio
     */
    fun setAspectRatio(aspectRatio: AspectRatio) {
        _selectedAspectRatio.value = aspectRatio
        // Update crop rect based on new aspect ratio
        _editedBitmap.value?.let { bitmap ->
            initializeCropRect(bitmap.width, bitmap.height)
        }
    }

    /**
     * Initialize crop rect to cover full image or with aspect ratio
     */
    private fun initializeCropRect(width: Int, height: Int) {
        val aspectRatio = _selectedAspectRatio.value.ratio

        val rect = if (aspectRatio != null) {
            // Calculate crop rect that fits the aspect ratio
            val imageAspectRatio = width.toFloat() / height.toFloat()

            if (imageAspectRatio > aspectRatio) {
                // Image is wider, constrain width
                val cropWidth = (height * aspectRatio).toInt()
                val left = (width - cropWidth) / 2
                Rect(left, 0, left + cropWidth, height)
            } else {
                // Image is taller, constrain height
                val cropHeight = (width / aspectRatio).toInt()
                val top = (height - cropHeight) / 2
                Rect(0, top, width, top + cropHeight)
            }
        } else {
            // Free aspect ratio - use full image
            Rect(0, 0, width, height)
        }

        _cropRect.value = rect
    }

    /**
     * Set crop rectangle manually
     */
    fun setCropRect(rect: Rect) {
        _cropRect.value = rect
    }

    /**
     * Enter crop mode
     */
    fun enterCropMode() {
        _isCropMode.value = true
        _editedBitmap.value?.let { bitmap ->
            initializeCropRect(bitmap.width, bitmap.height)
        }
    }

    /**
     * Exit crop mode without applying
     */
    fun exitCropMode() {
        _isCropMode.value = false
        _cropRect.value = null
    }

    /**
     * Apply crop to image
     */
    fun applyCrop() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val current = _editedBitmap.value ?: return@launch
                val rect = _cropRect.value ?: return@launch

                val cropped = imageProcessor.cropImage(current, rect)
                _editedBitmap.value = cropped
                _isCropMode.value = false
                _cropRect.value = null
            } catch (e: Exception) {
                _error.value = "Failed to crop image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset all edits
     */
    fun reset() {
        viewModelScope.launch {
            val original = _originalBitmap.value ?: return@launch
            _editedBitmap.value = original.copy(original.config ?: Bitmap.Config.ARGB_8888, true)
            _selectedFilter.value = PhotoFilter.NONE
            _brightness.value = 0f
            _contrast.value = 1f
            _saturation.value = 1f
            _rotationDegrees.value = 0f
        }
    }

    /**
     * Save edited photo
     */
    fun savePhoto() {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                val bitmap = _editedBitmap.value ?: return@launch

                val savedFile = imageProcessor.saveBitmap(bitmap)
                if (savedFile != null) {
                    // Trigger media scan to add to gallery
                    mediaRepository.syncPhotosFromMediaStore()
                } else {
                    _error.value = "Failed to save photo"
                }
            } catch (e: Exception) {
                _error.value = "Failed to save photo: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        _originalBitmap.value?.recycle()
        _editedBitmap.value?.recycle()
    }
}
