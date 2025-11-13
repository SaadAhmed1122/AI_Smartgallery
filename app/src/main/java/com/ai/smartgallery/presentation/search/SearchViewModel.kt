package com.ai.smartgallery.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.smartgallery.data.local.dao.ImageLabelDao
import com.ai.smartgallery.data.model.toDomain
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.model.Tag
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Search filter types
 */
enum class SearchFilter {
    ALL,        // Search all (filename, labels, tags)
    LABELS,     // Search AI-detected labels only
    TAGS,       // Search user tags only
    DATE        // Search by date range
}

/**
 * ViewModel for Search screen with AI-powered capabilities
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val tagRepository: TagRepository,
    private val imageLabelDao: ImageLabelDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Photo>>(emptyList())
    val searchResults: StateFlow<List<Photo>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTags: StateFlow<Set<Long>> = _selectedTags.asStateFlow()

    private val _dateRangeStart = MutableStateFlow<Long?>(null)
    val dateRangeStart: StateFlow<Long?> = _dateRangeStart.asStateFlow()

    private val _dateRangeEnd = MutableStateFlow<Long?>(null)
    val dateRangeEnd: StateFlow<Long?> = _dateRangeEnd.asStateFlow()

    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Debounce search queries
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch()
                }
        }

        // Re-search when filter changes
        viewModelScope.launch {
            _searchFilter.collectLatest {
                if (_searchQuery.value.isNotBlank() || it != SearchFilter.ALL) {
                    performSearch()
                }
            }
        }

        // Re-search when tags change
        viewModelScope.launch {
            _selectedTags.collectLatest {
                if (it.isNotEmpty()) {
                    performSearch()
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
        if (filter != SearchFilter.TAGS) {
            _selectedTags.value = emptySet()
        }
        if (filter != SearchFilter.DATE) {
            _dateRangeStart.value = null
            _dateRangeEnd.value = null
        }
    }

    fun toggleTag(tagId: Long) {
        val currentTags = _selectedTags.value.toMutableSet()
        if (currentTags.contains(tagId)) {
            currentTags.remove(tagId)
        } else {
            currentTags.add(tagId)
        }
        _selectedTags.value = currentTags
        if (currentTags.isNotEmpty()) {
            _searchFilter.value = SearchFilter.TAGS
        }
    }

    fun setDateRange(start: Long?, end: Long?) {
        _dateRangeStart.value = start
        _dateRangeEnd.value = end
        if (start != null || end != null) {
            _searchFilter.value = SearchFilter.DATE
        }
    }

    fun showFilterDialog() {
        _showFilterDialog.value = true
    }

    fun hideFilterDialog() {
        _showFilterDialog.value = false
    }

    private suspend fun performSearch() {
        try {
            _isSearching.value = true
            _error.value = null

            val query = _searchQuery.value
            val filter = _searchFilter.value
            val results = when (filter) {
                SearchFilter.ALL -> {
                    if (query.isBlank()) {
                        emptyList()
                    } else {
                        searchAll(query)
                    }
                }
                SearchFilter.LABELS -> {
                    if (query.isBlank()) {
                        emptyList()
                    } else {
                        searchByLabels(query)
                    }
                }
                SearchFilter.TAGS -> {
                    if (_selectedTags.value.isNotEmpty()) {
                        searchByTags(_selectedTags.value.toList())
                    } else if (query.isNotBlank()) {
                        searchTagsByName(query)
                    } else {
                        emptyList()
                    }
                }
                SearchFilter.DATE -> {
                    searchByDateRange(
                        _dateRangeStart.value,
                        _dateRangeEnd.value
                    )
                }
            }

            _searchResults.value = results
        } catch (e: Exception) {
            _error.value = e.message ?: "Search failed"
        } finally {
            _isSearching.value = false
        }
    }

    private suspend fun searchAll(query: String): List<Photo> {
        // Search by filename using existing repository method
        val filenameResults = mediaRepository.searchPhotos(query)

        // Search by labels
        val labelResults = searchByLabels(query)

        // Search by tags
        val tagResults = searchTagsByName(query)

        // Combine and deduplicate by photo ID
        return (filenameResults + labelResults + tagResults)
            .distinctBy { it.id }
            .sortedByDescending { it.dateTaken }
    }

    private suspend fun searchByLabels(query: String): List<Photo> = withContext(viewModelScope.coroutineContext) {
        try {
            // Search labels that match the query
            val labels = imageLabelDao.searchLabels("%$query%")

            // Get all photo IDs that have matching labels
            val photoIds = labels.map { entity -> entity.photoId }.distinct()

            // Fetch photos by IDs
            val photos = photoIds.mapNotNull { photoId ->
                mediaRepository.getPhotoById(photoId)
            }
            photos
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchTagsByName(query: String): List<Photo> = withContext(viewModelScope.coroutineContext) {
        try {
            // Find tags that match the query
            val tag = tagRepository.getTagByName(query)
            if (tag != null) {
                tagRepository.getPhotosWithTag(tag.id).firstOrNull() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchByTags(tagIds: List<Long>): List<Photo> = withContext(viewModelScope.coroutineContext) {
        try {
            // Get photos for each tag and find intersection (photos with all selected tags)
            if (tagIds.isEmpty()) {
                return@withContext emptyList()
            }

            var results: Set<Photo>? = null
            tagIds.forEach { tagId ->
                val photosWithTag = tagRepository.getPhotosWithTag(tagId).firstOrNull() ?: emptyList()
                results = if (results == null) {
                    photosWithTag.toSet()
                } else {
                    results!!.intersect(photosWithTag.toSet())
                }
            }

            results?.toList()?.sortedByDescending { it.dateTaken } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchByDateRange(start: Long?, end: Long?): List<Photo> = withContext(viewModelScope.coroutineContext) {
        try {
            // Get all photos and filter by date range
            val allPhotos = mediaRepository.getAllPhotos().firstOrNull() ?: emptyList()
            allPhotos.filter { photo ->
                val dateTaken = photo.dateTaken
                val afterStart = start == null || dateTaken >= start
                val beforeEnd = end == null || dateTaken <= end
                afterStart && beforeEnd
            }.sortedByDescending { it.dateTaken }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchFilter.value = SearchFilter.ALL
        _selectedTags.value = emptySet()
        _dateRangeStart.value = null
        _dateRangeEnd.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
