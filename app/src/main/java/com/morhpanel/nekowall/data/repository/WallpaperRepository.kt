package com.morhpanel.nekowall.data.repository

import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.CatalogResponse
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WallpaperRepository {

    var remoteCatalogUrl: String = "https://raw.githubusercontent.com/MorhPanel/WallNice/main/catalog.json"

    suspend fun fetchCatalog(): Result<CatalogResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(remoteCatalogUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP Error: ${connection.responseCode}"))
            }

            val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(rawJson)

            var appUpdate: AppUpdateInfo? = null
            if (root.has("appUpdate")) {
                val updateObj = root.getJSONObject("appUpdate")
                appUpdate = AppUpdateInfo(
                    versionCode = updateObj.optInt("versionCode", 0),
                    versionName = updateObj.optString("versionName", ""),
                    changelog = updateObj.optString("changelog", ""),
                    downloadUrl = updateObj.optString("downloadUrl", "")
                )
            }

            val categoriesList = mutableListOf<String>()
            categoriesList.add("All")
            if (root.has("categories")) {
                val categoriesArray = root.getJSONArray("categories")
                for (i in 0 until categoriesArray.length()) {
                    val cat = categoriesArray.getString(i)
                    if (!cat.equals("All", ignoreCase = true)) {
                        categoriesList.add(cat)
                    }
                }
            }

            val wallpapersList = mutableListOf<WallpaperItem>()
            if (root.has("wallpapers")) {
                val itemsArray = root.getJSONArray("wallpapers")
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    val mediaTypeStr = item.optString("mediaType", "IMAGE")
                    val type = when (mediaTypeStr.uppercase()) {
                        "VIDEO" -> MediaType.VIDEO
                        "GIF" -> MediaType.GIF
                        else -> MediaType.IMAGE
                    }

                    wallpapersList.add(
                        WallpaperItem(
                            id = item.optString("id", "wp_$i"),
                            title = item.optString("title", "Untitled"),
                            author = item.optString("author", "Unknown"),
                            category = item.optString("category", "All"),
                            mediaType = type,
                            url = item.getString("url"),
                            thumbnailUrl = item.optString("thumbnailUrl", item.getString("url")),
                            resolution = item.optString("resolution", "1080x2400"),
                            isAmoled = item.optBoolean("isAmoled", false),
                            likesCount = item.optInt("likesCount", 0)
                        )
                    )
                }
            }

            Result.success(
                CatalogResponse(
                    updateInfo = appUpdate,
                    categories = categoriesList,
                    wallpapers = wallpapersList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
