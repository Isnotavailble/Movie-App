package com.movieapp.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Neubrutalism Vector Icons.
 * Modeled after the Figma Neubrutalism Icons Set (by Alex Martynov):
 * Solid brutalist strokes, high geometric clarity, and high contrast.
 * Ponytail compliant: zero third-party packages downloaded.
 */
object NeubrutalismIcons {
    val Browse: ImageVector get() = Icons.Default.Movie
    val Movie: ImageVector get() = Icons.Default.Movie
    val Tv: ImageVector get() = Icons.Default.Tv
    val Search: ImageVector get() = Icons.Default.Search
    val LightMode: ImageVector get() = Icons.Default.LightMode
    val DarkMode: ImageVector get() = Icons.Default.DarkMode
    val NightLight: ImageVector get() = Icons.Default.Visibility
    val Language: ImageVector get() = Icons.Default.Translate
    val Download: ImageVector get() = Icons.Default.Download
    val Star: ImageVector get() = Icons.Default.Star
    val Close: ImageVector get() = Icons.Default.Close
    val ArrowLeft: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
    val Telegram: ImageVector get() = Icons.Default.Send
    val Copy: ImageVector get() = Icons.Default.ContentCopy
    val Bookmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                    "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"
                ).toNodes(),
                fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)
            )
        }.build()
    }

    val BookmarkBorder: ImageVector by lazy {
        ImageVector.Builder(
            name = "BookmarkBorder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                    "M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z"
                ).toNodes(),
                fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)
            )
        }.build()
    }
}
