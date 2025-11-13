package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Image label entity for storing AI-detected labels
 */
@Entity(
    tableName = "image_labels",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["label"])
    ]
)
data class ImageLabelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "photo_id")
    val photoId: Long,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
