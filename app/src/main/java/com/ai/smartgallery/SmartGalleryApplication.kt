package com.ai.smartgallery

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Smart Gallery
 * Enables Hilt dependency injection throughout the app
 */
@HiltAndroidApp
class SmartGalleryApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("SmartGalleryApp", "Application onCreate() - HiltWorkerFactory: ${if (::workerFactory.isInitialized) "initialized" else "not initialized"}")
    }

    override val workManagerConfiguration: Configuration
        get() {
            android.util.Log.d("SmartGalleryApp", "Providing WorkManager configuration with HiltWorkerFactory")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        }
}
