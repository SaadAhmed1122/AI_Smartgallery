package com.ai.smartgallery.domain.repository

import com.ai.smartgallery.domain.model.Album
import com.ai.smartgallery.domain.model.Photo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for album operations
 */
interface AlbumRepository {

    /**
     * Get all albums
     */
    fun getAllAlbums(): Flow<List<Album>>

    /**
     * Get album by ID
     */
    suspend fun getAlbumById(albumId: Long): Album?

    /**
     * Get photos in album
     */
    fun getPhotosInAlbum(albumId: Long): Flow<List<Photo>>

    /**
     * Create album
     */
    suspend fun createAlbum(name: String): Long

    /**
     * Update album
     */
    suspend fun updateAlbum(album: Album)

    /**
     * Delete album
     */
    suspend fun deleteAlbum(albumId: Long)

    /**
     * Add photo to album
     */
    suspend fun addPhotoToAlbum(photoId: Long, albumId: Long)

    /**
     * Remove photo from album
     */
    suspend fun removePhotoFromAlbum(photoId: Long, albumId: Long)

    /**
     * Create smart albums (Camera, Screenshots, etc.)
     */
    suspend fun createSmartAlbums()
}
