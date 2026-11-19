package com.morhpanel.nekowall.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

object DownloadHelper {

    private val ALLOWED_EXTENSIONS = setOf("mp4", "jpg", "jpeg", "png", "gif", "webp")

    private fun sanitizeExtension(ext: String, default: String): String {
        val clean = ext.removePrefix(".").lowercase().trim()
        return if (clean in ALLOWED_EXTENSIONS) clean else default
    }

    private fun urlHash(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

    fun getCachedFile(context: Context, fileUrl: String, extension: String): File? {
        if (!fileUrl.startsWith("https://", ignoreCase = true)) {
            return null
        }
        val safeExt = sanitizeExtension(extension, "jpg")
        val fileName = "cached_wp_${urlHash(fileUrl)}.$safeExt"
        val targetFile = File(context.cacheDir, fileName)
        if (!targetFile.canonicalPath.startsWith(context.cacheDir.canonicalPath)) {
            return null
        }
        return if (targetFile.exists() && targetFile.isFile && targetFile.length() > 0L && !targetFile.name.endsWith(".tmp")) {
            targetFile
        } else {
            null
        }
    }

    fun isCached(context: Context, fileUrl: String, extension: String): Boolean {
        return getCachedFile(context, fileUrl, extension) != null
    }

    suspend fun downloadToCache(context: Context, fileUrl: String, extension: String): File? = withContext(Dispatchers.IO) {
        if (!fileUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext null
        }
        val safeExt = sanitizeExtension(extension, "jpg")
        val fileName = "cached_wp_${urlHash(fileUrl)}.$safeExt"
        val targetFile = File(context.cacheDir, fileName)
        if (!targetFile.canonicalPath.startsWith(context.cacheDir.canonicalPath)) {
            return@withContext null
        }

        if (targetFile.exists() && targetFile.isFile && targetFile.length() > 0L && !targetFile.name.endsWith(".tmp")) {
            return@withContext targetFile
        }

        val tmpFile = File(context.cacheDir, "$fileName.${System.nanoTime()}.tmp")
        var isRenamed = false
        try {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                if (tmpFile.exists()) {
                    tmpFile.delete()
                }
                return@withContext null
            }

            connection.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            if (tmpFile.exists() && tmpFile.isFile && tmpFile.length() > 0L) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (tmpFile.renameTo(targetFile) && targetFile.exists() && targetFile.isFile && targetFile.length() > 0L) {
                    isRenamed = true
                    targetFile
                } else {
                    if (tmpFile.exists()) {
                        tmpFile.delete()
                    }
                    null
                }
            } else {
                if (tmpFile.exists()) {
                    tmpFile.delete()
                }
                null
            }
        } catch (e: Exception) {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            null
        } finally {
            if (!isRenamed && tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    suspend fun saveToGallery(
        context: Context,
        fileUrl: String,
        title: String,
        isVideo: Boolean,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        var uri: Uri? = null
        var isCompleted = false
        try {
            val defaultExtension = if (isVideo) "mp4" else "jpg"
            val cachedFile = getCachedFile(context, fileUrl, defaultExtension)
                ?: if (!isVideo) (getCachedFile(context, fileUrl, "png") ?: getCachedFile(context, fileUrl, "gif")) else null
            val extension = cachedFile?.extension?.ifEmpty { defaultExtension } ?: defaultExtension
            val mimeType = when (extension.lowercase()) {
                "mp4" -> "video/mp4"
                "gif" -> "image/gif"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> if (isVideo) "video/mp4" else "image/jpeg"
            }
            val safeTitle = "WallNice_${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${System.currentTimeMillis()}"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeTitle.$extension")
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = if (isVideo) {
                        "${Environment.DIRECTORY_MOVIES}/WallNice"
                    } else {
                        "${Environment.DIRECTORY_PICTURES}/WallNice"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            uri = context.contentResolver.insert(collection, contentValues) ?: return@withContext false

            val totalBytes: Long
            val inputStream = if (cachedFile != null) {
                totalBytes = cachedFile.length()
                cachedFile.inputStream()
            } else {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP ${connection.responseCode}")
                }
                totalBytes = connection.contentLength.toLong()
                connection.inputStream
            }

            var downloadedBytes = 0L

            inputStream.use { input ->
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Unable to open output stream")
                outputStream.use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0L) {
                            val progress = ((downloadedBytes * 100L) / totalBytes).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (downloadedBytes <= 0L) {
                throw IOException("Empty download content")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            isCompleted = true
            true
        } catch (e: Exception) {
            if (uri != null) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (ignored: Exception) {
                }
            }
            false
        } finally {
            if (!isCompleted && uri != null) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (ignored: Exception) {
                }
            }
        }
    }
}
