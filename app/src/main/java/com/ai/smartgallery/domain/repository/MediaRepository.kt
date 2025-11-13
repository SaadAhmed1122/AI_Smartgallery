package com.ai.smartgallery.domain.repository

import androidx.paging.PagingData
import com.ai.smartgallery.domain.model.Photo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for media operations
 */
interface MediaRepository {

    /**
     * Get all photos as paginated flow
     */
    fun getAllPhotosPaged(): Flow<PagingData<Photo>>

    /**
     * Get all photos as simple flow
     */
    fun getAllPhotos(): Flow<List<Photo>>

    /**
     * Get photo by ID
     */
    suspend fun getPhotoById(photoId: Long): Photo?

    /**
     * Get favorite photos
     */
    fun getFavoritePhotos(): Flow<List<Photo>>

    /**
     * Get videos
     */
    fun getVideos(): Flow<List<Photo>>

    /**
     * Search photos by query
     */
    suspend fun searchPhotos(query: String): List<Photo>

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(photoId: Long, isFavorite: Boolean)

    /**
     * Update rating
     */
    suspend fun updateRating(photoId: Long, rating: Int)

    /**
     * Delete photo
     */
    suspend fun deletePhoto(photoId: Long)

    /**
     * Delete multiple photos
     */
    suspend fun deletePhotos(photoIds: List<Long>)

    /**
     * Sync photos from MediaStore
     */
    suspend fun syncPhotosFromMediaStore()

    /**
     * Get photo count
     */
    suspend fun getPhotoCount(): Int

    /**
     * Get video count
     */
    suspend fun getVideoCount(): Int
}
