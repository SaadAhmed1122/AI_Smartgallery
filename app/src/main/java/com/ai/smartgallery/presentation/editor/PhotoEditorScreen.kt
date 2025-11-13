package com.ai.smartgallery.presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.smartgallery.utils.AspectRatio
import com.ai.smartgallery.utils.PhotoFilter

/**
 * Photo editor screen with filters, adjustments, and transformations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    photoId: Long,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: PhotoEditorViewModel = hiltViewModel()
) {
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val contrast by viewModel.contrast.collectAsState()
    val saturation by viewModel.saturation.collectAsState()
    val isCropMode by viewModel.isCropMode.collectAsState()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsState()

    var selectedTab by remember { mutableStateOf(EditorTab.FILTERS) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // Enter crop mode when CROP tab is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == EditorTab.CROP && !isCropMode) {
            viewModel.enterCropMode()
        } else if (selectedTab != EditorTab.CROP && isCropMode) {
            viewModel.exitCropMode()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCropMode) "Crop Photo" else "Edit Photo") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isCropMode) {
                            viewModel.exitCropMode()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = if (isCropMode) "Cancel" else "Close")
                    }
                },
                actions = {
                    if (isCropMode) {
                        IconButton(onClick = { viewModel.applyCrop() }) {
                            Icon(Icons.Default.Check, contentDescription = "Apply Crop")
                        }
                    } else {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        }
                        IconButton(
                            onClick = {
                                viewModel.savePhoto()
                                onSave()
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isCropMode) {
                // Crop controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    CropControls(
                        selectedAspectRatio = selectedAspectRatio,
                        onAspectRatioSelected = { viewModel.setAspectRatio(it) }
                    )
                }
            } else {
                Column {
                    // Tab selector
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        EditorTab.values().forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                }
                            )
                        }
                    }

                    // Tab content
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        when (selectedTab) {
                            EditorTab.CROP -> CropControls(
                                selectedAspectRatio = selectedAspectRatio,
                                onAspectRatioSelected = { viewModel.setAspectRatio(it) }
                            )
                            EditorTab.FILTERS -> FilterControls(
                                selectedFilter = selectedFilter,
                                onFilterSelected = { viewModel.applyFilter(it) }
                            )
                            EditorTab.ADJUST -> AdjustControls(
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation,
                                onBrightnessChange = { viewModel.adjustBrightness(it) },
                                onContrastChange = { viewModel.adjustContrast(it) },
                                onSaturationChange = { viewModel.adjustSaturation(it) }
                            )
                            EditorTab.TRANSFORM -> TransformControls(
                                onRotate = { viewModel.rotate90() },
                                onFlipHorizontal = { viewModel.flipHorizontal() },
                                onFlipVertical = { viewModel.flipVertical() }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Photo preview
            editedBitmap?.let { bitmap ->
                val cropRect by viewModel.cropRect.collectAsState()

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Edited photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { imageSize = it }
                        .drawWithContent {
                            drawContent()

                            // Draw crop overlay when in crop mode
                            if (isCropMode && cropRect != null && imageSize.width > 0) {
                                val rect = cropRect!!
                                val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                                val viewAspect = size.width / size.height

                                // Calculate displayed image bounds
                                val (displayWidth, displayHeight) = if (bitmapAspect > viewAspect) {
                                    size.width to size.width / bitmapAspect
                                } else {
                                    size.height * bitmapAspect to size.height
                                }

                                val offsetX = (size.width - displayWidth) / 2f
                                val offsetY = (size.height - displayHeight) / 2f

                                // Calculate crop rect in view coordinates
                                val scaleX = displayWidth / bitmap.width
                                val scaleY = displayHeight / bitmap.height

                                val cropLeft = offsetX + rect.left * scaleX
                                val cropTop = offsetY + rect.top * scaleY
                                val cropWidth = rect.width() * scaleX
                                val cropHeight = rect.height() * scaleY

                                // Draw darkened overlay outside crop area
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, cropTop)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, cropTop + cropHeight),
                                    size = Size(size.width, size.height - cropTop - cropHeight)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, cropTop),
                                    size = Size(cropLeft, cropHeight)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(cropLeft + cropWidth, cropTop),
                                    size = Size(size.width - cropLeft - cropWidth, cropHeight)
                                )

                                // Draw crop rect border
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(cropLeft, cropTop),
                                    size = Size(cropWidth, cropHeight),
                                    style = Stroke(width = 3f)
                                )
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
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
private fun FilterControls(
    selectedFilter: PhotoFilter,
    onFilterSelected: (PhotoFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PhotoFilter.values()) { filter ->
            FilterItem(
                filter = filter,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterItem(
    filter: PhotoFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for filter preview
            Text(
                text = filter.name.take(1),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = filter.name.lowercase().capitalize(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun AdjustControls(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Brightness
        AdjustmentSlider(
            label = "Brightness",
            value = brightness,
            valueRange = -100f..100f,
            onValueChange = onBrightnessChange,
            icon = Icons.Default.LightMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contrast
        AdjustmentSlider(
            label = "Contrast",
            value = contrast,
            valueRange = 0.5f..2f,
            onValueChange = onContrastChange,
            icon = Icons.Default.Contrast
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Saturation
        AdjustmentSlider(
            label = "Saturation",
            value = saturation,
            valueRange = 0f..2f,
            onValueChange = onSaturationChange,
            icon = Icons.Default.Palette
        )
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange
            )
        }
    }
}

@Composable
private fun TransformControls(
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TransformButton(
            icon = Icons.Default.RotateRight,
            label = "Rotate",
            onClick = onRotate
        )
        TransformButton(
            icon = Icons.Default.FlipCameraAndroid,
            label = "Flip H",
            onClick = onFlipHorizontal
        )
        TransformButton(
            icon = Icons.Default.Flip,
            label = "Flip V",
            onClick = onFlipVertical
        )
    }
}

@Composable
private fun TransformButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CropControls(
    selectedAspectRatio: AspectRatio,
    onAspectRatioSelected: (AspectRatio) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AspectRatio.values()) { ratio ->
                AspectRatioChip(
                    aspectRatio = ratio,
                    isSelected = selectedAspectRatio == ratio,
                    onClick = { onAspectRatioSelected(ratio) }
                )
            }
        }
    }
}

@Composable
private fun AspectRatioChip(
    aspectRatio: AspectRatio,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(aspectRatio.label) },
        leadingIcon = if (isSelected) {
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

private enum class EditorTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CROP("Crop", Icons.Default.Crop),
    FILTERS("Filters", Icons.Default.FilterVintage),
    ADJUST("Adjust", Icons.Default.Tune),
    TRANSFORM("Transform", Icons.Default.Transform)
}

// String extension for capitalization
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
