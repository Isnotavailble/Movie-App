package com.movieapp.features.movielist

import com.google.gson.annotations.SerializedName
import com.movieapp.features.moviedetail.CategoryDTO

/**
 * Encapsulates a single movie or television title returned in catalog feeds or search results.
 */
data class MovieDTO(
    @SerializedName("id")
    private val rawId: Any? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("name")
    private val nameFallback: String? = null,

    @SerializedName("slug")
    val slug: String? = null,

    @SerializedName("poster", alternate = ["poster_path", "image"])
    val poster: String? = null,

    @SerializedName("release_year", alternate = ["year", "first_air_date"])
    val releaseYear: String? = null,

    @SerializedName("rating", alternate = ["vote_average", "score"])
    private val rawRating: Any? = null,

    @SerializedName("type", alternate = ["media_type"])
    val mediaType: String? = null,

    @SerializedName("resolution")
    val resolution: String? = null,

    @SerializedName("categories")
    val categories: List<CategoryDTO>? = null
) {
    val id: Long
        get() = when (rawId) {
            is Number -> rawId.toLong()
            is String -> rawId.toLongOrNull() ?: 0L
            else -> 0L
        }

    val rating: Double?
        get() = when (rawRating) {
            is Number -> rawRating.toDouble()
            is String -> rawRating.toDoubleOrNull()
            else -> null
        }

    /**
     * Resolves the primary title cleanly, falling back to nameFallback if title is null.
     */
    val displayTitle: String
        get() = title ?: nameFallback ?: "Untitled"

    /**
     * Formats rating as a clean single-decimal string (e.g. "8.5").
     */
    val formattedRating: String
        get() = when (rawRating) {
            is Number -> String.format(java.util.Locale.US, "%.1f", rawRating.toDouble())
            is String -> rawRating.toDoubleOrNull()?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: rawRating
            else -> "N/A"
        }

    /**
     * Resolves the 4-digit release year safely.
     */
    val displayYear: String
        get() = releaseYear?.take(4) ?: "Unknown"

    /**
     * Identifies if this entry is a TV show.
     */
    val isTvShow: Boolean
        get() = mediaType?.contains("tv", ignoreCase = true) == true

    val categoryNames: List<String>
        get() = categories?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
}
