package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Photo entity representing a photo/video in the database
 */
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["date_taken"], orders = [Index.Order.DESC]),
        Index(value = ["path"]),
        Index(value = ["perceptual_hash"]),
        Index(value = ["media_store_id"], unique = true)
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "media_store_id")
    val mediaStoreId: Long,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "date_taken")
    val dateTaken: Long,

    @ColumnInfo(name = "date_modified")
    val dateModified: Long,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "width")
    val width: Int? = null,

    @ColumnInfo(name = "height")
    val height: Int? = null,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "orientation")
    val orientation: Int = 0,

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,

    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "rating")
    val rating: Int = 0,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "is_video")
    val isVideo: Boolean = false,

    @ColumnInfo(name = "duration")
    val duration: Long? = null, // Video duration in milliseconds

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
