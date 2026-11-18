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
            LATEST_APK_URL
        } else if (trimmed.contains("/releases/tag/", ignoreCase = true)) {
            val tag = trimmed.substringAfter("/releases/tag/").substringBefore('/')
            "https://github.com/MorhPanel/WallNice/releases/download/$tag/app-release.apk"
        } else if (trimmed.endsWith("/releases", ignoreCase = true)) {
            LATEST_APK_URL
        } else {
            null
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

    private fun isTrustedGithubApkUrl(value: String): Boolean {
        val uri = Uri.parse(value)
        val path = uri.path ?: return false
        return uri.scheme == "https" &&
            uri.host == "github.com" &&
            path.startsWith("/MorhPanel/WallNice/releases/") &&
            path.endsWith("/app-release.apk", ignoreCase = true)
    }
}
