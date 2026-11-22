package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

sealed class UpdateInstallState {
    object Idle : UpdateInstallState()
    data class Downloading(val progressPercent: Int, val currentBytes: Long, val totalBytes: Long) : UpdateInstallState()
    data class NeedsPermission(val apkFile: File) : UpdateInstallState()
    data class ReadyToInstall(val apkFile: File) : UpdateInstallState()
    data class Error(val message: String) : UpdateInstallState()
}

object AppUpdateManager {

    const val LATEST_APK_URL =
        "https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk"

    private const val RELEASES_PATH = "/MorhPanel/WallNice/releases"
    private const val TAG_PATH = "$RELEASES_PATH/tag/"
    private val releaseTagPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

    private val trustedRedirectHosts = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "github-releases.githubusercontent.com",
        "raw.githubusercontent.com"
    )

    fun resolveApkUrl(originalUrl: String): String? {
        val source = Uri.parse(originalUrl.trim().trimEnd('/'))
        if (source.scheme != "https" || source.host != "github.com" || source.port != -1 ||
            source.userInfo != null || source.query != null || source.fragment != null
        ) {
            return null
        }

        val path = source.path ?: return null
        return when {
            path.equals(RELEASES_PATH, ignoreCase = true) ||
                path.equals("$RELEASES_PATH/latest", ignoreCase = true) ||
                path.equals("$RELEASES_PATH/latest/download/app-release.apk", ignoreCase = true) -> LATEST_APK_URL
            isTrustedGithubApkPath(path) -> "https://github.com$path"
            path.startsWith(TAG_PATH, ignoreCase = true) -> {
                val tag = path.substring(TAG_PATH.length)
                    .takeIf { it.isNotEmpty() && releaseTagPattern.matches(it) }
                    ?: return null
                "https://github.com$RELEASES_PATH/download/$tag/app-release.apk"
            }
            else -> null
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
            updatesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk") || file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
            val destinationFile = File(updatesDir, "wallnice_v${versionCode}.apk")
            val tempFile = File(updatesDir, "wallnice_v${versionCode}.apk.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }
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

            try {
                finalConn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalDownloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            coroutineContext.ensureActive()
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead
                            val percent = if (totalBytes > 0) {
                                ((totalDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            onProgress(percent, totalDownloaded, totalBytes)
                        }
                    }
                }
            } finally {
                finalConn.disconnect()
            }

            if (totalBytes > 0 && tempFile.length() != totalBytes) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Incomplete APK download"))
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (hasMatchingPackageSignature(context, tempFile)) {
                    if (tempFile.renameTo(destinationFile)) {
                        Result.success(destinationFile)
                    } else {
                        tempFile.delete()
                        Result.failure(Exception("Failed to finalize update file"))
                    }
                } else {
                    tempFile.delete()
                    Result.failure(Exception("The downloaded APK is not a trusted WallNice update"))
                }
            } else {
                tempFile.delete()
                Result.failure(Exception("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canInstallApk(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
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
            val resolveInfos = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            val systemInstaller = resolveInfos.firstOrNull { info ->
                (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            }
            if (systemInstaller != null) {
                intent.setPackage(systemInstaller.activityInfo.packageName)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    fun isPlayManagedInstall(context: Context): Boolean {
        val packageManager = context.packageManager
        val installerPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            packageManager.getInstallerPackageName(context.packageName)
        }
        return installerPackage == "com.android.vending"
    }

    private fun isTrustedGithubApkPath(path: String): Boolean {
        return (path.startsWith("$RELEASES_PATH/download/", ignoreCase = true) &&
            path.endsWith("/app-release.apk", ignoreCase = true)) ||
            path.equals("$RELEASES_PATH/latest/download/app-release.apk", ignoreCase = true) ||
            path.startsWith("$RELEASES_PATH/latest", ignoreCase = true)
    }

    private fun isTrustedDownloadUrl(value: String, isRedirect: Boolean): Boolean {
        val uri = Uri.parse(value)
        if (uri.scheme != "https" || uri.port != -1 || uri.userInfo != null) {
            return false
        }
        val host = uri.host?.lowercase() ?: return false
        return if (isRedirect) {
            if (host == "github.com") {
                val path = uri.path ?: return false
                path.startsWith("$RELEASES_PATH/download/", ignoreCase = true) ||
                    path.startsWith("$RELEASES_PATH/latest", ignoreCase = true)
            } else {
                host in trustedRedirectHosts
            }
        } else {
            val path = uri.path ?: return false
            host == "github.com" && isTrustedGithubApkPath(path)
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

        val currentVersion = PackageInfoCompat.getLongVersionCode(installedInfo)
        val archiveVersion = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersion <= currentVersion) {
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
        val archiveDigests = archiveSignatures.map(::certificateDigest).toSet()
        return archiveDigests.isNotEmpty() && (archiveDigests == installedDigests || archiveDigests.all { it in installedDigests })
    }

    private fun certificateDigest(signature: android.content.pm.Signature): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
