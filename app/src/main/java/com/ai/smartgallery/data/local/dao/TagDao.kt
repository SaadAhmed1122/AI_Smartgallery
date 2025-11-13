package com.ai.smartgallery.data.local.dao

import androidx.room.*
import com.ai.smartgallery.data.local.entity.PhotoEntity
import com.ai.smartgallery.data.local.entity.PhotoTagCrossRef
import com.ai.smartgallery.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing tag data
 */
@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("""
        SELECT photos.* FROM photos
        INNER JOIN photo_tags ON photos.id = photo_tags.photo_id
        WHERE photo_tags.tag_id = :tagId
        ORDER BY photos.date_taken DESC
    """)
    fun getPhotosWithTag(tagId: Long): Flow<List<PhotoEntity>>

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN photo_tags ON tags.id = photo_tags.tag_id
        WHERE photo_tags.photo_id = :photoId
    """)
    suspend fun getTagsForPhoto(photoId: Long): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToPhoto(crossRef: PhotoTagCrossRef)

    @Delete
    suspend fun removeTagFromPhoto(crossRef: PhotoTagCrossRef)

    @Query("DELETE FROM photo_tags WHERE photo_id = :photoId AND tag_id = :tagId")
    suspend fun removeTagFromPhotoById(photoId: Long, tagId: Long)

    @Query("DELETE FROM photo_tags WHERE photo_id = :photoId")
    suspend fun removeAllTagsFromPhoto(photoId: Long)
}
