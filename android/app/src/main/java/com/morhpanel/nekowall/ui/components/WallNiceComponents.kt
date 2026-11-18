package com.morhpanel.nekowall.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morhpanel.nekowall.R
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineStrong
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceHigh
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSizes
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = WallNiceSizes.brandMark
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(WallNiceRadii.small))
            .background(Color.Black)
            .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.small)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "WallNice",
            modifier = Modifier.size(size)
        )
    }
}

@Composable
fun WallNiceIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .size(WallNiceSizes.minimumTouchTarget)
            .clip(RoundedCornerShape(WallNiceRadii.small))
            .background(SurfaceLow)
            .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.small))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun CategoryPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(WallNiceRadii.pill)
    Row(
        modifier = modifier
            .height(WallNiceSizes.minimumTouchTarget)
            .clip(shape)
            .background(if (selected) NeonPink else SurfaceLow)
            .border(
                width = 1.dp,
                color = if (selected) NeonPink else OutlineStrong,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = WallNiceSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WallNiceSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(WallNiceRadii.large))
                .background(SurfaceLow)
                .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.large)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(WallNiceSpacing.md))
        Text(
            text = title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(WallNiceSpacing.xs))
        Text(
            text = message,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(WallNiceSpacing.lg))
            Box(
                modifier = Modifier
                    .height(WallNiceSizes.minimumTouchTarget)
                    .clip(RoundedCornerShape(WallNiceRadii.medium))
                    .background(NeonPink)
                    .clickable(onClick = onAction)
                    .padding(horizontal = WallNiceSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SkeletonWallpaperCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "WallpaperSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WallpaperSkeletonAlpha"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(WallNiceRadii.large))
                .background(SurfaceHigh.copy(alpha = alpha))
        )
        Spacer(Modifier.height(WallNiceSpacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHigh.copy(alpha = alpha))
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(9.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHigh.copy(alpha = alpha * 0.8f))
        )
    }
}
