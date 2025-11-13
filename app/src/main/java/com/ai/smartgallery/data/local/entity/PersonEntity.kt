package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Person entity for face grouping
 */
@Entity(
    tableName = "persons",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["representative_photo_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "representative_photo_id")
    val representativePhotoId: Long? = null,

    @ColumnInfo(name = "face_count")
    val faceCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
