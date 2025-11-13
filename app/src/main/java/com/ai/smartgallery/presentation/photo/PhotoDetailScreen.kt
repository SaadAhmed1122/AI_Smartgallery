package com.ai.smartgallery.presentation.photo

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Photo detail screen with full-screen viewer and gestures
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel()
) {
    val photo by viewModel.photo.collectAsState()
    val photoTags by viewModel.photoTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showInfo by viewModel.showInfo.collectAsState()
    val showActions by viewModel.showActions.collectAsState()
    val showTagDialog by viewModel.showTagDialog.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Photo viewer
        photo?.let { currentPhoto ->
            AsyncImage(
                model = currentPhoto.path,
                contentDescription = currentPhoto.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.toggleActions()
                            },
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2f
                                if (scale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        }

        // Top bar
        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text(photo?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (photo?.isFavorite == true) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = "Favorite",
                            tint = if (photo?.isFavorite == true) {
                                Color.Red
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(onClick = { /* TODO: Add to album */ }) {
                        Icon(Icons.Default.AddToPhotos, contentDescription = "Add to album")
                    }
                    IconButton(onClick = { viewModel.toggleInfo() }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }

        // Bottom actions bar
        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            photo?.let { currentPhoto ->
                BottomActionsBar(
                    photo = currentPhoto,
                    onShare = {
                        sharePhoto(context, currentPhoto)
                    },
                    onEdit = onEdit,
                    onDelete = {
                        viewModel.deletePhoto()
                        onBack()
                    }
                )
            }
        }

        // Info bottom sheet
        if (showInfo) {
            photo?.let { currentPhoto ->
                PhotoInfoBottomSheet(
                    photo = currentPhoto,
                    tags = photoTags,
                    onDismiss = { viewModel.toggleInfo() },
                    onManageTags = { viewModel.showTagDialog() },
                    onRemoveTag = { tagId -> viewModel.removeTagFromPhoto(tagId) }
                )
            }
        }

        // Tag management dialog
        if (showTagDialog) {
            TagManagementDialog(
                photoTags = photoTags,
                allTags = allTags,
                onDismiss = { viewModel.hideTagDialog() },
                onAddTag = { tagId -> viewModel.addTagToPhoto(tagId) },
                onRemoveTag = { tagId -> viewModel.removeTagFromPhoto(tagId) },
                onCreateTag = { name -> viewModel.createAndAddTag(name) }
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

@Composable
private fun BottomActionsBar(
    photo: com.ai.smartgallery.domain.model.Photo,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = onShare
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = "Edit",
                onClick = onEdit
            )
            ActionButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete,
                tint = Color.Red
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoInfoBottomSheet(
    photo: com.ai.smartgallery.domain.model.Photo,
    tags: List<com.ai.smartgallery.domain.model.Tag>,
    onDismiss: () -> Unit,
    onManageTags: () -> Unit,
    onRemoveTag: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Photo Information",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tags section
            if (tags.isNotEmpty() || true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tags:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(0.4f)
                    )
                    Column(modifier = Modifier.weight(0.6f)) {
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag.name) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onRemoveTag(tag.id) }
                                        )
                                    }
                                )
                            }
                            AssistChip(
                                onClick = onManageTags,
                                label = { Text("Add tag") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            InfoRow("Name", photo.displayName)
            InfoRow("Size", formatFileSize(photo.size))
            photo.width?.let { width ->
                photo.height?.let { height ->
                    InfoRow("Dimensions", "${width}x${height}")
                }
            }
            InfoRow("Date", formatDate(photo.dateTaken))
            InfoRow("Type", photo.mimeType)
            photo.latitude?.let { lat ->
                photo.longitude?.let { lon ->
                    InfoRow("Location", String.format("%.6f, %.6f", lat, lon))
                }
            }
            InfoRow("Path", photo.path)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagManagementDialog(
    photoTags: List<com.ai.smartgallery.domain.model.Tag>,
    allTags: List<com.ai.smartgallery.domain.model.Tag>,
    onDismiss: () -> Unit,
    onAddTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    val photoTagIds = photoTags.map { it.id }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Current tags
                if (photoTags.isNotEmpty()) {
                    Text(
                        text = "Current Tags",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        photoTags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { onRemoveTag(tag.id) },
                                label = { Text(tag.name) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Available tags
                val availableTags = allTags.filter { !photoTagIds.contains(it.id) }
                if (availableTags.isNotEmpty()) {
                    Text(
                        text = "Available Tags",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableTags.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = { onAddTag(tag.id) },
                                label = { Text(tag.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Create new tag
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Create New Tag",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                onCreateTag(newTagName)
                                newTagName = ""
                            }
                        },
                        enabled = newTagName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, "Create and add tag")
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

private fun sharePhoto(context: android.content.Context, photo: com.ai.smartgallery.domain.model.Photo) {
    val file = File(photo.path)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = photo.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share photo"))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
