package com.morhpanel.nekowall.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.morhpanel.nekowall.MainActivity
import com.morhpanel.nekowall.data.repository.PlaylistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NextWallpaperTileService : TileService() {

    private var serviceScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "Next Wallpaper"
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val scope = serviceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also {
            serviceScope = it
        }
        scope.launch {
            val rotated = runCatching {
                PlaylistManager.rotateNow(applicationContext)
            }.getOrDefault(false)
            if (!rotated) {
                withContext(Dispatchers.Main) {
                    openMainActivity()
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
