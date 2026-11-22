package com.morhpanel.nekowall.data.repository

import android.content.Context
import com.morhpanel.nekowall.data.models.PlaybackQuality
import com.morhpanel.nekowall.data.models.WallpaperFitMode

object WallNicePreferences {
    const val PREFS_NAME = "nekowall_prefs"
    const val KEY_FIT_MODE = "wallpaper_fit_mode"
    const val KEY_PLAYBACK_QUALITY = "playback_quality"
    const val KEY_PAUSE_ON_LOW_BATTERY = "pause_on_low_battery"
    const val KEY_AUTO_PLAY_PREVIEWS = "auto_play_previews"
    const val KEY_ROTATION_ENABLED = "playlist_rotation_enabled"
    const val KEY_ROTATION_INTERVAL_HOURS = "playlist_rotation_interval_hours"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFitMode(context: Context): WallpaperFitMode {
        val value = prefs(context).getString(KEY_FIT_MODE, WallpaperFitMode.CROP.name)
        return runCatching { WallpaperFitMode.valueOf(value.orEmpty()) }.getOrDefault(WallpaperFitMode.CROP)
    }

    fun setFitMode(context: Context, mode: WallpaperFitMode) {
        prefs(context).edit().putString(KEY_FIT_MODE, mode.name).apply()
    }

    fun getPlaybackQuality(context: Context): PlaybackQuality {
        val value = prefs(context).getString(KEY_PLAYBACK_QUALITY, PlaybackQuality.BALANCED.name)
        return runCatching { PlaybackQuality.valueOf(value.orEmpty()) }.getOrDefault(PlaybackQuality.BALANCED)
    }

    fun setPlaybackQuality(context: Context, quality: PlaybackQuality) {
        prefs(context).edit().putString(KEY_PLAYBACK_QUALITY, quality.name).apply()
    }

    fun shouldPauseOnLowBattery(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PAUSE_ON_LOW_BATTERY, true)
    }

    fun setPauseOnLowBattery(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAUSE_ON_LOW_BATTERY, enabled).apply()
    }

    fun shouldAutoPlayPreviews(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_PLAY_PREVIEWS, true)
    }

    fun setAutoPlayPreviews(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_PLAY_PREVIEWS, enabled).apply()
    }

    fun isRotationEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ROTATION_ENABLED, false)
    }

    fun setRotationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ROTATION_ENABLED, enabled).apply()
    }

    fun getRotationIntervalHours(context: Context): Int {
        return prefs(context).getInt(KEY_ROTATION_INTERVAL_HOURS, 6).coerceIn(1, 24)
    }

    fun setRotationIntervalHours(context: Context, hours: Int) {
        prefs(context).edit().putInt(KEY_ROTATION_INTERVAL_HOURS, hours.coerceIn(1, 24)).apply()
    }
}
