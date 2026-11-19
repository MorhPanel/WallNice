# WallNice Engineering Roadmap & AI Agent Specification

This document provides a comprehensive roadmap, system architecture summary, and actionable task backlog for WallNice. It serves as the primary guidance document for software engineers and autonomous AI agents.

---

## 1. Executive Summary & Vision

**WallNice** is an ultra-fast, battery-efficient, AMOLED-optimized live wallpaper application and media hub for Android, built with modern declarative UI paradigms and high-performance media rendering pipelines.

### Core Architecture Principles
- **Battery-First Runtime**: Zero idle drain. Video decoding is strictly suspended when the app is backgrounded, during scroll, or when wallpapers are obscured.
- **Hardware-Accelerated UI**: 100% of motion and micro-interactions run on RenderNodes via `graphicsLayer` without layout recalculations or CPU redraw overhead.
- **Persistent Data Isolation**: Active live wallpapers and user preferences are isolated in `filesDir` and `shared_prefs`, making them completely immune to OS cache pruning and app updates.
- **Dynamic Content Distribution**: Remote wallpaper catalog is hosted as static JSON on raw GitHub CDN and automatically rebuilt on media pool pushes via GitHub Actions.

---

## 2. Current Implementation State (v0.0.26-beta)

### Core Capabilities
| Subsystem | Implementation Details |
|---|---|
| **UI Framework** | Jetpack Compose 1.7.5, Material 3, Kotlin 2.0, Dark Neon Theme |
| **Media Engine** | AndroidX Media3 1.5.0 (`ExoPlayer`), `WallpaperService` with `SurfaceHolder` |
| **Image Loading** | Single global `WallNiceImageLoader` singleton with `VideoFrameDecoder`, memory cache, disk cache |
| **Live Wallpaper** | `VideoLiveWallpaperService` with `SCALE_TO_FIT_WITH_CROPPING`, isolated local MP4 file |
| **Motion System** | 7 unified motion systems: Spring Bounce, Heart Burst, Sliding Pill, Staggered Entry, Scanline Wave, Video Pulse, Shared Element Navigation |
| **Media Filters** | Multi-selection state machine across categories, anime subcategories, and media types (`All`, `Photos`, `Videos`, `GIFs`) |
| **Security & Updates** | In-app self-updater targeting system PackageInstaller, active package signer verification, downgrade prevention, fail-closed release signing |
| **Storage Architecture** | Temporary feed previews capped at 200MB with LRU auto-pruning; gallery exports via `MediaStore`; live engine isolated to `filesDir` |

---

## 3. Product & Technical Milestones

```
┌─────────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────────┐
│       Milestone 1       │     │       Milestone 2       │     │       Milestone 3       │
│  Media Resilience &     │ ──> │   Customization &       │ ──> │   Community Hub &       │
│  Offline Persistence    │     │   Visual FX Shaders     │     │   Dynamic Sync          │
└─────────────────────────┘     └─────────────────────────┘     └─────────────────────────┘
```

### Milestone 1 — Media Resilience & Offline Persistence
- [x] **Dedicated Favorites Storage**:
  - Automatically copy downloaded media for favorited wallpapers into `filesDir/favorites/` instead of temporary `cacheDir`.
  - Provide 100% offline playback with zero risk of cache eviction.
- [ ] **Seamless Video Ping-Pong Looping**:
  - Implement optional reverse playback / ping-pong loop for short-form live wallpapers to prevent jarring loop resets.
- [ ] **Smart Cache Settings Panel**:
  - Add cache size inspection, one-tap cache purge, and custom cache quota slider (100MB – 1GB) in `SettingsScreen`.

### Milestone 2 — Customization Engine & Visual FX Shaders
- [ ] **GPU Post-Processing Shaders**:
  - Implement Compose `RuntimeShader` (Android 13+) / OpenGL pipeline for live effects:
    - Retro CRT scanline filter
    - Dynamic chromatic aberration
    - Film grain and vinyl texture overlay
    - AMOLED Pure Black boost (threshold cutoff)
- [ ] **Interactive Gyroscope Parallax**:
  - Optional motion-sensor tilt parallax for static wallpapers and 3D depth-map layers.
- [ ] **Double-Tap Live Controls**:
  - Double-tap on desktop to toggle wallpaper playback or switch between favorite wallpapers.

### Milestone 3 — Community Hub & Synchronization
- [ ] **Decentralized Likes Sync**:
  - Lightweight synchronization of wallpaper likes via serverless backend or encrypted public endpoints.
- [ ] **In-App Media Submission**:
  - Direct contributor submission flow with automated 9:16 aspect ratio validation, resolution checks, and watermark detection.
- [ ] **Color Palette Filtering**:
  - Extract dominant colors via Palette API on catalog build and allow color-based search (e.g. Neon Pink, Cyan, AMOLED Black, Sakura White).

### Milestone 4 — Desktop Cross-Platform Expansion
- [ ] **Linux Client**:
  - Native Wayland live wallpaper daemon using `swww` or `mpvpaper` integration.
- [ ] **Windows Client**:
  - Lightweight Win32/C++ background process with DirectX 11 surface rendering hooked into Desktop Window Manager (DWM).

---

## 4. Guidelines for AI Agents & Contributors

Any autonomous AI agent or software engineer modifying this codebase must adhere strictly to these rules:

1. **Zero Comments Policy**:
   - Absolutely zero code comments (`//`, `/*`, `*`, `#`) inside Kotlin, Java, XML, or JSON files.
   - Code must be self-documenting with descriptive naming and clean architecture.
2. **English Only**:
   - All code symbols, user-facing UI strings, error toasts, commit messages, and docs must be in English.
   - `resourceConfigurations += listOf("en")` must be preserved in `build.gradle.kts`.
3. **Verified Code Only**:
   - Never push unverified code. Always run:
     ```bash
     python3 -m unittest discover -s tests
     cd android && ./gradlew compileDebugKotlin assembleDebug
     ```
4. **Battery & Performance Guardrails**:
   - Never perform image processing or file I/O on the main thread.
   - All animations must use `Modifier.graphicsLayer` or layer-level transforms to avoid triggering Compose layout/measure passes.
   - Feed video playback must pause immediately when scrolled out of view or obscured.
5. **Security & Permissions**:
   - Never track keystores or private environment variables in git.
   - Use Android Scoped Storage (`MediaStore`) for public exports; do not request legacy storage permissions on Android 10+.
