package com.ai.smartgallery.presentation.trash

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ai.smartgallery.domain.model.Photo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Trash screen showing deleted photos that can be restored
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedPhotos by viewModel.deletedPhotos.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showEmptyTrashDialog by viewModel.showEmptyTrashDialog.collectAsState()

    Scaffold(
        topBar = {
            TrashTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedPhotos.size,
                onBack = {
                    if (isSelectionMode) {
                        viewModel.clearSelection()
                    } else {
                        onBack()
                    }
                },
                onSelectAll = { viewModel.selectAll() },
                onEmptyTrash = { viewModel.showEmptyTrashDialog() }
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                TrashBottomBar(
                    onRestore = { viewModel.restoreSelected() },
                    onDelete = { viewModel.deleteSelectedPermanently() }
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
                deletedPhotos.isEmpty() -> {
                    TrashEmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(deletedPhotos, key = { it.id }) { photo ->
                            TrashPhotoItem(
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

                    // Info banner
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Photos in trash will be permanently deleted after 30 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Empty trash confirmation dialog
            if (showEmptyTrashDialog) {
                EmptyTrashDialog(
                    photoCount = deletedPhotos.size,
                    onConfirm = { viewModel.emptyTrash() },
                    onDismiss = { viewModel.hideEmptyTrashDialog() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                if (isSelectionMode) "$selectedCount selected" else "Trash"
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
            } else {
                IconButton(onClick = onEmptyTrash) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Empty trash")
                }
            }
        }
    )
}

@Composable
private fun TrashBottomBar(
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalButton(
                onClick = onRestore,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.RestoreFromTrash, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore")
            }

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashPhotoItem(
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

        // Days until deletion badge
        photo.deletedAt?.let { deletedTime ->
            val daysRemaining = 30 - ((System.currentTimeMillis() - deletedTime) / (24 * 60 * 60 * 1000)).toInt()
            if (daysRemaining > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${daysRemaining}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Trash is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Deleted photos will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun EmptyTrashDialog(
    photoCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Empty trash?") },
        text = {
            Text("This will permanently delete $photoCount ${if (photoCount == 1) "photo" else "photos"}. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Empty trash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
