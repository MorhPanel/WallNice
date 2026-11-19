package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var player: ExoPlayer? = null
        private var surfaceHolder: SurfaceHolder? = null
        private val mainHandler = Handler(Looper.getMainLooper())
        private val prefs by lazy {
            getSharedPreferences("nekowall_prefs", MODE_PRIVATE)
        }
        private var audioManager: AudioManager? = null

        private fun runOnMainThread(action: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action()
            } else {
                mainHandler.post(action)
            }
        }

        private fun updateVolumeState() {
            val isUserMuted = WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)
            val am = audioManager
            val inCall = am != null && am.mode != AudioManager.MODE_NORMAL
            val shouldSilence = isUserMuted || (!isVisible && !isPreview) || inCall
            player?.volume = if (shouldSilence) 0f else 1f
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            surfaceHolder = holder
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
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
                val shouldPlay = visible || isPreview
                player?.playWhenReady = shouldPlay
                if (shouldPlay) {
                    player?.play()
                } else {
                    player?.pause()
                }
                updateVolumeState()
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                WallpaperHelper.KEY_IS_MUTED -> {
                    runOnMainThread {
                        updateVolumeState()
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

                        val media3Attrs = Media3AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .setUsage(C.USAGE_MEDIA)
                            .build()
                        setAudioAttributes(media3Attrs, true)

                        val uri = Uri.parse(videoUriString)
                        setMediaItem(MediaItem.fromUri(uri))
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                stop()
                            }
                        })
                        prepare()
                        val shouldPlay = isVisible || isPreview
                        playWhenReady = shouldPlay
                        if (shouldPlay) {
                            play()
                        }
                        updateVolumeState()
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
            audioManager = null
            super.onDestroy()
        }
    }
}
