package com.movieapp.features.moviedetail

import com.google.gson.annotations.SerializedName
import com.movieapp.features.downloadlinks.DownloadLinkDTO

/**
 * CategoryDTO represents an individual genre/category classification for movies and TV shows.
 *
 * @property id Unique category identifier.
 * @property name Category title (e.g. "Action", "Comedy", "Drama").
 */
data class CategoryDTO(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("name")
    val name: String = ""
)

/**
 * Encapsulates full detail metadata for a single movie or television series.
 * Supports download links for both movies and multi-season TV episodes.
 */
data class MovieDetailDTO(
    @SerializedName("id")
    val id: Any? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("name")
    private val nameFallback: String? = null,

    @SerializedName("slug")
    val slug: String? = null,

    @SerializedName("poster", alternate = ["poster_path", "image"])
    val poster: String? = null,

    @SerializedName("backdrop", alternate = ["backdrop_path", "banner"])
    val backdrop: String? = null,

    @SerializedName("release_year", alternate = ["year", "first_air_date", "release_date"])
    val releaseYear: String? = null,

    @SerializedName("runtime", alternate = ["duration"])
    val runtime: String? = null,

    @SerializedName("rating", alternate = ["vote_average", "score"])
    private val rawRating: Any? = null,

    @SerializedName("overview", alternate = ["plot", "description", "summary"])
    val plot: String? = null,

    @SerializedName("categories")
    val categories: List<CategoryDTO>? = null,

    @SerializedName("genres", alternate = ["genre_list"])
    private val rawGenres: List<String>? = null,

    @SerializedName("type", alternate = ["media_type"])
    val mediaType: String? = null,

    @SerializedName("resolution")
    val resolution: String? = null,

    @SerializedName("movie_download_links")
    val movieDownloadLinks: List<DownloadLinkDTO>? = null,

    @SerializedName("seasons")
    val seasons: List<SeasonDTO>? = null
) {
    val rating: Double?
        get() = when (rawRating) {
            is Number -> rawRating.toDouble()
            is String -> rawRating.toDoubleOrNull()
            else -> null
        }

    val displayTitle: String
        get() = title ?: nameFallback ?: "Untitled"

    val displayYear: String
        get() = releaseYear?.take(4) ?: "Unknown"

    val formattedRating: String
        get() = when (rawRating) {
            is Number -> String.format(java.util.Locale.US, "%.1f / 10", rawRating.toDouble())
            is String -> rawRating.toDoubleOrNull()?.let { String.format(java.util.Locale.US, "%.1f / 10", it) } ?: rawRating
            else -> "Not rated"
        }

    val safeGenres: List<String>
        get() = categories?.map { it.name }?.filter { it.isNotBlank() }
            ?: rawGenres
            ?: emptyList()

    val safeSeasons: List<SeasonDTO>
        get() = seasons ?: emptyList()

    val safeMovieDownloadLinks: List<DownloadLinkDTO>
        get() = movieDownloadLinks ?: emptyList()

    val cleanPlot: String?
        get() = cleanHtml(plot)

    val isTvShow: Boolean
        get() = mediaType?.equals("tv-show", ignoreCase = true) == true || !seasons.isNullOrEmpty()
}

/**
 * Strips HTML markup tags and decodes common HTML entities to ensure clean plain-text on the client side.
 * Follows the Ponytail principle: zero external dependencies, robust Kotlin stdlib regex.
 */
fun cleanHtml(input: String?): String? {
    if (input.isNullOrBlank()) return null
    val s = input
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>|</div>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" *\n *"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
    return s.ifEmpty { null }
}

/**
 * Encapsulates a television show season containing episodes and associated download links.
 */
data class SeasonDTO(
    @SerializedName("id")
    val id: Any? = null,

    @SerializedName("season_number", alternate = ["number", "season"])
    private val rawSeasonNumber: Any? = null,

    @SerializedName("season_name", alternate = ["name", "title"])
    val name: String? = null,

    @SerializedName("episodes")
    val episodes: List<EpisodeDTO>? = null
) {
    val idLong: Long
        get() = when (id) {
            is Number -> id.toLong()
            is String -> id.toLongOrNull() ?: 0L
            else -> 0L
        }

    val seasonNumber: Int
        get() {
            val fromRaw = when (rawSeasonNumber) {
                is Number -> rawSeasonNumber.toInt()
                is String -> rawSeasonNumber.toIntOrNull()
                else -> null
            }
            if (fromRaw != null && fromRaw > 0) return fromRaw

            val fromName = name?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }
            if (fromName != null && fromName > 0) return fromName

            val fromEpisode = episodes?.firstOrNull()?.seasonNumber
            if (fromEpisode != null && fromEpisode > 0) return fromEpisode

            return 1
        }

    val safeEpisodes: List<EpisodeDTO>
        get() = episodes ?: emptyList()

    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Season $seasonNumber"
}

/**
 * Encapsulates an individual television episode with its download links.
 */
data class EpisodeDTO(
    @SerializedName("id")
    val id: Any? = null,

    @SerializedName("season_number", alternate = ["season"])
    private val rawSeasonNumber: Any? = null,

    @SerializedName("episode_number", alternate = ["number", "episode"])
    private val rawEpisodeNumber: Any? = null,

    @SerializedName("name", alternate = ["title"])
    val title: String? = null,

    @SerializedName("runtime", alternate = ["duration"])
    val runtime: String? = null,

    @SerializedName("poster", alternate = ["image"])
    val poster: String? = null,

    @SerializedName("tvshow_download_links", alternate = ["download_links"])
    val tvDownloadLinks: List<DownloadLinkDTO>? = null
) {
    val idLong: Long
        get() = when (id) {
            is Number -> id.toLong()
            is String -> id.toLongOrNull() ?: 0L
            else -> 0L
        }

    val seasonNumber: Int
        get() = when (rawSeasonNumber) {
            is Number -> rawSeasonNumber.toInt()
            is String -> rawSeasonNumber.toIntOrNull() ?: 1
            else -> 1
        }

    val episodeNumber: Int
        get() = when (rawEpisodeNumber) {
            is Number -> rawEpisodeNumber.toInt()
            is String -> rawEpisodeNumber.toIntOrNull() ?: 1
            else -> 1
        }

    val displayTitle: String
        get() = if (!title.isNullOrBlank()) "Ep $episodeNumber: $title" else "Episode $episodeNumber"

    val safeDownloadLinks: List<DownloadLinkDTO>
        get() = tvDownloadLinks ?: emptyList()
}
