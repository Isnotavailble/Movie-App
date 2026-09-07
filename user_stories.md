## Feature: User Stories

### US-01: Toggle Content Type

- **As a user**,
  I want to switch between a "Movies" tab and a "TV Shows" tab,
  So that I can filter the feed strictly to the media type I want to browse.
- **Acceptance Criteria**:
  - A top app bar with segmented tabs or tabs for "Movies" and "TV Shows" is displayed.
  - Selecting "Movies" triggers `GET /api/movies?page=1` and displays the movie grid.
  - Selecting "TV Shows" triggers `GET /api/tv-shows?page=1` and displays the TV show grid.
  - Switching between tabs preserves the existing feed list and scroll position in-memory without firing duplicate network requests for cached data.

### US-02: Browse Media Grid

- **As a user**,
  I want to view a responsive vertical grid of media posters,
  So that I can scan titles visually and select one to explore.
- **Acceptance Criteria**:
  - Feed items render in a 2-column or 3-column `LazyVerticalGrid` based on screen width.
  - Each item card displays:
    - Poster image (loaded asynchronously).
    - Title.
    - Release year and rating badge (if present in API response).
  - Shimmer placeholder is displayed while images are loading.
  - Fallback placeholder image appears if the image URL fails to load.
  - Tapping an item navigates to the Media Detail screen using the item's `slug`.

### US-03: Infinite Pagination

- **As a user**,
  I want the feed to load more titles automatically as I scroll down,
  So that I have an uninterrupted browsing experience without manual page navigation.
- **Acceptance Criteria**:
  - Next page query (`page = currentPage + 1`) automatically triggers when the user scrolls within 3-4 items from the bottom.
  - A bottom circular progress indicator is shown while fetching the next page.
  - If the last page is reached (`has_more: false` or empty list), pagination stops and no further API requests are dispatched.
  - Network pagination failure shows a bottom inline banner with a "Retry" button.

---

## Feature: Search & Discovery

### US-04: Search Titles

- **As a user**,
  I want to search for titles via a keyword search bar,
  So that I can quickly locate specific movies or TV shows.
- **Acceptance Criteria**:
  - A persistent search input with a clear ("X") button is provided.
  - API call to `GET /api/search?keyword={query}&page=1` triggers after a 400ms debounce once the user stops typing.
  - Search results display in a list or grid format.
  - Each card displays a clear badge indicating `[Movie]` or `[TV Show]`.
  - Tapping the clear ("X") button clears the input and restores the default/empty search state.

### US-05: Handle Search Feedback & Edge Cases

- **As a user**,
  I want to see descriptive feedback when a search returns no items or fails,
  So that I understand whether my query yielded zero results or the system encountered an error.
- **Acceptance Criteria**:
  - Empty results return an empty-state illustration with: `"No results found for '{query}'"`.
  - Network timeout or HTTP 5xx error shows an error state with a dedicated "Retry" button.
  - White space only queries (e.g., `"   "`) do not trigger API requests.

---

## Feature: Media Details & Episodes

### US-06: View Media Overview

- **As a user**,
  I want to view full metadata for a selected title,
  So that I can evaluate plot details, ratings, and download availability.
- **Acceptance Criteria**:
  - Route triggers `GET /api/movies/{slug}` or `GET /api/tv-shows/{slug}`.
  - Detail screen renders:
    - Backdrop / Poster image.
    - Full title.
    - Release year, runtime, and content/user ratings.
    - Genre chips.
    - Plot summary text.
  - Centered loading spinner is shown during initial load.
  - If the request fails, an error view with a "Retry" button replaces the screen content.

### US-07: TV Show Season & Episode Selection

- **As a user**,
  I want to choose a specific season and episode for a TV show,
  So that I can access the exact download links associated with that episode.
- **Acceptance Criteria**:
  - If the media type is `TV Show`, a season selector (segmented tabs or dropdown) is rendered.
  - Selecting a season updates the list of available episodes for that season.
  - Default selection targets Season 1, Episode 1 on screen load.
  - Selecting an episode updates the Download Link Action Hub to display links matching that specific episode.

---

## Feature: Download Action Hub & Link Resolver

### US-08: View External Download Links & Resolution Filter

- **As a user**,
  I want to view download links with resolution filtering chips (`All`, `720p`, `1080p`, `4K`),
  So that I can instantly find the file quality matching my device storage and internet speed.
