package com.ai.smartgallery.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ai.smartgallery.presentation.gallery.GalleryScreen

/**
 * Main navigation graph for the app
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Gallery.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Gallery screen (home)
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                },
                onNavigateToAlbums = {
                    navController.navigate(Screen.Albums.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        // Photo detail screen
        composable(
            route = Screen.PhotoDetail.route,
            arguments = listOf(
                navArgument("photoId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            // PhotoDetailScreen will be implemented
            // PhotoDetailScreen(photoId = photoId, onBack = { navController.popBackStack() })
        }

        // Albums screen
        composable(Screen.Albums.route) {
            // AlbumsScreen will be implemented
            // AlbumsScreen(onAlbumClick = { albumId ->
            //     navController.navigate(Screen.AlbumDetail.createRoute(albumId))
            // })
        }

        // Search screen
        composable(Screen.Search.route) {
            // SearchScreen will be implemented
        }

        // Settings screen
        composable(Screen.Settings.route) {
            // SettingsScreen will be implemented
        }

        // Vault screen
        composable(Screen.Vault.route) {
            // VaultScreen will be implemented
        }

        // People screen
        composable(Screen.People.route) {
            // PeopleScreen will be implemented
        }
    }
}
