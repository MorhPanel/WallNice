package com.morhpanel.nekowall.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.morhpanel.nekowall.data.models.AppUpdateInfo
import com.morhpanel.nekowall.data.models.MediaType
import com.morhpanel.nekowall.data.models.WallpaperItem
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
import com.morhpanel.nekowall.ui.theme.OutlineSubtle
import com.morhpanel.nekowall.ui.theme.SurfaceLow
import com.morhpanel.nekowall.ui.theme.TextPrimary
import com.morhpanel.nekowall.ui.theme.TextSecondary
import com.morhpanel.nekowall.ui.theme.WallNiceRadii
import com.morhpanel.nekowall.ui.theme.WallNiceSpacing
import kotlinx.coroutines.launch

enum class MainTab {
    WALLPAPERS,
    SETTINGS
}

@Composable
fun HomeScreen(
    onWallpaperSelected: (WallpaperItem) -> Unit,
    onUpdateAvailable: (AppUpdateInfo) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(MainTab.WALLPAPERS) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf("All")) }
    var wallpapers by remember { mutableStateOf<List<WallpaperItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        if (isLoading && wallpapers.isNotEmpty()) return
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            WallpaperRepository.fetchCatalog()
                .onSuccess { response ->
                    val nextCategories = response.categories.ifEmpty { listOf("All") }
                    categories = nextCategories
                    wallpapers = response.wallpapers
                    if (selectedCategory !in nextCategories) selectedCategory = "All"
                    response.updateInfo?.let(onUpdateAvailable)
                }
                .onFailure { error ->
                    errorMessage = error.localizedMessage ?: "The catalog could not be reached."
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredList = remember(selectedCategory, searchQuery, wallpapers) {
        val query = searchQuery.trim()
        wallpapers.filter { item ->
            val decodedUrl = Uri.decode(item.url)
            val categoryMatches = when (selectedCategory) {
                "All" -> true
                "Live Video" -> item.mediaType == MediaType.VIDEO
                "GIF Loops" -> item.mediaType == MediaType.GIF
                "AMOLED 4K" -> item.isAmoled
                "Anime" -> item.category.equals("Anime", ignoreCase = true) ||
                    decodedUrl.contains("/Anime/", ignoreCase = true)
                else -> item.category.equals(selectedCategory, ignoreCase = true) ||
                    decodedUrl.contains("/$selectedCategory/", ignoreCase = true) ||
                    decodedUrl.contains(selectedCategory, ignoreCase = true)
            }
            val searchMatches = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.author.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                decodedUrl.contains(query, ignoreCase = true)
            categoryMatches && searchMatches
        }
    }

    Scaffold(
        containerColor = AmoledBlack,
        topBar = {
            if (currentTab == MainTab.WALLPAPERS) {
                CatalogHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    onRefresh = { loadData() },
                    onOpenSettings = { currentTab = MainTab.SETTINGS }
                )
            } else {
                SettingsHeader(
                    onBack = { currentTab = MainTab.WALLPAPERS }
                )
            }
        },
        bottomBar = {
            HomeBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "MainTabTransition"
        ) { tab ->
            when (tab) {
                MainTab.WALLPAPERS -> {
                    when {
                        isLoading && wallpapers.isEmpty() -> {
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

                        errorMessage != null && wallpapers.isEmpty() -> {
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

                        wallpapers.isEmpty() -> {
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

                        filteredList.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStatePanel(
                                    icon = Icons.Default.Search,
                                    title = "Nothing found",
                                    message = "Try another title, creator, or category.",
                                    actionLabel = "Clear search",
                                    onAction = {
                                        searchQuery = ""
                                        selectedCategory = "All"
                                    }
                                )
                            }
                        }

                        else -> {
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
                                verticalArrangement = Arrangement.spacedBy(WallNiceSpacing.sm)
                            ) {
                                items(filteredList, key = { it.id }) { item ->
                                    WallpaperCard(
                                        wallpaper = item,
                                        onClick = { onWallpaperSelected(item) }
                                    )
                                }
                            }
                        }
                    }
                }

                MainTab.SETTINGS -> {
                    SettingsScreen(
                        wallpapers = wallpapers,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceLow,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = OutlineSubtle,
            shape = RoundedCornerShape(topStart = WallNiceRadii.large, topEnd = WallNiceRadii.large)
        )
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.WALLPAPERS,
            onClick = { onTabSelected(MainTab.WALLPAPERS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Wallpapers"
                )
            },
            label = {
                Text(
                    text = "Wallpapers",
                    fontWeight = if (currentTab == MainTab.WALLPAPERS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                indicatorColor = NeonPink.copy(alpha = 0.15f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = currentTab == MainTab.SETTINGS,
            onClick = { onTabSelected(MainTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            },
            label = {
                Text(
                    text = "Settings",
                    fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                indicatorColor = NeonPink.copy(alpha = 0.15f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

@Composable
private fun SettingsHeader(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .statusBarsPadding()
            .padding(horizontal = WallNiceSpacing.md, vertical = WallNiceSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark()
        Spacer(Modifier.width(WallNiceSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "WallNice",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Settings & Info",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        WallNiceIconButton(
            icon = Icons.Default.Collections,
            contentDescription = "Back to Wallpapers",
            onClick = onBack,
            tint = NeonCyan
        )
    }
}

@Composable
private fun CatalogHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
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
                if (searchQuery.isNotEmpty()) {
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
                CategoryPill(
                    text = category,
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}
