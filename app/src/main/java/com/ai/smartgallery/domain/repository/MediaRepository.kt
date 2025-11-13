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
     * Schedule AI processing for photos
     */
    fun scheduleAIProcessing()

    /**
     * Get photo count
     */
    suspend fun getPhotoCount(): Int

    /**
     * Get video count
     */
    suspend fun getVideoCount(): Int

    /**
     * Get deleted photos (trash)
     */
    fun getDeletedPhotos(): Flow<List<Photo>>

    /**
     * Move photo to trash (soft delete)
     */
    suspend fun moveToTrash(photoId: Long)

    /**
     * Move multiple photos to trash
     */
    suspend fun moveToTrashBatch(photoIds: List<Long>)

    /**
     * Restore photo from trash
     */
    suspend fun restoreFromTrash(photoId: Long)

    /**
     * Restore multiple photos from trash
     */
    suspend fun restoreFromTrashBatch(photoIds: List<Long>)

    /**
     * Permanently delete photos older than specified time
     * @param olderThanDays Number of days after which to permanently delete
     */
    suspend fun permanentlyDeleteOldTrash(olderThanDays: Int = 30)

    /**
     * Empty trash (permanently delete all trashed photos)
     */
    suspend fun emptyTrash()

    /**
     * Get deleted photo count
     */
    suspend fun getDeletedPhotoCount(): Int

    /**
     * Get AI-generated albums based on image labels
     * Returns list of label names with photo counts (minimum 3 photos per label)
     */
    fun getAIGeneratedAlbums(): Flow<List<Pair<String, Int>>>

    /**
     * Get photos for a specific AI label
     */
    suspend fun getPhotosForLabel(label: String): List<Photo>
}
