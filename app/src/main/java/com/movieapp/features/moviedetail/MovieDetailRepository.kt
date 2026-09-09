package com.movieapp.features.moviedetail

import com.google.gson.Gson
import com.movieapp.MovieApplication
import com.movieapp.data.local.MovieDao
import com.movieapp.data.local.MovieEntity
import com.movieapp.network.MovieApiService
import com.movieapp.network.NetworkClient
import com.movieapp.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Single source of truth for title metadata, storyline, TV season details, caching and bookmarks.
 * Follows Ponytail: cache-first to prevent rate-limit blocks and conserve bandwidth.
 */
class MovieDetailRepository(
    private val apiService: MovieApiService = NetworkClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val movieDao: MovieDao? = null,
    private val fallbackApiService: MovieApiService = NetworkClient.fallbackApiService
) {
    private val activeDao: MovieDao
        get() = movieDao ?: MovieApplication.instance.database.movieDao()

    private val gson = Gson()

    /**
     * Attempts to fetch TV show metadata across primary (HomieTV) and secondary (YSFlix) backends.
     */
    private suspend fun fetchTvShowNetworkData(slug: String): MovieDetailDTO? {
        // 1. Primary: HomieTV tv-shows
        try {
            val response = apiService.getTvShowDetail(slug)
            if (response.data != null) return response.data
        } catch (e: Exception) {
            android.util.Log.w("MovieDetailRepository", "Primary tv-show fetch failed for $slug: ${e.message}")
        }

        // 2. Secondary: YSFlix tv-shows (e.g. The Bear)
        try {
            val fallbackResponse = fallbackApiService.getTvShowDetail(slug)
            if (fallbackResponse.data != null) return fallbackResponse.data
        } catch (e: Exception) {
            android.util.Log.w("MovieDetailRepository", "Fallback tv-show fetch failed for $slug: ${e.message}")
        }

        // 3. Safety net: Check movie endpoints in case of classification mismatch
        try {
            val movieResponse = apiService.getMovieDetail(slug)
            if (movieResponse.data != null) return movieResponse.data
        } catch (_: Exception) {}

        try {
            val fallbackMovieResponse = fallbackApiService.getMovieDetail(slug)
            if (fallbackMovieResponse.data != null) return fallbackMovieResponse.data
        } catch (_: Exception) {}

        return null
    }

    /**
     * Attempts to fetch movie metadata across primary (HomieTV) and secondary (YSFlix) backends.
     */
    private suspend fun fetchMovieNetworkData(slug: String): MovieDetailDTO? {
        // 1. Primary: HomieTV movies
        try {
            val response = apiService.getMovieDetail(slug)
            if (response.data != null) return response.data
        } catch (e: Exception) {
            android.util.Log.w("MovieDetailRepository", "Primary movie fetch failed for $slug: ${e.message}")
        }

        // 2. Secondary: YSFlix movies
        try {
            val fallbackResponse = fallbackApiService.getMovieDetail(slug)
            if (fallbackResponse.data != null) return fallbackResponse.data
        } catch (e: Exception) {
            android.util.Log.w("MovieDetailRepository", "Fallback movie fetch failed for $slug: ${e.message}")
        }

        // 3. Safety net: Check tv-show endpoints in case of classification mismatch
        try {
            val tvResponse = apiService.getTvShowDetail(slug)
            if (tvResponse.data != null) return tvResponse.data
        } catch (_: Exception) {}

        try {
            val fallbackTvResponse = fallbackApiService.getTvShowDetail(slug)
            if (fallbackTvResponse.data != null) return fallbackTvResponse.data
        } catch (_: Exception) {}

        return null
    }

    /**
     * Retrieves full detail metadata for a movie by slug with Room caching.
     */
    fun getMovieDetail(slug: String): Flow<Resource<MovieDetailDTO>> = flow {
        emit(Resource.Loading())

        // 1. Check local Room cache first (prevent rate-limiting)
        var cachedEntity = activeDao.getMovieBySlug(slug)
        if (cachedEntity?.jsonDetail != null) {
            try {
                val cachedDto = gson.fromJson(cachedEntity.jsonDetail, MovieDetailDTO::class.java)
                if (cachedDto != null) {
                    emit(Resource.Success(cachedDto))
                }
            } catch (e: Exception) {
                // If parsing cache fails, continue to network
            }
        }

        // 2. Fetch fresh data from network with dual-backend fallback
        try {
            val data = fetchMovieNetworkData(slug)
            if (data != null) {
                val isActuallyTv = data.isTvShow
                // Save to Room cache without overwriting user bookmark status
                val updatedRows = activeDao.updateMetadata(
                    slug = slug,
                    title = data.displayTitle,
                    poster = data.poster,
                    rating = data.formattedRating,
                    releaseYear = data.displayYear,
                    isTvShow = isActuallyTv,
                    plot = data.plot,
                    jsonDetail = gson.toJson(data),
                    cachedAt = System.currentTimeMillis()
                )
                if (updatedRows == 0) {
                    val entity = MovieEntity(
                        slug = slug,
                        title = data.displayTitle,
                        poster = data.poster,
                        rating = data.formattedRating,
                        releaseYear = data.displayYear,
                        isTvShow = isActuallyTv,
                        plot = data.plot,
                        jsonDetail = gson.toJson(data),
                        isBookmarked = false,
                        bookmarkedAt = 0L,
                        cachedAt = System.currentTimeMillis()
                    )
                    activeDao.insertOrUpdate(entity)
                }
                emit(Resource.Success(data))
            } else if (cachedEntity == null) {
                emit(Resource.Error("Movie details aren't available."))
            }
        } catch (e: Exception) {
            android.util.Log.e("MovieDetailRepository", "Failed to fetch movie detail: ${e.javaClass.simpleName}: ${e.message}", e)
            if (cachedEntity == null) {
                emit(Resource.Error("Couldn't load movie details. Check your connection and try again."))
            }
        }
    }.flowOn(ioDispatcher)

    /**
     * Retrieves full detail metadata for a TV show by slug with Room caching.
     */
    fun getTvShowDetail(slug: String): Flow<Resource<MovieDetailDTO>> = flow {
        emit(Resource.Loading())

        // 1. Check local Room cache first
        var cachedEntity = activeDao.getMovieBySlug(slug)
        if (cachedEntity?.jsonDetail != null) {
            try {
                val cachedDto = gson.fromJson(cachedEntity.jsonDetail, MovieDetailDTO::class.java)
                if (cachedDto != null) {
                    emit(Resource.Success(cachedDto))
                }
            } catch (e: Exception) {
                // Continue to network
            }
        }

        // 2. Fetch fresh data from network with dual-backend fallback
        try {
            val data = fetchTvShowNetworkData(slug)
            if (data != null) {
                val isActuallyTv = data.isTvShow
                // Save to Room cache without overwriting user bookmark status
                val updatedRows = activeDao.updateMetadata(
                    slug = slug,
                    title = data.displayTitle,
                    poster = data.poster,
                    rating = data.formattedRating,
                    releaseYear = data.displayYear,
                    isTvShow = isActuallyTv,
                    plot = data.plot,
                    jsonDetail = gson.toJson(data),
                    cachedAt = System.currentTimeMillis()
                )
                if (updatedRows == 0) {
                    val entity = MovieEntity(
                        slug = slug,
                        title = data.displayTitle,
                        poster = data.poster,
                        rating = data.formattedRating,
                        releaseYear = data.displayYear,
                        isTvShow = isActuallyTv,
                        plot = data.plot,
                        jsonDetail = gson.toJson(data),
                        isBookmarked = false,
                        bookmarkedAt = 0L,
                        cachedAt = System.currentTimeMillis()
                    )
                    activeDao.insertOrUpdate(entity)
                }
                emit(Resource.Success(data))
            } else if (cachedEntity == null) {
                emit(Resource.Error("Show details aren't available."))
            }
        } catch (e: Exception) {
            android.util.Log.e("MovieDetailRepository", "Failed to fetch TV show detail: ${e.javaClass.simpleName}: ${e.message}", e)
            if (cachedEntity == null) {
                emit(Resource.Error("Couldn't load show details. Check your connection and try again."))
            }
        }
    }.flowOn(ioDispatcher)

    /**
     * Observes bookmark status for a specific title.
     */
    fun isBookmarked(slug: String): Flow<Boolean?> = activeDao.isBookmarked(slug)

    /**
     * Toggles bookmark state for a title.
     */
    suspend fun toggleBookmark(detail: MovieDetailDTO, isTvShow: Boolean): Boolean = withContext(ioDispatcher) {
        val slug = detail.slug ?: return@withContext false
        val existing = activeDao.getMovieBySlug(slug)
        val newBookmarkedState = !(existing?.isBookmarked ?: false)
        val timestamp = if (newBookmarkedState) System.currentTimeMillis() else 0L

        if (existing != null) {
            activeDao.updateBookmarkStatus(slug, newBookmarkedState, timestamp)
        } else {
            val entity = MovieEntity(
                slug = slug,
                title = detail.displayTitle,
                poster = detail.poster,
                rating = detail.formattedRating,
                releaseYear = detail.displayYear,
                isTvShow = isTvShow,
                plot = detail.plot,
                jsonDetail = gson.toJson(detail),
                isBookmarked = newBookmarkedState,
                bookmarkedAt = timestamp,
                cachedAt = System.currentTimeMillis()
            )
            activeDao.insertOrUpdate(entity)
        }
        newBookmarkedState
    }

    /**
     * Observes all bookmarked titles.
     */
    fun getBookmarkedMovies(): Flow<List<MovieEntity>> = activeDao.getBookmarkedMovies()
}

