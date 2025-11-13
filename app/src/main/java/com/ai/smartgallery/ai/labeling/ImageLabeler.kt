package com.ai.smartgallery.ai.labeling

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Image labeler using ML Kit
 * Detects objects, scenes, and concepts in images
 */
@Singleton
class ImageLabeler @Inject constructor() {

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.7f
    }

    private val options = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
        .build()

    private val labeler = ImageLabeling.getClient(options)

    /**
     * Label an image with detected objects and scenes
     * @param bitmap Input image
     * @return List of labels with confidence scores
     */
    suspend fun labelImage(bitmap: Bitmap): List<ImageLabel> {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val labels = labeler.process(inputImage).await()

            return labels.map { label ->
                ImageLabel(
                    text = label.text,
                    confidence = label.confidence,
                    index = label.index
                )
            }.sortedByDescending { it.confidence }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Categorize photo based on detected labels
     */
    fun categorizePhoto(labels: List<ImageLabel>): PhotoCategory {
        if (labels.isEmpty()) return PhotoCategory.UNCATEGORIZED

        val topLabel = labels.first().text.lowercase()

        return when {
            // People
            topLabel.contains("person") ||
            topLabel.contains("people") ||
            topLabel.contains("human") ||
            topLabel.contains("portrait") -> PhotoCategory.PEOPLE

            // Pets
            topLabel.contains("dog") ||
            topLabel.contains("cat") ||
            topLabel.contains("pet") ||
            topLabel.contains("animal") -> PhotoCategory.PETS

            // Food
            topLabel.contains("food") ||
            topLabel.contains("meal") ||
            topLabel.contains("dish") ||
            topLabel.contains("cuisine") -> PhotoCategory.FOOD

            // Nature
            topLabel.contains("nature") ||
            topLabel.contains("landscape") ||
            topLabel.contains("mountain") ||
            topLabel.contains("tree") ||
            topLabel.contains("flower") ||
            topLabel.contains("plant") -> PhotoCategory.NATURE

            // Travel
            topLabel.contains("travel") ||
            topLabel.contains("beach") ||
            topLabel.contains("vacation") ||
            topLabel.contains("tourism") -> PhotoCategory.TRAVEL

            // Documents
            topLabel.contains("document") ||
            topLabel.contains("text") ||
            topLabel.contains("receipt") ||
            topLabel.contains("paper") -> PhotoCategory.DOCUMENTS

            // Screenshots
            topLabel.contains("screenshot") ||
            topLabel.contains("screen") ||
            topLabel.contains("interface") -> PhotoCategory.SCREENSHOTS

            else -> PhotoCategory.OTHER
        }
    }

    /**
     * Create smart album suggestions based on labels
     */
    fun suggestAlbums(labels: List<ImageLabel>): List<String> {
        val suggestions = mutableSetOf<String>()

        labels.forEach { label ->
            val text = label.text.lowercase()
            when {
                text.contains("food") -> suggestions.add("Food")
                text.contains("pet") || text.contains("dog") || text.contains("cat") ->
                    suggestions.add("Pets")
                text.contains("travel") || text.contains("beach") ->
                    suggestions.add("Travel")
                text.contains("nature") || text.contains("landscape") ->
                    suggestions.add("Nature")
                text.contains("document") || text.contains("receipt") ->
                    suggestions.add("Documents")
            }
        }

        return suggestions.toList()
    }

    /**
     * Clean up resources
     */
    fun close() {
        labeler.close()
    }
}

/**
 * Data class representing an image label
 */
data class ImageLabel(
    val text: String,
    val confidence: Float,
    val index: Int
)

/**
 * Photo categories for organization
 */
enum class PhotoCategory {
    PEOPLE,
    PETS,
    FOOD,
    NATURE,
    TRAVEL,
    DOCUMENTS,
    SCREENSHOTS,
    OTHER,
    UNCATEGORIZED
}
