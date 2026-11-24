@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.morhpanel.nekowall.ui.screens

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.morhpanel.nekowall.data.repository.FavoritesManager
import java.io.File
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperFitMode
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.data.repository.PlaylistManager
import com.morhpanel.nekowall.data.repository.WallNicePreferences
import com.morhpanel.nekowall.engine.DownloadHelper
import com.morhpanel.nekowall.engine.ReportHelper
import com.morhpanel.nekowall.engine.WallpaperBitmapProcessor
import com.morhpanel.nekowall.engine.WallpaperHelper
import com.morhpanel.nekowall.engine.WallpaperTarget
import com.morhpanel.nekowall.ui.components.HeartBurstButton
import com.morhpanel.nekowall.ui.components.NavigationTab
import com.morhpanel.nekowall.ui.components.ScanlineSuccessOverlay
import com.morhpanel.nekowall.ui.components.SlidingSegmentedPill
import com.morhpanel.nekowall.ui.components.WallNiceBottomNavigation
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineStrong
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceHigh
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.SurfaceMid
import com.morhpanel.nekowall.util.WallNiceImageLoader
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSizes
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PreviewOverlayMode {
    CLEAN,
    LOCK_SCREEN,
    HOME_SCREEN
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PreviewScreen(
    wallpaper: WallpaperItem,
    currentTab: NavigationTab = NavigationTab.HOME,
    onBack: () -> Unit,
    onNavigateTab: (NavigationTab) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isUiHidden by remember { mutableStateOf(false) }
    var downloadToastMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var isMuted by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var showFullscreenPreview by remember { mutableStateOf(false) }
    var showScanline by remember { mutableStateOf(false) }
    var videoPlaybackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var videoError by remember { mutableStateOf(false) }
    var fitMode by remember(wallpaper.id) {
        mutableStateOf(WallNicePreferences.getFitMode(context))
    }
    var isInPlaylist by remember { mutableStateOf(PlaylistManager.contains(context, wallpaper.id)) }
    var isPlaylistBusy by remember { mutableStateOf(false) }

    val favoriteIds by FavoritesManager.favoriteIds
    val isFavorite = favoriteIds.contains(wallpaper.id)
    val extension = when (wallpaper.mediaType) {
        MediaType.VIDEO -> "mp4"
        MediaType.GIF -> "gif"
        MediaType.IMAGE -> "jpg"
    }
    val cachedFile = remember(wallpaper.url) {
        DownloadHelper.getCachedFile(context, wallpaper.url, extension)
    }

    BackHandler(enabled = !showFullscreenPreview, onBack = onBack)
    BackHandler(enabled = showFullscreenPreview) {
        showFullscreenPreview = false
    }

    val exoPlayer = remember(wallpaper.url, wallpaper.mediaType, cachedFile) {
        if (wallpaper.mediaType == MediaType.VIDEO) {
            ExoPlayer.Builder(context).build().apply {
                val mediaUri = cachedFile?.let { Uri.fromFile(it) } ?: Uri.parse(wallpaper.url)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
                setAudioAttributes(audioAttributes, false)
                setMediaItem(MediaItem.fromUri(mediaUri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isMuted) 0f else 1f
                val quality = WallNicePreferences.getPlaybackQuality(context)
                if (quality.maxWidth != Int.MAX_VALUE) {
                    trackSelectionParameters = trackSelectionParameters.buildUpon()
                        .setMaxVideoSize(quality.maxWidth, quality.maxHeight)
                        .build()
                }
                prepare()
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
        } else {
            null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                videoPlaybackState = playbackState
            }

            override fun onPlayerError(error: PlaybackException) {
                videoError = true
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer?.playWhenReady = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> exoPlayer?.playWhenReady = false
                else -> Unit
            }
        }
        exoPlayer?.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(fitMode, exoPlayer) {
        exoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
    }

    fun applyWallpaperDirect(target: WallpaperTarget) {
        if (isApplying) return
        if (wallpaper.mediaType == MediaType.VIDEO) {
            coroutineScope.launch {
                isApplying = true
                val readyFile = cachedFile ?: DownloadHelper.downloadToCache(
                    context = context,
                    fileUrl = wallpaper.url,
                    extension = "mp4"
                )
                isApplying = false
                if (readyFile != null) {
                    try {
                        val liveDir = File(context.filesDir, "live_wallpaper")
                        if (!liveDir.exists()) {
                            liveDir.mkdirs()
                        }
                        val permanentFile = File(liveDir, "live_wp_${System.currentTimeMillis()}.mp4")
                        readyFile.copyTo(permanentFile, overwrite = true)
                        liveDir.listFiles()?.forEach { oldFile ->
                            if (oldFile.name != permanentFile.name) {
                                oldFile.delete()
                            }
                        }
                        val videoUri = Uri.fromFile(permanentFile).toString()
                        showScanline = true
                        val appliedDirectly = WallpaperHelper.applyLiveWallpaper(
                            context = context,
                            videoUri = videoUri,
                            isMuted = isMuted,
                            target = target,
                            fitMode = fitMode
                        )
                        snackbarHostState.showSnackbar(
                            if (appliedDirectly) "Wallpaper applied" else "Confirm engine in system dialog"
                        )
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Unable to save live wallpaper.")
                    }
                } else {
                    snackbarHostState.showSnackbar("Unable to download live wallpaper.")
                }
            }
        } else {
            coroutineScope.launch {
                isApplying = true
                val success = loadAndApplyWallpaper(
                    context = context,
                    url = wallpaper.url,
                    target = target,
                    fitMode = fitMode
                )
                isApplying = false
                if (success) {
                    showScanline = true
                }
                snackbarHostState.showSnackbar(
                    if (success) "Wallpaper applied" else "Couldn’t apply wallpaper"
                )
            }
        }
    }

    val sharedMediaModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = "wallpaper_bounds_${wallpaper.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        if (!showFullscreenPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(sharedMediaModifier)
                    .clickable { isUiHidden = !isUiHidden }
            ) {
                WallpaperMedia(
                    wallpaper = wallpaper,
                    exoPlayer = exoPlayer,
                    videoPlaybackState = videoPlaybackState,
                    videoError = videoError,
                    cachedFile = cachedFile,
                    fitMode = fitMode
                )
            }

            AnimatedVisibility(
                visible = !isUiHidden,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                PreviewTopBar(
                    isFavorite = isFavorite,
                    isDownloading = isDownloading,
                    onToggleFavorite = {
                        val nowFav = FavoritesManager.toggleFavorite(context, wallpaper.id)
                        if (nowFav) {
                            coroutineScope.launch {
                                DownloadHelper.downloadFavorite(context, wallpaper.url, extension)
                            }
                        } else {
                            coroutineScope.launch {
                                DownloadHelper.removeFavoriteFile(context, wallpaper.url, extension)
                            }
                        }
                    },
                    onDownload = {
                        if (!isDownloading) {
                            coroutineScope.launch {
                                isDownloading = true
                                val success = DownloadHelper.saveToGallery(
                                    context = context,
                                    fileUrl = wallpaper.url,
                                    title = wallpaper.title,
                                    isVideo = wallpaper.mediaType == MediaType.VIDEO
                                )
                                isDownloading = false
                                if (success) {
                                    val targetPath = if (wallpaper.mediaType == MediaType.VIDEO) {
                                        "/storage/emulated/0/Movies/WallNice"
                                    } else {
                                        "/storage/emulated/0/Pictures/WallNice"
                                    }
                                    downloadToastMessage = "Wallpaper downloaded successfully to $targetPath"
                                    kotlinx.coroutines.delay(3500)
                                    downloadToastMessage = null
                                } else {
                                    snackbarHostState.showSnackbar("Unable to save to gallery.")
                                }
                            }
                        }
                    },
                    onOpenInfo = { showInfoDialog = true },
                    onBack = onBack
                )
            }

            AnimatedVisibility(
                visible = downloadToastMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 20.dp, end = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.88f))
                        .border(1.dp, OutlineStrong, RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = downloadToastMessage ?: "",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(
                visible = !isUiHidden,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.32f to Color.Black.copy(alpha = 0.72f),
                                    1f to Color.Black
                                )
                            )
                        )
                ) {
                    PreviewDualActionButtons(
                        isApplying = isApplying,
                        onSetAsWallpaper = { applyWallpaperDirect(WallpaperTarget.HOME_SCREEN) },
                        onSetAsLockScreen = { applyWallpaperDirect(WallpaperTarget.LOCK_SCREEN) }
                    )
                    WallNiceBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            onNavigateTab(tab)
                            onBack()
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        start = WallNiceSpacing.md,
                        end = WallNiceSpacing.md,
                        bottom = 150.dp
                    )
            )

            if (showInfoDialog) {
                WallpaperInfoDialog(
                    wallpaper = wallpaper,
                    fitMode = fitMode,
                    isInPlaylist = isInPlaylist,
                    isPlaylistBusy = isPlaylistBusy,
                    isMuted = isMuted,
                    onMuteToggle = { isMuted = !isMuted },
                    onFitModeToggle = {
                        val supportedModes = WallpaperFitMode.entries
                        val nextIndex = (supportedModes.indexOf(fitMode).coerceAtLeast(0) + 1) % supportedModes.size
                        fitMode = supportedModes[nextIndex]
                        WallNicePreferences.setFitMode(context, fitMode)
                    },
                    onPlaylistToggle = {
                        if (!isPlaylistBusy) {
                            coroutineScope.launch {
                                isPlaylistBusy = true
                                val success = try {
                                    runCatching {
                                        if (isInPlaylist) {
                                            PlaylistManager.remove(context, wallpaper.id)
                                        } else {
                                            PlaylistManager.add(context, wallpaper, fitMode)
                                        }
                                    }.getOrDefault(false)
                                } finally {
                                    isPlaylistBusy = false
                                }
                                if (success) isInPlaylist = !isInPlaylist
                            }
                        }
                    },
                    onOpenFullscreen = {
                        showInfoDialog = false
                        showFullscreenPreview = true
                    },
                    onDismiss = { showInfoDialog = false }
                )
            }
        }

        if (showFullscreenPreview) {
            FullscreenMediaViewer(
                wallpaper = wallpaper,
                exoPlayer = exoPlayer,
                videoPlaybackState = videoPlaybackState,
                videoError = videoError,
                cachedFile = cachedFile,
                fitMode = fitMode,
                onBack = { showFullscreenPreview = false }
            )
        }

        ScanlineSuccessOverlay(
            trigger = showScanline,
            onFinished = { showScanline = false }
        )
    }
}

