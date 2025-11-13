package com.ai.smartgallery.ai.ocr

import android.graphics.Bitmap
import android.graphics.Point
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text recognizer using ML Kit OCR
 * Extracts text from images for searching
 */
@Singleton
class TextRecognizer @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognize text in an image
     * @param bitmap Input image
     * @return Recognized text result
     */
    suspend fun recognizeText(bitmap: Bitmap): RecognizedText {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()

            val blocks = visionText.textBlocks.map { block ->
                TextBlock(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    cornerPoints = block.cornerPoints?.toList() ?: emptyList(),
                    confidence = 0.0f, // ML Kit doesn't provide per-block confidence
                    recognizedLanguages = block.recognizedLanguages.map { it.languageCode }
                )
            }

            return RecognizedText(
                fullText = visionText.text,
                blocks = blocks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return RecognizedText("", emptyList())
        }
    }

    /**
     * Extract searchable text from image (simplified for indexing)
     */
    suspend fun extractSearchableText(bitmap: Bitmap): String {
        val result = recognizeText(bitmap)
        return result.fullText
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Check if image likely contains text (useful for document detection)
     */
    suspend fun hasSignificantText(bitmap: Bitmap): Boolean {
        val result = recognizeText(bitmap)
        val wordCount = result.fullText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        return wordCount > 10 // More than 10 words suggests document/screenshot
    }

    /**
     * Extract key information from document (receipts, bills, etc.)
     */
    fun extractKeyInfo(recognizedText: RecognizedText): Map<String, String> {
        val info = mutableMapOf<String, String>()
        val text = recognizedText.fullText

        // Extract dates (basic pattern matching)
        val datePattern = Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}""")
        datePattern.find(text)?.let {
            info["date"] = it.value
        }

        // Extract amounts/prices
        val pricePattern = Regex("""\$?\d+\.\d{2}""")
        val prices = pricePattern.findAll(text).map { it.value }.toList()
        if (prices.isNotEmpty()) {
            info["amounts"] = prices.joinToString(", ")
        }

        // Extract phone numbers
        val phonePattern = Regex("""\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}""")
        phonePattern.find(text)?.let {
            info["phone"] = it.value
        }

        // Extract email addresses
        val emailPattern = Regex("""[\w.-]+@[\w.-]+\.\w+""")
        emailPattern.find(text)?.let {
            info["email"] = it.value
        }

        return info
    }

    /**
     * Clean up resources
     */
    fun close() {
        recognizer.close()
    }
}

/**
 * Data class representing recognized text
 */
data class RecognizedText(
    val fullText: String,
    val blocks: List<TextBlock>
)

/**
 * Data class representing a text block
 */
data class TextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val cornerPoints: List<Point>,
    val confidence: Float,
    val recognizedLanguages: List<String>
)
