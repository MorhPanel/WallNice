package com.morhpanel.nekowall.engine

import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    @OptIn(UnstableApi::class)
    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var player: ExoPlayer? = null
        private var surfaceHolder: SurfaceHolder? = null
        private val mainHandler = Handler(Looper.getMainLooper())
        private val prefs by lazy {
            getSharedPreferences("nekowall_prefs", MODE_PRIVATE)
        }

        private fun runOnMainThread(action: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action()
            } else {
                mainHandler.post(action)
            }
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

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceHolder = holder
            if (width <= 0 || height <= 0) {
                return
            }
            runOnMainThread {
                val activePlayer = player
                if (activePlayer == null) {
                    initPlayer()
                } else {
                    activePlayer.setVideoSurfaceHolder(holder)
                    activePlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            runOnMainThread {
                player?.clearVideoSurfaceHolder(holder)
                releasePlayer()
            }
            surfaceHolder = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            runOnMainThread {
                if (visible) {
                    player?.play()
                } else {
                    player?.pause()
                }
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                WallpaperHelper.KEY_IS_MUTED -> {
                    runOnMainThread {
                        player?.volume = if (WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)) 0f else 1f
                    }
                }
                WallpaperHelper.KEY_VIDEO_URI -> {
                    initPlayer()
                }
            }
        }

        private fun initPlayer() {
            runOnMainThread {
                val holder = surfaceHolder ?: return@runOnMainThread
                val videoUriString = WallpaperHelper.getSavedVideoUri(this@VideoLiveWallpaperService)

                if (videoUriString.isNullOrEmpty() ||
                    videoUriString.startsWith("http://", ignoreCase = true) ||
                    videoUriString.startsWith("https://", ignoreCase = true)
                ) {
                    return@runOnMainThread
                }

                try {
                    releasePlayer()

                    player = ExoPlayer.Builder(this@VideoLiveWallpaperService).build().apply {
                        setVideoSurfaceHolder(holder)
                        repeatMode = Player.REPEAT_MODE_ALL
                        videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

                        val isMuted = WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)
                        volume = if (isMuted) 0f else 1f
                        if (isMuted) {
                            trackSelectionParameters = trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                                .build()
                        }

                        val uri = Uri.parse(videoUriString)
                        setMediaItem(MediaItem.fromUri(uri))
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                stop()
                            }
                        })
                        prepare()

                        if (isVisible) {
                            play()
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }

        private fun releasePlayer() {
            runOnMainThread {
                player?.apply {
                    stop()
                    clearVideoSurfaceHolder(surfaceHolder)
                    release()
                }
                player = null
            }
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            mainHandler.removeCallbacksAndMessages(null)
            player?.apply {
                stop()
                clearVideoSurfaceHolder(surfaceHolder)
                release()
            }
            player = null
            surfaceHolder = null
            super.onDestroy()
        }
    }
}
