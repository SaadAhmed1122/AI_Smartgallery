package com.ai.smartgallery.presentation.album.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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

/**
 * Album detail screen showing photos in an album
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()

    Scaffold(
        topBar = {
            AlbumDetailTopBar(
                albumName = album?.name ?: "Album",
                isSelectionMode = isSelectionMode,
                selectedCount = selectedPhotos.size,
                isSmartAlbum = album?.isSmartAlbum ?: false,
                onBack = {
                    if (isSelectionMode) {
                        viewModel.clearSelection()
                    } else {
                        onBack()
                    }
                },
                onSelectAll = { viewModel.selectAll() },
                onRename = { viewModel.showRenameDialog() },
                onDelete = { viewModel.showDeleteDialog() }
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                AlbumDetailBottomBar(
                    onRemoveFromAlbum = { viewModel.removeSelectedFromAlbum() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                photos.isEmpty() -> {
                    AlbumEmptyState(
                        albumName = album?.name ?: "Album",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            AlbumPhotoItem(
                                photo = photo,
                                isSelected = selectedPhotos.contains(photo.id),
                                onPhotoClick = {
                                    if (isSelectionMode) {
                                        viewModel.togglePhotoSelection(photo.id)
                                    } else {
                                        onPhotoClick(photo.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.togglePhotoSelection(photo.id)
                                }
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
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

            // Delete album confirmation dialog
            if (showDeleteDialog) {
                DeleteAlbumDialog(
                    albumName = album?.name ?: "Album",
                    photoCount = photos.size,
                    onConfirm = { viewModel.deleteAlbum(onBack) },
                    onDismiss = { viewModel.hideDeleteDialog() }
                )
            }

            // Rename album dialog
            if (showRenameDialog) {
                RenameAlbumDialog(
                    currentName = album?.name ?: "",
                    onConfirm = { newName -> viewModel.renameAlbum(newName) },
                    onDismiss = { viewModel.hideRenameDialog() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailTopBar(
    albumName: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    isSmartAlbum: Boolean,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                if (isSelectionMode) "$selectedCount selected" else albumName
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                    contentDescription = if (isSelectionMode) "Cancel" else "Back"
                )
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
            } else if (!isSmartAlbum) {
                // Only show edit options for user-created albums
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename album")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete album")
                }
            }
        }
    )
}

@Composable
private fun AlbumDetailBottomBar(
    onRemoveFromAlbum: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilledTonalButton(
                onClick = onRemoveFromAlbum,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.RemoveCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove from Album")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumPhotoItem(
    photo: Photo,
    isSelected: Boolean,
    onPhotoClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onPhotoClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = photo.path,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            )

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }

        // Video indicator
        if (photo.isVideo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    photo.duration?.let { duration ->
                        val seconds = (duration / 1000).toInt()
                        val minutes = seconds / 60
                        val secs = seconds % 60
                        Text(
                            text = String.format("%d:%02d", minutes, secs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumEmptyState(
    albumName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoAlbum,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No photos in $albumName",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add photos to this album to see them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun DeleteAlbumDialog(
    albumName: String,
    photoCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete album?") },
        text = {
            Text("This will delete the album \"$albumName\" with $photoCount ${if (photoCount == 1) "photo" else "photos"}. Photos will not be deleted from your device.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameAlbumDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null
            )
        },
        title = { Text("Rename album") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Album name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
