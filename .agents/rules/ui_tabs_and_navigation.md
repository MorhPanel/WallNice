# UI Tabs, Categories & Navigation Architecture

Guidelines and constraints for all navigation, tabs, collections, and media filters in WallNice.

## Category Hierarchy
- Top-level category pills must include standard meta-categories: `All`, `Favorites`, content categories (`Anime`, `AMOLED 4K`, etc.).
- Sub-series, franchises, and thematic subsets (e.g., *Gothic*, *Nikke*, *Miss Kobayashi's Dragon Maid*, *The Quintessential Quintuplets*) must be nested inside their parent category (`Anime`) instead of cluttering the top bar.
- An animated slide-down collection dropdown menu (`AnimeSubcategoryDropdown`) presents available subcategories with counts and status indicators.

## Multi-Selection Filter State Machine
All collection selectors and media filters (`All`, `Photos`, `Videos`, `GIFs`) must conform to the multi-selection state machine:
1. **Default**: `All` is active by default with a visible checkmark.
2. **Transition**: When `All` is active and a specific item is clicked, `All` is unchecked immediately and only the clicked item is selected.
3. **Multi-Select**: Users can select multiple items concurrently (union filter matching any selected subcategory/format).
4. **Auto-Reset**:
   - Selecting all specific options collapses back to `All`.
   - Deselecting the last active specific item resets back to `All`.
   - Tapping `All` resets all filters to `All`.
5. **Dropdown Retention**: Dropdowns must stay open during selection toggling to avoid annoying repeated re-open clicks.

## Visual Indicators & Dynamic Labels
- Category pill label dynamic format:
  - All: `Category` (e.g. `Anime`)
  - Single: `Category: Subcategory` (e.g. `Anime: Gothic`)
  - Multiple: `Category (N)` (e.g. `Anime (2)`)
- Active pills display `Icons.Default.Check`.
- Dropdown items display `Icons.Default.CheckCircle` with neon pink highlighting and background tint.
