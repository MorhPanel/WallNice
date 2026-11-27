import os
import re
import json
import urllib.parse
import hashlib
import subprocess

try:
    from PIL import Image
except ImportError:
    Image = None

REPO_BASE = "https://raw.githubusercontent.com/MorhPanel/WallNice/main"
CATALOG_PATH = "catalog.json"
DLS_DIR = "DLS"

existing_update = {
    "versionCode": 38,
    "versionName": "0.0.38-beta",
    "changelog": "Fixed video playback in preview, feed cards, and live wallpaper GL surface rendering.",
    "downloadUrl": "https://github.com/MorhPanel/WallNice/releases/download/v0.0.38-beta/app-release.apk"
}

existing_added_at = {}

if os.path.exists(CATALOG_PATH):
    try:
        with open(CATALOG_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
            if "appUpdate" in data:
                existing_update.update(data["appUpdate"])
            for item in data.get("wallpapers", []):
                url = item.get("url", "")
                added_at = item.get("addedAt", 0)
                if url and added_at:
                    existing_added_at[url] = added_at
    except Exception:
        pass

existing_update.update({
    "versionCode": 38,
    "versionName": "0.0.38-beta",
    "changelog": "Fixed video playback in preview, feed cards, and live wallpaper GL surface rendering.",
    "downloadUrl": "https://github.com/MorhPanel/WallNice/releases/download/v0.0.38-beta/app-release.apk"
})

categories = set(["All"])
wallpapers = []

title_overrides = {
    "yotsuba_nakano": "Yotsuba Nakano Monochrome",
    "yotsuba_nakano_5c7a2e8d": "Yotsuba Nakano Spring",
}

valid_img = {".jpg", ".jpeg", ".png", ".webp", ".avif"}
valid_vid = {".mp4", ".webm", ".mkv", ".m4v", ".mov"}
valid_gif = {".gif"}

for root, _, files in os.walk(DLS_DIR):
    for file in sorted(files):
        if file.startswith("."):
            continue
        full_path = os.path.join(root, file).replace("\\", "/")
        rel_to_dls = os.path.relpath(full_path, DLS_DIR).replace("\\", "/")
        parts = rel_to_dls.split("/")
        
        folder = parts[0].lower() if len(parts) > 0 else ""
        ext = os.path.splitext(file)[1].lower()

        is_amoled = False
        resolution = "1440x3200"
        subcategory = ""

        if len(parts) >= 4 and parts[1].lower() == "anime":
            category = "Anime"
            subcategory = parts[2]
            categories.add("Anime")
        elif len(parts) >= 3:
            category = parts[1]
            categories.add(category)
        elif ext in valid_vid or folder == "video":
            category = "Live Video"
            categories.add(category)
        elif ext in valid_gif or folder == "gif":
            category = "GIF Loops"
            categories.add(category)
        else:
            category = "All"

        if ext in valid_vid or folder == "video":
            media_type = "VIDEO"
            resolution = "2160x3840"
            try:
                probe_cmd = ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height", "-of", "csv=s=x:p=0", full_path]
                res_out = subprocess.check_output(probe_cmd, text=True).strip()
                if res_out:
                    resolution = res_out
            except Exception:
                pass
        elif ext in valid_gif or folder == "gif":
            media_type = "GIF"
            resolution = "1080x1920"
        elif ext in valid_img or folder == "img":
            media_type = "IMAGE"
            if Image:
                try:
                    with Image.open(full_path) as im:
                        resolution = f"{im.width}x{im.height}"
                        colors = im.getcolors(maxcolors=1000000)
                        if colors:
                            total_pixels = im.width * im.height
                            black_pixels = sum(count for count, col in colors if max(col[:3]) < 10)
                            if (black_pixels / total_pixels) > 0.4:
                                is_amoled = True
                except Exception:
                    pass
        else:
            continue

        base_name = os.path.splitext(file)[0]
        clean_name = re.sub(r'_[a-f0-9]{8,16}$', '', base_name, flags=re.IGNORECASE)
        title = clean_name.replace("_", " ").replace("-", " ").title()
        title = title_overrides.get(base_name, title)
        
        encoded_path = "/".join(urllib.parse.quote(part) for part in full_path.split("/"))
        raw_url = f"{REPO_BASE}/{encoded_path}"

        added_at = existing_added_at.get(raw_url, 0)
        if not added_at:
            try:
                added_at = int(subprocess.check_output(
                    ["git", "log", "-1", "--format=%ct", "--", full_path],
                    text=True
                ).strip() or "0")
            except Exception:
                added_at = 0
        if not added_at:
            added_at = int(os.path.getmtime(full_path))

        thumbnail_url = raw_url
        if media_type == "VIDEO":
            for thumb_ext in (".jpg", ".jpeg", ".png", ".webp"):
                thumb_path = os.path.join("assets", "thumbnails", f"{base_name}{thumb_ext}")
                if os.path.exists(thumb_path):
                    thumbnail_url = f"{REPO_BASE}/assets/thumbnails/{base_name}{thumb_ext}"
                    break

        wp_id = f"wp_{hashlib.sha256(rel_to_dls.encode('utf-8')).hexdigest()[:8]}"

        wallpapers.append({
            "id": wp_id,
            "title": title,
            "author": "morhpanel",
            "category": category,
            "subcategory": subcategory,
            "mediaType": media_type,
            "url": raw_url,
            "thumbnailUrl": thumbnail_url,
            "resolution": resolution,
            "isAmoled": is_amoled,
            "likesCount": 0,
            "collection": subcategory or category,
            "addedAt": added_at
        })

output_data = {
    "appUpdate": existing_update,
    "categories": sorted(list(categories)),
    "wallpapers": wallpapers
}

with open(CATALOG_PATH, "w", encoding="utf-8") as f:
    json.dump(output_data, f, indent=2, ensure_ascii=False)
