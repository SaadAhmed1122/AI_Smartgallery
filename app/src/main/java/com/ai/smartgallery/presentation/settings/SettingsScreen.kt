package com.ai.smartgallery.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val gridColumnCount by viewModel.gridColumnCount.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAiProcessingEnabled by viewModel.isAiProcessingEnabled.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Display Section
            SettingSection(title = "Display")

            SettingItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = when (themeMode) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "System default"
                },
                onClick = { showThemeDialog = true }
            )

            SettingItem(
                icon = Icons.Default.GridView,
                title = "Grid columns",
                subtitle = "$gridColumnCount columns",
                onClick = { showGridDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Privacy & Security Section
            SettingSection(title = "Privacy & Security")

            SwitchSetting(
                icon = Icons.Default.Lock,
                title = "App Lock",
                subtitle = "Require authentication to open app",
                checked = isAppLockEnabled,
                onCheckedChange = { viewModel.setAppLockEnabled(it) }
            )

            SwitchSetting(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Authentication",
                subtitle = "Use fingerprint or face unlock",
                checked = isBiometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // AI Features Section
            SettingSection(title = "AI Features")

            SwitchSetting(
                icon = Icons.Default.AutoAwesome,
                title = "AI Processing",
                subtitle = "Enable on-device face detection, object recognition, and duplicate detection",
                checked = isAiProcessingEnabled,
                onCheckedChange = { viewModel.setAiProcessingEnabled(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Storage Section
            SettingSection(title = "Storage")

            SettingItem(
                icon = Icons.Default.Storage,
                title = "Clear Cache",
                subtitle = "Free up space by clearing image cache",
                onClick = { viewModel.clearCache() }
            )

            SettingItem(
                icon = Icons.Default.Delete,
                title = "Empty Trash",
                subtitle = "Permanently delete all items in trash",
                onClick = { viewModel.emptyTrash() }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            SettingSection(title = "About")

            SettingItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0-MVP",
                onClick = { }
            )

            SettingItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "Learn about our privacy practices",
                onClick = { /* TODO: Open privacy policy */ }
            )
        }
    }

    // Theme selection dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onThemeSelected = { theme ->
                viewModel.setThemeMode(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Grid column selection dialog
    if (showGridDialog) {
        GridColumnDialog(
            currentColumns = gridColumnCount,
            onColumnsSelected = { columns ->
                viewModel.setGridColumnCount(columns)
                showGridDialog = false
            },
            onDismiss = { showGridDialog = false }
        )
    }
}

@Composable
private fun SettingSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SwitchSetting(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                ThemeOption(
                    title = "System default",
                    selected = currentTheme == "system",
                    onClick = { onThemeSelected("system") }
                )
                ThemeOption(
                    title = "Light",
                    selected = currentTheme == "light",
                    onClick = { onThemeSelected("light") }
                )
                ThemeOption(
                    title = "Dark",
                    selected = currentTheme == "dark",
                    onClick = { onThemeSelected("dark") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title)
    }
}

@Composable
private fun GridColumnDialog(
    currentColumns: Int,
    onColumnsSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grid Columns") },
        text = {
            Column {
                listOf(2, 3, 4, 5, 6).forEach { columns ->
                    GridColumnOption(
                        columns = columns,
                        selected = currentColumns == columns,
                        onClick = { onColumnsSelected(columns) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GridColumnOption(
    columns: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = "$columns columns")
    }
}
