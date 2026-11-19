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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.morhpanel.nekowall.BuildConfig
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.engine.AppUpdateManager
import com.morhpanel.nekowall.engine.UpdateInstallState
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineStrong
import com.morhpanel.nekowall.ui.theme.SurfaceHigh
import com.morhpanel.nekowall.ui.theme.SurfaceMid
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceMotion
import com.morhpanel.nekowall.ui.theme.WallNiceSizes
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf<UpdateInstallState>(UpdateInstallState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun closeDialog() {
        downloadJob?.cancel()
        onDismiss()
    }

    fun startDownload() {
        downloadJob?.cancel()
        state = UpdateInstallState.Downloading(0, 0, 0)
        downloadJob = coroutineScope.launch {
            AppUpdateManager.downloadApk(
                context = context,
                urlStr = updateInfo.downloadUrl,
                versionCode = updateInfo.versionCode,
                onProgress = { progress, current, total ->
                    state = UpdateInstallState.Downloading(progress, current, total)
                }
            ).onSuccess { apkFile ->
                if (!AppUpdateManager.canInstallApk(context)) {
                    state = UpdateInstallState.NeedsPermission(apkFile)
                } else {
                    state = UpdateInstallState.ReadyToInstall(apkFile)
                    AppUpdateManager.launchInstaller(context, apkFile)
                        .onFailure { error ->
                            state = UpdateInstallState.Error(
                                error.localizedMessage ?: "Couldn’t open the installer."
                            )
                        }
                }
            }.onFailure { error ->
                state = UpdateInstallState.Error(
                    error.localizedMessage ?: "Download failed."
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentState = state
                if (currentState is UpdateInstallState.NeedsPermission && AppUpdateManager.canInstallApk(context)) {
                    state = UpdateInstallState.ReadyToInstall(currentState.apkFile)
                    AppUpdateManager.launchInstaller(context, currentState.apkFile)
                        .onFailure { error ->
                            state = UpdateInstallState.Error(
                                error.localizedMessage ?: "Couldn’t open the installer."
                            )
                        }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(onDismissRequest = { closeDialog() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = OutlineStrong,
                    shape = RoundedCornerShape(WallNiceRadii.large)
                ),
            shape = RoundedCornerShape(WallNiceRadii.large),
            color = SurfaceMid
        ) {
            Column(modifier = Modifier.padding(WallNiceSpacing.xl)) {
                UpdateHeader(state)

                Spacer(Modifier.height(WallNiceSpacing.lg))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(WallNiceMotion.standardDurationMillis)) +
                            slideInVertically(
                                animationSpec = tween(WallNiceMotion.standardDurationMillis),
                                initialOffsetY = { it / 10 }
                            )) togetherWith
                            (fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis)) +
                                slideOutVertically(
                                    animationSpec = tween(WallNiceMotion.fastDurationMillis),
                                    targetOffsetY = { -it / 12 }
                                ))
                    },
                    label = "UpdateStateTransition"
                ) { currentState ->
                    when (currentState) {
                        UpdateInstallState.Idle -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(WallNiceSpacing.md)
                            ) {
                                if (updateInfo.changelog.isNotBlank()) {
                                    Text(
                                        text = updateInfo.changelog,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
                                ) {
                                    VersionTile(
                                        label = "Installed",
                                        version = BuildConfig.VERSION_NAME,
                                        modifier = Modifier.weight(1f)
                                    )
                                    VersionTile(
                                        label = "Available",
                                        version = updateInfo.versionName,
                                        modifier = Modifier.weight(1f),
                                        accent = true
                                    )
                                }
                            }
                        }

                        is UpdateInstallState.Downloading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
                            ) {
                                Text(
                                    text = formatProgress(currentState),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LinearProgressIndicator(
                                    progress = { currentState.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = NeonPink,
                                    trackColor = SurfaceHigh
                                )
                            }
                        }

                        is UpdateInstallState.NeedsPermission -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = NeonCyan.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(WallNiceRadii.medium)
                                    )
                                    .padding(WallNiceSpacing.md)
                            ) {
                                Text(
                                    text = "To finish the update, allow WallNice to install unknown apps in system settings.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        is UpdateInstallState.ReadyToInstall -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = NeonCyan.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(WallNiceRadii.medium)
                                    )
                                    .padding(WallNiceSpacing.md)
                            ) {
                                Text(
                                    text = "The update file is verified and ready. Tap below to launch the installer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        is UpdateInstallState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = NeonPink.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(WallNiceRadii.medium)
                                    )
                                    .padding(WallNiceSpacing.md)
                            ) {
                                Text(
                                    text = currentState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(WallNiceSpacing.xl))

                when (val currentState = state) {
                    UpdateInstallState.Idle -> {
                        UpdatePrimaryButton(
                            text = "Update now",
                            onClick = { startDownload() }
                        )
                        UpdateSecondaryButton(
                            text = "Later",
                            onClick = { closeDialog() }
                        )
                    }

                    is UpdateInstallState.Downloading -> {
                        UpdateSecondaryButton(
                            text = "Cancel",
                            onClick = { closeDialog() }
                        )
                    }

                    is UpdateInstallState.NeedsPermission -> {
                        UpdatePrimaryButton(
                            text = "Allow in settings",
                            onClick = { AppUpdateManager.openInstallPermissionSettings(context) }
                        )
                        UpdateSecondaryButton(
                            text = "Cancel",
                            onClick = { closeDialog() }
                        )
                    }

                    is UpdateInstallState.ReadyToInstall -> {
                        UpdatePrimaryButton(
                            text = "Install update",
                            onClick = {
                                AppUpdateManager.launchInstaller(context, currentState.apkFile)
                                    .onFailure { error ->
                                        state = UpdateInstallState.Error(
                                            error.localizedMessage ?: "Couldn’t open the installer."
                                        )
                                    }
                            }
                        )
                        UpdateSecondaryButton(
                            text = "Close",
                            onClick = { closeDialog() }
                        )
                    }

                    is UpdateInstallState.Error -> {
                        UpdatePrimaryButton(
                            text = "Retry",
                            onClick = { startDownload() }
                        )
                        UpdateSecondaryButton(
                            text = "Dismiss",
                            onClick = { closeDialog() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateHeader(state: UpdateInstallState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
    ) {
        Surface(
            shape = RoundedCornerShape(WallNiceRadii.medium),
            color = NeonPink.copy(alpha = 0.14f)
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdateAlt,
                contentDescription = null,
                tint = NeonPink,
                modifier = Modifier
                    .padding(WallNiceSpacing.sm)
                    .size(26.dp)
            )
        }
        Column {
            Text(
                text = when (state) {
                    UpdateInstallState.Idle -> "Update ready"
                    is UpdateInstallState.Downloading -> "Downloading"
                    is UpdateInstallState.NeedsPermission -> "Permission required"
                    is UpdateInstallState.ReadyToInstall -> "Ready to install"
                    is UpdateInstallState.Error -> "Update failed"
                },
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = when (state) {
                    UpdateInstallState.Idle -> "A newer WallNice build is available"
                    is UpdateInstallState.Downloading -> "Keep the app open for a moment"
                    is UpdateInstallState.NeedsPermission -> "Allow installation from this source"
                    is UpdateInstallState.ReadyToInstall -> "The download is complete"
                    is UpdateInstallState.Error -> "Your current version is unchanged"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun VersionTile(
    label: String,
    version: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(WallNiceRadii.medium),
        color = AmoledBlack
    ) {
        Column(modifier = Modifier.padding(WallNiceSpacing.sm)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = version,
                style = MaterialTheme.typography.titleMedium,
                color = if (accent) NeonCyan else TextPrimary
            )
        }
    }
}

@Composable
private fun UpdatePrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(WallNiceSizes.primaryActionHeight),
        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
        shape = RoundedCornerShape(WallNiceRadii.medium)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UpdateSecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(WallNiceSizes.minimumTouchTarget)
    ) {
        Text(
            text = text,
            color = NeonCyan,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatProgress(state: UpdateInstallState.Downloading): String {
    val downloadedMb = state.currentBytes / (1024f * 1024f)
    return if (state.totalBytes > 0) {
        val totalMb = state.totalBytes / (1024f * 1024f)
        String.format(
            Locale.US,
            "%d%%  •  %.1f / %.1f MB",
            state.progressPercent,
            downloadedMb,
            totalMb
        )
    } else {
        String.format(Locale.US, "%.1f MB downloaded", downloadedMb)
    }
}
