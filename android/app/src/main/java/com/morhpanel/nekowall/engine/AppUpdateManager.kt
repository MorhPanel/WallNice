package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateInstallState {
    object Idle : UpdateInstallState()
    data class Downloading(val progressPercent: Int, val currentBytes: Long, val totalBytes: Long) : UpdateInstallState()
    data class NeedsPermission(val apkFile: File) : UpdateInstallState()
    data class ReadyToInstall(val apkFile: File) : UpdateInstallState()
    data class Error(val message: String) : UpdateInstallState()
}

object AppUpdateManager {

    fun resolveApkUrl(originalUrl: String): String {
        if (originalUrl.endsWith(".apk", ignoreCase = true)) {
            return originalUrl
        }
        val trimmed = originalUrl.trimEnd('/')
        return if (trimmed.endsWith("/releases/latest", ignoreCase = true)) {
            "$trimmed/download/app-release.apk"
        } else if (trimmed.contains("/releases/tag/", ignoreCase = true)) {
            val tag = trimmed.substringAfter("/releases/tag/").substringBefore('/')
            "https://github.com/MorhPanel/WallNice/releases/download/$tag/app-release.apk"
        } else if (trimmed.endsWith("/releases", ignoreCase = true)) {
            "https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk"
        } else {
            "https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk"
        }
    }

    suspend fun downloadApk(
        context: Context,
        urlStr: String,
        versionCode: Int,
        onProgress: (progress: Int, currentBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = resolveApkUrl(urlStr)
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            val destinationFile = File(updatesDir, "wallnice_v${versionCode}.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = targetUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "WallNice-AppUpdateEngine")

                val status = connection.responseCode
                if (status in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl == null) {
                        return@withContext Result.failure(Exception("Redirect location missing"))
                    }
                    currentUrl = if (newUrl.startsWith("http")) newUrl else URL(url, newUrl).toString()
                    redirects++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    connection.disconnect()
                    return@withContext Result.failure(Exception("HTTP error $status"))
                }
            }

            val finalConn = connection ?: return@withContext Result.failure(Exception("Connection failed"))
            val totalBytes = finalConn.contentLengthLong
            var downloadedBytes = 0L

            finalConn.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            -1
                        }
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }
            finalConn.disconnect()

            if (destinationFile.exists() && destinationFile.length() > 0) {
                Result.success(destinationFile)
            } else {
                Result.failure(Exception("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canInstallApk(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchInstaller(context: Context, apkFile: File): Result<Unit> {
        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
