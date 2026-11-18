package com.morhpanel.nekowall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.ui.screens.HomeScreen
import com.morhpanel.nekowall.ui.screens.PreviewScreen
import com.morhpanel.nekowall.ui.theme.BgCard
import com.morhpanel.nekowall.ui.theme.BgDark
import com.morhpanel.nekowall.ui.theme.NekoWallTheme
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NekoWallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
                    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

                    AnimatedContent(
                        targetState = selectedWallpaper,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { wallpaper ->
                        if (wallpaper == null) {
                            HomeScreen(
                                onWallpaperSelected = { item ->
                                    selectedWallpaper = item
                                },
                                onUpdateAvailable = { updateInfo ->
                                    if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                                        pendingUpdate = updateInfo
                                    }
                                }
                            )
                        } else {
                            PreviewScreen(
                                wallpaper = wallpaper,
                                onBack = { selectedWallpaper = null }
                            )
                        }
                    }

                    pendingUpdate?.let { update ->
                        AlertDialog(
                            onDismissRequest = { pendingUpdate = null },
                            containerColor = BgCard,
                            title = {
                                Text(
                                    text = "Update Available",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Version ${update.versionName} is now available.",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (update.changelog.isNotEmpty()) {
                                        Text(
                                            text = update.changelog,
                                            color = TextSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        pendingUpdate = null
                                        if (update.downloadUrl.isNotEmpty()) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                                            startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingUpdate = null }) {
                                    Text("Later", color = TextSecondary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