@Composable
private fun FullscreenMediaViewer(
    wallpaper: WallpaperItem,
    exoPlayer: ExoPlayer?,
    videoPlaybackState: Int,
    videoError: Boolean,
    cachedFile: File?,
    fitMode: WallpaperFitMode,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        WallpaperMedia(
            wallpaper = wallpaper,
            exoPlayer = exoPlayer,
            videoPlaybackState = videoPlaybackState,
            videoError = videoError,
            cachedFile = cachedFile,
            fitMode = fitMode,
            fitContent = true
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(WallNiceSpacing.md)
                .size(WallNiceSizes.minimumTouchTarget)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back from fullscreen preview",
                tint = Color.White
            )
        }
    }
}

private fun createTexturePlayerView(context: Context): PlayerView {
    val playerView = PlayerView(context)
    val oldView = playerView.videoSurfaceView
    val parent = oldView?.parent as? ViewGroup
    if (parent != null && oldView !is TextureView) {
        val index = parent.indexOfChild(oldView)
        val lp = oldView.layoutParams
        parent.removeView(oldView)
        val textureView = TextureView(context).apply {
            layoutParams = lp
        }
        parent.addView(textureView, if (index >= 0) index else 0)
        try {
            val field = PlayerView::class.java.getDeclaredField("surfaceView")
            field.isAccessible = true
            field.set(playerView, textureView)
        } catch (_: Exception) {
        }
    }
    return playerView
}

