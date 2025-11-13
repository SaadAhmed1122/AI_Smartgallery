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
    version = 3,
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

        /**
         * Migration from version 2 to 3
         * Adds performance indices for common query patterns
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add indices for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photos_is_deleted ON photos(is_deleted)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photos_is_hidden ON photos(is_hidden)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photos_is_favorite ON photos(is_favorite)")

                // Composite index for most common query pattern
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photos_is_deleted_is_hidden_date_taken ON photos(is_deleted ASC, is_hidden ASC, date_taken DESC)")

                // Index for trash queries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photos_is_deleted_deleted_at ON photos(is_deleted ASC, deleted_at DESC)")
            }
        }
    }
}
