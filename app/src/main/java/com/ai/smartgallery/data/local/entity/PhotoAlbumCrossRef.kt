package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between Photos and Albums
 */
@Entity(
    tableName = "photo_albums",
    primaryKeys = ["photo_id", "album_id"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["album_id"])
    ]
)
data class PhotoAlbumCrossRef(
    @ColumnInfo(name = "photo_id")
    val photoId: Long,

    @ColumnInfo(name = "album_id")
    val albumId: Long
)
