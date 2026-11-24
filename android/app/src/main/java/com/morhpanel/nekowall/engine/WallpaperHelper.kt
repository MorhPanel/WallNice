package com.morhpanel.nekowall.engine

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.morhpanel.nekowall.data.models.WallpaperFitMode
import com.morhpanel.nekowall.data.repository.WallNicePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class WallpaperTarget {
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH
}

object WallpaperHelper {

    private const val PREFS_NAME = WallNicePreferences.PREFS_NAME
    const val KEY_VIDEO_URI = "current_video_uri"
    const val KEY_IS_MUTED = "is_video_muted"
    const val KEY_TARGET = "wallpaper_target"
    const val KEY_UPDATE_TIMESTAMP = "wallpaper_update_timestamp"
    const val KEY_DOUBLE_TAP_TO_PAUSE = "double_tap_to_pause"

    fun saveLiveWallpaperConfig(
        context: Context,
        videoUri: String,
        isMuted: Boolean = false,
        target: WallpaperTarget = WallpaperTarget.BOTH,
        fitMode: WallpaperFitMode = WallNicePreferences.getFitMode(context)
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_VIDEO_URI, videoUri)
            .putBoolean(KEY_IS_MUTED, isMuted)
            .putString(KEY_TARGET, target.name)
            .putString(WallNicePreferences.KEY_FIT_MODE, fitMode.name)
            .putLong(KEY_UPDATE_TIMESTAMP, System.currentTimeMillis())
            .apply()
        WallNicePreferences.setFitMode(context, fitMode)
    }

    fun isLiveWallpaperActive(context: Context): Boolean {
        return try {
            val info = WallpaperManager.getInstance(context).wallpaperInfo ?: return false
            info.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun applyLiveWallpaper(
        context: Context,
        videoUri: String,
        isMuted: Boolean = false,
        target: WallpaperTarget = WallpaperTarget.BOTH,
        fitMode: WallpaperFitMode = WallNicePreferences.getFitMode(context)
    ): Boolean {
        saveLiveWallpaperConfig(context, videoUri, isMuted, target, fitMode)
        if (isLiveWallpaperActive(context)) {
            return true
        }
        openLiveWallpaperChooser(context, videoUri, isMuted, target, fitMode)
        return false
    }

    fun getSavedVideoUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VIDEO_URI, null)
    }

    fun isVideoMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MUTED, false)
    }

    fun isDoubleTapToPauseEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DOUBLE_TAP_TO_PAUSE, false)
    }

    fun setDoubleTapToPauseEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DOUBLE_TAP_TO_PAUSE, enabled).apply()
    }

    fun getSavedTarget(context: Context): WallpaperTarget {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_TARGET, WallpaperTarget.BOTH.name)
        return try {
            WallpaperTarget.valueOf(name ?: WallpaperTarget.BOTH.name)
        } catch (_: Exception) {
            WallpaperTarget.BOTH
        }
    }

    fun openLiveWallpaperChooser(
        context: Context,
        videoUri: String,
        isMuted: Boolean = false,
        target: WallpaperTarget = WallpaperTarget.BOTH,
        fitMode: WallpaperFitMode = WallNicePreferences.getFitMode(context)
    ) {
        saveLiveWallpaperConfig(context, videoUri, isMuted, target, fitMode)

        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, VideoLiveWallpaperService::class.java)
                )
                when (target) {
                    WallpaperTarget.HOME_SCREEN -> {
                        putExtra("SET_LOCKSCREEN_WALLPAPER", false)
                        putExtra("destination", 1)
                    }
                    WallpaperTarget.LOCK_SCREEN -> {
                        putExtra("SET_LOCKSCREEN_WALLPAPER", true)
                        putExtra("destination", 2)
                    }
                    WallpaperTarget.BOTH -> {
                        putExtra("SET_LOCKSCREEN_WALLPAPER", true)
                        putExtra("destination", 3)
                    }
                }
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun applyStaticWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = WallpaperManager.getInstance(context)
            val flags = when (target) {
                WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            manager.setBitmap(bitmap, null, true, flags)
            true
        } catch (e: Exception) {
            false
        }
    }
}