@Composable
private fun WallpaperMedia(
    wallpaper: WallpaperItem,
    exoPlayer: ExoPlayer?,
    videoPlaybackState: Int,
    videoError: Boolean,
    cachedFile: File? = null,
    fitMode: WallpaperFitMode = WallpaperFitMode.BLUR,
    fitContent: Boolean = false
) {
    val context = LocalContext.current
    val effectiveMode = if (fitContent) WallpaperFitMode.FIT else fitMode

    when (wallpaper.mediaType) {
        MediaType.VIDEO -> {
            Box(modifier = Modifier.fillMaxSize()) {
                if (effectiveMode == WallpaperFitMode.BLUR) {
                    WallpaperPoster(
                        wallpaper = wallpaper,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.15f
                                scaleY = 1.15f
                            }
                            .blur(36.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                } else {
                    WallpaperPoster(
                        wallpaper = wallpaper,
                        contentScale = if (effectiveMode == WallpaperFitMode.FIT) ContentScale.Fit else ContentScale.Crop
                    )
                }
                AndroidView(
                    factory = { viewContext ->
                        createTexturePlayerView(viewContext).apply {
                            player = exoPlayer
                            useController = false
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            resizeMode = when (effectiveMode) {
                                WallpaperFitMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                WallpaperFitMode.FIT, WallpaperFitMode.BLUR -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                        view.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        view.resizeMode = when (effectiveMode) {
                            WallpaperFitMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            WallpaperFitMode.FIT, WallpaperFitMode.BLUR -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxSize()
                )
                when {
                    videoError -> MediaErrorState()
                    videoPlaybackState != Player.STATE_READY -> MediaLoadingState()
                }
            }
        }

        MediaType.GIF -> {
            val imageLoader = remember { WallNiceImageLoader.get(context) }
            WallpaperImage(
                wallpaper = wallpaper,
                imageLoader = imageLoader,
                cachedFile = cachedFile,
                fitMode = effectiveMode
            )
        }

        MediaType.IMAGE -> {
            WallpaperImage(
                wallpaper = wallpaper,
                cachedFile = cachedFile,
                fitMode = effectiveMode
            )
        }
    }
}

@Composable
private fun WallpaperPoster(
    wallpaper: WallpaperItem,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(wallpaper.thumbnailUrl.ifBlank { wallpaper.url })
            .crossfade(180)
            .build(),
        contentDescription = wallpaper.title,
        contentScale = contentScale,
        modifier = modifier.fillMaxSize(),
        loading = {
            Box(modifier = Modifier.fillMaxSize().background(AmoledBlack))
        },
        error = {
            Box(modifier = Modifier.fillMaxSize().background(AmoledBlack))
        },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
private fun WallpaperImage(
    wallpaper: WallpaperItem,
    imageLoader: ImageLoader? = null,
    cachedFile: File? = null,
    fitMode: WallpaperFitMode = WallpaperFitMode.BLUR
) {
    val context = LocalContext.current
    val model = ImageRequest.Builder(context)
        .data(cachedFile ?: wallpaper.url)
        .crossfade(220)
        .build()

    val contentScale = if (fitMode == WallpaperFitMode.CROP) ContentScale.Crop else ContentScale.Fit
    val content: @Composable (Modifier, ContentScale) -> Unit = { modifier, scale ->
        SubcomposeAsyncImage(
            model = model,
            imageLoader = imageLoader ?: WallNiceImageLoader.get(context),
            contentDescription = wallpaper.title,
            contentScale = scale,
            modifier = modifier,
            loading = { MediaLoadingState() },
            error = { MediaErrorState() },
            success = { SubcomposeAsyncImageContent() }
        )
    }

    if (fitMode == WallpaperFitMode.BLUR) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                    }
                    .blur(30.dp),
                ContentScale.Crop
            )
            content(Modifier.fillMaxSize(), ContentScale.Fit)
        }
    } else {
        content(Modifier.fillMaxSize(), contentScale)
    }
}

@Composable
private fun MediaLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = NeonPink,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun MediaErrorState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(WallNiceSpacing.sm))
        Text(
            text = "Preview unavailable",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PreviewTopBar(
    isFavorite: Boolean,
    isDownloading: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onOpenInfo: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = WallNiceSpacing.md,
                vertical = WallNiceSpacing.xs
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(WallNiceSizes.minimumTouchTarget)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.72f))
                .border(1.dp, OutlineSubtle, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeartBurstButton(
                isFavorite = isFavorite,
                onClick = onToggleFavorite
            )

            IconButton(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier
                    .size(WallNiceSizes.minimumTouchTarget)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, OutlineSubtle, CircleShape)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download wallpaper",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier
                    .size(WallNiceSizes.minimumTouchTarget)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, OutlineSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Wallpaper details",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewDualActionButtons(
    isApplying: Boolean,
    onSetAsWallpaper: () -> Unit,
    onSetAsLockScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onSetAsWallpaper,
            enabled = !isApplying,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38363F),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Set as Wallpaper",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Button(
            onClick = onSetAsLockScreen,
            enabled = !isApplying,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE2DFE6),
                contentColor = Color(0xFF1E1A22)
            )
        ) {
            Text(
                text = "Set as Lock Screen",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
@Composable
private fun WallpaperInfoDialog(
    wallpaper: WallpaperItem,
    fitMode: WallpaperFitMode,
    isInPlaylist: Boolean,
    isPlaylistBusy: Boolean,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    onFitModeToggle: () -> Unit,
    onPlaylistToggle: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OutlineStrong, RoundedCornerShape(WallNiceRadii.large)),
            shape = RoundedCornerShape(WallNiceRadii.large),
            color = SurfaceMid
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wallpaper.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "@" + wallpaper.author + "  •  " + wallpaper.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onOpenFullscreen,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Open fullscreen preview",
                            tint = NeonCyan
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Resolution", color = TextSecondary)
                    Text(wallpaper.resolution, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Format", color = TextSecondary)
                    Text(wallpaper.mediaType.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLow)
                        .clickable(onClick = onFitModeToggle)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display Fit Mode", color = TextPrimary)
                    Text(fitMode.label, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLow)
                        .clickable(enabled = !isPlaylistBusy, onClick = onPlaylistToggle)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rotation Playlist", color = TextPrimary)
                    Text(
                        if (isInPlaylist) "In Playlist" else "Add to Playlist",
                        color = if (isInPlaylist) NeonPink else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (wallpaper.mediaType == MediaType.VIDEO) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLow)
                            .clickable(onClick = onMuteToggle)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Sound", color = TextPrimary)
                        Text(
                            if (isMuted) "Muted" else "Enabled",
                            color = if (isMuted) TextSecondary else NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = NeonPink)
                }
            }
        }
    }
}

