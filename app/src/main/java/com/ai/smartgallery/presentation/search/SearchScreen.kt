package com.ai.smartgallery.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.*

/**
 * Search screen for finding photos with AI-powered filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPhotoClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val showFilterDialog by viewModel.showFilterDialog.collectAsState()
    val dateRangeStart by viewModel.dateRangeStart.collectAsState()
    val dateRangeEnd by viewModel.dateRangeEnd.collectAsState()

    Scaffold(
        topBar = {
            SearchTopBar(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onBack = onBack,
                onClear = { viewModel.clearSearch() },
                onFilterClick = { viewModel.showFilterDialog() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips row
            FilterChipsRow(
                currentFilter = searchFilter,
                onFilterChange = { viewModel.setSearchFilter(it) },
                selectedTagsCount = selectedTags.size,
                hasDateRange = dateRangeStart != null || dateRangeEnd != null
            )

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isSearching -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    searchQuery.isBlank() && selectedTags.isEmpty() && dateRangeStart == null && dateRangeEnd == null -> {
                        SearchEmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    searchResults.isEmpty() -> {
                        NoResultsState(
                            query = searchQuery,
                            filter = searchFilter,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        SearchResults(
                            results = searchResults,
                            onPhotoClick = onPhotoClick,
                            resultCount = searchResults.size
                        )
                    }
                }

                // Error snackbar
                error?.let { errorMessage ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(errorMessage)
                    }
                }
            }
        }
    }

    // Filter dialogs
    if (showFilterDialog) {
        when (searchFilter) {
            SearchFilter.TAGS -> {
                TagSelectionDialog(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onToggleTag = { viewModel.toggleTag(it) },
                    onDismiss = { viewModel.hideFilterDialog() }
                )
            }
            SearchFilter.DATE -> {
                DateRangePickerDialog(
                    startDate = dateRangeStart,
                    endDate = dateRangeEnd,
                    onDateRangeSelected = { start, end ->
                        viewModel.setDateRange(start, end)
                        viewModel.hideFilterDialog()
                    },
                    onDismiss = { viewModel.hideFilterDialog() }
                )
            }
            else -> {
                // No dialog for ALL and LABELS filters
                viewModel.hideFilterDialog()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onFilterClick: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search photos...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.transparent,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.transparent
                ),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
private fun FilterChipsRow(
    currentFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    selectedTagsCount: Int,
    hasDateRange: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == SearchFilter.ALL,
            onClick = { onFilterChange(SearchFilter.ALL) },
            label = { Text("All") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = currentFilter == SearchFilter.LABELS,
            onClick = { onFilterChange(SearchFilter.LABELS) },
            label = { Text("AI Labels") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = currentFilter == SearchFilter.TAGS,
            onClick = { onFilterChange(SearchFilter.TAGS) },
            label = {
                Text(if (selectedTagsCount > 0) "Tags ($selectedTagsCount)" else "Tags")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = currentFilter == SearchFilter.DATE,
            onClick = { onFilterChange(SearchFilter.DATE) },
            label = { Text(if (hasDateRange) "Date ✓" else "Date") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun SearchResults(
    results: List<Photo>,
    onPhotoClick: (Long) -> Unit,
    resultCount: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Result count header
        Text(
            text = "$resultCount ${if (resultCount == 1) "photo" else "photos"} found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(results) { photo ->
                AsyncImage(
                    model = photo.path,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onPhotoClick(photo.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI-Powered Search",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search by filename, AI labels, tags, or date",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try: \"cat\", \"beach\", or select filters above",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun NoResultsState(
    query: String,
    filter: SearchFilter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val filterText = when (filter) {
            SearchFilter.LABELS -> "in AI labels"
            SearchFilter.TAGS -> "in tags"
            SearchFilter.DATE -> "in date range"
            else -> ""
        }
        if (query.isNotBlank()) {
            Text(
                text = "No photos match \"$query\" $filterText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        } else {
            Text(
                text = "Try adjusting your filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSelectionDialog(
    allTags: List<Tag>,
    selectedTags: Set<Long>,
    onToggleTag: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tags") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (allTags.isEmpty()) {
                    Text(
                        text = "No tags available. Create tags in photo details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTags.contains(tag.id),
                                onClick = { onToggleTag(tag.id) },
                                label = { Text(tag.name) },
                                leadingIcon = if (selectedTags.contains(tag.id)) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    startDate: Long?,
    endDate: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
