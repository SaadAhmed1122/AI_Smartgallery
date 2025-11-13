package com.ai.smartgallery.ai.duplicate

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Perceptual hash generator using difference hash (dHash) algorithm
 * Fast and accurate for finding duplicate and similar images
 */
@Singleton
class PerceptualHasher @Inject constructor() {

    companion object {
        private const val HASH_SIZE = 8 // 8x8 grid = 64-bit hash
        private const val SIMILARITY_THRESHOLD = 0.90f // 90% similarity
    }

    /**
     * Generate perceptual hash for a bitmap using dHash algorithm
     * @param bitmap Input image
     * @return 64-character hex string hash
     */
    suspend fun generateHash(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        try {
            // Step 1: Reduce size to 9x8 (we need 8 vertical differences)
            val resized = Bitmap.createScaledBitmap(
                bitmap,
                HASH_SIZE + 1,
                HASH_SIZE,
                true
            )

            // Step 2: Convert to grayscale and calculate differences
            val differences = mutableListOf<Boolean>()

            for (y in 0 until HASH_SIZE) {
                for (x in 0 until HASH_SIZE) {
                    val leftPixel = resized.getPixel(x, y)
                    val rightPixel = resized.getPixel(x + 1, y)

                    val leftGray = toGrayscale(leftPixel)
                    val rightGray = toGrayscale(rightPixel)

                    // Compare left pixel with right pixel
                    differences.add(leftGray < rightGray)
                }
            }

            // Step 3: Convert boolean array to hex string
            booleanArrayToHex(differences)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Calculate similarity between two hashes (0.0 to 1.0)
     * @return Similarity score (1.0 = identical, 0.0 = completely different)
     */
    fun calculateSimilarity(hash1: String, hash2: String): Float {
        if (hash1.isEmpty() || hash2.isEmpty() || hash1.length != hash2.length) {
            return 0f
        }

        val hammingDistance = calculateHammingDistance(hash1, hash2)
        val maxDistance = hash1.length * 4 // 4 bits per hex character
        return 1f - (hammingDistance.toFloat() / maxDistance)
    }

    /**
     * Check if two images are duplicates based on hash similarity
     */
    fun areDuplicates(hash1: String, hash2: String): Boolean {
        return calculateSimilarity(hash1, hash2) >= SIMILARITY_THRESHOLD
    }

    /**
     * Calculate Hamming distance between two hex hashes
     * (number of differing bits)
     */
    private fun calculateHammingDistance(hash1: String, hash2: String): Int {
        var distance = 0
        for (i in hash1.indices) {
            val xor = hexCharToInt(hash1[i]) xor hexCharToInt(hash2[i])
            distance += Integer.bitCount(xor)
        }
        return distance
    }

    /**
     * Convert pixel to grayscale value
     */
    private fun toGrayscale(pixel: Int): Int {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        // Using luminance formula
        return (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
    }

    /**
     * Convert boolean array to hex string
     */
    private fun booleanArrayToHex(bits: List<Boolean>): String {
        val result = StringBuilder()
        for (i in bits.indices step 4) {
            var value = 0
            for (j in 0 until 4) {
                if (i + j < bits.size && bits[i + j]) {
                    value = value or (1 shl (3 - j))
                }
            }
            result.append(Integer.toHexString(value))
        }
        return result.toString()
    }

    /**
     * Convert hex character to integer
     */
    private fun hexCharToInt(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            in 'A'..'F' -> c - 'A' + 10
            else -> 0
        }
    }
}

/**
 * Data class representing a group of duplicate photos
 */
data class DuplicateGroup(
    val representativePhotoId: Long,
    val duplicatePhotoIds: List<Long>,
    val similarity: Float
)
