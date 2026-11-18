import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class SecurityControlTests(unittest.TestCase):
    def test_manifest_has_no_app_initiated_package_install_authority(self):
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text()
        self.assertNotIn("REQUEST_INSTALL_PACKAGES", manifest)
        self.assertNotIn("FileProvider", manifest)

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
        self.assertNotIn("downloadApk(", updater)
        self.assertNotIn("launchInstaller", updater)
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

    def test_release_metadata_is_synchronized(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text()
        catalog = json.loads((ROOT / "catalog.json").read_text())
        self.assertIn("versionCode = 5", gradle)
        self.assertIn('versionName = "0.0.5-beta"', gradle)
        self.assertEqual(catalog["appUpdate"]["versionCode"], 5)
        self.assertEqual(catalog["appUpdate"]["versionName"], "0.0.5-beta")


if __name__ == "__main__":
    unittest.main()
