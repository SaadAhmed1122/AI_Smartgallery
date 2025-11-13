package com.ai.smartgallery.presentation.settings

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
                onClick = { /* TODO: Show theme dialog */ }
            )

            SettingItem(
                icon = Icons.Default.GridView,
                title = "Grid columns",
                subtitle = "$gridColumnCount columns",
                onClick = { /* TODO: Show column picker */ }
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
