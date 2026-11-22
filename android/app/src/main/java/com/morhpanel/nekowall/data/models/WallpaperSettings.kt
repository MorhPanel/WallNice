package com.morhpanel.nekowall.data.models

enum class WallpaperFitMode(val label: String) {
    CROP("Fill"),
    FIT("Fit"),
    BLUR("Blur")
}

enum class PlaybackQuality(
    val label: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxAutoPlayCards: Int
) {
    ECO("Eco", 720, 1280, 1),
    BALANCED("Balanced", 1080, 1920, 2),
    MAXIMUM("Maximum", Int.MAX_VALUE, Int.MAX_VALUE, 4)
}
