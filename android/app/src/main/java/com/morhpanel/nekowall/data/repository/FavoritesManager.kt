package com.morhpanel.nekowall.data.repository

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object FavoritesManager {

    private const val PREFS_NAME = "wallnice_favorites"
    private const val KEY_FAVORITES = "favorite_wallpaper_ids"

    private val _favoriteIds = mutableStateOf<Set<String>>(emptySet())
    val favoriteIds: State<Set<String>> = _favoriteIds

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = saved.toSet()
        isInitialized = true
    }

    fun isFavorite(id: String): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun toggleFavorite(context: Context, id: String): Boolean {
        init(context)
        val current = _favoriteIds.value.toMutableSet()
        val nowFavorite = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        _favoriteIds.value = current.toSet()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return nowFavorite
    }
}
