package com.ai.smartgallery.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ai.smartgallery.presentation.gallery.GalleryScreen
import com.ai.smartgallery.presentation.photo.PhotoDetailScreen
import com.ai.smartgallery.presentation.album.AlbumsScreen
import com.ai.smartgallery.presentation.search.SearchScreen
import com.ai.smartgallery.presentation.settings.SettingsScreen
import com.ai.smartgallery.presentation.editor.PhotoEditorScreen
import com.ai.smartgallery.presentation.trash.TrashScreen

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
            PhotoDetailScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.PhotoEditor.createRoute(photoId)) }
            )
        }

        // Albums screen
        composable(Screen.Albums.route) {
            AlbumsScreen(
                onAlbumClick = { albumId ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Search screen
        composable(Screen.Search.route) {
            SearchScreen(
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Vault screen
        composable(Screen.Vault.route) {
            // VaultScreen will be implemented
        }

        // People screen
        composable(Screen.People.route) {
            // PeopleScreen will be implemented
        }

        // Trash screen
        composable(Screen.Trash.route) {
            TrashScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                }
            )
        }

        // Photo editor screen
        composable(
            route = Screen.PhotoEditor.route,
            arguments = listOf(
                navArgument("photoId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            PhotoEditorScreen(
                photoId = photoId,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
