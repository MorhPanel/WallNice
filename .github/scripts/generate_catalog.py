import os
import re
import json
import urllib.parse
import hashlib

try:
    from PIL import Image
except ImportError:
    Image = None

REPO_BASE = "https://raw.githubusercontent.com/MorhPanel/WallNice/main"
CATALOG_PATH = "catalog.json"
DLS_DIR = "DLS"

existing_update = {
    "versionCode": 16,
    "versionName": "0.0.14-beta",
    "changelog": "Adds offline wallpaper cache, favorites, and new Gothic & Nikke collections.",
    "downloadUrl": "https://github.com/MorhPanel/WallNice/releases/download/v0.0.14-beta/app-release.apk"
}

if os.path.exists(CATALOG_PATH):
    try:
        with open(CATALOG_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
            if "appUpdate" in data:
                existing_update = data["appUpdate"]
    except Exception:
        pass

categories = set(["All"])
wallpapers = []

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
            resolution = "1080x1920"
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
        clean_name = re.sub(r'_[a-f0-9]{8}$', '', base_name, flags=re.IGNORECASE)
        title = clean_name.replace("_", " ").replace("-", " ").title()
        
        encoded_path = "/".join(urllib.parse.quote(part) for part in full_path.split("/"))
        raw_url = f"{REPO_BASE}/{encoded_path}"

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
            "likesCount": 0
        })

output_data = {
    "appUpdate": existing_update,
    "categories": sorted(list(categories)),
    "wallpapers": wallpapers
}

with open(CATALOG_PATH, "w", encoding="utf-8") as f:
    json.dump(output_data, f, indent=2, ensure_ascii=False)