- **Acceptance Criteria**:
  - Download options render as distinct action cards with:
    - Provider label (e.g., Telegram, MegaUp, Yoteshin Portal).
    - Resolution/Quality badge (e.g., 720p, 1080p, 4K).
    - File size (if provided by payload).
  - A horizontal row of resolution chips (`All`, `720p`, `1080p`, `4K`) is displayed at the top of the sheet.
  - Selecting a resolution chip instantly filters the list in real-time without reloading.
  - If no download links exist for the title, selected episode, or active filter, a friendly empty-state message is displayed.

### US-09: In-App Ad-Bypass Direct Download

- **As a user**,
  I want the app to bypass intermediate ad-gates (e.g., MegaUp, Yoteshin Portal) and download files directly,
  So that I don't have to navigate frustrating ads, redirects, and popups in a web browser.
- **Acceptance Criteria**:
  - Tapping "Direct Download" on supported hosting servers triggers an internal resolver/scraper (`DirectDownloadResolver`).
  - While resolving, the button shows a loading spinner and a Toast message (`"Bypassing ads & preparing download..."`).
  - Upon successful resolution, the media file is enqueued into Android's native `DownloadManager` with notification progress.
  - If resolving fails, it gracefully falls back to opening the link in the user's default web browser.

### US-10: Copy Download Link

- **As a user**,
  I want to copy the direct link URL to the clipboard with one tap,
  So that I can easily paste it into external download managers (such as 1DM or ADM).
- **Acceptance Criteria**:
  - Each download option card contains a dedicated "Copy" icon button.
  - Tapping the icon writes the raw link URL to Android's `ClipboardManager`.
  - A brief Toast or feedback displays: `"Link copied to clipboard"`.

### US-11: Telegram Protocol Instant Deep Linking

- **As a user**,
  I want Telegram links to launch directly in my installed Telegram app using native protocols (`tg://`),
  So that I avoid browser redirect hops and can view or download the file with zero extra taps.
- **Acceptance Criteria**:
  - Telegram download links convert web URLs (`https://t.me/...`) to native `tg://resolve` URI formats.
  - Tapping "Telegram" launches the Telegram app directly via `Intent.ACTION_VIEW`.
  - If the Telegram app is not installed, it falls back to opening the web URL in the default browser.

### US-12: TV Show Full Season Batch Download

- **As a user**,
  I want to download an entire TV season or export all episode links at once,
  So that I don't have to manually click through each episode one by one.
- **Acceptance Criteria**:
  - TV show detail screens provide a "Download Season (X eps)" action button when viewing a season.
  - Tapping the button opens a batch dialog or sheet listing links for all episodes in the season.
  - Users can trigger batch native downloads or copy all episode links formatted to the clipboard for batch download managers.

---

## Feature: Bookmarks & Offline Caching

### US-13: Bookmark Titles to Watch Later

- **As a user**,
  I want to bookmark movies and TV shows from their detail screen,
  So that I can curate a personal watchlist and return to them anytime.
- **Acceptance Criteria**:
  - A bookmark toggle icon button is prominently displayed in the top bar of the Detail screen.
  - Tapping the button saves/removes the title in the local Room database (`movies` table).
  - The icon visually toggles between outlined and filled states with immediate Toast feedback (`"Added to bookmarks"` / `"Removed from bookmarks"`).
  - Bookmarked status persists across app restarts without requiring internet access or user login.

### US-14: Dedicated Bookmarks Screen

- **As a user**,
  I want to access all my bookmarked titles in a dedicated "Bookmarks" tab from the bottom navigation,
  So that I can view, browse, and launch my saved watchlist.
- **Acceptance Criteria**:
  - A "Bookmarks" item is available in the bottom navigation bar alongside "Browse" and "Search".
  - The screen displays saved movies and TV shows in a 2-column responsive grid with poster, title, year, and type badge.
  - Tapping any bookmarked card immediately opens its Detail screen.
  - If no bookmarks exist, an illustrated empty state explains how to add bookmarks.
  - Bookmarks update dynamically in real-time using Kotlin `Flow` observation from Room.

### US-15: Local Room Caching & Bandwidth Conservation

- **As a user**,
  I want previously visited movie and show details to load instantly from local storage,
  So that I save mobile data and don't encounter API rate-limiting errors (HTTP 429).
- **Acceptance Criteria**:
  - Detail screens follow a cache-first architecture: if a cached record exists in Room (`jsonDetail`), it renders immediately.
  - Fresh data is fetched in the background and silently updates the cache.
  - Cached data remains available even when offline or in airplane mode.
