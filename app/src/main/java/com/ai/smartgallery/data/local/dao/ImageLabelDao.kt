package com.ai.smartgallery.data.local.dao

import androidx.room.*
import com.ai.smartgallery.data.local.entity.ImageLabelEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing image label data
 */
@Dao
interface ImageLabelDao {

    @Query("SELECT * FROM image_labels WHERE photo_id = :photoId")
    suspend fun getLabelsForPhoto(photoId: Long): List<ImageLabelEntity>

    @Query("SELECT * FROM image_labels WHERE label = :label ORDER BY confidence DESC")
    suspend fun getPhotosWithLabel(label: String): List<ImageLabelEntity>

    @Query("SELECT DISTINCT label FROM image_labels ORDER BY label ASC")
    fun getAllDistinctLabels(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: ImageLabelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<ImageLabelEntity>): List<Long>

    @Delete
    suspend fun deleteLabel(label: ImageLabelEntity)

    @Query("DELETE FROM image_labels WHERE photo_id = :photoId")
    suspend fun deleteLabelsForPhoto(photoId: Long)

    @Query("""
        SELECT COUNT(DISTINCT photo_id) FROM image_labels
        WHERE label = :label
    """)
    suspend fun getPhotoCountForLabel(label: String): Int
}
