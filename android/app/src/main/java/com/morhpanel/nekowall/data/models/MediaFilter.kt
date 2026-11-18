package com.morhpanel.nekowall.data.models

enum class MediaFilter(
    val label: String,
    val mediaType: MediaType?
) {
    ALL("All", null),
    PHOTOS("Photos", MediaType.IMAGE),
    VIDEOS("Videos", MediaType.VIDEO),
    GIFS("GIFs", MediaType.GIF)
}

fun MediaFilter.matches(item: WallpaperItem): Boolean {
    return mediaType == null || item.mediaType == mediaType
}
