package com.ai.smartgallery.di

import com.ai.smartgallery.data.repository.AlbumRepositoryImpl
import com.ai.smartgallery.data.repository.MediaRepositoryImpl
import com.ai.smartgallery.data.repository.PreferencesRepositoryImpl
import com.ai.smartgallery.data.repository.TagRepositoryImpl
import com.ai.smartgallery.domain.repository.AlbumRepository
import com.ai.smartgallery.domain.repository.MediaRepository
import com.ai.smartgallery.domain.repository.PreferencesRepository
import com.ai.smartgallery.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module binding repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(
        albumRepositoryImpl: AlbumRepositoryImpl
    ): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        tagRepositoryImpl: TagRepositoryImpl
    ): TagRepository
}
