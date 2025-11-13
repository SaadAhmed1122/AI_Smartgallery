package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between Photos and Tags
 */
@Entity(
    tableName = "photo_tags",
    primaryKeys = ["photo_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["tag_id"])
    ]
)
data class PhotoTagCrossRef(
    @ColumnInfo(name = "photo_id")
    val photoId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long
)
