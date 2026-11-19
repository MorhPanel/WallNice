package com.morhpanel.nekowall.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.MediaFilter
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.data.models.matches
import com.morhpanel.nekowall.data.repository.FavoritesManager
import com.morhpanel.nekowall.data.repository.WallpaperRepository
import com.morhpanel.nekowall.ui.components.BrandMark
import com.morhpanel.nekowall.ui.components.CategoryPill
import com.morhpanel.nekowall.ui.components.EmptyStatePanel
import com.morhpanel.nekowall.ui.components.SkeletonWallpaperCard
import com.morhpanel.nekowall.ui.components.WallNiceIconButton
import com.morhpanel.nekowall.ui.components.WallpaperCard
import com.morhpanel.nekowall.ui.theme.AmoledBlack
import com.morhpanel.nekowall.ui.theme.NeonCyan
import com.morhpanel.nekowall.ui.theme.NeonPink
import com.morhpanel.nekowall.ui.theme.OutlineStrong
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.SurfaceMid
import com.morhpanel.nekowall.ui.theme.TextMuted
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceMotion
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import kotlinx.coroutines.launch

private enum class CatalogContentState {
    LOADING,
    OFFLINE,
    EMPTY,
    NO_RESULTS,
    CONTENT
}

private val AnimeSubcategorySetSaver: Saver<Set<String>, List<String>> = Saver(
    save = { it.toList() },
    restore = { it.toSet().ifEmpty { setOf("All Anime") } }
)

private fun toggleAnimeSubcategory(
    current: Set<String>,
    subcategory: String,
    allAvailable: List<String>
): Set<String> {
    val specificSubs = allAvailable.filter { it != "All Anime" }.toSet()
    if (subcategory == "All Anime") {
        return setOf("All Anime")
    }

    val activeSpecific = if ("All Anime" in current) {
        emptySet()
    } else {
        current.filter { it != "All Anime" }.toSet()
    }

    val nextSpecific = if (subcategory in activeSpecific) {
        activeSpecific - subcategory
    } else {
        activeSpecific + subcategory
    }

    return when {
        nextSpecific.isEmpty() || (specificSubs.isNotEmpty() && nextSpecific == specificSubs) -> setOf("All Anime")
        else -> nextSpecific
    }
}

private val MediaFilterSetSaver: Saver<Set<MediaFilter>, List<String>> = Saver(
    save = { set -> set.map { it.name } },
    restore = { list ->
        list.mapNotNull { name ->
            MediaFilter.entries.firstOrNull { it.name == name }
        }.toSet().ifEmpty { setOf(MediaFilter.ALL) }
    }
)

private fun toggleMediaFilter(current: Set<MediaFilter>, filter: MediaFilter): Set<MediaFilter> {
    val specificFilters = setOf(MediaFilter.PHOTOS, MediaFilter.VIDEOS, MediaFilter.GIFS)
    if (filter == MediaFilter.ALL) {
        return setOf(MediaFilter.ALL)
    }

    val activeSpecific = if (MediaFilter.ALL in current) {
        emptySet()
    } else {
        current.filter { it != MediaFilter.ALL }.toSet()
    }

    val nextSpecific = if (filter in activeSpecific) {
        activeSpecific - filter
    } else {
        activeSpecific + filter
    }

    return when {
        nextSpecific.isEmpty() || nextSpecific == specificFilters -> setOf(MediaFilter.ALL)
        else -> nextSpecific
    }
}

private data class SearchableWallpaper(
    val item: WallpaperItem,
    val decodedUrl: String
)