@Composable
private fun ApplyTargetDialog(
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    OutlineStrong,
                    RoundedCornerShape(WallNiceRadii.large)
                ),
            shape = RoundedCornerShape(WallNiceRadii.large),
            color = SurfaceMid
        ) {
            Column(modifier = Modifier.padding(WallNiceSpacing.xl)) {
                Text(
                    text = "Apply wallpaper",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(WallNiceSpacing.xs))
                Text(
                    text = "Choose where this wallpaper should appear.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(WallNiceSpacing.lg))

                ApplyTargetOption(
                    icon = Icons.Default.Home,
                    title = "Home screen",
                    description = "Behind your apps and widgets",
                    enabled = !isApplying,
                    onClick = { onTargetSelected(WallpaperTarget.HOME_SCREEN) }
                )
                Spacer(Modifier.height(WallNiceSpacing.xs))
                ApplyTargetOption(
                    icon = Icons.Default.Lock,
                    title = "Lock screen",
                    description = "Shown before unlocking",
                    enabled = !isApplying,
                    onClick = { onTargetSelected(WallpaperTarget.LOCK_SCREEN) }
                )
                Spacer(Modifier.height(WallNiceSpacing.xs))
                ApplyTargetOption(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Both screens",
                    description = "Use one wallpaper everywhere",
                    enabled = !isApplying,
                    onClick = { onTargetSelected(WallpaperTarget.BOTH) }
                )

                Spacer(Modifier.height(WallNiceSpacing.sm))
                TextButton(
                    onClick = onDismiss,
                    enabled = !isApplying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WallNiceSizes.minimumTouchTarget)
                ) {
                    Text(
                        text = "Cancel",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplyTargetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WallNiceRadii.medium))
            .background(SurfaceLow)
            .border(
                1.dp,
                OutlineSubtle,
                RoundedCornerShape(WallNiceRadii.medium)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(WallNiceSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonPink,
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.width(WallNiceSpacing.sm))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private suspend fun loadAndApplyWallpaper(
    context: android.content.Context,
    url: String,
    target: WallpaperTarget,
    fitMode: WallpaperFitMode
): Boolean {
    return try {
        val extension = if (url.endsWith(".gif", ignoreCase = true)) "gif" else "jpg"
        val cached = DownloadHelper.getCachedFile(context, url, extension)
        val loader = WallNiceImageLoader.get(context)
        val request = ImageRequest.Builder(context)
            .data(cached ?: url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        val bitmap = (result as? SuccessResult)
            ?.drawable
            ?.let { it as? BitmapDrawable }
            ?.bitmap
            ?: return false
        val processed = WallpaperBitmapProcessor.processForDevice(context, bitmap, fitMode)
        val success = WallpaperHelper.applyStaticWallpaper(context, processed, target)
        if (processed !== bitmap) processed.recycle()
        success
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun LockScreenSimulation() {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val now = remember { Date() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(WallNiceSpacing.md))
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light
        )
        Text(
            text = dateFormat.format(now),
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun HomeScreenSimulation() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = WallNiceSpacing.xl, vertical = 72.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(WallNiceRadii.pill),
                color = Color.Black.copy(alpha = 0.42f)
            ) {
                Text(
                    text = "13:37  •  72°F",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(
                        horizontal = WallNiceSpacing.lg,
                        vertical = WallNiceSpacing.sm
                    )
                )
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(WallNiceRadii.small))
                            .background(Color.White.copy(alpha = 0.24f))
                    )
                }
            }
            Spacer(Modifier.height(WallNiceSpacing.md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(WallNiceRadii.large))
                    .background(Color.Black.copy(alpha = 0.48f))
            )
        }
    }
}
