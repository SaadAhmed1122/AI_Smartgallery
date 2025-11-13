package com.ai.smartgallery.utils

import android.content.Context
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Image processing utilities for photo editing
 */
@Singleton
class ImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Crop image to specified rectangle
     */
    suspend fun cropImage(
        bitmap: Bitmap,
        cropRect: Rect
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            Bitmap.createBitmap(
                bitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Rotate image by degrees
     */
    suspend fun rotateImage(
        bitmap: Bitmap,
        degrees: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val matrix = Matrix().apply {
                postRotate(degrees)
            }
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Flip image horizontally
     */
    suspend fun flipHorizontal(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            val matrix = Matrix().apply {
                preScale(-1f, 1f)
            }
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Flip image vertically
     */
    suspend fun flipVertical(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            val matrix = Matrix().apply {
                preScale(1f, -1f)
            }
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Apply brightness adjustment (-255 to 255)
     */
    suspend fun adjustBrightness(
        bitmap: Bitmap,
        value: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    set(floatArrayOf(
                        1f, 0f, 0f, 0f, value,
                        0f, 1f, 0f, 0f, value,
                        0f, 0f, 1f, 0f, value,
                        0f, 0f, 0f, 1f, 0f
                    ))
                })
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Apply contrast adjustment (0.5 to 2.0)
     */
    suspend fun adjustContrast(
        bitmap: Bitmap,
        contrast: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val translate = (-.5f * contrast + .5f) * 255f
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    set(floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    ))
                })
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Apply saturation adjustment (0 to 2)
     */
    suspend fun adjustSaturation(
        bitmap: Bitmap,
        saturation: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    setSaturation(saturation)
                })
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Apply grayscale filter
     */
    suspend fun applyGrayscale(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        adjustSaturation(bitmap, 0f)
    }

    /**
     * Apply sepia filter
     */
    suspend fun applySepia(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    set(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                })
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Apply vintage filter
     */
    suspend fun applyVintage(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            var result = adjustContrast(bitmap, 0.9f)
            result = adjustSaturation(result, 0.7f)
            result = adjustBrightness(result, 10f)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Save bitmap to gallery
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        quality: Int = 95
    ): File? = withContext(Dispatchers.IO) {
        try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val galleryDir = File(picturesDir, "SmartGallery")
            if (!galleryDir.exists()) {
                galleryDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "IMG_${timestamp}_edited.jpg"
            val file = File(galleryDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            // Notify media scanner
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Photo filter presets
 */
enum class PhotoFilter {
    NONE,
    GRAYSCALE,
    SEPIA,
    VINTAGE
}

/**
 * Aspect ratio presets for cropping
 */
enum class AspectRatio(val ratio: Float?, val label: String) {
    FREE(null, "Free"),
    SQUARE(1f, "1:1"),
    PORTRAIT_3_4(3f / 4f, "3:4"),
    PORTRAIT_9_16(9f / 16f, "9:16"),
    LANDSCAPE_4_3(4f / 3f, "4:3"),
    LANDSCAPE_16_9(16f / 9f, "16:9")
}
