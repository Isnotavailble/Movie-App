package com.movieapp.features.movielist

import com.google.gson.annotations.SerializedName

/**
 * Encapsulates a paginated list response from the movie or TV show feed endpoints.
 *
 * @property items List of titles returned for the current page.
 * @property meta Nested pagination metadata (current page, last page, total items).
 * @property hasMore Optional direct flag for more pages.
 */
data class MovieListResponseDTO(
    @SerializedName("data", alternate = ["results", "items"])
    val items: List<MovieDTO>? = null,

    @SerializedName("meta")
    val meta: PaginationMetaDTO? = null,

    @SerializedName("page", alternate = ["current_page"])
    private val rawCurrentPage: Int? = null,

    @SerializedName("total_pages", alternate = ["last_page"])
    private val rawTotalPages: Int? = null,

    @SerializedName("has_more")
    val hasMore: Boolean? = null
) {
    /**
     * Resolves the list of titles safely, returning an empty list if null.
     */
    val safeItems: List<MovieDTO>
        get() = items ?: emptyList()

    val currentPage: Int
        get() = meta?.currentPage ?: rawCurrentPage ?: 1

    val totalPages: Int
        get() = meta?.lastPage ?: rawTotalPages ?: 1

    val canLoadMore: Boolean
        get() = when {
            hasMore != null -> hasMore
            meta != null -> meta.currentPage < meta.lastPage
            rawTotalPages != null -> currentPage < rawTotalPages
            else -> safeItems.isNotEmpty()
        }
}
