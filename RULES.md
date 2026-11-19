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

## 5. UI Tabs, Categories & Filter Architecture

All navigation and filter controls in WallNice must follow the unified multi-selection and hierarchy architecture:

### 5.1 Category Structure & Hierarchy
- **Top-Level Categories**:
  - `All`: Default selection showing wallpapers from all categories and formats.
  - `Favorites`: Local on-device user-favorited items.
  - Content Categories: `Anime`, `AMOLED 4K`, `Gothic`, `Live Video`, `GIF Loops`, etc.
- **Nested Collections (Anime & Franchises)**:
  - Specific franchise subcategories (e.g. *Gothic*, *Nikke*, *Miss Kobayashi's Dragon Maid*, *The Quintessential Quintuplets*) must NOT be scattered across top-level category pills.
  - They must be nested within their parent category (e.g. `Anime`).
  - Expanding the parent pill toggles an animated slide-down collection dropdown (`AnimeSubcategoryDropdown`) with smooth `expandVertically` + `fadeIn` transitions.
  - The parent pill displays dynamic trailing arrows (`KeyboardArrowDown` / `KeyboardArrowUp`).

### 5.2 Dynamic Category Pill Labeling
- When `All` (or `All Anime`) is active: Pill displays `Category` (e.g. `Anime`).
- When 1 subcategory is active: Pill displays `Category: Subcategory` (e.g. `Anime: Gothic`).
- When multiple subcategories are active: Pill displays `Category (N)` (e.g. `Anime (2)`).

### 5.3 Unified Multi-Selection Filter State Logic
Applies identically to **Anime Collections** and **Media Type Filters** (`All`, `Photos`, `Videos`, `GIFs`):
1. **Default State**:
   - `All` is selected by default with an active indicator (checkmark `✓` / `CheckCircle`).
2. **Transition from All to Specific**:
   - When `All` is active and the user taps any specific option, `All` is immediately deselected.
   - Only the chosen specific option becomes active with a checkmark.
3. **Simultaneous Multi-Selection**:
   - Users may select multiple specific options concurrently (e.g. `Photos + Videos` or `Gothic + Nikke`).
   - The feed displays wallpapers matching ANY active selection (union filter).
4. **Automatic Reset to All**:
   - If all specific options are selected, selection automatically collapses back to `All`.
   - If the user deselects the only remaining active specific option, selection automatically resets back to `All`.
   - Tapping `All` directly clears all specific options and returns to `All`.
5. **Non-Intrusive Dropdown Behavior**:
   - Multi-select dropdown menus must remain open while toggling items to facilitate multi-selection.
   - The dropdown only closes when the user taps the close button (`✕`) or taps the parent category pill.

### 5.4 Visual Feedback & Accessibility
- Active pills and list items must display high-contrast visual indicators:
  - Pills: `Icons.Default.Check` trailing icon, `NeonPink` background, white text.
  - List items: `Icons.Default.CheckCircle` leading icon in `NeonPink`, soft pink background tint (`NeonPink.copy(alpha = 0.12f)`), and pink border.
  - Unselected items: `Icons.Default.RadioButtonUnchecked`, subtle borders, and muted text.

### 5.5 State Preservation & Recreations
- All selected filters (`selectedCategory`, `selectedAnimeSubcategories`, `selectedMediaFilter`, `searchQuery`) must survive configuration changes and process death using Compose `rememberSaveable` with dedicated custom savers.
- Empty search/filter states must provide a single-tap reset action that returns all filters to `All`.
