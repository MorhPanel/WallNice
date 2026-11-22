package com.morhpanel.nekowall.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.morhpanel.nekowall.data.repository.PlaylistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WallpaperRotationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PlaylistManager.rotateNow(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
