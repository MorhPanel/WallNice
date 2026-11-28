# Media Upload & Processing Pipeline

When adding new wallpapers (photos or videos) to WallNice, always follow this pipeline:

## 1. Photos (`DLS/img/<Category>/` or `DLS/img/Anime/<Subcategory>/`)
- **Target Resolution & Aspect Ratio:** `1440x3200` (`9:20` vertical portrait). Crop center to `0.45` aspect ratio and upscale using Lanczos + subtle `UnsharpMask` (`radius=1.5, percent=120, threshold=2`).
- **Background:** Preserve the original background. Only clean compression noise into pure `#000000` if the source is already a dark/AMOLED scene.
- **Naming & Hash:** `<descriptive_snake_case>_<8char_sha256>.jpg` (e.g., `ellen_joe_a1b2c3d4.jpg`).
- **Classification:** Place in the matching category folder (`Anime/<Series>`, `Gym`, `IRL`, etc.).

## 2. Videos (`DLS/video/<Category>/` or `DLS/video/Anime/<Subcategory>/`)
- **Target Resolution & Codec:** `2160x3840` (`9:16` / `9:20`), H.264 (`libx264`, `yuv420p`, `crf 18`, `+faststart`) with `hqdn3d` denoise + `lanczos` scale + `unsharp`.
- **Thumbnail:** Extract a crisp frame to `assets/thumbnails/<same_base_name>.jpg`.

## 3. Catalog & Verification
1. Run `python .github/scripts/generate_catalog.py` to update `catalog.json`.
2. Run `python -m unittest tests/test_security_controls.py` to verify all checks pass.
3. Commit and push to `origin main`.
