package com.morhpanel.nekowall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.engine.AppUpdateManager
import com.morhpanel.nekowall.ui.components.AppUpdateDialog
import com.morhpanel.nekowall.ui.screens.HomeScreen
import com.morhpanel.nekowall.ui.screens.PreviewScreen
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NekoWallTheme
import com.morhpanel.nekowall.ui.theme.WallNiceMotion

private val WallpaperItemSaver: Saver<WallpaperItem?, Any> = Saver(
    save = { item ->
        if (item == null) {
            null
        } else {
            arrayListOf(
                item.id,
                item.title,
                item.author,
                item.category,
                item.mediaType.name,
                item.url,
                item.thumbnailUrl,
                item.resolution,
                item.isAmoled.toString(),
                item.likesCount.toString(),
                item.subcategory
            )
        }
    },
    restore = { value ->
        (value as? List<*>)?.let { list ->
            if (list.size >= 10) {
                WallpaperItem(
                    id = list[0] as? String ?: "",
                    title = list[1] as? String ?: "",
                    author = list[2] as? String ?: "",
                    category = list[3] as? String ?: "",
                    subcategory = if (list.size >= 11) list[10] as? String ?: "" else "",
                    mediaType = (list[4] as? String)?.let { name ->
                        runCatching { MediaType.valueOf(name) }.getOrNull()
                    } ?: MediaType.IMAGE,
                    url = list[5] as? String ?: "",
                    thumbnailUrl = list[6] as? String ?: "",
                    resolution = list[7] as? String ?: "1080x2400",
                    isAmoled = (list[8] as? String)?.toBooleanStrictOrNull() ?: false,
                    likesCount = (list[9] as? String)?.toIntOrNull() ?: 0
                )
            } else {
                null
            }
        }
    }
)

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
                    var selectedWallpaper by rememberSaveable(stateSaver = WallpaperItemSaver) {
                        mutableStateOf<WallpaperItem?>(null)
                    }
                    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

                    AnimatedContent(
                        targetState = selectedWallpaper,
                        transitionSpec = {
                            if (targetState == null) {
                                (fadeIn(animationSpec = tween(WallNiceMotion.standardDurationMillis)) +
                                    slideInHorizontally(
                                        animationSpec = tween(WallNiceMotion.standardDurationMillis),
                                        initialOffsetX = { -it / 12 }
                                    )) togetherWith
                                    (fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(WallNiceMotion.fastDurationMillis),
                                            targetOffsetX = { it / 12 }
                                        ))
                            } else {
                                (fadeIn(animationSpec = tween(WallNiceMotion.standardDurationMillis)) +
                                    slideInHorizontally(
                                        animationSpec = tween(WallNiceMotion.standardDurationMillis),
                                        initialOffsetX = { it / 12 }
                                    )) togetherWith
                                    (fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(WallNiceMotion.fastDurationMillis),
                                            targetOffsetX = { -it / 12 }
                                        ))
                            }
                        },
                        label = "ScreenTransition"
                    ) { wallpaper ->
                        if (wallpaper == null) {
                            HomeScreen(
                                onWallpaperSelected = { selectedWallpaper = it },
                                onUpdateAvailable = { updateInfo ->
                                    if (updateInfo.versionCode > BuildConfig.VERSION_CODE &&
                                        !AppUpdateManager.isPlayManagedInstall(this@MainActivity)
                                    ) {
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
