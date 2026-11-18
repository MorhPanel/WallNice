package com.morhpanel.nekowall.engine

import android.content.SharedPreferences
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var player: ExoPlayer? = null
        private var surfaceHolder: SurfaceHolder? = null
        private val prefs by lazy {
            getSharedPreferences("nekowall_prefs", MODE_PRIVATE)
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            surfaceHolder = holder
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            initPlayer()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                player?.play()
            } else {
                player?.pause()
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == WallpaperHelper.KEY_VIDEO_URI || key == WallpaperHelper.KEY_IS_MUTED) {
                initPlayer()
            }
        }

        @OptIn(UnstableApi::class)
        private fun initPlayer() {
            val holder = surfaceHolder ?: return
            val videoUriString = WallpaperHelper.getSavedVideoUri(this@VideoLiveWallpaperService)

            if (videoUriString.isNullOrEmpty()) {
                return
            }

            try {
                releasePlayer()

                player = ExoPlayer.Builder(this@VideoLiveWallpaperService).build().apply {
                    setVideoSurfaceHolder(holder)
                    repeatMode = Player.REPEAT_MODE_ALL
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

                    val isMuted = WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)
                    volume = if (isMuted) 0f else 1f

                    val uri = Uri.parse(videoUriString)
                    setMediaItem(MediaItem.fromUri(uri))
                    prepare()

                    if (isVisible) {
                        play()
                    }
                }
            } catch (e: Exception) {
            }
        }

        private fun releasePlayer() {
            player?.apply {
                stop()
                clearVideoSurfaceHolder(surfaceHolder)
                release()
            }
            player = null
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
            super.onDestroy()
        }
    }
}
