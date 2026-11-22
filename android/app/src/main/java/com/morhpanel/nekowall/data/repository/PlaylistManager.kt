package com.morhpanel.nekowall.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperFitMode
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.engine.DownloadHelper
import com.morhpanel.nekowall.engine.WallpaperBitmapProcessor
import com.morhpanel.nekowall.engine.WallpaperHelper
import com.morhpanel.nekowall.engine.WallpaperRotationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PlaylistEntry(
    val id: String,
    val title: String,
    val mediaType: MediaType,
    val localPath: String,
    val fitMode: WallpaperFitMode
)

object PlaylistManager {
    private const val KEY_ENTRIES = "playlist_entries"
    private const val KEY_INDEX = "playlist_index"
    private const val REQUEST_CODE = 24031

    private fun prefs(context: Context) = context.getSharedPreferences(WallNicePreferences.PREFS_NAME, Context.MODE_PRIVATE)

    fun getEntries(context: Context): List<PlaylistEntry> {
        val raw = prefs(context).getString(KEY_ENTRIES, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val mediaType = runCatching { MediaType.valueOf(item.optString("mediaType")) }.getOrNull() ?: continue
                    val fitMode = runCatching { WallpaperFitMode.valueOf(item.optString("fitMode")) }.getOrDefault(WallpaperFitMode.CROP)
                    val path = item.optString("localPath")
                    if (path.isBlank()) continue
                    add(
                        PlaylistEntry(
                            id = item.optString("id"),
                            title = item.optString("title", "Wallpaper"),
                            mediaType = mediaType,
                            localPath = path,
                            fitMode = fitMode
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun contains(context: Context, id: String): Boolean {
        return getEntries(context).any { it.id == id }
    }

    suspend fun add(context: Context, wallpaper: WallpaperItem, fitMode: WallpaperFitMode): Boolean = withContext(Dispatchers.IO) {
        if (contains(context, wallpaper.id)) return@withContext true
        val extension = when (wallpaper.mediaType) {
            MediaType.VIDEO -> "mp4"
            MediaType.GIF -> "gif"
            MediaType.IMAGE -> wallpaper.url.substringAfterLast('.', "jpg").substringBefore('?').lowercase()
        }
        val source = DownloadHelper.getCachedFile(context, wallpaper.url, extension)
            ?: DownloadHelper.downloadToCache(context, wallpaper.url, extension)
            ?: return@withContext false
        val playlistDir = File(context.filesDir, "playlist")
        if (!playlistDir.exists() && !playlistDir.mkdirs()) return@withContext false
        val safeId = wallpaper.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val target = File(playlistDir, "${safeId}.${source.extension.ifBlank { extension }}")
        if (target.parentFile?.canonicalFile != playlistDir.canonicalFile) return@withContext false
        runCatching { source.copyTo(target, overwrite = true) }.getOrElse { return@withContext false }
        if (!target.isFile || target.length() <= 0L) return@withContext false

        val next = getEntries(context) + PlaylistEntry(
            id = wallpaper.id,
            title = wallpaper.title,
            mediaType = wallpaper.mediaType,
            localPath = target.absolutePath,
            fitMode = fitMode
        )
        saveEntries(context, next)
        true
    }

    fun remove(context: Context, id: String): Boolean {
        val entries = getEntries(context)
        val removed = entries.firstOrNull { it.id == id } ?: return false
        val playlistDir = File(context.filesDir, "playlist")
        runCatching {
            val target = File(removed.localPath)
            if (target.parentFile?.canonicalFile == playlistDir.canonicalFile) target.delete()
        }
        saveEntries(context, entries.filterNot { it.id == id })
        return true
    }

    suspend fun rotateNow(context: Context): Boolean = withContext(Dispatchers.IO) {
        val availableEntries = getEntries(context).filter { File(it.localPath).isFile }
        val liveEngineActive = WallpaperHelper.isLiveWallpaperActive(context)
        val entries = if (liveEngineActive) {
            availableEntries.filter { it.mediaType == MediaType.VIDEO }
                .ifEmpty { availableEntries.filter { it.mediaType != MediaType.VIDEO } }
        } else {
            availableEntries.filter { it.mediaType != MediaType.VIDEO }
        }
        if (entries.isEmpty()) return@withContext false
        val currentIndex = prefs(context).getInt(KEY_INDEX, -1)
        val nextIndex = (currentIndex + 1).mod(entries.size)
        val entry = entries[nextIndex]
        val file = File(entry.localPath)
        val success = when (entry.mediaType) {
            MediaType.VIDEO -> {
                if (!liveEngineActive) {
                    false
                } else {
                    WallpaperHelper.saveLiveWallpaperConfig(
                        context = context,
                        videoUri = Uri.fromFile(file).toString(),
                        isMuted = WallpaperHelper.isVideoMuted(context),
                        target = WallpaperHelper.getSavedTarget(context),
                        fitMode = entry.fitMode
                    )
                    true
                }
            }
            MediaType.IMAGE, MediaType.GIF -> {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
                val processed = WallpaperBitmapProcessor.processForDevice(context, bitmap, entry.fitMode)
                val applied = WallpaperHelper.applyStaticWallpaper(context, processed, WallpaperHelper.getSavedTarget(context))
                if (processed !== bitmap) processed.recycle()
                bitmap.recycle()
                applied
            }
        }
        if (success) prefs(context).edit().putInt(KEY_INDEX, nextIndex).apply()
        success
    }

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = rotationPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        if (!WallNicePreferences.isRotationEnabled(context) || getEntries(context).isEmpty()) return
        val interval = WallNicePreferences.getRotationIntervalHours(context) * 60L * 60L * 1000L
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + interval,
            interval,
            pendingIntent
        )
    }

    fun cancelSchedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(rotationPendingIntent(context))
    }

    private fun rotationPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WallpaperRotationReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun saveEntries(context: Context, entries: List<PlaylistEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
                    .put("mediaType", entry.mediaType.name)
                    .put("localPath", entry.localPath)
                    .put("fitMode", entry.fitMode.name)
            )
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply()
        if (entries.isEmpty()) cancelSchedule(context) else schedule(context)
    }
}
