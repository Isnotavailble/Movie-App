package com.movieapp.features.movielist

import com.movieapp.network.MovieApiService
import com.movieapp.network.NetworkClient
import com.movieapp.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Single source of truth for retrieving movie and TV show catalog feeds.
 */
class MovieListRepository(
    private val apiService: MovieApiService = NetworkClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Retrieves a page of movies from the remote service.
     */
    fun getMovies(page: Int): Flow<Resource<MovieListResponseDTO>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getMovies(page)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            android.util.Log.e("MovieListRepository", "Failed to fetch movies: ${e.javaClass.simpleName}: ${e.message}", e)
            emit(Resource.Error("Couldn't load movies. Check your connection and try again."))
        }
    }.flowOn(ioDispatcher)

    /**
     * Retrieves a page of television shows from the remote service.
     */
    fun getTvShows(page: Int): Flow<Resource<MovieListResponseDTO>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getTvShows(page)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            android.util.Log.e("MovieListRepository", "Failed to fetch TV shows: ${e.javaClass.simpleName}: ${e.message}", e)
            emit(Resource.Error("Couldn't load TV shows. Check your connection and try again."))
        }
    }.flowOn(ioDispatcher)
}
