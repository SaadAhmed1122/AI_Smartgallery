package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Album entity representing a collection of photos
 */
@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cover_photo_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "cover_photo_id")
    val coverPhotoId: Long? = null,

    @ColumnInfo(name = "photo_count")
    val photoCount: Int = 0,

    @ColumnInfo(name = "is_smart_album")
    val isSmartAlbum: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
