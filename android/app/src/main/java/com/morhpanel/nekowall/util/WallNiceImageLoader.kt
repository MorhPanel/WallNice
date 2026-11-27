package com.morhpanel.nekowall.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

object WallNiceImageLoader {

    private const val PREFS_NAME = "nekowall_prefs"
    private const val KEY_CACHE_QUOTA_MB = "cache_quota_mb"
    const val DEFAULT_CACHE_QUOTA_MB = 200
    val CACHE_QUOTA_PRESETS_MB = listOf(50, 100, 200, 500, 1024, 2560, 5120, 10240)

    @Volatile
    private var instance: ImageLoader? = null

    fun getCacheQuotaMb(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getInt(KEY_CACHE_QUOTA_MB, DEFAULT_CACHE_QUOTA_MB)
        return if (stored in CACHE_QUOTA_PRESETS_MB) stored else DEFAULT_CACHE_QUOTA_MB
    }

    fun updateCacheQuota(context: Context, quotaMb: Int) {
        val validQuota = if (quotaMb in CACHE_QUOTA_PRESETS_MB) quotaMb else DEFAULT_CACHE_QUOTA_MB
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CACHE_QUOTA_MB, validQuota)
            .apply()
        resetImageLoader()
    }

    fun resetImageLoader() {
        synchronized(this) {
            instance = null
        }
    }

    @SuppressLint("NewApi")
    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            val quotaBytes = getCacheQuotaMb(context).toLong() * 1024L * 1024L
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    add(VideoFrameDecoder.Factory())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("coil_media_cache"))
                        .maxSizeBytes(quotaBytes)
                        .build()
                }
                .build().also { instance = it }
        }
    }
}
