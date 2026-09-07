package com.movieapp.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "EN"),
    MY("my", "မြန်မာ")
}

/**
 * Lightweight localization manager parsing translation.json via Android SDK built-in JSONObject.
 * Complies with the Ponytail principle (zero external dependencies).
 */
object LocalizationManager {

    var currentLanguage by mutableStateOf(AppLanguage.EN)
        private set

    private val translations = mutableMapOf<String, MutableMap<String, String>>()

    // In-memory fallback dictionary to ensure tests and early renders always succeed
    private val defaultFallback = mapOf(
        "app_title" to "Movie Catalog",
        "app_subtitle" to "Find what to watch",
        "search_btn" to "Search",
        "nav_browse" to "Browse",
        "nav_search" to "Search",
        "theme_light" to "Light",
        "theme_dark" to "Dark",
        "night_light" to "Night Light",
        "night_light_on" to "Night Light ON",
        "night_light_off" to "Night Light OFF",
        "lang_en" to "EN",
        "lang_my" to "MY",
        "category_movies" to "Movies",
        "category_tv_shows" to "TV Shows",
        "try_again" to "Try Again",
        "loading" to "Loading...",
        "badge_movie" to "Movie",
        "badge_tv_show" to "TV Show",
        "search_heading" to "Look for a movie or show",
        "search_placeholder" to "Type a title or topic...",
        "search_note" to "Results update automatically as you type.",
        "search_clear" to "Clear",
        "search_prompt_title" to "Search by Title",
        "search_prompt_desc" to "Type in the box above to find movies or television series.",
        "search_empty_title" to "No Matches Found",
        "search_empty_desc" to "We could not find any titles matching your search. Check your spelling or search for another title.",
        "back_to_list" to "Back to List",
        "story_summary" to "Story Summary",
        "no_summary" to "No summary is currently available for this title.",
        "get_download_links" to "Get Download Links (%d)",
        "choose_season_episode" to "Choose Season and Episode",
        "download_episode" to "Download Episode",
        "download_links" to "Download Links",
        "no_download_links" to "No download links available for this title yet.",
        "download_action" to "Download",
        "telegram_action" to "Telegram",
        "copy_link" to "Copy Link",
        "copied" to "Link copied to clipboard",
        "nav_bookmarks" to "Bookmarks",
        "bookmarks_title" to "My Bookmarks",
        "bookmarks_subtitle" to "Saved movies and shows to watch later",
        "bookmarks_empty_title" to "No Bookmarks Yet",
        "bookmarks_empty_desc" to "Save titles by tapping the bookmark button on any movie or TV show detail screen.",
        "bookmark_added" to "Added to bookmarks",
        "bookmark_removed" to "Removed from bookmarks",
        "res_all" to "All",
        "direct_download" to "Direct Download",
        "resolving_link" to "Bypassing ads & preparing download...",
        "download_full_season" to "Download Season (%d eps)",
        "download_all_episodes" to "Download All Episodes (%s)",
        "open_in_telegram" to "Open in Telegram",
        "nav_downloads" to "Downloads",
        "downloads_title" to "Downloads",
        "downloads_subtitle" to "Active downloads & offline history",
        "currently_downloading" to "Downloading",
        "download_history" to "History",
        "no_active_downloads" to "No active downloads",
        "no_active_downloads_desc" to "When you start downloading movies, track real-time progress here.",
        "no_download_history" to "No downloaded movies yet",
        "no_download_history_desc" to "Completed movie and episode downloads will appear here.",
        "download_started" to "Download started",
        "direct_link_copied" to "Direct download link copied!",
        "resolving_direct_link" to "Resolving direct link...",
        "install_telegram_prompt" to "Please install Telegram to download via this link",
        "open_movie" to "Play Video",
        "delete_download" to "Delete",
        "opening_browser_for_full_video" to "Opening download page in browser to download full video file..."
    )

    fun initialize(context: Context) {
        try {
            val jsonString = context.assets.open("translation.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            val languages = listOf("en", "my")
            for (lang in languages) {
                if (root.has(lang)) {
                    val langObj = root.getJSONObject(lang)
                    val langMap = mutableMapOf<String, String>()
                    val keys = langObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        langMap[key] = langObj.getString(key)
                    }
                    translations[lang] = langMap
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to default in-memory maps
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.EN) AppLanguage.MY else AppLanguage.EN
    }

    fun getString(key: String, vararg args: Any): String {
        val langCode = currentLanguage.code
        val raw = translations[langCode]?.get(key)
            ?: translations["en"]?.get(key)
            ?: defaultFallback[key]
            ?: key

        return if (args.isNotEmpty()) {
            try {
                String.format(raw, *args)
            } catch (_: Exception) {
                raw
            }
        } else {
            raw
        }
    }
}

/**
 * Composable string accessor that automatically recomposes when language changes.
 */
@Composable
fun t(key: String, vararg args: Any): String {
    // Reading currentLanguage creates Compose state dependency
    LocalizationManager.currentLanguage
    return LocalizationManager.getString(key, *args)
}
