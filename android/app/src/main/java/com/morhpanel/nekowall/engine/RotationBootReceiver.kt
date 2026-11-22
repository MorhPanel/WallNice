package com.morhpanel.nekowall.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.morhpanel.nekowall.data.repository.PlaylistManager

class RotationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            PlaylistManager.schedule(context.applicationContext)
        }
    }
}
