package com.morhpanel.nekowall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.ui.components.AppUpdateDialog
import com.morhpanel.nekowall.ui.screens.HomeScreen
import com.morhpanel.nekowall.ui.screens.PreviewScreen
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NekoWallTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NekoWallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
                    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

                    AnimatedContent(
                        targetState = selectedWallpaper,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { wallpaper ->
                        if (wallpaper == null) {
                            HomeScreen(
                                onWallpaperSelected = { selectedWallpaper = it },
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
                        AppUpdateDialog(
                            updateInfo = update,
                            onDismiss = { pendingUpdate = null }
                        )
                    }
                }
            }
        }
    }
}
