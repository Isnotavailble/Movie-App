package com.movieapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MovieApplication : Application(), ImageLoaderFactory {

    lateinit var database: com.movieapp.data.local.AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.movieapp.util.LocalizationManager.initialize(this)
        database = com.movieapp.data.local.AppDatabase.getInstance(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100 MB
                    .build()
            }
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .allowRgb565(true)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: MovieApplication
            private set
    }
}
