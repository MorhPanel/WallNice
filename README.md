# WallNice

Ultra-fast Android wallpaper client and live engine. Zero GC lag, zero battery drain.

## Features
- **Live Video** — Hardware-accelerated MP4/WebM playback via Media3.
- **GIF Loops** — Smooth animated background rendering.
- **4K Images** — Static wallpaper applicator for Home, Lock, or both screens.
- **Auto Pool** — Dynamic wallpaper feed synchronized from `DLS/`.
- **In-App Downloader** — Direct saving to system gallery via MediaStore.
- **Update Engine** — Automatic version check against remote releases.

## Media Pool
Drop files into `DLS/` to automatically serve them to the app:
```
DLS/
├── img/     # Static images (.jpg, .png, .webp)
├── video/   # Live video loops (.mp4, .webm)
└── gif/     # Animated loops (.gif)
```

## Build
```bash
./gradlew assembleDebug
```

## Rules
See [RULES.md](RULES.md) for contribution guidelines.
