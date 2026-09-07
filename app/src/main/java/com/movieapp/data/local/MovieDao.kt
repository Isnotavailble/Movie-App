package com.movieapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for movie details caching and user bookmarks.
 */
@Dao
interface MovieDao {

    @Query("SELECT * FROM movies WHERE isBookmarked = 1 ORDER BY bookmarkedAt DESC")
    fun getBookmarkedMovies(): Flow<List<MovieEntity>>

    @Query("SELECT isBookmarked FROM movies WHERE slug = :slug")
    fun isBookmarked(slug: String): Flow<Boolean?>

    @Query("SELECT * FROM movies WHERE slug = :slug LIMIT 1")
    suspend fun getMovieBySlug(slug: String): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(movie: MovieEntity)

    @Query("UPDATE movies SET title = :title, poster = :poster, rating = :rating, releaseYear = :releaseYear, isTvShow = :isTvShow, plot = :plot, jsonDetail = :jsonDetail, cachedAt = :cachedAt WHERE slug = :slug")
    suspend fun updateMetadata(
        slug: String,
        title: String,
        poster: String?,
        rating: String?,
        releaseYear: String?,
        isTvShow: Boolean,
        plot: String?,
        jsonDetail: String?,
        cachedAt: Long
    ): Int

    @Query("UPDATE movies SET isBookmarked = :isBookmarked, bookmarkedAt = :bookmarkedAt WHERE slug = :slug")
    suspend fun updateBookmarkStatus(slug: String, isBookmarked: Boolean, bookmarkedAt: Long)
}
