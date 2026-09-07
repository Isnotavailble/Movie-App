package com.movieapp.navigation

/**
 * Screen destinations supported in the application.
 */
sealed class Screen(val route: String) {
    object Movies : Screen("movies")
    object TvShows : Screen("tv_shows")
    object Feed : Screen("movies")
    object Bookmarks : Screen("bookmarks")
    object Downloads : Screen("downloads")
    object Search : Screen("search")
    object Detail : Screen("detail/{slug}?isTv={isTv}") {
        fun createRoute(slug: String, isTvShow: Boolean): String {
            return "detail/$slug?isTv=$isTvShow"
        }
    }
}
