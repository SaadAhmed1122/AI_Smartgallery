package com.ai.smartgallery.data.repository

import com.ai.smartgallery.data.local.dao.PhotoDao
import com.ai.smartgallery.data.local.dao.TagDao
import com.ai.smartgallery.data.local.entity.PhotoTagCrossRef
import com.ai.smartgallery.data.local.entity.TagEntity
import com.ai.smartgallery.data.model.toDomain
import com.ai.smartgallery.data.model.toEntity
import com.ai.smartgallery.di.IoDispatcher
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.model.Tag
import com.ai.smartgallery.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TagRepository
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val photoDao: PhotoDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTagById(tagId: Long): Tag? = withContext(ioDispatcher) {
        tagDao.getTagById(tagId)?.toDomain()
    }

    override suspend fun getTagByName(name: String): Tag? = withContext(ioDispatcher) {
        tagDao.getTagByName(name)?.toDomain()
    }

    override fun getPhotosWithTag(tagId: Long): Flow<List<Photo>> {
        return tagDao.getPhotosWithTag(tagId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTagsForPhoto(photoId: Long): List<Tag> = withContext(ioDispatcher) {
        tagDao.getTagsForPhoto(photoId).map { it.toDomain() }
    }

    override suspend fun createTag(name: String): Long = withContext(ioDispatcher) {
        val tag = TagEntity(name = name)
        tagDao.insertTag(tag)
    }

    override suspend fun updateTag(tag: Tag) = withContext(ioDispatcher) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun deleteTag(tagId: Long) = withContext(ioDispatcher) {
        val tag = tagDao.getTagById(tagId)
        if (tag != null) {
            tagDao.deleteTag(tag)
        }
    }

    override suspend fun addTagToPhoto(photoId: Long, tagId: Long) = withContext(ioDispatcher) {
        val crossRef = PhotoTagCrossRef(photoId = photoId, tagId = tagId)
        tagDao.addTagToPhoto(crossRef)
    }

    override suspend fun removeTagFromPhoto(photoId: Long, tagId: Long) = withContext(ioDispatcher) {
        tagDao.removeTagFromPhotoById(photoId, tagId)
    }

    override suspend fun removeAllTagsFromPhoto(photoId: Long) = withContext(ioDispatcher) {
        tagDao.removeAllTagsFromPhoto(photoId)
    }
}
