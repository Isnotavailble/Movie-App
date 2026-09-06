package com.movieapp.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Lightweight, accessible vector icons inspired by Heroicons (https://heroicons.com/).
 * Built without heavy third-party dependencies to keep APK size minimal (Ponytail principle).
 */
object Heroicons {
    val Search: ImageVector get() = NeubrutalismIcons.Search
    val Clear: ImageVector get() = NeubrutalismIcons.Close
    val ArrowLeft: ImageVector get() = NeubrutalismIcons.ArrowLeft
    val Refresh: ImageVector get() = Icons.Default.Refresh
    val Star: ImageVector get() = NeubrutalismIcons.Star
    val Browse: ImageVector get() = NeubrutalismIcons.Browse
    val LightMode: ImageVector get() = NeubrutalismIcons.LightMode
    val DarkMode: ImageVector get() = NeubrutalismIcons.DarkMode
    val NightLight: ImageVector get() = NeubrutalismIcons.NightLight
    val Language: ImageVector get() = NeubrutalismIcons.Language
}
