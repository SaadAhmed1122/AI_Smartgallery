package com.ai.smartgallery.ai.face

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Face detector using ML Kit
 * Detects faces in images and extracts face information
 */
@Singleton
class FaceDetector @Inject constructor() {

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f) // Minimum face size (15% of image)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(highAccuracyOpts)

    /**
     * Detect faces in a bitmap
     * @param bitmap Input image
     * @return List of detected faces with metadata
     */
    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(inputImage).await()

            return faces.map { face ->
                DetectedFace(
                    bounds = face.boundingBox,
                    trackingId = face.trackingId ?: -1,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ,
                    smilingProbability = face.smilingProbability,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Extract face bitmap from original image
     */
    fun extractFaceBitmap(originalBitmap: Bitmap, faceBounds: Rect): Bitmap? {
        try {
            // Add padding around face
            val padding = 20
            val left = maxOf(0, faceBounds.left - padding)
            val top = maxOf(0, faceBounds.top - padding)
            val width = minOf(
                originalBitmap.width - left,
                faceBounds.width() + padding * 2
            )
            val height = minOf(
                originalBitmap.height - top,
                faceBounds.height() + padding * 2
            )

            if (width > 0 && height > 0) {
                return Bitmap.createBitmap(originalBitmap, left, top, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Clean up resources
     */
    fun close() {
        detector.close()
    }
}

/**
 * Data class representing a detected face
 */
data class DetectedFace(
    val bounds: Rect,
    val trackingId: Int,
    val headEulerAngleX: Float, // Head rotation around X axis
    val headEulerAngleY: Float, // Head rotation around Y axis
    val headEulerAngleZ: Float, // Head rotation around Z axis
    val smilingProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?
)

/**
 * Simple face embeddings generator
 * In production, use FaceNet or MobileFaceNet for better accuracy
 */
@Singleton
class FaceEmbeddingGenerator @Inject constructor() {

    /**
     * Generate a simple embedding vector from face bitmap
     * This is a simplified version - in production use a proper model
     */
    suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray {
        // Resize to standard size (112x112 is common for face recognition)
        val resized = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true)

        // Simple feature extraction (color histogram)
        // In production, use a proper neural network model
        val features = FloatArray(128)

        var index = 0
        for (y in 0 until resized.height step 8) {
            for (x in 0 until resized.width step 8) {
                if (index < features.size) {
                    val pixel = resized.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel) / 255f
                    val g = android.graphics.Color.green(pixel) / 255f
                    val b = android.graphics.Color.blue(pixel) / 255f
                    features[index++] = (r + g + b) / 3f
                }
            }
        }

        return features
    }

    /**
     * Calculate cosine similarity between two embedding vectors
     * @return Similarity score (0.0 to 1.0)
     */
    fun calculateSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val denominator = kotlin.math.sqrt(norm1 * norm2)
        return if (denominator > 0) {
            dotProduct / denominator
        } else {
            0f
        }
    }

    /**
     * Check if two faces belong to the same person
     */
    fun areSamePerson(embedding1: FloatArray, embedding2: FloatArray): Boolean {
        val similarity = calculateSimilarity(embedding1, embedding2)
        return similarity > 0.7f // 70% threshold
    }
}
