package com.ai.smartgallery.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ai.smartgallery.ai.duplicate.PerceptualHasher
import com.ai.smartgallery.ai.face.FaceDetector
import com.ai.smartgallery.ai.face.FaceEmbeddingGenerator
import com.ai.smartgallery.ai.labeling.ImageLabeler
import com.ai.smartgallery.ai.ocr.TextRecognizer
import com.ai.smartgallery.data.local.dao.FaceEmbeddingDao
import com.ai.smartgallery.data.local.dao.ImageLabelDao
import com.ai.smartgallery.data.local.dao.PhotoDao
import com.ai.smartgallery.data.local.entity.FaceEmbeddingEntity
import com.ai.smartgallery.data.local.entity.ImageLabelEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Battery-optimized background worker for AI processing of photos
 *
 * Performance & Battery Optimizations:
 * - Downsamples images to max 1024px for ML processing
 * - Checks if photo is already processed to avoid redundant work
 * - Implements batching with delays to prevent CPU throttling
 * - Properly recycles bitmaps to prevent memory leaks
 * - Uses BitmapFactory.Options for memory-efficient loading
 * - Yields between photos to allow for battery-friendly scheduling
 *
 * Use with WorkManager constraints:
 * - setRequiresBatteryNotLow(true)
 * - setRequiresDeviceIdle(true) for bulk processing
 * - setRequiresCharging(true) for non-urgent processing
 */
