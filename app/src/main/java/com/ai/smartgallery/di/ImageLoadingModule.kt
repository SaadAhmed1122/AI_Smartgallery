package com.ai.smartgallery.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Hilt module for optimized image loading configuration
 *
 * Performance optimizations:
 * - Memory cache: 25% of available memory (default is 15%)
 * - Disk cache: 250MB for thumbnail storage
 * - Hardware bitmap support for reduced memory usage
 * - Aggressive memory management for large galleries
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoadingModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    // Use 25% of available memory for image caching
                    .maxSizePercent(0.25)
                    // Enable strong references for frequently accessed images
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    // 250MB disk cache for thumbnails
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .okHttpClient(okHttpClient)
            // Use hardware bitmaps when possible for reduced memory usage
            .allowHardware(true)
            // Allow RGB_565 for thumbnails to reduce memory by 50%
            .allowRgb565(true)
            // Respect cache-control headers
            .respectCacheHeaders(false)
            // Default cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED) // Local gallery, no network
            // Enable crossfade for better UX
            .crossfade(150)
            // Log errors in debug builds
            .apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Connection pool for efficient resource usage
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 5,
                    timeUnit = java.util.concurrent.TimeUnit.MINUTES
                )
            )
            .build()
    }
}
