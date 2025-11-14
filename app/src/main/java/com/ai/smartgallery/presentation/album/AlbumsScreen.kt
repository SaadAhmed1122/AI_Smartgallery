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
    onNavigateToGallery: () -> Unit = {},
    onAIAlbumClick: (String) -> Unit = {},
    onPeopleClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onDuplicatesClick: () -> Unit = {},
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    val favoritePhotos by viewModel.favoritePhotos.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val aiAlbums by viewModel.aiGeneratedAlbums.collectAsState()
    val peopleCount by viewModel.peopleCount.collectAsState()
    val documentsCount by viewModel.documentsCount.collectAsState()
    val duplicatesCount by viewModel.duplicatesCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val processingProgress by viewModel.processingProgress.collectAsState()

    // Debug logging
    LaunchedEffect(aiAlbums) {
        android.util.Log.d("AlbumsScreen", "AI Albums count: ${aiAlbums.size}")
        aiAlbums.forEach { (label, count, _) ->
            android.util.Log.d("AlbumsScreen", "  - $label: $count photos")
        }
    }
    LaunchedEffect(allPhotos) {
        android.util.Log.d("AlbumsScreen", "Total photos: ${allPhotos.size}")
    }

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
                        onClick = onNavigateToGallery
                    )
                }

                // Videos
                item {
                    SmartAlbumCard(
                        title = "Videos",
                        count = videos.size,
                        icon = Icons.Default.VideoLibrary,
                        coverPhotos = videos.take(4).map { it.path },
                        onClick = onNavigateToGallery
                    )
                }

                // Favorites
                item {
                    SmartAlbumCard(
                        title = "Favorites",
                        count = favoritePhotos.size,
                        icon = Icons.Default.Favorite,
                        coverPhotos = favoritePhotos.take(4).map { it.path },
                        onClick = onNavigateToGallery
                    )
                }

                // People (Faces)
                if (peopleCount > 0) {
                    item {
                        SmartAlbumCard(
                            title = "People",
                            count = peopleCount,
                            icon = Icons.Default.Face,
                            coverPhotos = emptyList(), // Will be loaded from repository
                            onClick = onPeopleClick
                        )
                    }
                }

                // Documents (Text/OCR)
                if (documentsCount > 0) {
                    item {
                        SmartAlbumCard(
                            title = "Documents",
                            count = documentsCount,
                            icon = Icons.Default.Description,
                            coverPhotos = emptyList(),
                            onClick = onDocumentsClick
                        )
                    }
                }

                // Duplicates
                if (duplicatesCount > 0) {
                    item {
                        SmartAlbumCard(
                            title = "Duplicates",
                            count = duplicatesCount,
                            icon = Icons.Default.ContentCopy,
                            coverPhotos = emptyList(),
                            onClick = onDuplicatesClick
                        )
                    }
                }

                // AI-Generated Albums Section Header
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column {
                        Text(
                            text = "AI Albums",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, top = 16.dp)
                        )
                        // Debug info
                        Text(
                            text = "Debug: ${aiAlbums.size} AI albums found, ${allPhotos.size} total photos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (aiAlbums.isNotEmpty()) {
                    // AI-generated album cards
                    items(aiAlbums) { (label, count, coverPhotos) ->
                        AIAlbumCard(
                            label = label,
                            count = count,
                            coverPhotos = coverPhotos,
                            onClick = { onAIAlbumClick(label) }
                        )
                    }
                } else {
                    // AI Processing Status Card
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        AIProcessingCard(
                            photoCount = allPhotos.size,
                            onStartProcessing = { viewModel.triggerAIProcessing() }
                        )
                    }
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

            // Processing progress indicator
            processingProgress?.let { progress ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
private fun AIAlbumCard(
    label: String,
    count: Int,
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
                // Fallback to icon if no cover photos
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            label.contains("dog", ignoreCase = true) ||
                            label.contains("cat", ignoreCase = true) ||
                            label.contains("pet", ignoreCase = true) -> Icons.Default.Pets
                            label.contains("food", ignoreCase = true) -> Icons.Default.Restaurant
                            label.contains("car", ignoreCase = true) ||
                            label.contains("vehicle", ignoreCase = true) -> Icons.Default.DirectionsCar
                            label.contains("person", ignoreCase = true) ||
                            label.contains("people", ignoreCase = true) ||
                            label.contains("face", ignoreCase = true) -> Icons.Default.Person
                            label.contains("nature", ignoreCase = true) ||
                            label.contains("tree", ignoreCase = true) ||
                            label.contains("flower", ignoreCase = true) ||
                            label.contains("plant", ignoreCase = true) -> Icons.Default.LocalFlorist
                            label.contains("building", ignoreCase = true) ||
                            label.contains("architecture", ignoreCase = true) ||
                            label.contains("city", ignoreCase = true) -> Icons.Default.LocationCity
                            label.contains("sunset", ignoreCase = true) ||
                            label.contains("sky", ignoreCase = true) ||
                            label.contains("cloud", ignoreCase = true) -> Icons.Default.WbSunny
                            label.contains("water", ignoreCase = true) ||
                            label.contains("ocean", ignoreCase = true) ||
                            label.contains("sea", ignoreCase = true) ||
                            label.contains("beach", ignoreCase = true) -> Icons.Default.WaterDrop
                            else -> Icons.Default.Label
                        },
                        contentDescription = label,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }

            // Album info overlay with semi-transparent background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
private fun AIProcessingCard(
    photoCount: Int,
    onStartProcessing: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (photoCount > 0) {
                    "AI Albums Not Ready"
                } else {
                    "No Photos Yet"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (photoCount > 0) {
                    "Tap below to start AI processing. Your $photoCount ${if (photoCount == 1) "photo" else "photos"} will be analyzed to create smart albums based on content (pets, food, nature, etc.)"
                } else {
                    "Add some photos first, then AI will automatically categorize them for you"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (photoCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        android.util.Log.d("AIProcessingCard", "Start AI Processing button clicked!")
                        onStartProcessing()
                    },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start AI Processing")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Processing runs in background and may take a few minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Check logcat for: AIProcessingWorker, AlbumsScreen, MediaRepository",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
