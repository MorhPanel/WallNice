package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.security.MessageDigest

sealed class UpdateInstallState {
    object Idle : UpdateInstallState()
    data class Downloading(val progressPercent: Int, val currentBytes: Long, val totalBytes: Long) : UpdateInstallState()
    data class NeedsPermission(val apkFile: File) : UpdateInstallState()
    data class ReadyToInstall(val apkFile: File) : UpdateInstallState()
    data class Error(val message: String) : UpdateInstallState()
}

object AppUpdateManager {

    private val trustedRedirectHosts = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    fun resolveApkUrl(originalUrl: String): String? {
        val trimmed = originalUrl.trim().trimEnd('/')

        if (trimmed.endsWith(".apk", ignoreCase = true)) {
            return trimmed.takeIf(::isTrustedGithubApkUrl)
        }

        val source = Uri.parse(trimmed)
        if (source.scheme != "https" || source.host != "github.com" ||
            source.path?.startsWith("/MorhPanel/WallNice/releases") != true
        ) {
            return null
        }

        return if (trimmed.endsWith("/releases/latest", ignoreCase = true)) {
            "$trimmed/download/app-release.apk"
        } else if (trimmed.contains("/releases/tag/", ignoreCase = true)) {
            val tag = trimmed.substringAfter("/releases/tag/").substringBefore('/')
            "https://github.com/MorhPanel/WallNice/releases/download/$tag/app-release.apk"
        } else if (trimmed.endsWith("/releases", ignoreCase = true)) {
            "https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk"
        } else {
            null
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
                ?: return@withContext Result.failure(Exception("The update source is not trusted"))
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
            var downloadReady = false

            while (redirects < maxRedirects) {
                if (!isTrustedDownloadUrl(currentUrl, redirects > 0)) {
                    return@withContext Result.failure(Exception("The update redirect is not trusted"))
                }
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
                    downloadReady = true
                    break
                } else {
                    connection.disconnect()
                    return@withContext Result.failure(Exception("HTTP error $status"))
                }
            }

            if (!downloadReady) {
                connection?.disconnect()
                return@withContext Result.failure(Exception("Too many redirects"))
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
                if (hasMatchingPackageSignature(context, destinationFile)) {
                    Result.success(destinationFile)
                } else {
                    destinationFile.delete()
                    Result.failure(Exception("The downloaded APK is not a trusted WallNice update"))
                }
            } else {
                Result.failure(Exception("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isTrustedGithubApkUrl(value: String): Boolean {
        val uri = Uri.parse(value)
        val path = uri.path ?: return false
        return uri.scheme == "https" &&
            uri.host == "github.com" &&
            path.startsWith("/MorhPanel/WallNice/releases/") &&
            path.endsWith("/app-release.apk", ignoreCase = true)
    }

    private fun isTrustedDownloadUrl(value: String, isRedirect: Boolean): Boolean {
        val uri = Uri.parse(value)
        if (uri.scheme != "https") {
            return false
        }
        val host = uri.host?.lowercase() ?: return false
        return if (isRedirect) {
            if (host == "github.com") {
                uri.path?.startsWith("/MorhPanel/WallNice/releases/") == true
            } else {
                host in trustedRedirectHosts
            }
        } else {
            isTrustedGithubApkUrl(value)
        }
    }

    @Suppress("DEPRECATION")
    private fun hasMatchingPackageSignature(context: Context, apkFile: File): Boolean {
        val packageManager = context.packageManager
        val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
        } ?: return false

        if (archiveInfo.packageName != context.packageName) {
            return false
        }

        val installedSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = installedInfo.signingInfo ?: return false
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            installedInfo.signatures ?: return false
        }
        val archiveSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = archiveInfo.signingInfo ?: return false
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            archiveInfo.signatures ?: return false
        }

        val installedDigests = installedSignatures.map(::certificateDigest).toSet()
        return archiveSignatures.any { certificateDigest(it) in installedDigests }
    }

    private fun certificateDigest(signature: android.content.pm.Signature): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
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
