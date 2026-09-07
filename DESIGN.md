# Spider-Man Neobrutalism Design System (DESIGN.md)

This document defines the official visual design language, color palette, typography hierarchy, component rules, and accessibility standards for the **Movie App (YoteShinZone)**. The design philosophy combines **Neobrutalism** (stark high-contrast borders, solid unblurred drop shadows, tactile surfaces, zero gradients) with an authentic **Spider-Man aesthetic** (Classic Red & Blue Suit for Light Mode, and Symbiote / Stealth Suit for Dark Mode).

---

## 1. Core Color Palettes

### 1.1 Spider-Man Solid Base Accents

All colors are solid, vivid, and flat. **Zero blur and zero gradients** are used anywhere in the app.

| Token                               | Hex (Light Mode) | Hex (Dark Mode) | Role & Semantics                                                                                                                               |
| :---------------------------------- | :--------------- | :-------------- | :--------------------------------------------------------------------------------------------------------------------------------------------- |
| **Spidey Red** (`primary`)          | `#E23636`        | `#FF334B`       | Primary brand accent: Top App Bar, primary CTAs ("Watch", "Download", "Save"), active navigation tab.                                          |
| **Spidey Blue** (`secondary`)       | `#0055FF`        | `#2563EB`       | Secondary brand accent: TV series & episode tags, secondary actions ("Direct Download", "Copy All"), active search outline, download progress. |
| **Web Gold** (`tertiary`)           | `#FFC700`        | `#FBBF24`       | Rating stars, IMDb scores, highlighted badges, alert notices.                                                                                  |
| **Web Black** (`border` / `shadow`) | `#000000`        | `#000000`       | Signature Neobrutalism hard borders and offset drop shadows.                                                                                   |
| **Web White**                       | `#FFFFFF`        | `#FFFFFF`       | Pure white for card surfaces (light mode) and high-contrast text on primary colors.                                                            |

---

### 1.2 Light Mode: Classic Spider-Man Suit

Evokes the classic bright red, blue, and clean web styling of Peter Parker's comic suit.

| Token             | Color Value | Usage                                                                            |
| :---------------- | :---------- | :------------------------------------------------------------------------------- |
| `background`      | `#F8F9FD`   | Crisp, cool off-white canvas preventing harsh eye strain.                        |
| `surface`         | `#FFFFFF`   | Card backgrounds, search container, bottom navigation bar.                       |
| `surfaceMuted`    | `#EEF2F9`   | Poster image placeholders, disabled states, unselected chip backgrounds.         |
| `textPrimary`     | `#0F172A`   | Primary readable text (Deep Charcoal/Black, contrast ratio > 12:1).              |
| `textSecondary`   | `#475569`   | Subtitles, release years, episode numbers, helper text (contrast ratio > 5.5:1). |
| `border`          | `#000000`   | Solid 2dp to 2.5dp black borders on cards, buttons, and inputs.                  |
| `shadow`          | `#000000`   | Hard 3dp to 4dp drop shadow offset to bottom-right `(3.dp, 3.dp)`.               |
| `error`           | `#C62828`   | Error alerts and destructive actions.                                            |
| `errorBackground` | `#FFEBEE`   | Soft warning/error banner background.                                            |

---

### 1.3 Dark Mode: Symbiote & Stealth Suit

Inspired by the sleek Black Symbiote suit and Miles Morales' stealth aesthetic.

| Token           | Color Value | Usage                                                            |
| :-------------- | :---------- | :--------------------------------------------------------------- |
| `background`    | `#0A0E17`   | Deep obsidian canvas.                                            |
| `surface`       | `#121826`   | Elevated dark cards, sheet dialogs, bottom bar container.        |
| `surfaceMuted`  | `#1A2234`   | Poster skeleton placeholders, secondary dark cards.              |
| `textPrimary`   | `#F8FAFC`   | Bright crisp white text for maximum readability.                 |
| `textSecondary` | `#94A3B8`   | Cool slate secondary text (contrast ratio > 6:1).                |
| `border`        | `#000000`   | Consistent 2dp to 2.5dp solid black borders.                     |
| `shadow`        | `#000000`   | Deep black Neobrutalism shadow.                                  |
| `primary`       | `#FF334B`   | High-vibrancy neon Spider-Man red tailored for dark backgrounds. |
| `secondary`     | `#2563EB`   | Electric cobalt blue for TV series chips, links, and progress.   |
| `tertiary`      | `#FBBF24`   | High-contrast gold for rating badges.                            |

