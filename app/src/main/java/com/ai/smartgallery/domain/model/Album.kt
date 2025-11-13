package com.ai.smartgallery.domain.model

/**
 * Domain model for Album
 */
data class Album(
    val id: Long = 0,
    val name: String,
    val coverPhotoPath: String? = null,
    val photoCount: Int = 0,
    val isSmartAlbum: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
