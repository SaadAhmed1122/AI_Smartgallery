package com.ai.smartgallery.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ai.smartgallery.presentation.navigation.NavGraph
import com.ai.smartgallery.presentation.navigation.Screen

/**
 * Main screen with bottom navigation bar
 * Provides access to all major sections of the app
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if we should show bottom bar
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Gallery.route,
        Screen.Albums.route,
        Screen.Search.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Bottom navigation items
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Gallery.route,
        icon = Icons.Default.PhotoLibrary,
        label = "Photos"
    ),
    BottomNavItem(
        route = Screen.Albums.route,
        icon = Icons.Default.PhotoAlbum,
        label = "Albums"
    ),
    BottomNavItem(
        route = Screen.Search.route,
        icon = Icons.Default.Search,
        label = "Search"
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        icon = Icons.Default.Settings,
        label = "Settings"
    )
)

/**
 * Data class for bottom navigation items
 */
private data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)
