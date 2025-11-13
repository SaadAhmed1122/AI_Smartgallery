package com.ai.smartgallery.domain.repository

import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for tag operations
 */
interface TagRepository {

    /**
     * Get all tags
     */
    fun getAllTags(): Flow<List<Tag>>

    /**
     * Get tag by ID
     */
    suspend fun getTagById(tagId: Long): Tag?

    /**
     * Get tag by name
     */
    suspend fun getTagByName(name: String): Tag?

    /**
     * Get photos with tag
     */
    fun getPhotosWithTag(tagId: Long): Flow<List<Photo>>

    /**
     * Get tags for photo
     */
    suspend fun getTagsForPhoto(photoId: Long): List<Tag>

    /**
     * Create tag
     */
    suspend fun createTag(name: String): Long

    /**
     * Update tag
     */
    suspend fun updateTag(tag: Tag)

    /**
     * Delete tag
     */
    suspend fun deleteTag(tagId: Long)

    /**
     * Add tag to photo
     */
    suspend fun addTagToPhoto(photoId: Long, tagId: Long)

    /**
     * Remove tag from photo
     */
    suspend fun removeTagFromPhoto(photoId: Long, tagId: Long)

    /**
     * Remove all tags from photo
     */
    suspend fun removeAllTagsFromPhoto(photoId: Long)
}
