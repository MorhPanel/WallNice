package com.morhpanel.nekowall.data.models

import android.net.Uri
import java.util.Locale

object MediaTypeResolver {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "avif")
    private val videoExtensions = setOf("mp4", "webm", "mkv", "m4v", "mov")

    fun fromCatalog(value: String?, url: String): MediaType {
        return when (value?.trim()?.uppercase(Locale.US)) {
            "VIDEO" -> MediaType.VIDEO
            "GIF" -> MediaType.GIF
            "IMAGE", "PHOTO" -> MediaType.IMAGE
            else -> fromUrl(url)
        }
    }

    fun fromUrl(url: String): MediaType {
        val path = runCatching { Uri.parse(url).path.orEmpty() }.getOrDefault("")
        val extension = path.substringAfterLast('.', "").lowercase(Locale.US)
        return when {
            extension == "gif" -> MediaType.GIF
            extension in videoExtensions -> MediaType.VIDEO
            extension in imageExtensions -> MediaType.IMAGE
            else -> MediaType.IMAGE
        }
    }

    fun label(mediaType: MediaType): String {
        return when (mediaType) {
            MediaType.IMAGE -> "Photo"
            MediaType.VIDEO -> "Video"
            MediaType.GIF -> "GIF"
        }
    }
}
