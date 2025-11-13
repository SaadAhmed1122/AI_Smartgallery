package com.ai.smartgallery.presentation.navigation

/**
 * Sealed class representing all navigation destinations
 */
sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    object Albums : Screen("albums")
    object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: Long) = "photo_detail/$photoId"
    }
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: Long) = "album_detail/$albumId"
    }
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Vault : Screen("vault")
    object People : Screen("people")
    object PhotoEditor : Screen("photo_editor/{photoId}") {
        fun createRoute(photoId: Long) = "photo_editor/$photoId"
    }
}
