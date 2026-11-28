import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read_text(relative_path):
    return (ROOT / relative_path).read_text(encoding="utf-8")


def read_json(relative_path):
    return json.loads(read_text(relative_path))


class CatalogAndUiRuleTests(unittest.TestCase):
    def test_amoled_flag_does_not_overlap_anime_or_gym(self):
        catalog = read_json("catalog.json")
        for item in catalog["wallpapers"]:
            if item.get("isAmoled"):
                self.assertIn(
                    item.get("category", "").lower(),
                    {"amoled", "amoled 4k"},
                    f"Unexpected isAmoled on non-AMOLED category: {item}",
                )

    def test_sorting_and_zero_comments_policy_holds(self):
        repo = read_text("android/app/src/main/java/com/morhpanel/nekowall/data/repository/WallpaperRepository.kt")
        self.assertIn("sortedByDescending { it.addedAtEpochSeconds }", repo)

        for rel in (
            "android/app/src/main/java/com/morhpanel/nekowall/data/models/WallpaperItem.kt",
            "android/app/src/main/java/com/morhpanel/nekowall/data/repository/WallpaperRepository.kt",
            "android/app/src/main/java/com/morhpanel/nekowall/ui/components/WallNiceComponents.kt",
            "android/app/src/main/java/com/morhpanel/nekowall/ui/screens/HomeScreen.kt",
        ):
            src = read_text(rel)
            stripped = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
            self.assertNotIn("//", stripped, f"Line comment found in {rel}")
            self.assertNotIn("/*", stripped, f"Block comment found in {rel}")


if __name__ == "__main__":
    unittest.main()
