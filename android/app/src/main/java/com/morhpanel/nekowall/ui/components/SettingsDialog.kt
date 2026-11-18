package com.morhpanel.nekowall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.morhpanel.nekowall.BuildConfig
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceHigh
import com.morhpanel.nekowall.ui.theme.SurfaceLow
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

@Composable
fun SettingsDialog(
    wallpapers: List<WallpaperItem>,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    val totalWallpapers = wallpapers.size
    val categoryCounts = remember(wallpapers) {
        wallpapers.groupingBy { it.category.ifBlank { "All" } }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    val localBuildTime = remember {
        val date = Date(BuildConfig.BUILD_TIME_MILLIS)
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getDefault()
        }
        format.format(date)
    }

    val localTimezone = remember {
        val tz = TimeZone.getDefault()
        val now = Date()
        val offsetMinutes = tz.getOffset(now.time) / (1000 * 60)
        val hours = offsetMinutes / 60
        val minutes = Math.abs(offsetMinutes % 60)
        val sign = if (hours >= 0) "+" else ""
        if (minutes == 0) {
            "${tz.id} (UTC$sign$hours)"
        } else {
            "${tz.id} (UTC$sign$hours:${String.format(Locale.ENGLISH, "%02d", minutes)})"
        }
    }

    val norwayBuildTime = remember {
        val date = Date(BuildConfig.BUILD_TIME_MILLIS)
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Europe/Oslo")
        }
        format.format(date)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WallNiceRadii.large))
                .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.large)),
            color = SurfaceMid
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WallNiceSpacing.md)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh)
                                .border(1.dp, OutlineSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(WallNiceSpacing.sm))
                        Text(
                            text = "Settings & Info",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(WallNiceSpacing.md))

                SettingsSectionHeader(
                    icon = Icons.Default.Collections,
                    title = "Wallpaper Collection",
                    tint = NeonCyan
                )
                Spacer(Modifier.height(WallNiceSpacing.xs))
                SettingsCard {
                    SettingsRow(label = "Total Wallpapers", value = totalWallpapers.toString())
                    Spacer(Modifier.height(WallNiceSpacing.xs))
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = WallNiceSpacing.xs, bottom = 4.dp)
                    )
                    if (categoryCounts.isEmpty()) {
                        Text(
                            text = "No wallpapers loaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        categoryCounts.forEach { (categoryName, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(WallNiceRadii.small))
                                        .background(SurfaceLow)
                                        .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.small))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(WallNiceSpacing.md))

                SettingsSectionHeader(
                    icon = Icons.Default.Info,
                    title = "Build Version",
                    tint = NeonPink
                )
                Spacer(Modifier.height(WallNiceSpacing.xs))
                SettingsCard {
                    SettingsRow(label = "Version", value = BuildConfig.VERSION_NAME)
                    SettingsRow(label = "Build Code", value = BuildConfig.VERSION_CODE.toString())
                }

                Spacer(Modifier.height(WallNiceSpacing.md))

                SettingsSectionHeader(
                    icon = Icons.Default.Schedule,
                    title = "Build Timestamp",
                    tint = NeonCyan
                )
                Spacer(Modifier.height(WallNiceSpacing.xs))
                SettingsCard {
                    SettingsRow(label = "Local System Time", value = localBuildTime)
                    SettingsRow(label = "Detected Timezone", value = localTimezone)
                    SettingsRow(label = "Norway Time (CET)", value = norwayBuildTime)
                }

                Spacer(Modifier.height(WallNiceSpacing.md))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(WallNiceRadii.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceHigh
                    )
                ) {
                    Text(
                        text = "Close",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(WallNiceSpacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WallNiceRadii.medium))
            .background(SurfaceHigh)
            .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.medium))
            .padding(WallNiceSpacing.sm)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
