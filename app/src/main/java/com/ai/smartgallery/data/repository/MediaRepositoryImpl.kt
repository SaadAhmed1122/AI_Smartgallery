package com.ai.smartgallery.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ai.smartgallery.data.local.MediaStoreManager
import com.ai.smartgallery.data.local.dao.FaceEmbeddingDao
import com.ai.smartgallery.data.local.dao.ImageLabelDao
import com.ai.smartgallery.data.local.dao.PhotoDao
import com.ai.smartgallery.data.model.toDomain
import com.ai.smartgallery.di.IoDispatcher
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.workers.AIProcessingWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val imageLabelDao: ImageLabelDao,
    private val faceEmbeddingDao: FaceEmbeddingDao,
    private val mediaStoreManager: MediaStoreManager,
    private val workManager: WorkManager,
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

    override suspend fun syncPhotosFromMediaStore(): Unit = withContext(ioDispatcher) {
        // Load images from MediaStore
        val images = mediaStoreManager.loadAllImages()
        photoDao.insertPhotos(images)

        // Load videos from MediaStore
        val videos = mediaStoreManager.loadAllVideos()
        photoDao.insertPhotos(videos)

        // Don't auto-schedule AI processing - let user trigger it manually
        // This prevents re-processing every time the screen is navigated to
    }

    override fun scheduleAIProcessing() {
        android.util.Log.d("MediaRepository", "scheduleAIProcessing() called - enqueueing WorkManager job")
        val workRequest = OneTimeWorkRequestBuilder<AIProcessingWorker>()
            // Remove battery constraint for immediate execution during debugging
            // TODO: Re-enable battery constraint in production
            .build()

        workManager.enqueueUniqueWork(
            "ai_processing",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        android.util.Log.d("MediaRepository", "AI Processing work enqueued successfully with ID: ${workRequest.id}")
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

    override fun getAIGeneratedAlbums(): Flow<List<Triple<String, Int, List<String>>>> {
        return imageLabelDao.getAllDistinctLabels().map { labels ->
            withContext(ioDispatcher) {
                android.util.Log.d("MediaRepository", "getAIGeneratedAlbums: Found ${labels.size} distinct labels")
                labels.forEach { label ->
                    val count = imageLabelDao.getPhotoCountForLabel(label)
                    android.util.Log.d("MediaRepository", "  Label '$label': $count photos")
                }

                val albums = labels.mapNotNull { label ->
                    val count = imageLabelDao.getPhotoCountForLabel(label)
                    // Only include labels with at least 3 photos
                    if (count >= 3) {
                        // Get cover photos for this label
                        val labelEntities = imageLabelDao.getPhotosWithLabel(label)
                        val photoIds = labelEntities.map { it.photoId }.distinct().take(4)
                        val coverPhotoPaths = photoIds.mapNotNull { photoId ->
                            photoDao.getPhotoById(photoId)?.path
                        }
                        Triple(label, count, coverPhotoPaths)
                    } else null
                }.sortedByDescending { it.second } // Sort by photo count descending
                .take(20) // Limit to top 20 AI albums

                android.util.Log.d("MediaRepository", "getAIGeneratedAlbums: Returning ${albums.size} AI albums")
                albums
            }
        }
    }

    override suspend fun getPhotosForLabel(label: String): List<Photo> = withContext(ioDispatcher) {
        val labelEntities = imageLabelDao.getPhotosWithLabel(label)
        val photoIds = labelEntities.map { it.photoId }.distinct()
        photoIds.mapNotNull { photoId ->
            photoDao.getPhotoById(photoId)?.toDomain()
        }
    }

    override suspend fun getPhotosWithFacesCount(): Int = withContext(ioDispatcher) {
        faceEmbeddingDao.getPhotosWithFaces().size
    }

    override suspend fun getPhotosWithFaces(): List<Photo> = withContext(ioDispatcher) {
        val photoIds = faceEmbeddingDao.getPhotosWithFaces()
        photoIds.mapNotNull { photoId ->
            photoDao.getPhotoById(photoId)?.toDomain()
        }
    }

    override suspend fun getPhotosWithTextCount(): Int = withContext(ioDispatcher) {
        // Text is stored as labels with "text:" prefix in OCR processing
        imageLabelDao.getPhotosWithLabel("text:").size
    }

    override suspend fun getPhotosWithText(): List<Photo> = withContext(ioDispatcher) {
        // Get all labels starting with "text:"
        val allLabels = imageLabelDao.getAllDistinctLabels().first()
        val textLabels = allLabels.filter { it.startsWith("text:") }

        val photoIds = textLabels.flatMap { label ->
            imageLabelDao.getPhotosWithLabel(label).map { it.photoId }
        }.distinct()

        photoIds.mapNotNull { photoId ->
            photoDao.getPhotoById(photoId)?.toDomain()
        }
    }

    override suspend fun getDuplicateGroups(): List<Triple<Photo, List<Photo>, Float>> = withContext(ioDispatcher) {
        val allPhotos = photoDao.getAllPhotosFlow().first()
        val groups = mutableListOf<Triple<Photo, List<Photo>, Float>>()
        val processed = mutableSetOf<Long>()

        allPhotos.forEach { photo ->
            if (photo.id !in processed && photo.perceptualHash != null) {
                val duplicates = allPhotos.filter { other ->
                    other.id != photo.id &&
                    other.perceptualHash != null &&
                    other.id !in processed &&
                    // Calculate similarity using PerceptualHasher
                    calculateHashSimilarity(photo.perceptualHash!!, other.perceptualHash!!) >= 0.90f
                }

                if (duplicates.isNotEmpty()) {
                    processed.add(photo.id)
                    processed.addAll(duplicates.map { it.id })

                    groups.add(
                        Triple(
                            photo.toDomain(),
                            duplicates.map { it.toDomain() },
                            0.90f
                        )
                    )
                }
            }
        }

        groups
    }

    private fun calculateHashSimilarity(hash1: String, hash2: String): Float {
        if (hash1.isEmpty() || hash2.isEmpty() || hash1.length != hash2.length) {
            return 0f
        }

        var hammingDistance = 0
        for (i in hash1.indices) {
            val xor = hexCharToInt(hash1[i]) xor hexCharToInt(hash2[i])
            hammingDistance += Integer.bitCount(xor)
        }

        val maxDistance = hash1.length * 4
        return 1f - (hammingDistance.toFloat() / maxDistance)
    }

    private fun hexCharToInt(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            in 'A'..'F' -> c - 'A' + 10
            else -> 0
        }
    }
}
