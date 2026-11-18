import os
import json
import urllib.parse

REPO_BASE = "https://raw.githubusercontent.com/MorhPanel/WallNice/main"
CATALOG_PATH = "catalog.json"
DLS_DIR = "DLS"

existing_update = {
    "versionCode": 1,
    "versionName": "0.0.1-beta",
    "changelog": "Initial beta release.",
    "downloadUrl": "https://github.com/MorhPanel/WallNice/releases/latest"
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

valid_img = {".jpg", ".jpeg", ".png", ".webp"}
valid_vid = {".mp4", ".webm", ".mkv"}
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

        if ext in valid_vid or folder == "video":
            media_type = "VIDEO"
            category = "Live Video"
            if len(parts) > 2:
                category = parts[1]
            categories.add(category)
        elif ext in valid_gif or folder == "gif":
            media_type = "GIF"
            category = "GIF Loops"
            if len(parts) > 2:
                category = parts[1]
            categories.add(category)
        elif ext in valid_img or folder == "img":
            media_type = "IMAGE"
            if len(parts) >= 4 and parts[1].lower() == "anime":
                category = "Anime"
                anime_name = parts[2]
                categories.add("Anime")
                categories.add(anime_name)
            elif len(parts) >= 3:
                category = parts[1]
                categories.add(category)
            else:
                category = "All"
        else:
            continue

        title = os.path.splitext(file)[0].replace("_", " ").replace("-", " ").title()
        
        encoded_path = "/".join(urllib.parse.quote(part) for part in full_path.split("/"))
        raw_url = f"{REPO_BASE}/{encoded_path}"

        wallpapers.append({
            "id": f"wp_{len(wallpapers) + 1}",
            "title": title,
            "author": "morhpanel",
            "category": category,
            "mediaType": media_type,
            "url": raw_url,
            "thumbnailUrl": raw_url,
            "resolution": "Original",
            "isAmoled": False,
            "likesCount": 0
        })

output_data = {
    "appUpdate": existing_update,
    "categories": sorted(list(categories)),
    "wallpapers": wallpapers
}

with open(CATALOG_PATH, "w", encoding="utf-8") as f:
    json.dump(output_data, f, indent=2, ensure_ascii=False)
