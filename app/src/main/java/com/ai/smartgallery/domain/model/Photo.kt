package com.ai.smartgallery.domain.model

/**
 * Domain model for Photo
 */
data class Photo(
    val id: Long = 0,
    val mediaStoreId: Long,
    val path: String,
    val displayName: String,
    val dateTaken: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String,
    val orientation: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    val isHidden: Boolean = false,
    val isVideo: Boolean = false,
    val duration: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
