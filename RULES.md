# Repository Rules & Code Guidelines

All contributors and automation agents working on the WallNice repository must adhere to the following mandatory rules:

## 1. Zero Comments Policy
- Code files (`.kt`, `.kts`, `.xml`, `.java`, `.cpp`, `.h`, `.json`, etc.) must contain **ZERO comments**.
- No inline comments (`//`), block comments (`/* ... */`), or XML comments (`<!-- ... -->`).
- The codebase must be self-documenting through clean variable naming, descriptive functions, and clear architecture.

## 2. English Only
- All documentation, commit messages, pull requests, issue descriptions, UI strings, and identifier names must be written in **English only**.
- No other languages in the repository.

## 3. Verified Code Only
- Push only tested and verified code.
- Broken builds, untracked syntax errors, incomplete stubs, and untested experimental code are strictly forbidden on the `main` branch.

## 4. Media Pool Specification
- All wallpapers must be organized inside the `DLS/` directory:
  - `DLS/img/` - Static wallpapers (`.jpg`, `.png`, `.webp`)
  - `DLS/video/` - Live video wallpapers (`.mp4`, `.webm`)
  - `DLS/gif/` - Looping animations (`.gif`)
- Optional category subdirectories may be created under `DLS/img/<Category>/` and `DLS/video/<Category>/`.
- The catalog is automatically scanned and generated via the GitHub Actions workflow.
