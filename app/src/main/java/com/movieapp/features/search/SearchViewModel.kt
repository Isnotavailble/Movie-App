package com.movieapp.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.features.movielist.MovieDTO
import com.movieapp.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the search screen.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<MovieDTO> = emptyList(),
    val isLoading: Boolean = false,
    val isInitial: Boolean = true,
    val errorMessage: String? = null
) {
    val isEmptyResult: Boolean
        get() = !isInitial && !isLoading && errorMessage == null && results.isEmpty() && query.isNotBlank()
}

/**
 * State holder for searching titles with 400ms debounce (US-04, US-05).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: SearchRepository = SearchRepository(),
    debounceTimeoutMillis: Long = 400L
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(debounceTimeoutMillis)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    val cleanQuery = query.trim()
                    if (cleanQuery.isBlank()) {
                        _uiState.update { it.copy(query = "", results = emptyList(), isInitial = true, isLoading = false, errorMessage = null) }
                        flowOf(null)
                    } else {
                        repository.searchTitles(cleanQuery, page = 1)
                    }
                }
                .collect { resource ->
                    if (resource == null) return@collect
                    when (resource) {
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true, isInitial = false, errorMessage = null) }
                        }
                        is Resource.Success -> {
                            val rawItems = resource.data?.safeItems ?: emptyList()
                            val currentQuery = _searchQuery.value.trim()
                            val rankedItems = SearchRanker.rank(rawItems, currentQuery)
                            _uiState.update {
                                it.copy(
                                    results = rankedItems,
                                    isLoading = false,
                                    isInitial = false,
                                    errorMessage = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isInitial = false,
                                    errorMessage = resource.message
                                )
                            }
                        }
                    }
                }
        }
    }

    /**
     * Dispatches query changes from the search input.
     */
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _searchQuery.value = newQuery
    }

    /**
     * Clears current query and returns to empty/prompt state.
     */
    fun clearQuery() {
        onQueryChange("")
    }

    /**
     * Retries search for current query.
     */
    fun retry() {
        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.isNotBlank()) {
            _searchQuery.value = ""
            _searchQuery.value = currentQuery
        }
    }
}
