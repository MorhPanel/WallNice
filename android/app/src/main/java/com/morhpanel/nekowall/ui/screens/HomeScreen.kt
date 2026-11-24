package com.morhpanel.nekowall.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.MediaFilter
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperItem
import com.morhpanel.nekowall.data.models.matches
import com.morhpanel.nekowall.data.repository.FavoritesManager
import com.morhpanel.nekowall.data.repository.WallNicePreferences
import com.morhpanel.nekowall.data.repository.WallpaperRepository
import com.morhpanel.nekowall.ui.components.CategoryCard
import com.morhpanel.nekowall.ui.components.CategoryItem
import com.morhpanel.nekowall.ui.components.CategoryPill
import com.morhpanel.nekowall.ui.components.EmptyStatePanel
import com.morhpanel.nekowall.ui.components.NavigationTab
import com.morhpanel.nekowall.ui.components.SkeletonWallpaperCard
import com.morhpanel.nekowall.ui.components.WallNiceBottomNavigation
import com.morhpanel.nekowall.ui.components.WallNiceIconButton
import com.morhpanel.nekowall.ui.components.WallpaperCard
import com.morhpanel.nekowall.ui.components.staggeredEntrance
import com.morhpanel.nekowall.ui.screens.SettingsScreen
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
import com.morhpanel.nekowall.ui.theme.WallNiceSizes
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    isActive: Boolean = true,
    currentTab: NavigationTab = NavigationTab.HOME,
    onTabSelected: (NavigationTab) -> Unit = {},
    onWallpaperSelected: (WallpaperItem) -> Unit,
    onUpdateAvailable: (AppUpdateInfo) -> Unit,
    homeGridState: LazyGridState = rememberLazyGridState(),
    categoriesGridState: LazyGridState = rememberLazyGridState(),
    categoryDetailGridState: LazyGridState = rememberLazyGridState(),
    favoritesGridState: LazyGridState = rememberLazyGridState(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowInfo = LocalWindowInfo.current
    val favoriteIds by FavoritesManager.favoriteIds

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var activeCategoryDetail by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAnimeSubcategories by rememberSaveable(stateSaver = AnimeSubcategorySetSaver) { mutableStateOf(setOf("All Anime")) }
    var isAnimeDropdownOpen by rememberSaveable { mutableStateOf(false) }
    var selectedMediaFilter by rememberSaveable(stateSaver = MediaFilterSetSaver) { mutableStateOf(setOf(MediaFilter.ALL)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val initialCatalog = remember { WallpaperRepository.getOrLoadCachedCatalog(context) }
    var wallpapers by remember { mutableStateOf(initialCatalog?.wallpapers ?: emptyList()) }
    var isLoading by remember { mutableStateOf(wallpapers.isEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val gridState = homeGridState

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

    val playbackQuality = WallNicePreferences.getPlaybackQuality(context)
    val canPlayVideos = isActive && isAppForeground && windowInfo.isWindowFocused && !gridState.isScrollInProgress && WallNicePreferences.shouldAutoPlayPreviews(context)

    var visibleItemKeys by remember { mutableStateOf(emptySet<Any>()) }

    LaunchedEffect(canPlayVideos, gridState, playbackQuality) {
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
            }.take(playbackQuality.maxAutoPlayCards).map { it.key }.toSet()
        }
        .collectLatest { keys ->
            delay(200L)
            visibleItemKeys = keys
        }
    }

    fun loadData(force: Boolean = false) {
        if (isLoading && wallpapers.isNotEmpty() && !force) return
        coroutineScope.launch {
            if (wallpapers.isEmpty() || force) {
                isLoading = true
            }
            errorMessage = null
            WallpaperRepository.fetchCatalog(context)
                .onSuccess { response ->
                    wallpapers = response.wallpapers
                    response.updateInfo?.let(onUpdateAvailable)
                }
                .onFailure { error ->
                    if (wallpapers.isEmpty()) {
                        errorMessage = error.localizedMessage ?: "The catalog could not be reached."
                    }
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

    val categoryCards = remember(wallpapers, searchableWallpapers) {
        val list = mutableListOf<CategoryItem>()

        val animeCount = searchableWallpapers.count {
            it.item.category.equals("Anime", ignoreCase = true) || it.decodedUrl.contains("/Anime/", ignoreCase = true)
        }
        val animeThumb = searchableWallpapers.firstOrNull {
            it.item.category.equals("Anime", ignoreCase = true) || it.decodedUrl.contains("/Anime/", ignoreCase = true)
        }?.item?.let { it.thumbnailUrl.ifBlank { it.url } } ?: ""
        if (animeCount > 0) list.add(CategoryItem("Anime", animeCount, animeThumb))

        val gymCount = searchableWallpapers.count {
            it.item.category.equals("Gym", ignoreCase = true) || it.decodedUrl.contains("/Gym/", ignoreCase = true)
        }
        val gymThumb = searchableWallpapers.firstOrNull {
            it.item.category.equals("Gym", ignoreCase = true) || it.decodedUrl.contains("/Gym/", ignoreCase = true)
        }?.item?.let { it.thumbnailUrl.ifBlank { it.url } } ?: ""
        if (gymCount > 0) list.add(CategoryItem("Gym", gymCount, gymThumb))

        val amoledCount = searchableWallpapers.count { it.item.isAmoled }
        val amoledThumb = searchableWallpapers.firstOrNull { it.item.isAmoled }?.item?.let { it.thumbnailUrl.ifBlank { it.url } } ?: ""
        if (amoledCount > 0) list.add(CategoryItem("AMOLED 4K", amoledCount, amoledThumb))

        val videoCount = searchableWallpapers.count { it.item.mediaType == MediaType.VIDEO }
        val videoThumb = searchableWallpapers.firstOrNull { it.item.mediaType == MediaType.VIDEO }?.item?.let { it.thumbnailUrl.ifBlank { it.url } } ?: ""
        if (videoCount > 0) list.add(CategoryItem("Live Video", videoCount, videoThumb))

        val gifCount = searchableWallpapers.count { it.item.mediaType == MediaType.GIF }
        val gifThumb = searchableWallpapers.firstOrNull { it.item.mediaType == MediaType.GIF }?.item?.let { it.thumbnailUrl.ifBlank { it.url } } ?: ""
        if (gifCount > 0) list.add(CategoryItem("GIF Loops", gifCount, gifThumb))

        list
    }

    val homeWallpapers = remember(searchQuery, selectedMediaFilter, searchableWallpapers) {
        val query = searchQuery.trim()
        searchableWallpapers.mapNotNull { entry ->
            val item = entry.item
            if (!selectedMediaFilter.matches(item)) return@mapNotNull null
            val matchesSearch = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.author.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.subcategory.contains(query, ignoreCase = true) ||
                entry.decodedUrl.contains(query, ignoreCase = true)
            if (matchesSearch) item else null
        }
    }

    val categoryDetailWallpapers = remember(activeCategoryDetail, selectedAnimeSubcategories, searchableWallpapers) {
        val targetCategory = activeCategoryDetail ?: return@remember emptyList()
        searchableWallpapers.mapNotNull { entry ->
            val item = entry.item
            val decodedUrl = entry.decodedUrl
            val matches = when (targetCategory) {
                "Anime" -> {
                    val isAnime = item.category.equals("Anime", ignoreCase = true) || decodedUrl.contains("/Anime/", ignoreCase = true)
                    if (!isAnime) {
                        false
                    } else if ("All Anime" in selectedAnimeSubcategories || selectedAnimeSubcategories.isEmpty()) {
                        true
                    } else {
                        selectedAnimeSubcategories.any { sub ->
                            item.subcategory.equals(sub, ignoreCase = true) || decodedUrl.contains("/$sub/", ignoreCase = true)
                        }
                    }
                }
                "Gym" -> item.category.equals("Gym", ignoreCase = true) || decodedUrl.contains("/Gym/", ignoreCase = true)
                "AMOLED 4K" -> item.isAmoled
                "Live Video" -> item.mediaType == MediaType.VIDEO
                "GIF Loops" -> item.mediaType == MediaType.GIF
                else -> item.category.equals(targetCategory, ignoreCase = true) || decodedUrl.contains(targetCategory, ignoreCase = true)
            }
            if (matches) item else null
        }
    }

    val favoriteWallpapers = remember(favoriteIds, searchableWallpapers) {
        searchableWallpapers.mapNotNull { entry ->
            if (favoriteIds.contains(entry.item.id)) entry.item else null
        }
    }

    val currentCatalogState = when {
        isLoading && wallpapers.isEmpty() -> CatalogContentState.LOADING
        errorMessage != null && wallpapers.isEmpty() -> CatalogContentState.OFFLINE
        wallpapers.isEmpty() -> CatalogContentState.EMPTY
        currentTab == NavigationTab.HOME && homeWallpapers.isEmpty() -> CatalogContentState.NO_RESULTS
        else -> CatalogContentState.CONTENT
    }

    val onOpenSettings = { showSettings = true }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    BackHandler(enabled = !showSettings && activeCategoryDetail != null) {
        activeCategoryDetail = null
    }

    BackHandler(enabled = !showSettings && activeCategoryDetail == null && currentTab != NavigationTab.HOME) {
        onTabSelected(NavigationTab.HOME)
    }

    Scaffold(
        containerColor = AmoledBlack,
        bottomBar = {
            if (!showSettings) {
                WallNiceBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        onTabSelected(tab)
                        if (tab != NavigationTab.CATEGORIES) {
                            activeCategoryDetail = null
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (showSettings) {
            SettingsScreen(
                wallpapers = wallpapers,
                onBack = { showSettings = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            when (currentTab) {
                NavigationTab.HOME -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        HomeTopBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedMediaFilter = selectedMediaFilter,
                            onMediaFilterSelected = { filter ->
                                selectedMediaFilter = toggleMediaFilter(selectedMediaFilter, filter)
                            },
                            onOpenSettings = onOpenSettings
                        )

                    when (currentCatalogState) {
                        CatalogContentState.LOADING -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                userScrollEnabled = false
                            ) {
                                items(6) { SkeletonWallpaperCard() }
                            }
                        }
                        CatalogContentState.OFFLINE -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStatePanel(
                                    icon = Icons.Default.CloudOff,
                                    title = "You’re offline",
                                    message = "WallNice couldn’t reach the catalog. Check your connection.",
                                    actionLabel = "Retry",
                                    onAction = { loadData() }
                                )
                            }
                        }
                        CatalogContentState.EMPTY -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStatePanel(
                                    icon = Icons.Default.CollectionsBookmark,
                                    title = "Catalog is empty",
                                    message = "New wallpapers will appear here soon.",
                                    actionLabel = "Refresh",
                                    onAction = { loadData() }
                                )
                            }
                        }
                        CatalogContentState.NO_RESULTS -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStatePanel(
                                    icon = Icons.Default.Search,
                                    title = "Nothing found",
                                    message = "Try another search keyword or filter.",
                                    actionLabel = "Clear search",
                                    onAction = {
                                        searchQuery = ""
                                        selectedMediaFilter = setOf(MediaFilter.ALL)
                                    }
                                )
                            }
                        }
                        CatalogContentState.CONTENT -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(homeWallpapers, key = { _, it -> it.id }) { index, item ->
                                    val autoPlay = item.id in visibleItemKeys
                                    WallpaperCard(
                                        wallpaper = item,
                                        autoPlay = autoPlay,
                                        onClick = { onWallpaperSelected(item) },
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        modifier = Modifier.staggeredEntrance(
                                            index = index,
                                            enabled = !gridState.canScrollBackward
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            NavigationTab.CATEGORIES -> {
                if (activeCategoryDetail == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = categoriesGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(categoryCards, key = { it.name }) { category ->
                                CategoryCard(
                                    category = category,
                                    onClick = {
                                        if (activeCategoryDetail != category.name) {
                                            activeCategoryDetail = category.name
                                            coroutineScope.launch {
                                                categoryDetailGridState.scrollToItem(0)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        CategoryDetailTopBar(
                            title = activeCategoryDetail ?: "",
                            isAnime = activeCategoryDetail == "Anime",
                            isAnimeDropdownOpen = isAnimeDropdownOpen,
                            selectedAnimeSubcategories = selectedAnimeSubcategories,
                            onAnimePillClick = { isAnimeDropdownOpen = !isAnimeDropdownOpen },
                            onBack = { activeCategoryDetail = null }
                        )

                        AnimatedVisibility(
                            visible = isAnimeDropdownOpen && activeCategoryDetail == "Anime",
                            enter = expandVertically(animationSpec = tween(WallNiceMotion.standardDurationMillis)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = tween(WallNiceMotion.fastDurationMillis)) + fadeOut()
                        ) {
                            AnimeSubcategoryDropdown(
                                subcategories = animeSubcategories,
                                selectedSubcategories = selectedAnimeSubcategories,
                                subcategoryCounts = animeSubcategoryCounts,
                                onSubcategorySelected = { sub ->
                                    selectedAnimeSubcategories = toggleAnimeSubcategory(
                                        current = selectedAnimeSubcategories,
                                        subcategory = sub,
                                        allAvailable = animeSubcategories
                                    )
                                },
                                onClose = { isAnimeDropdownOpen = false }
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = categoryDetailGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(categoryDetailWallpapers, key = { _, it -> it.id }) { index, item ->
                                val autoPlay = item.id in visibleItemKeys
                                WallpaperCard(
                                    wallpaper = item,
                                    autoPlay = autoPlay,
                                    onClick = { onWallpaperSelected(item) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    }
                }
            }

            NavigationTab.FAVORITES -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )

                    if (favoriteWallpapers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStatePanel(
                                icon = Icons.Default.FavoriteBorder,
                                title = "No favorites yet",
                                message = "No favorite wallpapers yet.",
                                actionLabel = "Explore wallpapers",
                                onAction = { onTabSelected(NavigationTab.HOME) }
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = favoritesGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(favoriteWallpapers, key = { _, it -> it.id }) { index, item ->
                                val autoPlay = item.id in visibleItemKeys
                                WallpaperCard(
                                    wallpaper = item,
                                    autoPlay = autoPlay,
                                    onClick = { onWallpaperSelected(item) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    }
                }
            }

            NavigationTab.ME -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    MeScreen(
                        wallpapers = wallpapers,
                        onCheckUpdate = { loadData() }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun HomeTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedMediaFilter: Set<MediaFilter>,
    onMediaFilterSelected: (MediaFilter) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(1.dp, OutlineSubtle, RoundedCornerShape(WallNiceRadii.medium)),
                placeholder = {
                    Text(
                        text = "Search wallpapers",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
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

            Spacer(Modifier.width(8.dp))

            WallNiceIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings and stats",
                onClick = onOpenSettings,
                tint = NeonPink
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MediaFilter.entries) { filter ->
                val isSelected = filter in selectedMediaFilter
                val checkmark: @Composable (() -> Unit)? = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Filter active",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else null

                CategoryPill(
                    text = filter.label,
                    selected = isSelected,
                    onClick = { onMediaFilterSelected(filter) },
                    trailingIcon = checkmark
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailTopBar(
    title: String,
    isAnime: Boolean,
    isAnimeDropdownOpen: Boolean,
    selectedAnimeSubcategories: Set<String>,
    onAnimePillClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceLow)
                    .border(1.dp, OutlineSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (isAnime) {
                val pillText = when {
                    "All Anime" in selectedAnimeSubcategories || selectedAnimeSubcategories.isEmpty() -> "All Anime"
                    selectedAnimeSubcategories.size == 1 -> selectedAnimeSubcategories.first()
                    else -> "Anime (${selectedAnimeSubcategories.size})"
                }
                CategoryPill(
                    text = pillText,
                    selected = true,
                    onClick = onAnimePillClick,
                    trailingIcon = {
                        Icon(
                            imageVector = if (isAnimeDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
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
                val isSelected = subcategory in selectedSubcategories
                val count = subcategoryCounts[subcategory] ?: 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WallNiceRadii.small))
                        .background(
                            if (isSelected) NeonPink.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .clickable { onSubcategorySelected(subcategory) }
                        .padding(horizontal = WallNiceSpacing.sm, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) NeonPink else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = subcategory,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(WallNiceRadii.pill),
                        color = if (isSelected) NeonPink.copy(alpha = 0.22f) else SurfaceLow,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonPink.copy(alpha = 0.45f) else OutlineSubtle
                        )
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NeonPink else TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
