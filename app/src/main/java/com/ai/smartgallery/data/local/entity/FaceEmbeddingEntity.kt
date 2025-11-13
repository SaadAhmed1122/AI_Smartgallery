package com.ai.smartgallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Face embedding entity for storing face recognition data
 */
@Entity(
    tableName = "face_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["person_id"])
    ]
)
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "photo_id")
    val photoId: Long,

    @ColumnInfo(name = "person_id")
    val personId: Long? = null,

    @ColumnInfo(name = "embedding_vector", typeAffinity = ColumnInfo.BLOB)
    val embeddingVector: ByteArray,

    @ColumnInfo(name = "face_bounds")
    val faceBounds: String, // JSON: {x, y, width, height}

    @ColumnInfo(name = "confidence")
    val confidence: Float? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEmbeddingEntity

        if (id != other.id) return false
        if (photoId != other.photoId) return false
        if (personId != other.personId) return false
        if (!embeddingVector.contentEquals(other.embeddingVector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + photoId.hashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        result = 31 * result + embeddingVector.contentHashCode()
        return result
    }
}
