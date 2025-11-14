package com.ai.smartgallery.data.local.dao

import androidx.room.*
import com.ai.smartgallery.data.local.entity.FaceEmbeddingEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing face embedding data
 */
@Dao
interface FaceEmbeddingDao {

    @Query("SELECT * FROM face_embeddings WHERE photo_id = :photoId")
    suspend fun getFacesInPhoto(photoId: Long): List<FaceEmbeddingEntity>

    @Query("SELECT * FROM face_embeddings WHERE person_id = :personId")
    suspend fun getFacesByPerson(personId: Long): List<FaceEmbeddingEntity>

    @Query("SELECT * FROM face_embeddings WHERE person_id IS NULL")
    suspend fun getUnassignedFaces(): List<FaceEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceEmbedding(face: FaceEmbeddingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceEmbeddings(faces: List<FaceEmbeddingEntity>): List<Long>

    @Update
    suspend fun updateFaceEmbedding(face: FaceEmbeddingEntity)

    @Delete
    suspend fun deleteFaceEmbedding(face: FaceEmbeddingEntity)

    @Query("UPDATE face_embeddings SET person_id = :personId WHERE id = :faceId")
    suspend fun assignFaceToPerson(faceId: Long, personId: Long)

    @Query("DELETE FROM face_embeddings WHERE photo_id = :photoId")
    suspend fun deleteFacesForPhoto(photoId: Long)

    @Query("SELECT COUNT(*) FROM face_embeddings WHERE person_id IS NULL")
    suspend fun getUnassignedFaceCount(): Int

    @Query("SELECT DISTINCT photo_id FROM face_embeddings")
    suspend fun getPhotosWithFaces(): List<Long>
}
