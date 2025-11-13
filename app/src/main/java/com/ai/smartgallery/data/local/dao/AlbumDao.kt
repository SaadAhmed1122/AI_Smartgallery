package com.ai.smartgallery.data.local.dao

import androidx.room.*
import com.ai.smartgallery.data.local.entity.AlbumEntity
import com.ai.smartgallery.data.local.entity.PhotoAlbumCrossRef
import com.ai.smartgallery.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing album data
 */
@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums WHERE is_smart_album = 0 ORDER BY created_at DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: Long): AlbumEntity?

    @Query("""
        SELECT photos.* FROM photos
        INNER JOIN photo_albums ON photos.id = photo_albums.photo_id
        WHERE photo_albums.album_id = :albumId
        ORDER BY photos.date_taken DESC
    """)
    fun getPhotosInAlbum(albumId: Long): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbumById(albumId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhotoToAlbum(crossRef: PhotoAlbumCrossRef)

    @Delete
    suspend fun removePhotoFromAlbum(crossRef: PhotoAlbumCrossRef)

    @Query("DELETE FROM photo_albums WHERE photo_id = :photoId AND album_id = :albumId")
    suspend fun removePhotoFromAlbumById(photoId: Long, albumId: Long)

    @Query("""
        SELECT COUNT(*) FROM photo_albums WHERE album_id = :albumId
    """)
    suspend fun getPhotoCountInAlbum(albumId: Long): Int

    @Query("UPDATE albums SET photo_count = :count WHERE id = :albumId")
    suspend fun updatePhotoCount(albumId: Long, count: Int)
}
