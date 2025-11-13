package com.ai.smartgallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ai.smartgallery.data.local.dao.*
import com.ai.smartgallery.data.local.entity.*

/**
 * Main Room database for Smart Gallery
 */
@Database(
    entities = [
        PhotoEntity::class,
        AlbumEntity::class,
        PhotoAlbumCrossRef::class,
        TagEntity::class,
        PhotoTagCrossRef::class,
        PersonEntity::class,
        FaceEmbeddingEntity::class,
        ImageLabelEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun albumDao(): AlbumDao
    abstract fun tagDao(): TagDao
    abstract fun personDao(): PersonDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
    abstract fun imageLabelDao(): ImageLabelDao
}
