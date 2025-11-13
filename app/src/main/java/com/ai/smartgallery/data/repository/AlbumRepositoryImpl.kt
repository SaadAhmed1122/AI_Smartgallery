package com.ai.smartgallery.data.repository

import com.ai.smartgallery.data.local.dao.AlbumDao
import com.ai.smartgallery.data.local.entity.AlbumEntity
import com.ai.smartgallery.data.local.entity.PhotoAlbumCrossRef
import com.ai.smartgallery.data.model.toDomain
import com.ai.smartgallery.di.IoDispatcher
import com.ai.smartgallery.domain.model.Album
import com.ai.smartgallery.domain.model.Photo
import com.ai.smartgallery.domain.repository.AlbumRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AlbumRepository
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AlbumRepository {

    override fun getAllAlbums(): Flow<List<Album>> {
        return albumDao.getAllAlbums().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAlbumById(albumId: Long): Album? = withContext(ioDispatcher) {
        albumDao.getAlbumById(albumId)?.toDomain()
    }

    override fun getPhotosInAlbum(albumId: Long): Flow<List<Photo>> {
        return albumDao.getPhotosInAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createAlbum(name: String): Long = withContext(ioDispatcher) {
        val album = AlbumEntity(
            name = name,
            isSmartAlbum = false
        )
        albumDao.insertAlbum(album)
    }

    override suspend fun updateAlbum(album: Album) = withContext(ioDispatcher) {
        val entity = AlbumEntity(
            id = album.id,
            name = album.name,
            photoCount = album.photoCount,
            isSmartAlbum = album.isSmartAlbum,
            createdAt = album.createdAt
        )
        albumDao.updateAlbum(entity)
    }

    override suspend fun deleteAlbum(albumId: Long) = withContext(ioDispatcher) {
        albumDao.deleteAlbumById(albumId)
    }

    override suspend fun addPhotoToAlbum(photoId: Long, albumId: Long) = withContext(ioDispatcher) {
        val crossRef = PhotoAlbumCrossRef(photoId, albumId)
        albumDao.addPhotoToAlbum(crossRef)

        // Update photo count
        val count = albumDao.getPhotoCountInAlbum(albumId)
        albumDao.updatePhotoCount(albumId, count)
    }

    override suspend fun removePhotoFromAlbum(photoId: Long, albumId: Long) = withContext(ioDispatcher) {
        albumDao.removePhotoFromAlbumById(photoId, albumId)

        // Update photo count
        val count = albumDao.getPhotoCountInAlbum(albumId)
        albumDao.updatePhotoCount(albumId, count)
    }

    override suspend fun createSmartAlbums() = withContext(ioDispatcher) {
        // Create predefined smart albums
        val smartAlbums = listOf(
            AlbumEntity(name = "Favorites", isSmartAlbum = true),
            AlbumEntity(name = "Videos", isSmartAlbum = true),
            AlbumEntity(name = "Screenshots", isSmartAlbum = true),
            AlbumEntity(name = "Camera", isSmartAlbum = true)
        )

        smartAlbums.forEach { album ->
            albumDao.insertAlbum(album)
        }
    }
}
