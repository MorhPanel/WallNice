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
import java.net.URLEncoder

object WallpaperRepository {

    var remoteCatalogUrl: String = "https://raw.githubusercontent.com/MorhPanel/WallNice/main/catalog.json"
    private const val GITHUB_TREE_API_URL = "https://api.github.com/repos/MorhPanel/WallNice/git/trees/main?recursive=1"
    private const val RAW_BASE_URL = "https://raw.githubusercontent.com/MorhPanel/WallNice/main"

    suspend fun fetchCatalog(): Result<CatalogResponse> = withContext(Dispatchers.IO) {
        try {
            var appUpdate: AppUpdateInfo? = null
            var categoriesList = mutableListOf<String>("All")
            var wallpapersList = mutableListOf<WallpaperItem>()

            try {
                val separator = if (remoteCatalogUrl.contains("?")) "&" else "?"
                val catalogConn = URL("$remoteCatalogUrl${separator}cacheBust=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
                catalogConn.connectTimeout = 8000
                catalogConn.readTimeout = 8000
                catalogConn.useCaches = false
                catalogConn.setRequestProperty("Cache-Control", "no-cache, no-store")
                catalogConn.setRequestProperty("Pragma", "no-cache")
                if (catalogConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = catalogConn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(rawJson)

                    if (root.has("appUpdate")) {
                        val updateObj = root.getJSONObject("appUpdate")
                        appUpdate = AppUpdateInfo(
                            versionCode = updateObj.optInt("versionCode", 0),
                            versionName = updateObj.optString("versionName", ""),
                            changelog = updateObj.optString("changelog", ""),
                            downloadUrl = updateObj.optString("downloadUrl", "")
                        )
                    }

                    if (root.has("categories")) {
                        val categoriesArray = root.getJSONArray("categories")
                        for (i in 0 until categoriesArray.length()) {
                            val cat = categoriesArray.getString(i)
                            if (!cat.equals("All", ignoreCase = true) && !categoriesList.contains(cat)) {
                                categoriesList.add(cat)
                            }
                        }
                    }

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
                                    author = item.optString("author", "morhpanel"),
                                    category = item.optString("category", "All"),
                                    mediaType = type,
                                    url = item.getString("url"),
                                    thumbnailUrl = item.optString("thumbnailUrl", item.getString("url")),
                                    resolution = item.optString("resolution", "Original"),
                                    isAmoled = item.optBoolean("isAmoled", false),
                                    likesCount = item.optInt("likesCount", 0)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
            }

            if (wallpapersList.isEmpty()) {
                val treeResult = fetchDirectFromDlsTree()
                if (treeResult.isSuccess) {
                    val (treeCats, treeWalls) = treeResult.getOrThrow()
                    for (cat in treeCats) {
                        if (!categoriesList.contains(cat)) {
                            categoriesList.add(cat)
                        }
                    }
                    wallpapersList.addAll(treeWalls)
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

    private fun fetchDirectFromDlsTree(): Result<Pair<List<String>, List<WallpaperItem>>> {
        return try {
            val url = URL(GITHUB_TREE_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "NekoWall-Android-Client")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("Tree API HTTP: ${connection.responseCode}"))
            }

            val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(rawJson)
            val treeArray = root.optJSONArray("tree") ?: return Result.failure(Exception("No tree found"))

            val categories = mutableListOf<String>()
            val wallpapers = mutableListOf<WallpaperItem>()

            for (i in 0 until treeArray.length()) {
                val node = treeArray.getJSONObject(i)
                val type = node.optString("type")
                val path = node.optString("path")

                if (type != "blob" || !path.startsWith("DLS/")) {
                    continue
                }

                val fileName = path.substringAfterLast("/")
                if (fileName.startsWith(".") || fileName == ".gitkeep") {
                    continue
                }

                val segments = path.split("/")
                val subFolder = if (segments.size > 1) segments[1].lowercase() else ""
                var category = "All"

                val ext = fileName.substringAfterLast(".", "").lowercase()
                val mediaType = when {
                    ext in listOf("mp4", "webm", "mkv") || subFolder == "video" -> {
                        category = "Live Video"
                        MediaType.VIDEO
                    }
                    ext in listOf("gif") || subFolder == "gif" -> {
                        category = "GIF Loops"
                        MediaType.GIF
                    }
                    else -> {
                        if (subFolder == "img" && segments.size >= 4 && segments[2].equals("Anime", ignoreCase = true)) {
                            category = "Anime"
                            val animeTitle = segments[3]
                            if (!categories.contains("Anime")) categories.add("Anime")
                            if (!categories.contains(animeTitle)) categories.add(animeTitle)
                        } else if (segments.size > 2) {
                            category = segments[2]
                        }
                        MediaType.IMAGE
                    }
                }

                if (category != "All" && !categories.contains(category)) {
                    categories.add(category)
                }

                val title = fileName.substringBeforeLast(".")
                    .replace("_", " ")
                    .replace("-", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                val encodedPath = path.split("/").joinToString("/") { segment ->
                    URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                }
                val rawUrl = "$RAW_BASE_URL/$encodedPath"

                wallpapers.add(
                    WallpaperItem(
                        id = "dls_$i",
                        title = title,
                        author = "morhpanel",
                        category = category,
                        mediaType = mediaType,
                        url = rawUrl,
                        thumbnailUrl = rawUrl,
                        resolution = "Original",
                        isAmoled = false,
                        likesCount = 0
                    )
                )
            }

            Result.success(Pair(categories, wallpapers))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
