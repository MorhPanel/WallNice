package com.morhpanel.nekowall.engine

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class WallpaperTarget {
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH
}

object WallpaperHelper {

    private const val PREFS_NAME = "nekowall_prefs"
    const val KEY_VIDEO_URI = "current_video_uri"
    const val KEY_IS_MUTED = "is_video_muted"

    fun saveLiveWallpaperConfig(context: Context, videoUri: String, isMuted: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_VIDEO_URI, videoUri)
            .putBoolean(KEY_IS_MUTED, isMuted)
            .apply()
    }

    fun getSavedVideoUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VIDEO_URI, null)
    }

    fun isVideoMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MUTED, true)
    }

    fun openLiveWallpaperChooser(context: Context, videoUri: String, isMuted: Boolean = true) {
        saveLiveWallpaperConfig(context, videoUri, isMuted)

        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, VideoLiveWallpaperService::class.java)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    suspend fun applyStaticWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (target) {
                    WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                manager.setBitmap(bitmap, null, true, flags)
            } else {
                manager.setBitmap(bitmap)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
