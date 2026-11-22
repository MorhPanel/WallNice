package com.morhpanel.nekowall.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.morhpanel.nekowall.BuildConfig
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.data.models.PlaybackQuality
import com.morhpanel.nekowall.data.repository.PlaylistManager
import com.morhpanel.nekowall.data.repository.WallNicePreferences
import com.morhpanel.nekowall.engine.DownloadHelper
import com.morhpanel.nekowall.engine.WallpaperHelper
import com.morhpanel.nekowall.ui.components.WallNiceIconButton
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceHigh
import com.morhpanel.nekowall.ui.theme.SurfaceMid
import com.morhpanel.nekowall.ui.theme.TextMuted
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    wallpapers: List<WallpaperItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var doubleTapToPause by remember {
        mutableStateOf(WallpaperHelper.isDoubleTapToPauseEnabled(context))
    }
    var playbackQuality by remember { mutableStateOf(WallNicePreferences.getPlaybackQuality(context)) }
    var pauseOnLowBattery by remember { mutableStateOf(WallNicePreferences.shouldPauseOnLowBattery(context)) }
    var autoPlayPreviews by remember { mutableStateOf(WallNicePreferences.shouldAutoPlayPreviews(context)) }
    var rotationEnabled by remember { mutableStateOf(WallNicePreferences.isRotationEnabled(context)) }
    var rotationInterval by remember { mutableStateOf(WallNicePreferences.getRotationIntervalHours(context)) }
    var cacheSize by remember { mutableStateOf(DownloadHelper.temporaryCacheSize(context)) }
    var rotationStatus by remember { mutableStateOf<String?>(null) }
    val favoriteSize = DownloadHelper.favoritesSize(context)
    val playlistSize = DownloadHelper.playlistSize(context)
    val playlistCount = PlaylistManager.getEntries(context).size
    val scrollState = rememberScrollState()
    val categoryCounts = remember(wallpapers) {
        wallpapers.groupingBy { it.category.ifBlank { "All" } }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    val localBuildTime = remember {
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getDefault()
        }
        format.format(Date(BuildConfig.BUILD_TIME_MILLIS))
    }

    val localTimezone = remember {
        val timezone = TimeZone.getDefault()
        val offsetMinutes = timezone.getOffset(Date().time) / (1000 * 60)
        val hours = offsetMinutes / 60
        val minutes = kotlin.math.abs(offsetMinutes % 60)
        val sign = if (hours >= 0) "+" else ""
        if (minutes == 0) {
            "${timezone.id} (UTC$sign$hours)"
        } else {
            "${timezone.id} (UTC$sign$hours:${String.format(Locale.ENGLISH, "%02d", minutes)})"
        }
    }

    val osloBuildTime = remember {
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Europe/Oslo")
        }
        format.format(Date(BuildConfig.BUILD_TIME_MILLIS))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        SettingsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = WallNiceSpacing.md)
        ) {
            Spacer(Modifier.height(WallNiceSpacing.sm))
            SettingsHero(
                wallpaperCount = wallpapers.size,
                categoryCount = categoryCounts.size
            )

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.Default.Collections,
                title = "Collection",
                tint = NeonCyan,
                detail = "What is available in this catalog"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                SettingsValueRow("Total wallpapers", wallpapers.size.toString())
                SettingsDivider()
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(
                        start = WallNiceSpacing.md,
                        end = WallNiceSpacing.md,
                        top = WallNiceSpacing.sm,
                        bottom = WallNiceSpacing.xs
                    )
                )
                if (categoryCounts.isEmpty()) {
                    Text(
                        text = "No wallpapers loaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(
                            horizontal = WallNiceSpacing.md,
                            vertical = WallNiceSpacing.sm
                        )
                    )
                } else {
                    categoryCounts.forEachIndexed { index, (category, count) ->
                        SettingsValueRow(category, count.toString(), valueTint = NeonCyan)
                        if (index < categoryCounts.lastIndex) SettingsDivider()
                    }
                }
            }

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.Default.BatterySaver,
                title = "Playback",
                tint = NeonCyan,
                detail = "Preview quality and battery behavior"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                PlaybackQuality.entries.forEachIndexed { index, quality ->
                    SettingsChoiceRow(
                        label = quality.label,
                        detail = when (quality) {
                            PlaybackQuality.ECO -> "720p preview · one animated card"
                            PlaybackQuality.BALANCED -> "1080p preview · two animated cards"
                            PlaybackQuality.MAXIMUM -> "Source quality · four animated cards"
                        },
                        selected = playbackQuality == quality,
                        onClick = {
                            playbackQuality = quality
                            WallNicePreferences.setPlaybackQuality(context, quality)
                        }
                    )
                    if (index < PlaybackQuality.entries.lastIndex) SettingsDivider()
                }
                SettingsDivider()
                SettingsSwitchRow(
                    label = "Animated catalog previews",
                    detail = "Play visible video cards while browsing",
                    checked = autoPlayPreviews,
                    onCheckedChange = { enabled ->
                        autoPlayPreviews = enabled
                        WallNicePreferences.setAutoPlayPreviews(context, enabled)
                    }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    label = "Pause below 15%",
                    detail = "Stops live wallpaper when battery is low and unplugged",
                    checked = pauseOnLowBattery,
                    onCheckedChange = { enabled ->
                        pauseOnLowBattery = enabled
                        WallNicePreferences.setPauseOnLowBattery(context, enabled)
                    }
                )
            }

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                title = "Rotation playlist",
                tint = NeonPink,
                detail = "Automatically cycle saved wallpapers"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                SettingsValueRow("Saved wallpapers", playlistCount.toString(), valueTint = NeonCyan)
                SettingsDivider()
                SettingsSwitchRow(
                    label = "Automatic rotation",
                    detail = "Rotates videos with the live engine, or static wallpapers when it is inactive",
                    checked = rotationEnabled,
                    onCheckedChange = { enabled ->
                        rotationEnabled = enabled
                        WallNicePreferences.setRotationEnabled(context, enabled)
                        if (enabled) PlaylistManager.schedule(context) else PlaylistManager.cancelSchedule(context)
                    }
                )
                SettingsDivider()
                listOf(1, 6, 12, 24).forEachIndexed { index, hours ->
                    SettingsChoiceRow(
                        label = if (hours == 1) "Every hour" else "Every $hours hours",
                        detail = "Rotation interval",
                        selected = rotationInterval == hours,
                        onClick = {
                            rotationInterval = hours
                            WallNicePreferences.setRotationIntervalHours(context, hours)
                            if (rotationEnabled) PlaylistManager.schedule(context)
                        }
                    )
                    if (index < 3) SettingsDivider()
                }
                SettingsDivider()
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = "Rotate now",
                    detail = rotationStatus ?: "Apply the next available wallpaper",
                    onClick = {
                        coroutineScope.launch {
                            rotationStatus = "Applying…"
                            rotationStatus = if (PlaylistManager.rotateNow(context)) "Wallpaper changed" else "Add wallpapers or activate the live engine"
                        }
                    }
                )
            }

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.Default.Cached,
                title = "Storage",
                tint = NeonCyan,
                detail = "Offline media and temporary files"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                SettingsValueRow("Temporary cache", DownloadHelper.formatBytes(cacheSize))
                SettingsDivider()
                SettingsValueRow("Offline favorites", DownloadHelper.formatBytes(favoriteSize))
                SettingsDivider()
                SettingsValueRow("Rotation playlist", DownloadHelper.formatBytes(playlistSize))
                SettingsDivider()
                SettingsActionRow(
                    icon = Icons.Default.DeleteSweep,
                    label = "Clear temporary cache",
                    detail = "Favorites and playlist files stay offline",
                    onClick = {
                        DownloadHelper.clearTemporaryCache(context)
                        cacheSize = DownloadHelper.temporaryCacheSize(context)
                    }
                )
            }

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.Default.TouchApp,
                title = "Gestures",
                tint = NeonPink,
                detail = "Home screen wallpaper interaction"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                SettingsSwitchRow(
                    label = "Double tap to pause",
                    detail = "Double-tap on the home screen to pause or resume video wallpaper",
                    checked = doubleTapToPause,
                    onCheckedChange = { enabled ->
                        doubleTapToPause = enabled
                        WallpaperHelper.setDoubleTapToPauseEnabled(context, enabled)
                    }
                )
            }

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = "Build",
                tint = NeonPink,
                detail = "The installed WallNice release"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                SettingsValueRow("Version", BuildConfig.VERSION_NAME, valueTint = NeonPink)
                SettingsDivider()
                SettingsValueRow("Build code", BuildConfig.VERSION_CODE.toString())
            }

            Spacer(Modifier.height(WallNiceSpacing.xl))
            SettingsSectionHeader(
                icon = Icons.Default.Schedule,
                title = "Environment",
                tint = NeonCyan,
                detail = "Useful device and build context"
            )
            Spacer(Modifier.height(WallNiceSpacing.xs))
            SettingsCard {
                SettingsValueRow("Local build time", localBuildTime)
                SettingsDivider()
                SettingsValueRow("Detected timezone", localTimezone)
                SettingsDivider()
                SettingsValueRow("Oslo build time", osloBuildTime)
            }

            Spacer(Modifier.height(WallNiceSpacing.xxl))
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WallNiceIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to wallpapers",
            onClick = onBack,
            tint = NeonCyan
        )
        Spacer(Modifier.width(WallNiceSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "WallNice overview",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(WallNiceRadii.small))
                .background(SurfaceHigh)
                .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.small)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = NeonPink,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun SettingsHero(
    wallpaperCount: Int,
    categoryCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.large)),
        color = SurfaceMid,
        shape = RoundedCornerShape(WallNiceRadii.large)
    ) {
        Row(
            modifier = Modifier.padding(WallNiceSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(WallNiceRadii.medium))
                    .background(NeonPink.copy(alpha = 0.14f))
                    .border(1.dp, NeonPink.copy(alpha = 0.35f), RoundedCornerShape(WallNiceRadii.medium)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = NeonPink,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your wallpaper library",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(WallNiceSpacing.xxs))
                Text(
                    text = "$wallpaperCount wallpapers · $categoryCount categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.large)),
        color = SurfaceMid,
        shape = RoundedCornerShape(WallNiceRadii.large)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    valueTint: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueTint,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = WallNiceSpacing.md),
        color = OutlineSubtle.copy(alpha = 0.7f)
    )
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    detail: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            if (detail != null) {
                Spacer(Modifier.height(WallNiceSpacing.xxs))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AmoledBlack,
                checkedTrackColor = NeonPink,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceHigh,
                uncheckedBorderColor = OutlineSubtle
            )
        )
    }
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) TextPrimary else TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) NeonPink else SurfaceHigh)
                .border(1.dp, if (selected) NeonPink else OutlineSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AmoledBlack)
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