@Composable
fun HomeScreen(
    isActive: Boolean = true,
    onWallpaperSelected: (WallpaperItem) -> Unit,
    onUpdateAvailable: (AppUpdateInfo) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowInfo = LocalWindowInfo.current
    val favoriteIds by FavoritesManager.favoriteIds
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedAnimeSubcategories by rememberSaveable(stateSaver = AnimeSubcategorySetSaver) { mutableStateOf(setOf("All Anime")) }
    var isAnimeDropdownOpen by rememberSaveable { mutableStateOf(false) }
    var selectedMediaFilter by rememberSaveable(stateSaver = MediaFilterSetSaver) { mutableStateOf(setOf(MediaFilter.ALL)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf("All")) }
    var wallpapers by remember { mutableStateOf<List<WallpaperItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()

    var isAppForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> isAppForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val canPlayVideos = isActive && !showSettings && !isAnimeDropdownOpen && isAppForeground && windowInfo.isWindowFocused && !gridState.isScrollInProgress

    var visibleItemKeys by remember { mutableStateOf(emptySet<Any>()) }

    LaunchedEffect(canPlayVideos, gridState) {
        if (!canPlayVideos) {
            visibleItemKeys = emptySet()
            return@LaunchedEffect
        }
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val visibleTop = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding
            val visibleEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            val center = (visibleTop + visibleEnd) / 2

            val fullyVisible = layoutInfo.visibleItemsInfo.filter { item ->
                val itemTop = item.offset.y
                val itemBottom = item.offset.y + item.size.height
                itemTop >= visibleTop && itemBottom <= visibleEnd
            }

            fullyVisible.sortedBy { item ->
                val itemCenter = item.offset.y + (item.size.height / 2)
                abs(itemCenter - center)
            }.take(2).map { it.key }.toSet()
        }
        .collectLatest { keys ->
            delay(200L)
            visibleItemKeys = keys
        }
    }

    fun loadData() {
        if (isLoading && wallpapers.isNotEmpty()) return
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            WallpaperRepository.fetchCatalog(context)
                .onSuccess { response ->
                    val baseCategories = response.categories.filter { !it.equals("All", ignoreCase = true) && !it.equals("Favorites", ignoreCase = true) }
                    val nextCategories = listOf("All", "Favorites") + baseCategories
                    categories = nextCategories
                    wallpapers = response.wallpapers
                    if (selectedCategory !in nextCategories && selectedCategory != "Anime") {
                        selectedCategory = "All"
                    }
                    response.updateInfo?.let(onUpdateAvailable)
                }
                .onFailure { error ->
                    errorMessage = error.localizedMessage ?: "The catalog could not be reached."
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        FavoritesManager.init(context)
        loadData()
    }

    val searchableWallpapers = remember(wallpapers) {
        wallpapers.map { item ->
            SearchableWallpaper(
                item = item,
                decodedUrl = Uri.decode(item.url)
            )
        }
    }

    val animeSubcategories = remember(searchableWallpapers) {
        val subs = mutableSetOf<String>()
        searchableWallpapers.forEach { entry ->
            val sub = entry.item.subcategory.ifBlank {
                if (entry.decodedUrl.contains("/Anime/", ignoreCase = true)) {
                    entry.decodedUrl.substringAfter("/Anime/", "").substringBefore("/")
                } else {
                    ""
                }
            }
            if (sub.isNotBlank() && !sub.endsWith(".jpg", ignoreCase = true) && !sub.endsWith(".png", ignoreCase = true)) {
                subs.add(sub)
            }
        }
        listOf("All Anime") + subs.sorted()
    }

    val animeSubcategoryCounts = remember(searchableWallpapers, animeSubcategories) {
        animeSubcategories.associateWith { sub ->
            if (sub == "All Anime") {
                searchableWallpapers.count { entry ->
                    entry.item.category.equals("Anime", ignoreCase = true) ||
                        entry.decodedUrl.contains("/Anime/", ignoreCase = true)
                }
            } else {
                searchableWallpapers.count { entry ->
                    (entry.item.category.equals("Anime", ignoreCase = true) || entry.decodedUrl.contains("/Anime/", ignoreCase = true)) &&
                        (entry.item.subcategory.equals(sub, ignoreCase = true) || entry.decodedUrl.contains("/$sub/", ignoreCase = true))
                }
            }
        }
    }

    val displayCategories = remember(categories, animeSubcategories) {
        val subSet = animeSubcategories.filter { it != "All Anime" }.toSet()
        val cleaned = categories.filter { it !in subSet }
        if ("Anime" !in cleaned) {
            cleaned + "Anime"
        } else {
            cleaned
        }
    }

    val filteredList = remember(selectedCategory, selectedAnimeSubcategories, selectedMediaFilter, searchQuery, searchableWallpapers, favoriteIds) {
        val query = searchQuery.trim()
        searchableWallpapers.mapNotNull { entry ->
            val item = entry.item
            if (!selectedMediaFilter.matches(item)) return@mapNotNull null

            val decodedUrl = entry.decodedUrl
            val categoryMatches = when (selectedCategory) {
                "All" -> true
                "Favorites" -> favoriteIds.contains(item.id)
                "Live Video" -> item.mediaType == MediaType.VIDEO
                "GIF Loops" -> item.mediaType == MediaType.GIF
                "AMOLED 4K" -> item.isAmoled
                "Anime" -> {
                    val isAnime = item.category.equals("Anime", ignoreCase = true) ||
                        decodedUrl.contains("/Anime/", ignoreCase = true)
                    if (!isAnime) {
                        false
                    } else if ("All Anime" in selectedAnimeSubcategories || selectedAnimeSubcategories.isEmpty()) {
                        true
                    } else {
                        selectedAnimeSubcategories.any { sub ->
                            item.subcategory.equals(sub, ignoreCase = true) ||
                                decodedUrl.contains("/$sub/", ignoreCase = true)
                        }
                    }
                }
                else -> item.category.equals(selectedCategory, ignoreCase = true) ||
                    decodedUrl.contains("/$selectedCategory/", ignoreCase = true) ||
                    decodedUrl.contains(selectedCategory, ignoreCase = true)
            }
            if (!categoryMatches) return@mapNotNull null

            val searchMatches = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.author.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.subcategory.contains(query, ignoreCase = true) ||
                decodedUrl.contains(query, ignoreCase = true)

            if (searchMatches) item else null
        }
    }

    val catalogState = when {
        isLoading && wallpapers.isEmpty() -> CatalogContentState.LOADING
        errorMessage != null && wallpapers.isEmpty() -> CatalogContentState.OFFLINE
        wallpapers.isEmpty() -> CatalogContentState.EMPTY
        filteredList.isEmpty() -> CatalogContentState.NO_RESULTS
        else -> CatalogContentState.CONTENT
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Scaffold(
        containerColor = AmoledBlack,
        topBar = {
            if (!showSettings) {
                CatalogHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    categories = displayCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        if (category == "Anime") {
                            if (selectedCategory == "Anime") {
                                isAnimeDropdownOpen = !isAnimeDropdownOpen
                            } else {
                                selectedCategory = "Anime"
                                isAnimeDropdownOpen = true
                            }
                        } else {
                            selectedCategory = category
                            isAnimeDropdownOpen = false
                        }
                    },
                    animeSubcategories = animeSubcategories,
                    selectedAnimeSubcategories = selectedAnimeSubcategories,
                    isAnimeDropdownOpen = isAnimeDropdownOpen,
                    animeSubcategoryCounts = animeSubcategoryCounts,
                    onAnimeSubcategorySelected = { sub ->
                        selectedAnimeSubcategories = toggleAnimeSubcategory(
                            current = selectedAnimeSubcategories,
                            subcategory = sub,
                            allAvailable = animeSubcategories
                        )
                    },
                    onCloseAnimeDropdown = { isAnimeDropdownOpen = false },
                    selectedMediaFilter = selectedMediaFilter,
                    onMediaFilterSelected = { filter ->
                        selectedMediaFilter = toggleMediaFilter(selectedMediaFilter, filter)
                    },
                    onRefresh = { loadData() },
                    onOpenSettings = { showSettings = true }
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                if (targetState) {
                    (fadeIn(animationSpec = tween(WallNiceMotion.standardDurationMillis)) +
                        slideInHorizontally(
                            animationSpec = tween(WallNiceMotion.standardDurationMillis),
                            initialOffsetX = { it / 10 }
                        )) togetherWith
                        (fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis)) +
                            slideOutHorizontally(
                                animationSpec = tween(WallNiceMotion.fastDurationMillis),
                                targetOffsetX = { -it / 12 }
                            ))
                } else {
                    (fadeIn(animationSpec = tween(WallNiceMotion.standardDurationMillis)) +
                        slideInHorizontally(
                            animationSpec = tween(WallNiceMotion.standardDurationMillis),
                            initialOffsetX = { -it / 10 }
                        )) togetherWith
                        (fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis)) +
                            slideOutHorizontally(
                                animationSpec = tween(WallNiceMotion.fastDurationMillis),
                                targetOffsetX = { it / 12 }
                            ))
                }
            },
            label = "ScreenTransition"
        ) { settingsOpen ->
            if (settingsOpen) {
                SettingsScreen(
                    wallpapers = wallpapers,
                    onBack = { showSettings = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedContent(
                    targetState = catalogState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(WallNiceMotion.standardDurationMillis)) togetherWith
                            fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis))
                    },
                    label = "CatalogStateTransition"
                ) { state ->
                    when (state) {
                        CatalogContentState.LOADING -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = WallNiceSpacing.md,
                                    end = WallNiceSpacing.md,
                                    top = innerPadding.calculateTopPadding() + WallNiceSpacing.sm,
                                    bottom = innerPadding.calculateBottomPadding() + WallNiceSpacing.md
                                ),
                                horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(WallNiceSpacing.lg),
                                userScrollEnabled = false
                            ) {
                                items(6) {
                                    SkeletonWallpaperCard()
                                }
                            }
                        }

                        CatalogContentState.OFFLINE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStatePanel(
                                    icon = Icons.Default.CloudOff,
                                    title = "You’re offline",
                                    message = "WallNice couldn’t reach the wallpaper catalog. Check your connection and try again.",
                                    actionLabel = "Retry",
                                    onAction = { loadData() }
                                )
                            }
                        }

                        CatalogContentState.EMPTY -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStatePanel(
                                    icon = Icons.Default.CollectionsBookmark,
                                    title = "Catalog is empty",
                                    message = "New wallpapers will appear here as soon as they are published.",
                                    actionLabel = "Refresh",
                                    onAction = { loadData() }
                                )
                            }
                        }

                        CatalogContentState.NO_RESULTS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedCategory == "Favorites" && searchQuery.isBlank()) {
                                    EmptyStatePanel(
                                        icon = Icons.Default.FavoriteBorder,
                                        title = "No favorites yet",
                                        message = "Tap the heart on any wallpaper preview to save it for quick offline access.",
                                        actionLabel = "Explore wallpapers",
                                        onAction = {
                                            selectedCategory = "All"
                                        }
                                    )
                                } else {
                                    EmptyStatePanel(
                                        icon = Icons.Default.Search,
                                        title = "Nothing found",
                                        message = "Try another title, creator, or category.",
                                        actionLabel = "Clear search",
                                        onAction = {
                                            searchQuery = ""
                                            selectedCategory = "All"
                                            selectedAnimeSubcategories = setOf("All Anime")
                                            selectedMediaFilter = setOf(MediaFilter.ALL)
                                        }
                                    )
                                }
                            }
                        }

                        CatalogContentState.CONTENT -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = WallNiceSpacing.md,
                                    end = WallNiceSpacing.md,
                                    top = innerPadding.calculateTopPadding() + WallNiceSpacing.sm,
                                    bottom = innerPadding.calculateBottomPadding() + WallNiceSpacing.md
                                ),
                                horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
                            ) {
                                items(filteredList, key = { it.id }) { item ->
                                    val autoPlay = item.id in visibleItemKeys
                                    WallpaperCard(
                                        wallpaper = item,
                                        autoPlay = autoPlay,
                                        onClick = { onWallpaperSelected(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    animeSubcategories: List<String>,
    selectedAnimeSubcategories: Set<String>,
    isAnimeDropdownOpen: Boolean,
    animeSubcategoryCounts: Map<String, Int>,
    onAnimeSubcategorySelected: (String) -> Unit,
    onCloseAnimeDropdown: () -> Unit,
    selectedMediaFilter: Set<MediaFilter>,
    onMediaFilterSelected: (MediaFilter) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .statusBarsPadding()
            .padding(top = WallNiceSpacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WallNiceSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark()
            Spacer(Modifier.size(WallNiceSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WallNice",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Curated for your screen",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            WallNiceIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh catalog",
                onClick = onRefresh,
                tint = NeonCyan
            )
            Spacer(Modifier.width(WallNiceSpacing.xs))
            WallNiceIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings and stats",
                onClick = onOpenSettings,
                tint = NeonPink
            )
        }

        Spacer(Modifier.height(WallNiceSpacing.sm))

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WallNiceSpacing.md)
                .height(52.dp)
                .border(
                    width = 1.dp,
                    color = OutlineSubtle,
                    shape = RoundedCornerShape(WallNiceRadii.medium)
                ),
            placeholder = {
                Text(
                    text = "Search wallpapers",
                    color = TextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(WallNiceMotion.fastDurationMillis)),
                    exit = fadeOut(animationSpec = tween(WallNiceMotion.fastDurationMillis))
                ) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(WallNiceRadii.medium),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceLow,
                unfocusedContainerColor = SurfaceLow,
                cursorColor = NeonCyan,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(Modifier.height(WallNiceSpacing.xs))

        LazyRow(
            contentPadding = PaddingValues(
                start = WallNiceSpacing.md,
                end = WallNiceSpacing.md,
                bottom = WallNiceSpacing.xs
            ),
            horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.xs)
        ) {
            items(categories, key = { it }) { category ->
                val isAnime = category == "Anime"
                val isSelected = selectedCategory == category
                val pillText = if (isAnime && isSelected) {
                    when {
                        "All Anime" in selectedAnimeSubcategories || selectedAnimeSubcategories.isEmpty() -> "Anime"
                        selectedAnimeSubcategories.size == 1 -> "Anime: ${selectedAnimeSubcategories.first()}"
                        else -> "Anime (${selectedAnimeSubcategories.size})"
                    }
                } else {
                    category
                }

                CategoryPill(
                    text = pillText,
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    trailingIcon = if (isAnime) {
                        {
                            Icon(
                                imageVector = if (isAnimeDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isAnimeDropdownOpen) "Collapse anime list" else "Expand anime list",
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }

        AnimatedVisibility(
            visible = isAnimeDropdownOpen && selectedCategory == "Anime",
            enter = expandVertically(
                animationSpec = tween(WallNiceMotion.standardDurationMillis)
            ) + fadeIn(
                animationSpec = tween(WallNiceMotion.standardDurationMillis)
            ),
            exit = shrinkVertically(
                animationSpec = tween(WallNiceMotion.fastDurationMillis)
            ) + fadeOut(
                animationSpec = tween(WallNiceMotion.fastDurationMillis)
            )
        ) {
            AnimeSubcategoryDropdown(
                subcategories = animeSubcategories,
                selectedSubcategories = selectedAnimeSubcategories,
                subcategoryCounts = animeSubcategoryCounts,
                onSubcategorySelected = onAnimeSubcategorySelected,
                onClose = onCloseAnimeDropdown
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WallNiceSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Media",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.width(WallNiceSpacing.sm))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.xs)
            ) {
                items(MediaFilter.entries, key = { it.name }) { filter ->
                    val isSelected = if (filter == MediaFilter.ALL) {
                        MediaFilter.ALL in selectedMediaFilter
                    } else {
                        filter in selectedMediaFilter
                    }

                    CategoryPill(
                        text = filter.label,
                        selected = isSelected,
                        onClick = { onMediaFilterSelected(filter) },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
        Spacer(Modifier.height(WallNiceSpacing.xs))
    }
}

@Composable
private fun AnimeSubcategoryDropdown(
    subcategories: List<String>,
    selectedSubcategories: Set<String>,
    subcategoryCounts: Map<String, Int>,
    onSubcategorySelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.xs),
        shape = RoundedCornerShape(WallNiceRadii.large),
        color = SurfaceMid,
        border = BorderStroke(1.dp, OutlineStrong)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WallNiceSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WallNiceSpacing.sm, vertical = WallNiceSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Anime Collections",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close dropdown",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(WallNiceSpacing.xs))

            subcategories.forEach { subcategory ->
                val isSelected = if (subcategory == "All Anime") {
                    "All Anime" in selectedSubcategories || selectedSubcategories.isEmpty()
                } else {
                    subcategory in selectedSubcategories
                }
                val count = subcategoryCounts[subcategory] ?: 0
                val shape = RoundedCornerShape(WallNiceRadii.medium)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (isSelected) NeonPink.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonPink.copy(alpha = 0.4f) else Color.Transparent,
                            shape = shape
                        )
                        .clickable { onSubcategorySelected(subcategory) }
                        .padding(horizontal = WallNiceSpacing.sm, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) NeonPink else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(WallNiceSpacing.sm))
                    Text(
                        text = subcategory,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(WallNiceRadii.pill))
                            .background(SurfaceLow)
                            .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.pill))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NeonPink else NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
