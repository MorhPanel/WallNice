# WallNice

Ultra-fast, cross-platform live wallpaper engine and media hub. Zero lag, zero battery drain.

---

## Platform Support

| Platform | Runtime | Status |
|---|---|---|
| **Android** | `WallpaperService` + Media3 (ExoPlayer) | 🟡 In Progress |
| **Windows** | DirectX 11 / DWM overlay + Win32 API | 🔜 Coming Soon |
| **Linux (Fedora / GNOME)** | `org.freedesktop.portal.Wallpaper` / `swww` daemon (Wayland) | 🔜 Coming Soon |

---

## Features (Android)
- **Live Video** — Hardware-accelerated MP4/WebM via Media3, zero-copy surface pipeline.
- **GIF Loops** — Animated rendering via Coil ImageDecoder.
- **4K Static** — Direct `WallpaperManager` injection for Home, Lock, or both screens.
- **Auto Pool** — Live catalog sync from GitHub `DLS/` folder via raw CDN.
- **In-App Downloader** — MediaStore scoped storage, no extra permissions on Android 10+.
- **Auto-Update Engine** — Remote version check on launch with one-tap update dialog.

---

## Media Pool Structure

Drop files into `DLS/` — catalog auto-rebuilds on every push via GitHub Actions:

```
DLS/
├── img/
│   ├── Anime/
│   │   └── <Anime Title>/   ← each title in its own folder
│   │       └── character.jpg
│   └── <Category>/
│       └── wallpaper.jpg
├── video/                   ← live video wallpapers (.mp4, .webm)
└── gif/                     ← looping animations (.gif)
```

---

## TODO

| # | Task | Platform | Priority | Status |
|---|---|---|---|---|
| 1 | Live video wallpaper engine | Android | 🔴 High | 🟡 In Progress |
| 2 | GitHub catalog auto-sync | Android | 🔴 High | 🟡 In Progress |
| 3 | Auto-update dialog | Android | 🔴 High | 🟡 In Progress |
| 4 | Category filter + search | Android | 🟡 Medium | 🔜 Planned |
| 5 | Favorites / local collection | Android | 🟡 Medium | 🔜 Planned |
| 6 | Custom thumbnail for video preview | Android | 🟡 Medium | 🔜 Planned |
| 7 | Win32 DWM live wallpaper layer | Windows | 🔴 High | ⬜ Not Started |
| 8 | DirectX 11 video renderer | Windows | 🔴 High | ⬜ Not Started |
| 9 | Windows system tray app + hotkeys | Windows | 🟡 Medium | ⬜ Not Started |
| 10 | Wayland `swww` daemon integration | Linux | 🔴 High | ⬜ Not Started |
| 11 | GNOME `org.freedesktop.portal.Wallpaper` static support | Linux | 🟡 Medium | ⬜ Not Started |
| 12 | KDE Plasma wallpaper plugin | Linux | 🟢 Low | ⬜ Not Started |
| 13 | Unified cross-platform catalog CLI | All | 🟡 Medium | ⬜ Not Started |

---

## Build

**Android:**
```bash
./gradlew assembleDebug
```

**CI:** Every push to `main` triggers an automated APK build via GitHub Actions.
Releases → [github.com/MorhPanel/WallNice/releases](https://github.com/MorhPanel/WallNice/releases)

---

## Rules

See [RULES.md](RULES.md) — English only, zero comments, verified code.
