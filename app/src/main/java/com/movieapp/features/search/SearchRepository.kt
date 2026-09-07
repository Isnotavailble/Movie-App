package com.movieapp.features.search

import com.movieapp.network.MovieApiService
import com.movieapp.network.NetworkClient
import com.movieapp.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Single source of truth for title search queries.
 */
class SearchRepository(
    private val apiService: MovieApiService = NetworkClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Executes keyword search across movies and television series.
     */
    fun searchTitles(keyword: String, page: Int = 1): Flow<Resource<SearchResponseDTO>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.searchTitles(keyword, page)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            android.util.Log.e("SearchRepository", "Failed to search titles: ${e.javaClass.simpleName}: ${e.message}", e)
            emit(Resource.Error("Couldn't complete search. Check your connection and try again."))
        }
    }.flowOn(ioDispatcher)
}
