package com.ai.smartgallery.presentation.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ai.smartgallery.domain.model.Album

/**
 * Albums screen showing all albums
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    val favoritePhotos by viewModel.favoritePhotos.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Albums") },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Create album")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Smart Albums Section Header
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Text(
                        text = "Smart Albums",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // All Photos
                item {
                    SmartAlbumCard(
                        title = "All Photos",
                        count = allPhotos.size,
                        icon = Icons.Default.PhotoLibrary,
                        coverPhotos = allPhotos.take(4).map { it.path },
                        onClick = { /* Navigate to all photos */ }
                    )
                }

                // Videos
                item {
                    SmartAlbumCard(
                        title = "Videos",
                        count = videos.size,
                        icon = Icons.Default.VideoLibrary,
                        coverPhotos = videos.take(4).map { it.path },
                        onClick = { /* Navigate to videos */ }
                    )
                }

                // Favorites
                item {
                    SmartAlbumCard(
                        title = "Favorites",
                        count = favoritePhotos.size,
                        icon = Icons.Default.Favorite,
                        coverPhotos = favoritePhotos.take(4).map { it.path },
                        onClick = { /* Navigate to favorites */ }
                    )
                }

                // User Albums Section Header
                if (albums.isNotEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Text(
                            text = "My Albums",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, top = 16.dp)
                        )
                    }

                    items(albums) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }

            // Create album dialog
            if (showCreateDialog) {
                CreateAlbumDialog(
                    onDismiss = { viewModel.hideCreateDialog() },
                    onCreate = { name -> viewModel.createAlbum(name) }
                )
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
        }
    }
}

@Composable
private fun SmartAlbumCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    coverPhotos: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverPhotos.isNotEmpty()) {
                // Display up to 4 images in a 2x2 grid
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left column
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Top-left image
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = coverPhotos.getOrNull(0),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Bottom-left image
                        if (coverPhotos.size > 2) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = coverPhotos.getOrNull(2),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    // Right column
                    if (coverPhotos.size > 1) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // Top-right image
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = coverPhotos.getOrNull(1),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Bottom-right image
                            if (coverPhotos.size > 3) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    AsyncImage(
                                        model = coverPhotos.getOrNull(3),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state with icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }

            // Gradient overlay with album info
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$count photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (album.coverPhotoPath != null) {
                AsyncImage(
                    model = album.coverPhotoPath,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Empty album
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = album.name,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }

            // Album info
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.photoCount} photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var albumName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Album") },
        text = {
            OutlinedTextField(
                value = albumName,
                onValueChange = { albumName = it },
                label = { Text("Album name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(albumName)
                    albumName = ""
                },
                enabled = albumName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