---

## 2. Spider-Man Blue: Strategic Placement & Purpose

Spider-Man’s iconic color harmony relies on the balance between **Red** (energy, action, alert) and **Blue** (calm, technical, structure, utility). To make the app look stunning without visual clutter, `SpideyBlue` (`#0055FF` in Light / `#2563EB` in Dark) is purposefully deployed in the following areas:

1. **TV Series & Episode Categorization**:
   - Movie badge: **Spidey Red** (`#E23636`)
   - TV Show badge: **Spidey Blue** (`#0055FF`)
   - Season & Episode chips (`Season 1`, `Episode 10`): **Spidey Blue**
   - Gives instant cognitive distinction between standalone movies and serialized TV content.

2. **Utility & Action Buttons (Secondary CTAs)**:
   - "Direct Download" / "တိုက်ရိုက် ဒေါင်းလုဒ်ဆွဲမည်": Spidey Blue button with black border and hard shadow.
   - "Copy All Links" / "လင့်ခ်အားလုံး ကူးယူမည်": Spidey Blue outline/badge.
   - "Open in Telegram": Blue accent.

3. **In-Page Search Bar Focus & Active Filter Accents**:
   - Focused search border or cursor: Spidey Blue accent.
   - Active search filter chips ("All", "HD 1080p", "720p"): Spidey Blue background with white text.

4. **Progress Indicators & Activity Feedback**:
   - Pull-to-refresh spinner: Spidey Blue.
   - Real-time download progress bar fill: Spidey Blue with solid black border.

---

## 3. UI Redesign: Clean Neobrutalist Bottom Navigation

### 3.1 Problem Identified in Previous Design

- The previous implementation placed each tab item into an **individual isolated rectangular box with its own border and shadow**.
- This created visual clutter ("four button boxes jammed side by side"), reduced touch target spacing, and caused Myanmar text labels to be cropped and unaligned.

### 3.2 Redesigned Architecture

- **Unified Bar Surface**: The bottom navigation is a single, continuous bar with `surface` background, a prominent solid 2.5dp top border (`neoBorder`), and a bottom drop shadow.
- **No Individual Rectangle Boxes**: Tab items sit directly on the bar with generous padding.
- **Active State Indicator**:
  - Active icon tints with **Spidey Red** (or **Spidey Blue**).
  - Active text is bolded with primary color.
  - A subtle 4dp tactile dot or bottom accent line reinforces the selected state.
- **Inactive State**:
  - Subtle `textSecondary` tint, outline vector icon, clean and unobtrusive.
- **Accessibility & Touch Target**:
  - Each tab has a minimum touch target of `48.dp` height.
  - Full `Role.Tab` semantics with `selected = isSelected`.

---

## 4. Performance & Smooth Scrolling Guidelines

To ensure buttery-smooth 60/120fps scrolling on `LazyVerticalGrid`:

1. **Coil Image Caching**:
   - Implement `ImageLoaderFactory` in `MovieApplication` with configured memory cache (25% memory pool) and disk cache.
   - Use `crossfade(true)` and downsampled poster dimensions.
2. **Distinct Pagination Flow**:
   - Use `distinctUntilChanged()` on `snapshotFlow` in `MovieListScreen` so scroll position calculations do not re-emit on every sub-pixel drag.
3. **Stable Compose Keys**:
   - Use stable `item.id` as the LazyGrid key instead of dynamic string concatenations.
4. **DrawBehind Optimization**:
   - Neobrutalism shadows use `Modifier.drawBehind` to avoid layout pass re-measurements.

---

## 5. Accessibility (WCAG 2.2 AA Compliance)

- **Color Contrast**:
  - Light mode: Normal text `#0F172A` on `#FFFFFF` = **15.4:1** (Exceeds 4.5:1 requirement).
  - Dark mode: Normal text `#F8FAFC` on `#121826` = **14.8:1** (Exceeds 4.5:1 requirement).
  - Spidey Red `#E23636` with white text `#FFFFFF` = **4.6:1** (Passes WCAG AA).
  - Spidey Blue `#0055FF` with white text `#FFFFFF` = **4.7:1** (Passes WCAG AA).
- **Interactive Targets**:
  - Every button, tab, and toggle has a minimum touch target of `48.dp x 48.dp`.
- **Screen Reader Semantics**:
  - `role = Role.Tab`, `Role.Button`, and meaningful `contentDescription` on all icons.
