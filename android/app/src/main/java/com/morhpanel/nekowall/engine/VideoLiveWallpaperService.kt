@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.morhpanel.nekowall.engine

import android.app.WallpaperManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.morhpanel.nekowall.data.models.WallpaperFitMode
import com.morhpanel.nekowall.data.repository.WallNicePreferences
import kotlin.math.abs

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
        private var isManuallyPaused = false
        private var lastTapTime: Long = 0
        private var lastTapX: Int = 0
        private var lastTapY: Int = 0
        private var lastDoubleTapActionTime: Long = 0
        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                runOnMainThread { updatePlaybackState() }
            }
        }

        private val gestureDetector by lazy {
            GestureDetector(this@VideoLiveWallpaperService, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    handleDoubleTap()
                    return true
                }
            })
        }

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
                        abandonAudioFocus()
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

        private fun handleDoubleTap() {
            if (!WallpaperHelper.isDoubleTapToPauseEnabled(this@VideoLiveWallpaperService)) {
                return
            }
            val now = SystemClock.uptimeMillis()
            if (now - lastDoubleTapActionTime < 500) {
                return
            }
            lastDoubleTapActionTime = now
            togglePlaybackPause()
        }

        private fun togglePlaybackPause() {
            runOnMainThread {
                val activePlayer = player ?: return@runOnMainThread
                if (isManuallyPaused) {
                    isManuallyPaused = false
                    if (isVisible || isPreview) {
                        activePlayer.playWhenReady = true
                        activePlayer.play()
                        updateVolumeState()
                    }
                } else {
                    isManuallyPaused = true
                    activePlayer.pause()
                    activePlayer.playWhenReady = false
                    abandonAudioFocus()
                    activePlayer.volume = 0f
                }
            }
        }

        private fun isExternalMusicPlaying(): Boolean {
            val am = audioManager ?: return false
            if (am.mode != AudioManager.MODE_NORMAL) {
                return true
            }
            if (!am.isMusicActive) {
                return false
            }
            try {
                val configs = am.activePlaybackConfigurations
                val isOurPlayerAudible = (player?.volume ?: 0f) > 0f
                if (!isOurPlayerAudible) {
                    return configs.any { config ->
                        val usage = config.audioAttributes.usage
                        usage == android.media.AudioAttributes.USAGE_MEDIA ||
                            usage == android.media.AudioAttributes.USAGE_GAME
                    }
                }
            } catch (_: Exception) {
            }
            return false
        }

        private fun requestAudioFocusIfNeeded() {
            val am = audioManager ?: return
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
            val am = audioManager
            val inCall = am != null && am.mode != AudioManager.MODE_NORMAL
            val isExternalPlaying = isExternalMusicPlaying()
            val shouldSilence = isUserMuted || (!isVisible && !isPreview) || inCall || isExternalPlaying || isManuallyPaused

            if (shouldSilence) {
                abandonAudioFocus()
                player?.volume = 0f
            } else {
                if (!hasAudioFocus) {
                    requestAudioFocusIfNeeded()
                }
                player?.volume = if (hasAudioFocus) 1f else 0f
            }
        }

        private fun isBatteryRestricted(): Boolean {
            if (!WallNicePreferences.shouldPauseOnLowBattery(this@VideoLiveWallpaperService)) return false
            val status = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
            val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (level < 0 || scale <= 0 || plugged) return false
            return level * 100 / scale <= 15
        }

        private fun updatePlaybackState() {
            val shouldPlay = (isVisible || isPreview) && !isManuallyPaused && !isBatteryRestricted()
            player?.playWhenReady = shouldPlay
            if (shouldPlay) {
                player?.play()
            } else {
                player?.pause()
                abandonAudioFocus()
            }
            updateVolumeState()
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(true)
            surfaceHolder = holder
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            try {
                audioManager?.registerAudioPlaybackCallback(playbackCallback, mainHandler)
            } catch (_: Exception) {
            }
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
                    activePlayer.videoScalingMode = videoScalingMode()
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

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (WallpaperHelper.isDoubleTapToPauseEnabled(this@VideoLiveWallpaperService)) {
                gestureDetector.onTouchEvent(event)
            }
        }

        override fun onCommand(
            action: String?,
            x: Int,
            y: Int,
            z: Int,
            extras: Bundle?,
            resultRequested: Boolean
        ): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) {
                val now = SystemClock.uptimeMillis()
                val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout()
                if (now - lastTapTime <= doubleTapTimeout &&
                    abs(x - lastTapX) < 120 &&
                    abs(y - lastTapY) < 120
                ) {
                    handleDoubleTap()
                    lastTapTime = 0
                } else {
                    lastTapTime = now
                    lastTapX = x
                    lastTapY = y
                }
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            runOnMainThread {
                updatePlaybackState()
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                WallpaperHelper.KEY_IS_MUTED -> {
                    runOnMainThread {
                        updateVolumeState()
                    }
                }
                WallpaperHelper.KEY_DOUBLE_TAP_TO_PAUSE -> {
                    runOnMainThread {
                        if (!WallpaperHelper.isDoubleTapToPauseEnabled(this@VideoLiveWallpaperService) && isManuallyPaused) {
                            isManuallyPaused = false
                            if (isVisible || isPreview) {
                                player?.playWhenReady = true
                                player?.play()
                                updateVolumeState()
                            }
                        }
                    }
                }
                WallpaperHelper.KEY_VIDEO_URI,
                WallpaperHelper.KEY_UPDATE_TIMESTAMP,
                WallNicePreferences.KEY_FIT_MODE,
                WallNicePreferences.KEY_PLAYBACK_QUALITY -> {
                    initPlayer()
                }
                WallNicePreferences.KEY_PAUSE_ON_LOW_BATTERY -> updatePlaybackState()
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
                    isManuallyPaused = false

                    player = ExoPlayer.Builder(this@VideoLiveWallpaperService).build().apply {
                        setVideoSurfaceHolder(holder)
                        repeatMode = Player.REPEAT_MODE_ALL
                        videoScalingMode = videoScalingMode()

                        val quality = WallNicePreferences.getPlaybackQuality(this@VideoLiveWallpaperService)
                        if (quality.maxWidth != Int.MAX_VALUE) {
                            trackSelectionParameters = trackSelectionParameters.buildUpon()
                                .setMaxVideoSize(quality.maxWidth, quality.maxHeight)
                                .build()
                        }

                        val media3Attrs = Media3AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .setUsage(C.USAGE_MEDIA)
                            .build()
                        setAudioAttributes(media3Attrs, false)

                        val uri = Uri.parse(videoUriString)
                        setMediaItem(MediaItem.fromUri(uri))
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                stop()
                            }
                        })
                        prepare()
                        val shouldPlay = (isVisible || isPreview) && !isBatteryRestricted()
                        playWhenReady = shouldPlay
                        if (shouldPlay) {
                            play()
                        }
                        updateVolumeState()
                    }
                } catch (_: Exception) {
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

        private fun videoScalingMode(): Int {
            return when (WallNicePreferences.getFitMode(this@VideoLiveWallpaperService)) {
                WallpaperFitMode.CROP -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                WallpaperFitMode.FIT, WallpaperFitMode.BLUR -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            try {
                audioManager?.unregisterAudioPlaybackCallback(playbackCallback)
            } catch (_: Exception) {
            }
            runCatching { unregisterReceiver(batteryReceiver) }
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
