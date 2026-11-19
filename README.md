<div align="center">

# WALLNICE

### WALLPAPER ENGINE

**Make your screen feel alive.**

A sleek, lightweight cross-platform experience for discovering, downloading, and applying animated live and 4K wallpapers.

<br />

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux-10151D?style=for-the-badge&logo=android&logoColor=3DDC84)](#platforms)
[![Kotlin](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-10151D?style=for-the-badge&logo=kotlin&logoColor=AA78FF)](android/)
[![Status](https://img.shields.io/badge/Status-Beta%20v0.0.14-10151D?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MorhPanel/WallNice/releases)
[![License](https://img.shields.io/badge/License-Not%20Specified-10151D?style=for-the-badge)](#license)

<br />

**[Overview](#overview) · [Platforms](#platforms) · [Features](#features) · [Architecture](#architecture) · [Getting Started](#getting-started) · [Roadmap](#roadmap)**

</div>

---

<a id="overview"></a>
<a id="-overview"></a>
## ✦ Overview

**WallNice** is a cross-platform live wallpaper engine and media hub focused on motion, battery efficiency, and simplicity. Browse a curated collection of live video backgrounds, high-contrast AMOLED art, and looping animations, preview them in full resolution, and apply them directly to your screen.

The goal is a smooth, distraction-free experience with a remotely maintained catalog and zero battery drain.

> **Project status:** Android is in active public Beta (v0.0.14-beta). Desktop clients for Windows and Linux are in development.

<a id="platforms"></a>
<a id="-platforms"></a>
<a id="platform-support"></a>
<a id="-platform-support"></a>
## ⊞ Platform Support

| Platform | Directory | Runtime & Stack | Status |
|:--:|---|---|---|
| **Android** | `./android` | Jetpack Compose · AndroidX Media3 · `WallpaperService` | 🟢 Working Beta |
| **Windows** | `./windows` | Win32 API · DirectX 11 · Desktop Window Manager (DWM) | 🔜 Coming Soon |
| **Linux (Fedora / GNOME)** | `./linux` | Wayland · `swww` daemon · Portal API | 🔜 Coming Soon |

<a id="features"></a>
<a id="-features"></a>
## ✧ Features

| | Feature | Description |
|:--:|---|---|
| ◈ | **Live video wallpapers** | Hardware-accelerated MP4/WebM video playback via AndroidX Media3 with zero-copy surface rendering. |
| ▣ | **Instant 9:20 previews** | Explore high-resolution wallpapers with dedicated safe-zone preview modes for Home and Lock screens. |
| ◉ | **AMOLED 4K static art** | Pure black (`#000000`) high-contrast backgrounds with zero battery penalty on OLED displays. |
| ⌕ | **Discover & categories** | Filter by Anime, series, resolution, and media types with reactive category chips. |
| ↓ | **Scoped storage downloads** | One-tap gallery export using Android MediaStore without legacy storage permissions. |
| ↻ | **Auto-syncing catalog** | Catalog metadata updates automatically on repository push via GitHub Actions. |
| ⇪ | **In-app updates** | Remote version check on launch with a trusted GitHub APK download handoff. |
| ♡ | **Offline cache & favorites** | Instant offline viewing and wallpaper playback with persistent favorites and local catalog caching. |

<a id="architecture"></a>
<a id="-architecture"></a>
## ⌘ Architecture

```text
                          WALLNICE ENGINE
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
   Android Client          Windows Client           Linux Client
  Kotlin + Compose        DirectX 11 + DWM         Wayland + swww
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                   ┌─────────────┴─────────────┐
                   │                           │
              Catalog API                 Shared Pool
             (static JSON)              (DLS/ directory)
                   │                           │
           GitHub Raw CDN              GitHub Releases
          catalog.json manifest         Signed APK & AAB
```

**Technology stack**

- **Android:** Kotlin 2.0, Jetpack Compose, Material 3, AndroidX Media3 (ExoPlayer), Coil 2.7
- **Media Engine:** Android `WallpaperService` with lifecycle-aware player detachment
- **Continuous Integration:** GitHub Actions (R8 shrinker, release signing, artifact publishing)
- **Catalog Automation:** Python workflow script auto-indexing `DLS/` on git push
- **Storage:** Android 10+ scoped storage via `MediaStore` API

<a id="getting-started"></a>
<a id="-getting-started"></a>
## ◇ Getting Started

### Android Installation

1. Navigate to the **[Releases](https://github.com/MorhPanel/WallNice/releases)** section.
2. Download the latest signed **`app-release.apk`**.
3. Install the APK on your Android device (Android 8.0+ / API 26+).
4. Launch WallNice, pick your favorite wallpaper, and tap **Apply**.

> Only install APKs from trusted sources. Android may prompt you to permit installation from your browser or file manager.

### Building from Source

```bash
git clone https://github.com/MorhPanel/WallNice.git
cd WallNice/android
./gradlew assembleRelease
```

<a id="repository-structure"></a>
<a id="-repository-structure"></a>
## ▤ Repository Structure

```text
WallNice/
├── android/                 # Android application (Kotlin, Compose, Media3)
├── windows/                 # Windows client (Win32, DirectX 11, DWM overlay)
├── linux/                   # Linux client (Wayland, swww daemon)
├── DLS/                     # Shared cross-platform media pool
│   ├── img/                 # Static images sorted by category and series
│   ├── video/               # Live video wallpapers (.mp4, .webm)
│   └── gif/                 # Looping animations (.gif)
├── .github/                 # Workflows (Release CI, Catalog generation)
├── catalog.json             # Remote catalog manifest
└── RULES.md                 # Contributor guidelines
```

<a id="roadmap"></a>
<a id="-roadmap"></a>
<a id="roadmap--todo"></a>
<a id="-roadmap--todo"></a>
## ⚑ Roadmap & TODO

| # | Task | Platform | Priority | Status |
|:--:|---|---|:--:|:--:|
| 1 | Hardware-accelerated live video engine | Android | 🔴 High | 🟢 Completed |
| 2 | Dynamic GitHub catalog auto-sync | Android | 🔴 High | 🟢 Completed |
| 3 | In-app release updater dialog | Android | 🔴 High | 🟢 Completed |
| 4 | Portrait 9:20 AMOLED safe-zone adaptation | Android | 🔴 High | 🟢 Completed |
| 5 | Release signing pipeline & Play Protect compliance | Android | 🔴 High | 🟢 Completed |
| 6 | Category filtering and multi-tag search | Android | 🟡 Medium | 🟢 Completed |
| 7 | Offline wallpaper cache and favorites | Android | 🟡 Medium | 🟢 Completed |
| 8 | Win32 DWM live wallpaper desktop injection | Windows | 🔴 High | ⬜ Not Started |
| 9 | DirectX 11 hardware video renderer | Windows | 🔴 High | ⬜ Not Started |
| 10 | System tray controls and hotkey manager | Windows | 🟡 Medium | ⬜ Not Started |
| 11 | Wayland `swww` daemon integration | Linux | 🔴 High | ⬜ Not Started |
| 12 | GNOME and KDE Plasma wallpaper plugin | Linux | 🟡 Medium | ⬜ Not Started |

<a id="design-principles"></a>
<a id="-design-principles"></a>
## ◈ Design Principles

**Zero battery waste.** Video decoding pauses instantly when your screen is locked or covered by another application.

**Minimal by default.** Content takes center stage with a clean AMOLED UI and zero clutter.

**No tracking or accounts.** Browse, preview, and apply wallpapers instantly without registration.

**Independent media pool.** Add new wallpapers to `DLS/` at any time without updating the application code.

<a id="license"></a>
<a id="-license"></a>
<a id="license--content"></a>
<a id="-license--content"></a>
## ⚖ License & Content

No software license has been specified yet. All original application source code is maintained by MorhPanel.

Wallpaper media remains subject to the rights and licenses of their respective creators. Only content intended for personal customization is included.

---

<div align="center">

### WALLNICE / ENGINE

*Less static. More alive.*

<sub>Designed for Android, Windows & Linux · Active Beta</sub>

</div>
