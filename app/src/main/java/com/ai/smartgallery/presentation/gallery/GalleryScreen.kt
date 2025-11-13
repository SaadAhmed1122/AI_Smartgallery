package com.ai.smartgallery.presentation.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.ai.smartgallery.domain.model.Photo

/**
 * Gallery screen - Main screen showing all photos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onPhotoClick: (Long) -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val photos = viewModel.photosFlow.collectAsLazyPagingItems()
    val gridColumnCount by viewModel.gridColumnCount.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (isSelectionMode) "${selectedPhotos.size} selected" else "Gallery") },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedPhotos() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showMenu = false
                                        // TODO: Navigate to settings
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Trash") },
                                    onClick = {
                                        showMenu = false
                                        // TODO: Navigate to trash
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sync") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.syncPhotos()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Sync, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { /* Camera action */ }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && photos.itemCount == 0) {
                // Initial loading
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (photos.itemCount == 0) {
                // Empty state
                EmptyGalleryView(
                    onRefresh = { viewModel.syncPhotos() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Photo grid
                PhotoGrid(
                    photos = photos,
                    gridColumnCount = gridColumnCount,
                    isSelectionMode = isSelectionMode,
                    selectedPhotos = selectedPhotos,
                    onPhotoClick = { photo ->
                        if (isSelectionMode) {
                            viewModel.togglePhotoSelection(photo.id)
                        } else {
                            onPhotoClick(photo.id)
                        }
                    },
                    onPhotoLongClick = { photo ->
                        if (!isSelectionMode) {
                            viewModel.enterSelectionMode()
                            viewModel.togglePhotoSelection(photo.id)
                        }
                    }
                )
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

@Composable
private fun PhotoGrid(
    photos: LazyPagingItems<Photo>,
    gridColumnCount: Int,
    isSelectionMode: Boolean,
    selectedPhotos: Set<Long>,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumnCount),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(photos.itemCount) { index ->
            photos[index]?.let { photo ->
                PhotoGridItem(
                    photo = photo,
                    isSelected = selectedPhotos.contains(photo.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onPhotoClick(photo) },
                    onLongClick = { onPhotoLongClick(photo) }
                )
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = photo.path,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selection overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Not selected",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Video indicator
        if (photo.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Video",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EmptyGalleryView(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No photos found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your photos will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}
