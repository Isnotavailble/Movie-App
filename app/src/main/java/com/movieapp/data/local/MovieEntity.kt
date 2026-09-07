package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Minimal Room Entity for caching movie details and persisting user bookmarks.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val slug: String,
    val title: String,
    val poster: String?,
    val rating: String?,
    val releaseYear: String?,
    val isTvShow: Boolean,
    val plot: String?,
    val jsonDetail: String?,
    val isBookmarked: Boolean = false,
    val bookmarkedAt: Long = 0L,
    val cachedAt: Long = System.currentTimeMillis()
)
