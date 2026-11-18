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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.morhpanel.nekowall.BuildConfig
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.engine.AppUpdateManager
import com.morhpanel.nekowall.engine.UpdateInstallState
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineStrong
import com.morhpanel.nekowall.ui.theme.SurfaceMid
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceMotion
import com.morhpanel.nekowall.ui.theme.WallNiceSizes
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<UpdateInstallState>(UpdateInstallState.Idle) }

    fun closeDialog() {
        onDismiss()
    }

    fun openDownload() {
        state = UpdateInstallState.OpeningDownload
        AppUpdateManager.openApkDownload(context, updateInfo.downloadUrl)
            .onSuccess {
                state = UpdateInstallState.DownloadStarted
            }
            .onFailure { error ->
                state = UpdateInstallState.Error(
                    error.localizedMessage ?: "Couldn’t open the secure download."
                )
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

                    UpdateInstallState.OpeningDownload -> {
                        Text(
                            text = "Opening the secure download for " + updateInfo.versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    UpdateInstallState.DownloadStarted -> {
                        Text(
                            text = "Your browser will download the APK. Open it from the download notification to finish the update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
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

                when (state) {
                    UpdateInstallState.Idle -> {
                        UpdatePrimaryButton(
                            text = "Download update",
                            onClick = { openDownload() }
                        )
                        UpdateSecondaryButton(
                            text = "Later",
                            onClick = { closeDialog() }
                        )
                    }

                    UpdateInstallState.OpeningDownload -> {
                        UpdateSecondaryButton(
                            text = "Later",
                            onClick = { closeDialog() }
                        )
                    }

                    UpdateInstallState.DownloadStarted -> {
                        UpdatePrimaryButton(
                            text = "Open download again",
                            onClick = { openDownload() }
                        )
                        UpdateSecondaryButton(
                            text = "Done",
                            onClick = { closeDialog() }
                        )
                    }

                    is UpdateInstallState.Error -> {
                        UpdatePrimaryButton(
                            text = "Retry",
                            onClick = { openDownload() }
                        )
                        UpdateSecondaryButton(
                            text = "Later",
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
                    UpdateInstallState.OpeningDownload -> "Opening download"
                    UpdateInstallState.DownloadStarted -> "Download started"
                    is UpdateInstallState.Error -> "Update failed"
                },
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = when (state) {
                    UpdateInstallState.Idle -> "A newer WallNice build is available"
                    UpdateInstallState.OpeningDownload -> "The system browser will handle the APK download"
                    UpdateInstallState.DownloadStarted -> "Finish from Android's download notification"
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