@HiltWorker
class AIProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoDao: PhotoDao,
    private val faceEmbeddingDao: FaceEmbeddingDao,
    private val imageLabelDao: ImageLabelDao,
    private val perceptualHasher: PerceptualHasher,
    private val faceDetector: FaceDetector,
    private val faceEmbeddingGenerator: FaceEmbeddingGenerator,
    private val imageLabeler: ImageLabeler,
    private val textRecognizer: TextRecognizer
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "ai_processing_work"
        const val KEY_PHOTO_ID = "photo_id"
        const val KEY_PROCESS_TYPE = "process_type"
        const val KEY_BATCH_SIZE = "batch_size"

        const val PROCESS_ALL = "all"
        const val PROCESS_FACES = "faces"
        const val PROCESS_LABELS = "labels"
        const val PROCESS_DUPLICATES = "duplicates"
        const val PROCESS_OCR = "ocr"

        // Max dimension for ML processing (reduces memory usage by ~75%)
        private const val MAX_PROCESSING_DIMENSION = 1024

        // Delay between batch processing to prevent thermal throttling
        private const val BATCH_DELAY_MS = 100L

        // Default batch size for processing
        private const val DEFAULT_BATCH_SIZE = 50
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("AIProcessingWorker", "=== AI Processing Worker Started ===")
        return try {
            val photoId = inputData.getLong(KEY_PHOTO_ID, -1)
            val processType = inputData.getString(KEY_PROCESS_TYPE) ?: PROCESS_ALL
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)

            android.util.Log.d("AIProcessingWorker", "Configuration: photoId=$photoId, processType=$processType, batchSize=$batchSize")

            if (photoId == -1L) {
                // Process all unprocessed photos in batches
                android.util.Log.d("AIProcessingWorker", "Processing all photos...")
                processAllPhotos(processType, batchSize)
            } else {
                // Process specific photo
                android.util.Log.d("AIProcessingWorker", "Processing single photo: $photoId")
                processPhoto(photoId, processType)
            }

            android.util.Log.d("AIProcessingWorker", "=== AI Processing Worker Completed Successfully ===")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AIProcessingWorker", "=== AI Processing Worker Failed ===", e)
            e.printStackTrace()
            // Only retry on recoverable errors
            if (e is OutOfMemoryError) {
                android.util.Log.e("AIProcessingWorker", "Out of memory error - not retrying")
                Result.failure() // Don't retry OOM errors
            } else {
                android.util.Log.w("AIProcessingWorker", "Recoverable error - will retry")
                Result.retry()
            }
        }
    }

    private suspend fun processAllPhotos(processType: String, batchSize: Int) {
        val photos = photoDao.getAllPhotosFlow().first()
        android.util.Log.d("AIProcessingWorker", "Found ${photos.size} total photos to process")
        var processedCount = 0

        photos.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            android.util.Log.d("AIProcessingWorker", "Processing batch $batchIndex (${batch.size} photos)")
            batch.forEachIndexed { index, photo ->
                try {
                    // Skip if already processed (has labels/embeddings)
                    val shouldProcess = when (processType) {
                        PROCESS_LABELS, PROCESS_ALL -> {
                            imageLabelDao.getLabelsForPhoto(photo.id).isEmpty()
                        }
                        PROCESS_FACES -> {
                            faceEmbeddingDao.getFacesInPhoto(photo.id).isEmpty()
                        }
                        PROCESS_DUPLICATES -> {
                            photo.perceptualHash == null
                        }
                        else -> true
                    }

                    if (shouldProcess) {
                        processPhoto(photo.id, processType)
                        processedCount++
                    } else {
                        android.util.Log.d("AIProcessingWorker", "Photo ${photo.id} already processed, skipping")
                    }

                    // Update progress
                    setProgress(
                        workDataOf(
                            "progress" to processedCount,
                            "total" to photos.size,
                            "batch" to batchIndex
                        )
                    )

                    // Yield between photos to allow cancellation and prevent throttling
                    if (index % 5 == 0) {
                        delay(BATCH_DELAY_MS)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with next photo
                }
            }

            // Longer delay between batches
            if (batchIndex < photos.size / batchSize) {
                delay(BATCH_DELAY_MS * 2)
            }
        }

        android.util.Log.d("AIProcessingWorker", "Finished processing all photos. Total processed: $processedCount out of ${photos.size}")
    }

    private suspend fun processPhoto(photoId: Long, processType: String) {
        val photo = photoDao.getPhotoById(photoId) ?: return

        // Load downsampled bitmap for memory efficiency
        val bitmap = loadDownsampledBitmap(photo.path) ?: return

        try {
            when (processType) {
                PROCESS_ALL -> {
                    // Check which tasks need to be done
                    val hasLabels = imageLabelDao.getLabelsForPhoto(photoId).isNotEmpty()
                    val hasFaces = faceEmbeddingDao.getFacesInPhoto(photoId).isNotEmpty()
                    val hasHash = photo.perceptualHash != null

                    if (!hasFaces) processFaces(photoId, bitmap)
                    if (!hasLabels) processLabels(photoId, bitmap)
                    if (!hasHash) processDuplicate(photoId, bitmap)
                    if (!hasLabels) processOCR(photoId, bitmap) // OCR stored as labels
                }
                PROCESS_FACES -> processFaces(photoId, bitmap)
                PROCESS_LABELS -> processLabels(photoId, bitmap)
                PROCESS_DUPLICATES -> processDuplicate(photoId, bitmap)
                PROCESS_OCR -> processOCR(photoId, bitmap)
            }
        } finally {
            // Always recycle bitmap to prevent memory leaks
            bitmap.recycle()
        }
    }

    /**
     * Loads a downsampled bitmap for memory-efficient ML processing
     * Reduces memory usage by ~75% while maintaining accuracy
     */
    private fun loadDownsampledBitmap(path: String): Bitmap? {
        return try {
            // First, decode bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            // Calculate inSampleSize
            val (width, height) = options.outWidth to options.outHeight
            var inSampleSize = 1

            if (width > MAX_PROCESSING_DIMENSION || height > MAX_PROCESSING_DIMENSION) {
                val halfWidth = width / 2
                val halfHeight = height / 2

                while (halfWidth / inSampleSize >= MAX_PROCESSING_DIMENSION &&
                    halfHeight / inSampleSize >= MAX_PROCESSING_DIMENSION
                ) {
                    inSampleSize *= 2
                }
            }

            // Decode with inSampleSize
            options.apply {
                inJustDecodeBounds = false
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                inMutable = false
            }

            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun processFaces(photoId: Long, bitmap: android.graphics.Bitmap) {
        // Detect faces
        val faces = faceDetector.detectFaces(bitmap)

        // Process each detected face
        faces.forEach { face ->
            try {
                // Extract face bitmap
                val faceBitmap = faceDetector.extractFaceBitmap(bitmap, face.bounds)

                if (faceBitmap != null) {
                    // Generate embedding
                    val embedding = faceEmbeddingGenerator.generateEmbedding(faceBitmap)

                    // Convert FloatArray to ByteArray
                    val embeddingBytes = floatArrayToByteArray(embedding)

                    // Store in database
                    val faceEntity = FaceEmbeddingEntity(
                        photoId = photoId,
                        embeddingVector = embeddingBytes,
                        faceBounds = "${face.bounds.left},${face.bounds.top},${face.bounds.right},${face.bounds.bottom}",
                        confidence = face.smilingProbability ?: 0f
                    )

                    faceEmbeddingDao.insertFaceEmbedding(faceEntity)
                    faceBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun processLabels(photoId: Long, bitmap: android.graphics.Bitmap) {
        // Label image
        android.util.Log.d("AIProcessingWorker", "Labeling photo $photoId...")
        val labels = imageLabeler.labelImage(bitmap)
        android.util.Log.d("AIProcessingWorker", "Photo $photoId: Found ${labels.size} labels: ${labels.map { it.text }}")

        // Store labels in database
        val labelEntities = labels.map { label ->
            ImageLabelEntity(
                photoId = photoId,
                label = label.text,
                confidence = label.confidence
            )
        }

        imageLabelDao.insertLabels(labelEntities)
        android.util.Log.d("AIProcessingWorker", "Photo $photoId: Inserted ${labelEntities.size} labels into database")
    }

    private suspend fun processDuplicate(photoId: Long, bitmap: android.graphics.Bitmap) {
        // Generate perceptual hash
        val hash = perceptualHasher.generateHash(bitmap)

        // Update photo with hash
        val photo = photoDao.getPhotoById(photoId)
        if (photo != null) {
            photoDao.updatePhoto(photo.copy(perceptualHash = hash))
        }
    }

    private suspend fun processOCR(photoId: Long, bitmap: android.graphics.Bitmap) {
        // Check if image has significant text
        if (textRecognizer.hasSignificantText(bitmap)) {
            // Extract text
            val text = textRecognizer.extractSearchableText(bitmap)

            // Store as a special label for searching
            if (text.isNotBlank()) {
                val textLabel = ImageLabelEntity(
                    photoId = photoId,
                    label = "text:$text",
                    confidence = 1.0f
                )
                imageLabelDao.insertLabel(textLabel)
            }
        }
    }

    private fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        floats.forEach { value ->
            val bytes = java.nio.ByteBuffer.allocate(4).putFloat(value).array()
            byteBuffer.write(bytes)
        }
        return byteBuffer.toByteArray()
    }
}
