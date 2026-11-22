import hashlib
import json
import os
import re
import sys
import urllib.parse

CATALOG_PATH = "catalog.json"
DLS_DIR = "DLS"
VALID_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".avif", ".mp4", ".webm", ".mkv", ".m4v", ".mov", ".gif"}
VIDEO_EXTENSIONS = {".mp4", ".webm", ".mkv", ".m4v", ".mov"}


def media_files():
    result = []
    for root, _, files in os.walk(DLS_DIR):
        for name in files:
            if name.startswith("."):
                continue
            path = os.path.join(root, name).replace("\\", "/")
            if os.path.splitext(name)[1].lower() in VALID_EXTENSIONS:
                result.append(path)
    return sorted(result)


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    errors = []
    notices = []
    with open(CATALOG_PATH, "r", encoding="utf-8") as file:
        catalog = json.load(file)

    entries = catalog.get("wallpapers", [])
    files = media_files()
    file_set = set(files)
    ids = set()
    urls = set()
    catalog_paths = set()
    hashes = {}

    for path in files:
        digest = sha256(path)
        if digest in hashes:
            errors.append(f"Exact duplicate media: {hashes[digest]} and {path}")
        else:
            hashes[digest] = path

    for entry in entries:
        wallpaper_id = entry.get("id", "")
        title = entry.get("title", "")
        url = entry.get("url", "")
        media_type = entry.get("mediaType", "")
        thumbnail_url = entry.get("thumbnailUrl", "")
        collection = entry.get("collection", "")
        added_at = entry.get("addedAt", 0)

        if not wallpaper_id or wallpaper_id in ids:
            errors.append(f"Missing or duplicate wallpaper id: {wallpaper_id}")
        ids.add(wallpaper_id)
        if not url or url in urls:
            errors.append(f"Missing or duplicate wallpaper URL: {url}")
        urls.add(url)
        if not title or "_" in title or re.search(r"[a-f0-9]{8,16}$", title, re.IGNORECASE):
            errors.append(f"Unclean display title: {title}")
        if not collection:
            errors.append(f"Missing collection for {wallpaper_id}")
        if not isinstance(added_at, int) or added_at <= 0:
            errors.append(f"Missing addedAt for {wallpaper_id}")

        decoded_url = urllib.parse.unquote(url)
        marker = "/main/"
        path = decoded_url.split(marker, 1)[1] if marker in decoded_url else ""
        if not path or path not in file_set:
            errors.append(f"Catalog file missing for {wallpaper_id}: {path}")
        else:
            catalog_paths.add(path)

        extension = os.path.splitext(path)[1].lower()
        expected_type = "VIDEO" if extension in VIDEO_EXTENSIONS else "GIF" if extension == ".gif" else "IMAGE"
        if media_type != expected_type:
            errors.append(f"Wrong media type for {path}: {media_type}")

        if media_type == "VIDEO":
            thumb_marker = "/main/"
            decoded_thumb = urllib.parse.unquote(thumbnail_url)
            thumb_path = decoded_thumb.split(thumb_marker, 1)[1] if thumb_marker in decoded_thumb else ""
            if not thumb_path.startswith("assets/thumbnails/") or not os.path.isfile(thumb_path):
                errors.append(f"Missing video poster for {path}")

        resolution = entry.get("resolution", "")
        match = re.fullmatch(r"(\d+)x(\d+)", resolution)
        if not match:
            errors.append(f"Invalid resolution for {path}: {resolution}")
        elif int(match.group(1)) > int(match.group(2)):
            notices.append(f"Landscape media handled by Smart Fit: {path}")

    for path in sorted(file_set - catalog_paths):
        errors.append(f"Media missing from catalog: {path}")

    for notice in notices:
        print(f"INFO: {notice}")
    for error in errors:
        print(f"ERROR: {error}")
    print(f"Validated {len(entries)} catalog entries with {len(errors)} errors and {len(notices)} notices")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
