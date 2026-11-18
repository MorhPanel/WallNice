# WallNice

WallNice is a high-performance Android live wallpaper application and media hub built with Kotlin, Jetpack Compose, Material 3, and Android Media3 (ExoPlayer).

## Features

- **Live Video Wallpapers**: Hardware-accelerated video rendering via `WallpaperService` and Media3 with zero-lag looping and battery-saver lifecycle management.
- **GIF Loops**: Animated wallpaper rendering powered by Coil decoders.
- **4K AMOLED Wallpapers**: High-resolution static wallpaper application via `WallpaperManager` for Home, Lock, or both screens.
- **Dynamic Media Pool**: Automatic catalog synchronization directly from the `DLS/` folder on GitHub.
- **Built-in Gallery Downloader**: Scoped storage integration using Android MediaStore API for saving media to device storage.
- **Auto-Update Engine**: Automatic version comparison against remote manifest with instant update notifications.
- **Interactive Previews**: Clean simulation of Lock Screen (clock, date, notifications) and Home Screen (apps, dockbar) prior to applying wallpapers.

## Media Pool Structure

Drop media files directly into the repository:
```
DLS/
├── img/     # Images (.jpg, .png, .webp)
├── video/   # Live wallpapers (.mp4, .webm)
└── gif/     # Looping animations (.gif)
```

The catalog is automatically synchronized on every push to `DLS/**` using GitHub Actions.

## Building the Project

Requirements:
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK 35

Build with Gradle:
```bash
./gradlew assembleDebug
```

## Repository Rules

All contributions must follow the guidelines outlined in [RULES.md](RULES.md):
- English language only.
- Strict zero-comments policy across all code files.
- Verified, compilable code only.
