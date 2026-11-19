package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
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
        private var audioFocusRequest: AudioFocusRequest? = null
        private var hasAudioFocus = false

        private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            runOnMainThread {
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasAudioFocus = true
                        updateVolumeState()
                    }
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        hasAudioFocus = false
                        updateVolumeState()
                    }
                }
            }
        }

        private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                runOnMainThread {
                    updateVolumeState()
                }
            }
        }

        private fun runOnMainThread(action: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action()
            } else {
                mainHandler.post(action)
            }
        }

        private fun isOtherAudioActive(): Boolean {
            val am = audioManager ?: return false
            if (am.mode != AudioManager.MODE_NORMAL) {
                return true
            }
            if (am.isMusicActive) {
                return true
            }
            return false
        }

        private fun requestAudioFocusIfNeeded() {
            val am = audioManager ?: return
            val isUserMuted = WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)
            if (isUserMuted || !isVisible) {
                abandonAudioFocus()
                return
            }
            if (hasAudioFocus) return

            val playbackAttrs = AndroidAudioAttributes.Builder()
                .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
                .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MOVIE)
                .build()

            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttrs)
                .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
                .setWillPauseWhenDucked(true)
                .build()

            audioFocusRequest = focusReq
            val res = am.requestAudioFocus(focusReq)
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        }

        private fun abandonAudioFocus() {
            audioFocusRequest?.let { req ->
                audioManager?.abandonAudioFocusRequest(req)
            }
            audioFocusRequest = null
            hasAudioFocus = false
        }

        private fun updateVolumeState() {
            val isUserMuted = WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)
            if (isUserMuted || !isVisible) {
                abandonAudioFocus()
                player?.volume = 0f
                return
            }

            if (!hasAudioFocus) {
                requestAudioFocusIfNeeded()
            }

            val shouldSilence = !hasAudioFocus || isOtherAudioActive()
            player?.volume = if (shouldSilence) 0f else 1f
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            surfaceHolder = holder
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            try {
                audioManager?.registerAudioPlaybackCallback(playbackCallback, mainHandler)
            } catch (_: Exception) {
            }
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
                    updateVolumeState()
                } else {
                    player?.pause()
                    updateVolumeState()
                }
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
                        setAudioAttributes(media3Attrs, false)

                        val isMuted = WallpaperHelper.isVideoMuted(this@VideoLiveWallpaperService)
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
                            updateVolumeState()
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }

        private fun releasePlayer() {
            runOnMainThread {
                abandonAudioFocus()
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
            try {
                audioManager?.unregisterAudioPlaybackCallback(playbackCallback)
            } catch (_: Exception) {
            }
            abandonAudioFocus()
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
