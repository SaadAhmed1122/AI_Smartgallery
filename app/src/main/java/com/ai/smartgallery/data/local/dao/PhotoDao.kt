package com.ai.smartgallery.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.ai.smartgallery.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing photo data
 */
@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE is_hidden = 0 AND is_deleted = 0 ORDER BY date_taken DESC")
    fun getAllPhotosPaged(): PagingSource<Int, PhotoEntity>

    @Query("SELECT * FROM photos WHERE is_hidden = 0 AND is_deleted = 0 ORDER BY date_taken DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE media_store_id = :mediaStoreId")
    suspend fun getPhotoByMediaStoreId(mediaStoreId: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE is_favorite = 1 AND is_deleted = 0 ORDER BY date_taken DESC")
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE is_video = 1 AND is_hidden = 0 AND is_deleted = 0 ORDER BY date_taken DESC")
    fun getVideos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE is_hidden = 1 AND is_deleted = 0 ORDER BY date_taken DESC")
    fun getHiddenPhotos(): Flow<List<PhotoEntity>>

    @Query("""
        SELECT * FROM photos
        WHERE date_taken >= :startDate AND date_taken <= :endDate
        AND is_hidden = 0 AND is_deleted = 0
        ORDER BY date_taken DESC
    """)
    suspend fun getPhotosByDateRange(startDate: Long, endDate: Long): List<PhotoEntity>

    @Query("""
        SELECT * FROM photos
        WHERE display_name LIKE '%' || :query || '%'
        AND is_hidden = 0 AND is_deleted = 0
        ORDER BY date_taken DESC
    """)
    suspend fun searchPhotos(query: String): List<PhotoEntity>

    // Trash-related queries
    @Query("SELECT * FROM photos WHERE is_deleted = 1 ORDER BY deleted_at DESC")
    fun getDeletedPhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET is_deleted = 1, deleted_at = :deletedAt WHERE id = :photoId")
    suspend fun moveToTrash(photoId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET is_deleted = 1, deleted_at = :deletedAt WHERE id IN (:photoIds)")
    suspend fun moveToTrashBatch(photoIds: List<Long>, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET is_deleted = 0, deleted_at = NULL WHERE id = :photoId")
    suspend fun restoreFromTrash(photoId: Long)

    @Query("UPDATE photos SET is_deleted = 0, deleted_at = NULL WHERE id IN (:photoIds)")
    suspend fun restoreFromTrashBatch(photoIds: List<Long>)

    @Query("DELETE FROM photos WHERE is_deleted = 1 AND deleted_at < :olderThan")
    suspend fun permanentlyDeleteOldTrash(olderThan: Long)

    @Query("DELETE FROM photos WHERE is_deleted = 1")
    suspend fun emptyTrash()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>): List<Long>

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Update
    suspend fun updatePhotos(photos: List<PhotoEntity>)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)

    @Query("DELETE FROM photos WHERE id IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<Long>)

    @Query("UPDATE photos SET is_favorite = :isFavorite WHERE id = :photoId")
    suspend fun updateFavoriteStatus(photoId: Long, isFavorite: Boolean)

    @Query("UPDATE photos SET is_hidden = :isHidden WHERE id = :photoId")
    suspend fun updateHiddenStatus(photoId: Long, isHidden: Boolean)

    @Query("UPDATE photos SET rating = :rating WHERE id = :photoId")
    suspend fun updateRating(photoId: Long, rating: Int)

    @Query("SELECT COUNT(*) FROM photos WHERE is_hidden = 0 AND is_deleted = 0")
    suspend fun getPhotoCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE is_video = 1 AND is_hidden = 0 AND is_deleted = 0")
    suspend fun getVideoCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE is_deleted = 1")
    suspend fun getDeletedPhotoCount(): Int
}
