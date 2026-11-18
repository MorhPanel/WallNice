package com.morhpanel.nekowall.ui.screens

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
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
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.engine.DownloadHelper
import com.morhpanel.nekowall.engine.WallpaperHelper
import com.morhpanel.nekowall.engine.WallpaperTarget
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineStrong
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceHigh
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.SurfaceMid
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

@OptIn(UnstableApi::class)
@Composable
fun PreviewScreen(
    wallpaper: WallpaperItem,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var overlayMode by remember { mutableStateOf(PreviewOverlayMode.CLEAN) }
    var isMuted by remember { mutableStateOf(true) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var videoPlaybackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var videoError by remember { mutableStateOf(false) }

    val exoPlayer = remember(wallpaper.url, wallpaper.mediaType) {
        if (wallpaper.mediaType == MediaType.VIDEO) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(wallpaper.url)))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                prepare()
                playWhenReady = true
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                videoPlaybackState = playbackState
            }

            override fun onPlayerError(error: PlaybackException) {
                videoError = true
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    val isBusy = isApplying

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WallpaperMedia(
            wallpaper = wallpaper,
            exoPlayer = exoPlayer,
            videoPlaybackState = videoPlaybackState,
            videoError = videoError
        )

        when (overlayMode) {
            PreviewOverlayMode.CLEAN -> Unit
            PreviewOverlayMode.LOCK_SCREEN -> LockScreenSimulation()
            PreviewOverlayMode.HOME_SCREEN -> HomeScreenSimulation()
        }

        PreviewTopBar(
            selectedMode = overlayMode,
            onModeSelected = { overlayMode = it },
            onBack = onBack
        )

        PreviewBottomPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            wallpaper = wallpaper,
            isMuted = isMuted,
            isApplying = isApplying,
            onMuteToggle = { isMuted = !isMuted },
            onApply = {
                if (!isBusy) {
                    if (wallpaper.mediaType == MediaType.VIDEO) {
                        coroutineScope.launch {
                            isApplying = true
                            val cachedFile = DownloadHelper.downloadToCache(
                                context = context,
                                fileUrl = wallpaper.url,
                                extension = "mp4"
                            )
                            isApplying = false
                            val videoUri = cachedFile?.let { Uri.fromFile(it).toString() }
                                ?: wallpaper.url
                            WallpaperHelper.openLiveWallpaperChooser(
                                context = context,
                                videoUri = videoUri,
                                isMuted = isMuted
                            )
                        }
                    } else {
                        showApplyDialog = true
                    }
                }
            }
        )

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

        if (showApplyDialog) {
            ApplyTargetDialog(
                isApplying = isApplying,
                onDismiss = {
                    if (!isApplying) showApplyDialog = false
                },
                onTargetSelected = { target ->
                    showApplyDialog = false
                    coroutineScope.launch {
                        isApplying = true
                        val success = loadAndApplyWallpaper(
                            context = context,
                            url = wallpaper.url,
                            target = target
                        )
                        isApplying = false
                        snackbarHostState.showSnackbar(
                            if (success) "Wallpaper applied" else "Couldn’t apply wallpaper"
                        )
                    }
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun WallpaperMedia(
    wallpaper: WallpaperItem,
    exoPlayer: ExoPlayer?,
    videoPlaybackState: Int,
    videoError: Boolean
) {
    val context = LocalContext.current

    when (wallpaper.mediaType) {
        MediaType.VIDEO -> {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                update = { it.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
            when {
                videoError -> MediaErrorState()
                videoPlaybackState != Player.STATE_READY -> MediaLoadingState()
            }
        }

        MediaType.GIF -> {
            val imageLoader = remember {
                ImageLoader.Builder(context)
                    .components {
                        if (android.os.Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()
            }
            WallpaperImage(
                wallpaper = wallpaper,
                imageLoader = imageLoader
            )
        }

        MediaType.IMAGE -> {
            WallpaperImage(wallpaper = wallpaper)
        }
    }
}

@Composable
private fun WallpaperImage(
    wallpaper: WallpaperItem,
    imageLoader: ImageLoader? = null
) {
    val context = LocalContext.current
    val model = ImageRequest.Builder(context)
        .data(wallpaper.url)
        .crossfade(220)
        .build()

    if (imageLoader == null) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { MediaLoadingState() },
            error = { MediaErrorState() },
            success = { SubcomposeAsyncImageContent() }
        )
    } else {
        SubcomposeAsyncImage(
            model = model,
            imageLoader = imageLoader,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { MediaLoadingState() },
            error = { MediaErrorState() },
            success = { SubcomposeAsyncImageContent() }
        )
    }
}

@Composable
private fun MediaLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
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
    selectedMode: PreviewOverlayMode,
    onModeSelected: (PreviewOverlayMode) -> Unit,
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
            modifier = Modifier
                .clip(RoundedCornerShape(WallNiceRadii.pill))
                .background(Color.Black.copy(alpha = 0.76f))
                .border(
                    1.dp,
                    OutlineSubtle,
                    RoundedCornerShape(WallNiceRadii.pill)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayTab(
                text = "Clean",
                selected = selectedMode == PreviewOverlayMode.CLEAN,
                onClick = { onModeSelected(PreviewOverlayMode.CLEAN) }
            )
            OverlayTab(
                text = "Lock",
                selected = selectedMode == PreviewOverlayMode.LOCK_SCREEN,
                onClick = { onModeSelected(PreviewOverlayMode.LOCK_SCREEN) }
            )
            OverlayTab(
                text = "Home",
                selected = selectedMode == PreviewOverlayMode.HOME_SCREEN,
                onClick = { onModeSelected(PreviewOverlayMode.HOME_SCREEN) }
            )
        }
    }
}

@Composable
private fun OverlayTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(WallNiceRadii.pill))
            .background(if (selected) NeonPink else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = WallNiceSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun PreviewBottomPanel(
    modifier: Modifier = Modifier,
    wallpaper: WallpaperItem,
    isMuted: Boolean,
    isApplying: Boolean,
    onMuteToggle: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.28f to Color.Black.copy(alpha = 0.68f),
                        1f to Color.Black
                    )
                )
            )
            .navigationBarsPadding()
            .padding(
                start = WallNiceSpacing.md,
                end = WallNiceSpacing.md,
                top = 64.dp,
                bottom = WallNiceSpacing.md
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "@" + wallpaper.author + "  •  " + wallpaper.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (wallpaper.mediaType == MediaType.VIDEO) {
                Spacer(Modifier.width(WallNiceSpacing.sm))
                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier
                        .size(WallNiceSizes.minimumTouchTarget)
                        .clip(CircleShape)
                        .background(SurfaceHigh)
                        .border(1.dp, OutlineSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Enable sound" else "Mute sound",
                        tint = if (isMuted) TextSecondary else NeonCyan
                    )
                }
            }
        }

        Spacer(Modifier.height(WallNiceSpacing.md))

        Button(
            onClick = onApply,
            enabled = !isApplying,
            modifier = Modifier
                .fillMaxWidth()
                .height(WallNiceSizes.primaryActionHeight),
            shape = RoundedCornerShape(WallNiceRadii.medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPink,
                disabledContainerColor = NeonPink.copy(alpha = 0.5f)
            )
        ) {
            if (isApplying) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(19.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(WallNiceSpacing.xs))
                Text(
                    text = "Set Wallpaper",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
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
    target: WallpaperTarget
): Boolean {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        val bitmap = (result as? SuccessResult)
            ?.drawable
            ?.let { it as? BitmapDrawable }
            ?.bitmap
            ?: return false
        WallpaperHelper.applyStaticWallpaper(context, bitmap, target)
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
