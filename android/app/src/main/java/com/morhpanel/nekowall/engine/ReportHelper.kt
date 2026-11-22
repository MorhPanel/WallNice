package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.morhpanel.nekowall.data.models.WallpaperItem

object ReportHelper {
    fun openWallpaperReport(context: Context, wallpaper: WallpaperItem): Boolean {
        val title = "Wallpaper issue: ${wallpaper.title}"
        val body = "Wallpaper ID: ${wallpaper.id}\nTitle: ${wallpaper.title}\nMedia: ${wallpaper.mediaType}\nURL: ${wallpaper.url}\n\nDescribe the problem:\n"
        val uri = Uri.parse("https://github.com/MorhPanel/WallNice/issues/new").buildUpon()
            .appendQueryParameter("title", title)
            .appendQueryParameter("body", body)
            .appendQueryParameter("labels", "wallpaper-report")
            .build()
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }
}
