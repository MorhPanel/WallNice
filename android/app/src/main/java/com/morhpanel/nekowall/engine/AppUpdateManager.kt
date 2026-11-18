package com.morhpanel.nekowall.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

sealed class UpdateInstallState {
    object Idle : UpdateInstallState()
    object OpeningDownload : UpdateInstallState()
    object DownloadStarted : UpdateInstallState()
    data class Error(val message: String) : UpdateInstallState()
}

object AppUpdateManager {

    const val LATEST_APK_URL =
        "https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk"

    private const val RELEASES_PATH = "/MorhPanel/WallNice/releases"
    private const val TAG_PATH = "$RELEASES_PATH/tag/"
    private val releaseTagPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

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
                path.equals("$RELEASES_PATH/latest", ignoreCase = true) -> LATEST_APK_URL
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

    fun openApkDownload(context: Context, originalUrl: String): Result<Unit> {
        val safeUrl = resolveApkUrl(originalUrl)
            ?: return Result.failure(Exception("The update source is not trusted"))

        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
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
        return path.startsWith("$RELEASES_PATH/download/", ignoreCase = true) &&
            path.endsWith("/app-release.apk", ignoreCase = true)
    }
}
