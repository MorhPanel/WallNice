import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class SecurityControlTests(unittest.TestCase):
    def test_manifest_has_no_app_initiated_package_install_authority(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text()
        self.assertNotIn("REQUEST_INSTALL_PACKAGES", manifest)
        self.assertNotIn("FileProvider", manifest)
        source_files = [
            path for path in (ROOT / "android/app/src/main").rglob("*")
            if path.is_file() and path.suffix in {".kt", ".xml"}
        ]
        source = "\n".join(path.read_text() for path in source_files)
        self.assertNotIn("REQUEST_INSTALL_PACKAGES", source)
        self.assertNotIn("ACTION_INSTALL_PACKAGE", source)
        self.assertNotIn("MANAGE_UNKNOWN_APP_SOURCES", source)
        self.assertNotIn("FileProvider", source)

    def test_update_flow_uses_only_the_trusted_direct_apk_url(self):
        updater = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/engine/AppUpdateManager.kt").read_text()
        dialog = (ROOT / "android/app/src/main/java/com/morhpanel/nekowall/ui/components/AppUpdateDialog.kt").read_text()
        catalog = json.loads((ROOT / "catalog.json").read_text())
        direct_url = "https://github.com/MorhPanel/WallNice/releases/latest/download/app-release.apk"

        self.assertIn(direct_url, updater)
        self.assertEqual(catalog["appUpdate"]["downloadUrl"], direct_url)
        self.assertIn("openApkDownload(context, updateInfo.downloadUrl)", dialog)
        self.assertIn("resolveApkUrl(originalUrl)", updater)
        self.assertIn("val safeUrl = resolveApkUrl(originalUrl)", updater)
        self.assertIn("Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))", updater)
        self.assertIn("isPlayManagedInstall", updater)
        self.assertIn('source.host != "github.com"', updater)
        self.assertIn("source.port != -1", updater)
        self.assertIn("source.query != null", updater)
        self.assertIn("source.fragment != null", updater)
        self.assertIn("releaseTagPattern", updater)
        self.assertNotIn("downloadApk(", updater)
        self.assertNotIn("launchInstaller", updater)
        self.assertNotIn("ACTION_INSTALL_PACKAGE", updater)
        self.assertNotIn("application/vnd.android.package-archive", updater)
        self.assertNotIn("context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))", dialog)

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
        self.assertIn("versionCode = 10", gradle)
        self.assertIn('versionName = "0.0.10-beta"', gradle)
        self.assertEqual(catalog["appUpdate"]["versionCode"], 10)
        self.assertEqual(catalog["appUpdate"]["versionName"], "0.0.10-beta")

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


if __name__ == "__main__":
    unittest.main()
