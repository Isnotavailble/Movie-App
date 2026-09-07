package com.movieapp.features.movielist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.features.search.SearchRepository
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
 * Supported media categories.
 */
enum class MediaCategory {
    MOVIES,
    TV_SHOWS
}

/**
 * Immutable UI state for the media feed.
 */
data class MovieListUiState(
    val activeCategory: MediaCategory = MediaCategory.MOVIES,
    val movies: List<MovieDTO> = emptyList(),
    val tvShows: List<MovieDTO> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val moviesPage: Int = 1,
    val tvShowsPage: Int = 1,
    val moviesHasMore: Boolean = true,
    val tvShowsHasMore: Boolean = true,
    val moviesSearchQuery: String = "",
    val tvShowsSearchQuery: String = "",
    val moviesSearchResults: List<MovieDTO> = emptyList(),
    val tvShowsSearchResults: List<MovieDTO> = emptyList(),
    val isSearching: Boolean = false
) {
    val currentSearchQuery: String
        get() = if (activeCategory == MediaCategory.MOVIES) moviesSearchQuery else tvShowsSearchQuery

    val isSearchActive: Boolean
        get() = currentSearchQuery.isNotBlank()

    val currentDisplayList: List<MovieDTO>
        get() = if (isSearchActive) {
            if (activeCategory == MediaCategory.MOVIES) moviesSearchResults else tvShowsSearchResults
        } else {
            if (activeCategory == MediaCategory.MOVIES) movies else tvShows
        }

    val currentHasMore: Boolean
        get() = if (isSearchActive) {
            false
        } else {
            if (activeCategory == MediaCategory.MOVIES) moviesHasMore else tvShowsHasMore
        }

    val isSearchEmpty: Boolean
        get() = isSearchActive && !isSearching && currentDisplayList.isEmpty()
}

