package com.morhpanel.nekowall.data.repository

import android.content.Context
import android.net.Uri
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.CatalogResponse
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.MediaTypeResolver
import com.morhpanel.nekowall.data.models.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WallpaperRepository {

    var remoteCatalogUrl: String = "https://raw.githubusercontent.com/MorhPanel/WallNice/main/catalog.json"
    private const val GITHUB_TREE_API_URL = "https://api.github.com/repos/MorhPanel/WallNice/git/trees/main?recursive=1"
    private const val RAW_BASE_URL = "https://raw.githubusercontent.com/MorhPanel/WallNice/main"
    private const val CACHE_FILE_NAME = "catalog_cache.json"
    @Volatile
    var cachedCatalog: CatalogResponse? = null

    fun getOrLoadCachedCatalog(context: Context? = null): CatalogResponse? {
        cachedCatalog?.let { return it }
        if (context != null) {
            val fromDisk = loadCatalogCache(context)
            if (fromDisk != null && fromDisk.wallpapers.isNotEmpty()) {
                cachedCatalog = fromDisk
                return fromDisk
            }
        }
        return null
    }

    fun saveCatalogCache(context: Context, rawJson: String) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(rawJson)
        } catch (_: Exception) {
        }
    }

    fun loadCatalogCache(context: Context): CatalogResponse? {
        return try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (!cacheFile.exists() || cacheFile.length() == 0L) return null
            val rawJson = cacheFile.readText()
            parseCatalogJson(rawJson)
        } catch (_: Exception) {
            null
        }
    }

    fun parseCatalogJson(rawJson: String): CatalogResponse {
        val root = JSONObject(rawJson)
        var appUpdate: AppUpdateInfo? = null
        val categoriesList = mutableListOf<String>("All")
        val wallpapersList = mutableListOf<WallpaperItem>()

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
                try {
                    val item = itemsArray.optJSONObject(i) ?: continue
                    val url = item.optString("url", "")
                    if (!url.startsWith("https://", ignoreCase = true)) continue
                    val type = MediaTypeResolver.fromCatalog(
                        value = item.optString("mediaType", ""),
                        url = url
                    )

                    val rawSubcategory = item.optString("subcategory", "")
                    val resolvedSubcategory = if (rawSubcategory.isNotBlank()) {
                        rawSubcategory
                    } else if (url.contains("/Anime/", ignoreCase = true)) {
                        val afterAnime = Uri.decode(url).substringAfter("/Anime/", "")
                        if (afterAnime.contains("/")) afterAnime.substringBefore("/") else ""
                    } else {
                        ""
                    }

                    wallpapersList.add(
                        WallpaperItem(
                            id = item.optString("id", "wp_$i"),
                            title = item.optString("title", "Untitled"),
                            author = item.optString("author", "morhpanel"),
                            category = item.optString("category", "All"),
                            subcategory = resolvedSubcategory,
                            mediaType = type,
                            url = url,
                            thumbnailUrl = item.optString("thumbnailUrl", url),
                            resolution = item.optString("resolution", "Original"),
                            isAmoled = item.optBoolean("isAmoled", false),
                            likesCount = item.optInt("likesCount", 0),
                            collection = item.optString("collection", resolvedSubcategory),
                            addedAtEpochSeconds = item.optLong("addedAt", 0L)
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }

        val response = CatalogResponse(
            updateInfo = appUpdate,
            categories = categoriesList,
            wallpapers = wallpapersList.reversed()
        )
        cachedCatalog = response
        return response
    }

    suspend fun fetchCatalog(context: Context? = null): Result<CatalogResponse> = withContext(Dispatchers.IO) {
        try {
            var lastFailure: Throwable? = null

            var catalogConn: HttpURLConnection? = null
            try {
                val separator = if (remoteCatalogUrl.contains("?")) "&" else "?"
                catalogConn = URL("$remoteCatalogUrl${separator}cacheBust=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
                catalogConn.connectTimeout = 8000
                catalogConn.readTimeout = 8000
                catalogConn.useCaches = false
                catalogConn.setRequestProperty("Cache-Control", "no-cache, no-store")
                catalogConn.setRequestProperty("Pragma", "no-cache")
                if (catalogConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = catalogConn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseCatalogJson(rawJson)
                    context?.let { saveCatalogCache(it, rawJson) }
                    return@withContext Result.success(parsed)
                } else {
                    lastFailure = Exception("Catalog HTTP: ${catalogConn.responseCode}")
                }
            } catch (e: Exception) {
                lastFailure = e
            } finally {
                catalogConn?.disconnect()
            }

            val treeResult = fetchDirectFromDlsTree()
            if (treeResult.isSuccess) {
                val (treeCats, treeWalls) = treeResult.getOrThrow()
                val categoriesList = mutableListOf<String>("All")
                for (cat in treeCats) {
                    if (!categoriesList.contains(cat)) {
                        categoriesList.add(cat)
                    }
                }
                val treeResponse = CatalogResponse(
                    updateInfo = null,
                    categories = categoriesList,
                    wallpapers = treeWalls
                )
                cachedCatalog = treeResponse
                return@withContext Result.success(treeResponse)
            } else {
                lastFailure = treeResult.exceptionOrNull() ?: lastFailure
            }

            if (context != null) {
                val cached = loadCatalogCache(context)
                if (cached != null && cached.wallpapers.isNotEmpty()) {
                    cachedCatalog = cached
                    return@withContext Result.success(cached)
                }
            }

            Result.failure(lastFailure ?: Exception("No wallpaper source is available"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchDirectFromDlsTree(): Result<Pair<List<String>, List<WallpaperItem>>> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(GITHUB_TREE_API_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "WallNice-Android-Client")

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
                val detectedMediaType = MediaTypeResolver.fromUrl("https://wallnice.invalid/$fileName")

                val mediaType = when {
                    detectedMediaType == MediaType.VIDEO || subFolder == "video" -> {
                        category = "Live Video"
                        MediaType.VIDEO
                    }
                    detectedMediaType == MediaType.GIF || subFolder == "gif" -> {
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

                val title = cleanTitle(fileName)

                val encodedPath = path.split("/").joinToString("/") { segment ->
                    URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                }
                val rawUrl = "$RAW_BASE_URL/$encodedPath"
                val pathHash = java.security.MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).joinToString("") { "%02x".format(it) }.take(8)

                val subcategory = if (path.startsWith("img/Anime/", ignoreCase = true)) {
                    path.split("/").getOrNull(2) ?: ""
                } else {
                    ""
                }

                wallpapers.add(
                    WallpaperItem(
                        id = "dls_$pathHash",
                        title = title,
                        author = "morhpanel",
                        category = category,
                        subcategory = subcategory,
                        mediaType = mediaType,
                        url = rawUrl,
                        thumbnailUrl = rawUrl,
                        resolution = "Original",
                        isAmoled = false,
                        likesCount = 0,
                        collection = subcategory,
                        addedAtEpochSeconds = 0L
                    )
                )
            }

            Result.success(Pair(categories, wallpapers.reversed()))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun cleanTitle(fileName: String): String {
        val stem = fileName.substringBeforeLast(".")
        val override = when (stem) {
            "yotsuba_nakano" -> "Yotsuba Nakano Monochrome"
            "yotsuba_nakano_5c7a2e8d" -> "Yotsuba Nakano Spring"
            else -> null
        }
        return override ?: stem
            .replace(Regex("_[a-fA-F0-9]{8,16}$"), "")
            .replace("_", " ")
            .replace("-", " ")
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }
}
