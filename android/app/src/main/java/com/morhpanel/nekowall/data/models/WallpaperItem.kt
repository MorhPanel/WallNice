package com.morhpanel.nekowall.data.models

enum class MediaType {
    IMAGE,
    GIF,
    VIDEO
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val subcategory: String = "",
    val mediaType: MediaType,
    val url: String,
    val thumbnailUrl: String,
    val resolution: String = "1080x2400",
    val isAmoled: Boolean = false,
    val likesCount: Int = 0,
    val collection: String = "",
    val addedAtEpochSeconds: Long = 0L
)

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String
)

data class CatalogResponse(
    val updateInfo: AppUpdateInfo?,
    val categories: List<String>,
    val wallpapers: List<WallpaperItem>
)