/**
 * State holder managing catalog feeds, infinite scrolling, category selection, and in-page search.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MovieListViewModel(
    private val repository: MovieListRepository = MovieListRepository(),
    private val searchRepository: SearchRepository = SearchRepository(),
    private val searchDebounceMillis: Long = 300L
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieListUiState())
    val uiState: StateFlow<MovieListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadInitialFeeds()
        setupSearch()
    }

    private fun loadInitialFeeds() {
        fetchMoviesPage(1)
        fetchTvShowsPage(1)
    }

    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(searchDebounceMillis)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    val cleanQuery = query.trim()
                    if (cleanQuery.isBlank()) {
                        _uiState.update { current ->
                            if (current.activeCategory == MediaCategory.MOVIES) {
                                current.copy(moviesSearchResults = emptyList(), isSearching = false)
                            } else {
                                current.copy(tvShowsSearchResults = emptyList(), isSearching = false)
                            }
                        }
                        flowOf(null)
                    } else {
                        searchRepository.searchTitles(cleanQuery, page = 1)
                    }
                }
                .collect { resource ->
                    if (resource == null) return@collect
                    when (resource) {
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isSearching = true) }
                        }
                        is Resource.Success -> {
                            val allResults = resource.data?.safeItems ?: emptyList()
                            val category = _uiState.value.activeCategory
                            val filtered = allResults.filter { item ->
                                if (category == MediaCategory.MOVIES) !item.isTvShow else item.isTvShow
                            }
                            _uiState.update { current ->
                                if (category == MediaCategory.MOVIES) {
                                    current.copy(moviesSearchResults = filtered, isSearching = false)
                                } else {
                                    current.copy(tvShowsSearchResults = filtered, isSearching = false)
                                }
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isSearching = false) }
                        }
                    }
                }
        }
    }

    /**
     * Updates in-page search query for the active category.
     */
    fun onSearchQueryChange(newQuery: String) {
        val category = _uiState.value.activeCategory
        _uiState.update { current ->
            if (category == MediaCategory.MOVIES) {
                current.copy(moviesSearchQuery = newQuery)
            } else {
                current.copy(tvShowsSearchQuery = newQuery)
            }
        }
        _searchQuery.value = newQuery
    }

    /**
     * Clears in-page search query for the active category.
     */
    fun clearSearchQuery() {
        onSearchQueryChange("")
    }

    /**
     * Pull-to-refresh: resets page to 1 and reloads current active category.
     */
    fun refresh() {
        val state = _uiState.value
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        if (state.activeCategory == MediaCategory.MOVIES) {
            fetchMoviesPage(1, isRefresh = true)
        } else {
            fetchTvShowsPage(1, isRefresh = true)
        }
    }

    /**
     * Toggles between Movies and TV Shows.
     * Retains already-loaded data in-memory without duplicate network requests.
     */
    fun selectCategory(category: MediaCategory) {
        if (_uiState.value.activeCategory != category) {
            _uiState.update { it.copy(activeCategory = category, errorMessage = null) }
            val currentCategoryQuery = if (category == MediaCategory.MOVIES) {
                _uiState.value.moviesSearchQuery
            } else {
                _uiState.value.tvShowsSearchQuery
            }
            _searchQuery.value = currentCategoryQuery
        }
    }

    /**
     * Loads the next page of items for continuous infinite scrolling.
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isPaginating || !state.currentHasMore || state.isSearchActive) return

        if (state.activeCategory == MediaCategory.MOVIES) {
            fetchMoviesPage(state.moviesPage + 1)
        } else {
            fetchTvShowsPage(state.tvShowsPage + 1)
        }
    }

    /**
     * Retries loading after an error.
     */
    fun retry() {
        val state = _uiState.value
        if (state.activeCategory == MediaCategory.MOVIES) {
            fetchMoviesPage(if (state.movies.isEmpty()) 1 else state.moviesPage + 1)
        } else {
            fetchTvShowsPage(if (state.tvShows.isEmpty()) 1 else state.tvShowsPage + 1)
        }
    }

    private fun fetchMoviesPage(page: Int, isRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getMovies(page).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update {
                            if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                            else if (page == 1 && it.movies.isEmpty()) it.copy(isInitialLoading = true, errorMessage = null)
                            else it.copy(isPaginating = true, errorMessage = null)
                        }
                    }
                    is Resource.Success -> {
                        val response = resource.data
                        val newItems = response?.safeItems ?: emptyList()
                        val hasMore = if (newItems.isEmpty()) false else (response?.canLoadMore ?: true)
                        _uiState.update { current ->
                            val combined = if (page == 1 || isRefresh) {
                                newItems
                            } else {
                                (current.movies + newItems).distinctBy { "${it.id}_${it.slug}" }
                            }
                            current.copy(
                                movies = combined,
                                moviesPage = page,
                                moviesHasMore = hasMore,
                                isInitialLoading = false,
                                isPaginating = false,
                                isRefreshing = false,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isInitialLoading = false,
                                isPaginating = false,
                                isRefreshing = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fetchTvShowsPage(page: Int, isRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getTvShows(page).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update {
                            if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                            else if (page == 1 && it.tvShows.isEmpty()) it.copy(isInitialLoading = true, errorMessage = null)
                            else it.copy(isPaginating = true, errorMessage = null)
                        }
                    }
                    is Resource.Success -> {
                        val response = resource.data
                        val newItems = response?.safeItems ?: emptyList()
                        val hasMore = if (newItems.isEmpty()) false else (response?.canLoadMore ?: true)
                        _uiState.update { current ->
                            val combined = if (page == 1 || isRefresh) {
                                newItems
                            } else {
                                (current.tvShows + newItems).distinctBy { "${it.id}_${it.slug}" }
                            }
                            current.copy(
                                tvShows = combined,
                                tvShowsPage = page,
                                tvShowsHasMore = hasMore,
                                isInitialLoading = false,
                                isPaginating = false,
                                isRefreshing = false,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isInitialLoading = false,
                                isPaginating = false,
                                isRefreshing = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
        }
    }
}
