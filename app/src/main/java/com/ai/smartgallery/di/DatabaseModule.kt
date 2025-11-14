package com.ai.smartgallery.di

import android.content.Context
import androidx.room.Room
import com.ai.smartgallery.data.local.GalleryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module providing Room database and DAOs
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGalleryDatabase(@ApplicationContext context: Context): GalleryDatabase {
        return Room.databaseBuilder(
            context,
            GalleryDatabase::class.java,
            "smart_gallery_database"
        )
            .addMigrations(
                GalleryDatabase.MIGRATION_1_2,
                GalleryDatabase.MIGRATION_2_3
            )
            // Removed fallbackToDestructiveMigration to persist data across app restarts
            .build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: GalleryDatabase) = database.photoDao()

    @Provides
    @Singleton
    fun provideAlbumDao(database: GalleryDatabase) = database.albumDao()

    @Provides
    @Singleton
    fun provideTagDao(database: GalleryDatabase) = database.tagDao()

    @Provides
    @Singleton
    fun providePersonDao(database: GalleryDatabase) = database.personDao()

    @Provides
    @Singleton
    fun provideFaceEmbeddingDao(database: GalleryDatabase) = database.faceEmbeddingDao()

    @Provides
    @Singleton
    fun provideImageLabelDao(database: GalleryDatabase) = database.imageLabelDao()
}
