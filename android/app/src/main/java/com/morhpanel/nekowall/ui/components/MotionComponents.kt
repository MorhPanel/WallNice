package com.morhpanel.nekowall.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceMotion
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSizes
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Modifier.springPressBounce(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.965f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = if (isPressed) {
            tween(
                durationMillis = WallNiceMotion.cardPressDurationMillis,
                easing = WallNiceMotion.easeDecelerate
            )
        } else {
            WallNiceMotion.springBouncy
        },
        label = "PressBounceScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    enabled: Boolean = true
): Modifier {
    if (!enabled || index > 8) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * WallNiceMotion.galleryStaggerDelayMillis).toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = WallNiceMotion.galleryEntryDurationMillis,
                easing = WallNiceMotion.easeDecelerate
            )
        )
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12.dp.value * density
    }
}

@Composable
fun HeartBurstButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var previousFavorite by remember { mutableStateOf(isFavorite) }
    val view = LocalView.current

    LaunchedEffect(isFavorite) {
        if (isFavorite != previousFavorite) {
            previousFavorite = isFavorite
            if (isFavorite) {
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Exception) {
                }
                coroutineScope.launch {
                    scaleAnim.animateTo(0.8f, tween(60, easing = WallNiceMotion.easeDecelerate))
                    rotationAnim.animateTo(7f, tween(100, easing = WallNiceMotion.easeStandard))
                    rotationAnim.animateTo(0f, WallNiceMotion.springBouncy)
                }
                coroutineScope.launch {
                    scaleAnim.animateTo(1.32f, tween(140, easing = WallNiceMotion.easeStandard))
                    scaleAnim.animateTo(1.0f, WallNiceMotion.springBouncy)
                }
            } else {
                coroutineScope.launch {
                    scaleAnim.animateTo(0.88f, tween(70))
                    scaleAnim.animateTo(1.0f, WallNiceMotion.springResponsive)
                }
            }
        }
    }

    val iconColor by animateColorAsState(
        targetValue = if (isFavorite) NeonPink else Color.White,
        animationSpec = tween(WallNiceMotion.fastDurationMillis),
        label = "HeartColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFavorite) NeonPink.copy(alpha = 0.7f) else OutlineSubtle,
        animationSpec = tween(WallNiceMotion.fastDurationMillis),
        label = "HeartBorder"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(WallNiceSizes.minimumTouchTarget)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, borderColor, CircleShape)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = iconColor,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    rotationZ = rotationAnim.value
                }
        )
    }
}

@Composable
fun ScanlineSuccessOverlay(
    trigger: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!trigger) return
    val progress = remember { Animatable(0f) }
    val view = LocalView.current

    LaunchedEffect(trigger) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } catch (_: Exception) {
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = WallNiceMotion.scanlineDurationMillis,
                easing = WallNiceMotion.easeStandard
            )
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentY = size.height * progress.value
        val beamHeight = 90.dp.toPx()
        val lineThickness = 3.dp.toPx()
        val alphaMultiplier = when {
            progress.value < 0.15f -> progress.value / 0.15f
            progress.value > 0.8f -> (1f - progress.value) / 0.2f
            else -> 1f
        }

        val trailBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                NeonCyan.copy(alpha = 0.35f * alphaMultiplier),
                NeonPink.copy(alpha = 0.75f * alphaMultiplier)
            ),
            startY = (currentY - beamHeight).coerceAtLeast(0f),
            endY = currentY
        )
        drawRect(
            brush = trailBrush,
            topLeft = Offset(0f, (currentY - beamHeight).coerceAtLeast(0f)),
            size = Size(size.width, beamHeight.coerceAtMost(currentY))
        )

        val lineBrush = Brush.horizontalGradient(
            colors = listOf(
                NeonCyan.copy(alpha = 0.2f * alphaMultiplier),
                Color.White.copy(alpha = 0.95f * alphaMultiplier),
                NeonPink.copy(alpha = 0.95f * alphaMultiplier),
                Color.White.copy(alpha = 0.95f * alphaMultiplier),
                NeonCyan.copy(alpha = 0.2f * alphaMultiplier)
            )
        )
        drawRect(
            brush = lineBrush,
            topLeft = Offset(0f, currentY - lineThickness / 2),
            size = Size(size.width, lineThickness)
        )
    }
}

@Composable
fun SlidingSegmentedPill(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(WallNiceRadii.pill)
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.76f))
            .border(1.dp, OutlineSubtle, shape)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / items.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = tween(
                durationMillis = WallNiceMotion.filterSlideDurationMillis,
                easing = WallNiceMotion.easeStandard
            ),
            label = "SlidingPillIndicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(36.dp)
                .clip(shape)
                .background(NeonPink)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextSecondary,
                    animationSpec = tween(WallNiceMotion.fastDurationMillis),
                    label = "TabTextColor"
                )
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(36.dp)
                        .clip(shape)
                        .clickable { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
