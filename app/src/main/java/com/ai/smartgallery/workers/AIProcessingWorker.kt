package com.ai.smartgallery.workers

import android.content.Context
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
import java.io.ByteArrayOutputStream

/**
 * Background worker for AI processing of photos
 * Runs face detection, image labeling, duplicate detection, and OCR
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

        const val PROCESS_ALL = "all"
        const val PROCESS_FACES = "faces"
        const val PROCESS_LABELS = "labels"
        const val PROCESS_DUPLICATES = "duplicates"
        const val PROCESS_OCR = "ocr"
    }

    override suspend fun doWork(): Result {
        return try {
            val photoId = inputData.getLong(KEY_PHOTO_ID, -1)
            val processType = inputData.getString(KEY_PROCESS_TYPE) ?: PROCESS_ALL

            if (photoId == -1L) {
                // Process all unprocessed photos
                processAllPhotos(processType)
            } else {
                // Process specific photo
                processPhoto(photoId, processType)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun processAllPhotos(processType: String) {
        val photos = photoDao.getAllPhotosFlow().first()

        photos.forEachIndexed { index, photo ->
            try {
                processPhoto(photo.id, processType)

                // Update progress
                setProgress(
                    workDataOf(
                        "progress" to (index + 1),
                        "total" to photos.size
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue with next photo
            }
        }
    }

    private suspend fun processPhoto(photoId: Long, processType: String) {
        val photo = photoDao.getPhotoById(photoId) ?: return

        // Load bitmap
        val bitmap = BitmapFactory.decodeFile(photo.path) ?: return

        try {
            when (processType) {
                PROCESS_ALL -> {
                    processFaces(photoId, bitmap)
                    processLabels(photoId, bitmap)
                    processDuplicate(photoId, bitmap)
                    processOCR(photoId, bitmap)
                }
                PROCESS_FACES -> processFaces(photoId, bitmap)
                PROCESS_LABELS -> processLabels(photoId, bitmap)
                PROCESS_DUPLICATES -> processDuplicate(photoId, bitmap)
                PROCESS_OCR -> processOCR(photoId, bitmap)
            }
        } finally {
            bitmap.recycle()
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
        val labels = imageLabeler.labelImage(bitmap)

        // Store labels in database
        val labelEntities = labels.map { label ->
            ImageLabelEntity(
                photoId = photoId,
                label = label.text,
                confidence = label.confidence
            )
        }

        imageLabelDao.insertLabels(labelEntities)
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
