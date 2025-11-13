package com.ai.smartgallery.data.model

import com.ai.smartgallery.data.local.entity.AlbumEntity
import com.ai.smartgallery.data.local.entity.PhotoEntity
import com.ai.smartgallery.domain.model.Album
import com.ai.smartgallery.domain.model.Photo

/**
 * Extension function to convert PhotoEntity to domain Photo
 */
fun PhotoEntity.toDomain(): Photo {
    return Photo(
        id = id,
        mediaStoreId = mediaStoreId,
        path = path,
        displayName = displayName,
        dateTaken = dateTaken,
        dateModified = dateModified,
        size = size,
        width = width,
        height = height,
        mimeType = mimeType,
        orientation = orientation,
        latitude = latitude,
        longitude = longitude,
        thumbnailPath = thumbnailPath,
        isFavorite = isFavorite,
        rating = rating,
        isHidden = isHidden,
        isVideo = isVideo,
        duration = duration,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )
}

/**
 * Extension function to convert domain Photo to PhotoEntity
 */
fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        mediaStoreId = mediaStoreId,
        path = path,
        displayName = displayName,
        dateTaken = dateTaken,
        dateModified = dateModified,
        size = size,
        width = width,
        height = height,
        mimeType = mimeType,
        orientation = orientation,
        latitude = latitude,
        longitude = longitude,
        thumbnailPath = thumbnailPath,
        isFavorite = isFavorite,
        rating = rating,
        isHidden = isHidden,
        isVideo = isVideo,
        duration = duration,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )
}

/**
 * Extension function to convert AlbumEntity to domain Album
 */
fun AlbumEntity.toDomain(coverPhotoPath: String? = null): Album {
    return Album(
        id = id,
        name = name,
        coverPhotoPath = coverPhotoPath,
        photoCount = photoCount,
        isSmartAlbum = isSmartAlbum,
        createdAt = createdAt
    )
}

/**
 * Extension function to convert domain Album to AlbumEntity
 */
fun Album.toEntity(coverPhotoId: Long? = null): AlbumEntity {
    return AlbumEntity(
        id = id,
        name = name,
        coverPhotoId = coverPhotoId,
        photoCount = photoCount,
        isSmartAlbum = isSmartAlbum,
        createdAt = createdAt
    )
}
