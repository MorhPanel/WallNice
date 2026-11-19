import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class SecurityControlTests(unittest.TestCase):
    def test_manifest_declares_package_install_and_file_provider(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text()
        paths = (ROOT / "android/app/src/main/res/xml/provider_paths.xml").read_text()

        self.assertIn("REQUEST_INSTALL_PACKAGES", manifest)
        self.assertIn("androidx.core.content.FileProvider", manifest)
        self.assertIn("${applicationId}.fileprovider", manifest)
        self.assertIn("cached_updates", paths)
        self.assertIn("external_cached_updates", paths)

    def test_update_flow_uses_in_app_download_with_progress_and_signature_verification(self):
        updater = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/engine/AppUpdateManager.kt").read_text()
        dialog = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/components/AppUpdateDialog.kt").read_text()
        catalog = json.loads((ROOT / "catalog.json").read_text())
        expected_url = "https://github.com/MorhPanel/WallNice/releases/download/v0.0.20-beta/app-release.apk"

        self.assertIn("https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk", updater)
        self.assertEqual(catalog["appUpdate"]["downloadUrl"], expected_url)
        self.assertIn("downloadApk", updater)
        self.assertIn("launchInstaller", updater)
        self.assertIn("hasMatchingPackageSignature", updater)
        self.assertIn("application/vnd.android.package-archive", updater)
        self.assertIn("LinearProgressIndicator", dialog)
        self.assertIn("formatProgress", dialog)
        self.assertIn("startDownload()", dialog)
        self.assertIn("ACTION_MANAGE_UNKNOWN_APP_SOURCES", updater)
        self.assertIn("trustedRedirectHosts", updater)
        self.assertIn("resolveApkUrl", updater)

    def test_release_signing_stays_fail_closed(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text()
        workflow = (ROOT / ".github/workflows/build-release.yml").read_text()

        self.assertNotIn("wallnice123", gradle)
        self.assertNotIn("wallnice123", workflow)
        self.assertNotIn("keytool -genkeypair", workflow)
        for name in (
            "RELEASE_KEYSTORE_BASE64",
            "RELEASE_KEYSTORE_PASSWORD",
            "RELEASE_KEY_ALIAS",
            "RELEASE_KEY_PASSWORD",
        ):
            self.assertIn(name, workflow)
        self.assertIn("All permanent release signing secrets are required.", workflow)
        self.assertIn("Release signing credentials are required.", gradle)
        self.assertIn("gradle.taskGraph.whenReady", gradle)

    def test_release_metadata_is_synchronized(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text()
        catalog = json.loads((ROOT / "catalog.json").read_text())
        self.assertIn("versionCode = 22", gradle)
        self.assertIn('versionName = "0.0.20-beta"', gradle)
        self.assertEqual(catalog["appUpdate"]["versionCode"], 22)
        self.assertEqual(catalog["appUpdate"]["versionName"], "0.0.20-beta")

    def test_settings_is_header_action_with_back_navigation_and_shared_motion(self):
        home = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/HomeScreen.kt").read_text()
        settings = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/SettingsScreen.kt").read_text()
        motion = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/theme/Motion.kt").read_text()

        self.assertNotIn("NavigationBar", home)
        self.assertNotIn("NavigationBarItem", home)
        self.assertNotIn("HomeBottomBar", home)
        self.assertNotIn("MainTab.SETTINGS", home)
        self.assertIn("contentDescription = \"Settings and stats\"", home)
        self.assertIn("onOpenSettings = { showSettings = true }", home)
        self.assertIn("BackHandler(enabled = showSettings)", home)
        self.assertIn("SettingsScreen(", home)
        self.assertIn("onBack = { showSettings = false }", home)
        self.assertIn("SettingsTopBar", settings)
        self.assertIn("Icons.AutoMirrored.Filled.ArrowBack", settings)
        self.assertIn("contentDescription = \"Back to wallpapers\"", settings)
        self.assertIn("statusBarsPadding()", settings)
        self.assertIn("navigationBarsPadding()", settings)
        self.assertIn("WallNiceMotion", home)
        self.assertIn("WallNiceMotion", motion)
        self.assertIn("standardDurationMillis = 240", motion)

    def test_media_types_filters_badges_and_fullscreen_preview_are_present(self):
        resolver = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/data/models/MediaTypeResolver.kt").read_text()
        filters = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/data/models/MediaFilter.kt").read_text()
        repository = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/data/repository/WallpaperRepository.kt").read_text()
        home = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/HomeScreen.kt").read_text()
        card = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/components/WallpaperCard.kt").read_text()
        preview = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/PreviewScreen.kt").read_text()

        for extension in ("jpg", "jpeg", "png", "webp", "avif"):
            self.assertIn(extension, resolver)
        for extension in ("mp4", "webm", "mkv", "m4v", "mov"):
            self.assertIn(extension, resolver)
        self.assertIn('extension == "gif"', resolver)
        self.assertIn("fromCatalog", repository)
        self.assertIn("fromUrl", repository)
        self.assertIn('"Photos"', filters)
        self.assertIn('"Videos"', filters)
        self.assertIn('"GIFs"', filters)
        self.assertIn("selectedMediaFilter", home)
        self.assertIn("MediaFilter.entries", home)
        self.assertIn('MediaTypeResolver.label(mediaType)', card)
        self.assertIn('MediaType.IMAGE -> "Photo"', resolver)
        self.assertIn('MediaType.VIDEO -> "Video"', resolver)
        self.assertIn('MediaType.GIF -> "GIF"', resolver)
        self.assertIn("showFullscreenPreview", preview)
        self.assertIn("FullscreenMediaViewer", preview)
        self.assertIn("fitContent = true", preview)
        self.assertIn('contentDescription = "Open fullscreen preview"', preview)
        self.assertIn('contentDescription = "Back from fullscreen preview"', preview)
        self.assertIn("BackHandler(enabled = showFullscreenPreview)", preview)
        self.assertIn("onOpenFullscreen", preview)
        self.assertIn("AspectRatioFrameLayout.RESIZE_MODE_FIT", preview)
        self.assertIn("ContentScale.Fit", preview)

    def test_offline_cache_and_favorites_are_present(self):
        fav_mgr = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/data/repository/FavoritesManager.kt").read_text()
        repo = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/data/repository/WallpaperRepository.kt").read_text()
        helper = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/engine/DownloadHelper.kt").read_text()
        home = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/HomeScreen.kt").read_text()
        preview = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/PreviewScreen.kt").read_text()
        card = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/components/WallpaperCard.kt").read_text()
        readme = (ROOT / "README.md").read_text()

        self.assertIn("object FavoritesManager", fav_mgr)
        self.assertIn("toggleFavorite", fav_mgr)
        self.assertIn("isFavorite", fav_mgr)
        self.assertIn("favoriteIds", fav_mgr)
        self.assertIn("saveCatalogCache", repo)
        self.assertIn("loadCatalogCache", repo)
        self.assertIn("catalog_cache.json", repo)
        self.assertIn("getCachedFile", helper)
        self.assertIn("isCached", helper)
        self.assertIn('"Favorites"', home)
        self.assertIn("FavoritesManager.init", home)
        self.assertIn('"No favorites yet"', home)
        self.assertIn("onToggleFavorite", preview)
        self.assertIn("cachedFile", preview)
        self.assertIn("FavoritesManager.isFavorite(wallpaper.id)", card)
        self.assertIn("| 7 | Offline wallpaper cache and favorites | Android | 🟡 Medium | 🟢 Completed |", readme)

    def test_ui_state_preservation_and_search_optimization_are_present(self):
        main = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/MainActivity.kt").read_text()
        home = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/HomeScreen.kt").read_text()

        self.assertIn("rememberSaveable", main)
        self.assertIn("WallpaperItemSaver", main)
        self.assertIn("selectedWallpaper by rememberSaveable", main)

        self.assertIn("rememberSaveable", home)
        self.assertIn("showSettings by rememberSaveable", home)
        self.assertIn("selectedCategory by rememberSaveable", home)
        self.assertIn("selectedMediaFilter by rememberSaveable", home)
        self.assertIn("searchQuery by rememberSaveable", home)
        self.assertIn("searchableWallpapers", home)
        self.assertIn("SearchableWallpaper", home)


if __name__ == "__main__":
    unittest.main()
