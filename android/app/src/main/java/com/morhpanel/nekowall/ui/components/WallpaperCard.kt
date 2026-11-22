@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.morhpanel.nekowall.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.MediaTypeResolver
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.data.repository.FavoritesManager
import com.morhpanel.nekowall.data.repository.WallNicePreferences
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceMotion
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import com.morhpanel.nekowall.util.WallNiceImageLoader

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperCard(
    wallpaper: WallpaperItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val shape = RoundedCornerShape(WallNiceRadii.large)
    val context = LocalContext.current
    val imageLoader = remember(context) { WallNiceImageLoader.get(context) }
    val interactionSource = remember { MutableInteractionSource() }

    val isVideoActive = wallpaper.mediaType == MediaType.VIDEO && autoPlay
    val pulseAlpha = if (isVideoActive) {
        val transition = rememberInfiniteTransition(label = "ActiveVideoPulse")
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.90f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = WallNiceMotion.easeStandard),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        ).value
    } else {
        1f
    }
    val borderColor = if (isVideoActive) NeonCyan.copy(alpha = pulseAlpha) else OutlineSubtle
    val borderWidth = if (isVideoActive) 1.5.dp else 1.dp

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = "wallpaper_bounds_${wallpaper.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .then(sharedModifier)
            .springPressBounce(interactionSource)
            .clip(shape)
            .background(SurfaceLow)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(wallpaper.thumbnailUrl.ifBlank { wallpaper.url })
                .crossfade(220)
                .build(),
            imageLoader = imageLoader,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceLow)
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (wallpaper.mediaType == MediaType.VIDEO) Icons.Default.PlayArrow else Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            success = { SubcomposeAsyncImageContent() }
        )

        if (wallpaper.mediaType == MediaType.VIDEO && autoPlay) {
            CardVideoPlayer(
                videoUrl = wallpaper.url,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.04f),
                            0.48f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        if (FavoritesManager.isFavorite(wallpaper.id)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(WallNiceSpacing.sm)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(1.dp, NeonPink.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorited",
                    tint = NeonPink,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        MediaBadge(
            mediaType = wallpaper.mediaType,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(WallNiceSpacing.sm)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(WallNiceSpacing.sm)
        ) {
            Text(
                text = wallpaper.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(3.dp))
            Text(
                text = wallpaper.category.ifBlank { "@" + wallpaper.author },
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CardVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isReady by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            val quality = WallNicePreferences.getPlaybackQuality(context)
            if (quality.maxWidth != Int.MAX_VALUE) {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(quality.maxWidth, quality.maxHeight)
                    .build()
            }
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isReady = true
            }

            override fun onPlayerError(error: PlaybackException) {
                isReady = false
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> exoPlayer.playWhenReady = false
                else -> Unit
            }
        }
        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(WallNiceMotion.fastDurationMillis),
        label = "CardVideoAlpha"
    )

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        update = { it.player = exoPlayer },
        onRelease = { it.player = null },
        modifier = modifier.alpha(alpha)
    )
}

@Composable
private fun MediaBadge(
    mediaType: MediaType,
    modifier: Modifier = Modifier
) {
    val label: String
    val icon = when (mediaType) {
        MediaType.VIDEO -> {
            label = MediaTypeResolver.label(mediaType)
            Icons.Default.PlayArrow
        }
        MediaType.GIF -> {
            label = MediaTypeResolver.label(mediaType)
            Icons.Default.Gif
        }
        MediaType.IMAGE -> {
            label = MediaTypeResolver.label(mediaType)
            Icons.Default.Photo
        }
    }
    val color = if (mediaType == MediaType.VIDEO) NeonPink else NeonCyan

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(WallNiceRadii.small))
            .background(Color.Black.copy(alpha = 0.78f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(WallNiceRadii.small))
            .padding(horizontal = WallNiceSpacing.xs, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
