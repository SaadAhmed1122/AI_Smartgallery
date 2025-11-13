package com.ai.smartgallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun albumDao(): AlbumDao
    abstract fun tagDao(): TagDao
    abstract fun personDao(): PersonDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
    abstract fun imageLabelDao(): ImageLabelDao

    companion object {
        /**
         * Migration from version 1 to 2
         * Adds trash/recovery system fields to photos table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add is_deleted and deleted_at columns to photos table
                database.execSQL("ALTER TABLE photos ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE photos ADD COLUMN deleted_at INTEGER")
            }
        }
    }
}
