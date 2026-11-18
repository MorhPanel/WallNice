package com.morhpanel.nekowall.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.engine.DownloadHelper
import com.morhpanel.nekowall.engine.WallpaperHelper
import com.morhpanel.nekowall.engine.WallpaperTarget
import com.morhpanel.nekowall.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var overlayMode by remember { mutableStateOf(PreviewOverlayMode.CLEAN) }
    var isMuted by remember { mutableStateOf(true) }
    var showStaticApplyDialog by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }

    val exoPlayer = remember(wallpaper.url) {
        if (wallpaper.mediaType == MediaType.VIDEO) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(wallpaper.url)))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isMuted) 0f else 1f
                prepare()
                playWhenReady = true
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        when (wallpaper.mediaType) {
            MediaType.VIDEO -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(wallpaper.url)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(wallpaper.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        when (overlayMode) {
            PreviewOverlayMode.CLEAN -> {}
            PreviewOverlayMode.LOCK_SCREEN -> {
                LockScreenSimulation()
            }
            PreviewOverlayMode.HOME_SCREEN -> {
                HomeScreenSimulation()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OverlayButton(
                    text = "Clean",
                    isSelected = overlayMode == PreviewOverlayMode.CLEAN,
                    onClick = { overlayMode = PreviewOverlayMode.CLEAN }
                )
                OverlayButton(
                    text = "Lock",
                    isSelected = overlayMode == PreviewOverlayMode.LOCK_SCREEN,
                    onClick = { overlayMode = PreviewOverlayMode.LOCK_SCREEN }
                )
                OverlayButton(
                    text = "Home",
                    isSelected = overlayMode == PreviewOverlayMode.HOME_SCREEN,
                    onClick = { overlayMode = PreviewOverlayMode.HOME_SCREEN }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = wallpaper.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${wallpaper.category} • ${wallpaper.resolution}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                if (wallpaper.mediaType == MediaType.VIDEO) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Sound",
                            tint = if (isMuted) TextSecondary else NeonPink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDownloading = true
                            downloadProgress = 0
                            val success = DownloadHelper.saveToGallery(
                                context = context,
                                fileUrl = wallpaper.url,
                                title = wallpaper.title,
                                isVideo = wallpaper.mediaType == MediaType.VIDEO,
                                onProgress = { progress -> downloadProgress = progress }
                            )
                            isDownloading = false
                            if (success) {
                                Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BgCardHover),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "$downloadProgress%", color = NeonCyan, fontSize = 13.sp)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        if (wallpaper.mediaType == MediaType.VIDEO) {
                            coroutineScope.launch {
                                isApplying = true
                                val cachedFile = DownloadHelper.downloadToCache(context, wallpaper.url, "mp4")
                                isApplying = false
                                val localUri = if (cachedFile != null) {
                                    Uri.fromFile(cachedFile).toString()
                                } else {
                                    wallpaper.url
                                }
                                WallpaperHelper.openLiveWallpaperChooser(
                                    context = context,
                                    videoUri = localUri,
                                    isMuted = isMuted
                                )
                            }
                        } else {
                            showStaticApplyDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    enabled = !isApplying
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        if (showStaticApplyDialog) {
            AlertDialog(
                onDismissRequest = { showStaticApplyDialog = false },
                title = { Text("Apply Wallpaper", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ApplyOptionItem(title = "Home Screen") {
                            applyBitmap(context, wallpaper.url, WallpaperTarget.HOME_SCREEN) {
                                isApplying = false
                                showStaticApplyDialog = false
                            }
                        }
                        ApplyOptionItem(title = "Lock Screen") {
                            applyBitmap(context, wallpaper.url, WallpaperTarget.LOCK_SCREEN) {
                                isApplying = false
                                showStaticApplyDialog = false
                            }
                        }
                        ApplyOptionItem(title = "Both Screens") {
                            applyBitmap(context, wallpaper.url, WallpaperTarget.BOTH) {
                                isApplying = false
                                showStaticApplyDialog = false
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showStaticApplyDialog = false }) {
                        Text("Cancel", color = NeonCyan)
                    }
                },
                containerColor = BgCard
            )
        }
    }
}

private fun applyBitmap(
    context: android.content.Context,
    url: String,
    target: WallpaperTarget,
    onComplete: () -> Unit
) {
    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    coroutineScope.launch {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val success = WallpaperHelper.applyStaticWallpaper(context, bitmap, target)
                if (success) {
                    Toast.makeText(context, "Wallpaper applied successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                }
            }
        }
        onComplete()
    }
}

@Composable
private fun ApplyOptionItem(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = BgCardHover),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OverlayButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) NeonPink else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LockScreenSimulation() {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.US) }
    val now = remember { Date() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = timeFormat.format(now),
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = dateFormat.format(now),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun HomeScreenSimulation() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 70.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "13:37  •  72°F  Sunny",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}
