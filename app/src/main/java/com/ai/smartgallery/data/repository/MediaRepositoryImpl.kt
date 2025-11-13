package com.ai.smartgallery.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ai.smartgallery.data.local.MediaStoreManager
import com.ai.smartgallery.data.local.dao.PhotoDao
import com.ai.smartgallery.data.model.toDomain
import com.ai.smartgallery.di.IoDispatcher
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MediaRepository
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val mediaStoreManager: MediaStoreManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {

    override fun getAllPhotosPaged(): Flow<PagingData<Photo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = true,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { photoDao.getAllPhotosPaged() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getAllPhotos(): Flow<List<Photo>> {
        return photoDao.getAllPhotosFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPhotoById(photoId: Long): Photo? = withContext(ioDispatcher) {
        photoDao.getPhotoById(photoId)?.toDomain()
    }

    override fun getFavoritePhotos(): Flow<List<Photo>> {
        return photoDao.getFavoritePhotos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getVideos(): Flow<List<Photo>> {
        return photoDao.getVideos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchPhotos(query: String): List<Photo> = withContext(ioDispatcher) {
        photoDao.searchPhotos(query).map { it.toDomain() }
    }

    override suspend fun toggleFavorite(photoId: Long, isFavorite: Boolean) = withContext(ioDispatcher) {
        photoDao.updateFavoriteStatus(photoId, isFavorite)
    }

    override suspend fun updateRating(photoId: Long, rating: Int) = withContext(ioDispatcher) {
        photoDao.updateRating(photoId, rating)
    }

    override suspend fun deletePhoto(photoId: Long) = withContext(ioDispatcher) {
        val photo = photoDao.getPhotoById(photoId)
        if (photo != null) {
            // Delete from MediaStore
            mediaStoreManager.deleteMediaItem(photo.mediaStoreId, photo.isVideo)
            // Delete from database
            photoDao.deletePhotoById(photoId)
        }
    }

    override suspend fun deletePhotos(photoIds: List<Long>) = withContext(ioDispatcher) {
        photoIds.forEach { photoId ->
            deletePhoto(photoId)
        }
    }

    override suspend fun syncPhotosFromMediaStore() = withContext(ioDispatcher) {
        // Load images from MediaStore
        val images = mediaStoreManager.loadAllImages()
        photoDao.insertPhotos(images)

        // Load videos from MediaStore
        val videos = mediaStoreManager.loadAllVideos()
        photoDao.insertPhotos(videos)
    }

    override suspend fun getPhotoCount(): Int = withContext(ioDispatcher) {
        photoDao.getPhotoCount()
    }

    override suspend fun getVideoCount(): Int = withContext(ioDispatcher) {
        photoDao.getVideoCount()
    }

    override fun getDeletedPhotos(): Flow<List<Photo>> {
        return photoDao.getDeletedPhotos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun moveToTrash(photoId: Long) = withContext(ioDispatcher) {
        photoDao.moveToTrash(photoId, System.currentTimeMillis())
    }

    override suspend fun moveToTrashBatch(photoIds: List<Long>) = withContext(ioDispatcher) {
        photoDao.moveToTrashBatch(photoIds, System.currentTimeMillis())
    }

    override suspend fun restoreFromTrash(photoId: Long) = withContext(ioDispatcher) {
        photoDao.restoreFromTrash(photoId)
    }

    override suspend fun restoreFromTrashBatch(photoIds: List<Long>) = withContext(ioDispatcher) {
        photoDao.restoreFromTrashBatch(photoIds)
    }

    override suspend fun permanentlyDeleteOldTrash(olderThanDays: Int) = withContext(ioDispatcher) {
        val olderThan = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        photoDao.permanentlyDeleteOldTrash(olderThan)
    }

    override suspend fun emptyTrash() = withContext(ioDispatcher) {
        photoDao.emptyTrash()
    }

    override suspend fun getDeletedPhotoCount(): Int = withContext(ioDispatcher) {
        photoDao.getDeletedPhotoCount()
    }
}
