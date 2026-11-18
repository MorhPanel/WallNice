package com.morhpanel.nekowall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.MediaTypeResolver
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.data.repository.FavoritesManager
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing

@Composable
fun WallpaperCard(
    wallpaper: WallpaperItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(WallNiceRadii.large)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(shape)
            .background(SurfaceLow)
            .border(1.dp, OutlineSubtle, shape)
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(wallpaper.thumbnailUrl)
                .crossfade(220)
                .build(),
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
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            success = { SubcomposeAsyncImageContent() }
        )

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
