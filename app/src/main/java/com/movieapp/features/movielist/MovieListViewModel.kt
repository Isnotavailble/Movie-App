package com.movieapp.features.movielist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.features.search.SearchRepository
import com.movieapp.util.Resource
import kotlinx.coroutines.Dispatchers
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
 * Supported movie & TV show sorting orders.
 */
enum class MovieSortOrder {
    DEFAULT,
    TOP_RATED,
    NEWEST,
    TITLE_AZ
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
    val isSearching: Boolean = false,
    val selectedGenre: String? = null,
    val minRating: Double = 0.0,
    val sortOrder: MovieSortOrder = MovieSortOrder.DEFAULT
) {
    val currentSearchQuery: String
        get() = if (activeCategory == MediaCategory.MOVIES) moviesSearchQuery else tvShowsSearchQuery

    val isSearchActive: Boolean
        get() = currentSearchQuery.isNotBlank()

    val isFilterActive: Boolean
        get() = selectedGenre != null || minRating > 0.0 || sortOrder != MovieSortOrder.DEFAULT

    val currentRawList: List<MovieDTO>
        get() = getRawListFor(activeCategory)

    fun getRawListFor(category: MediaCategory): List<MovieDTO> {
        val searchQ = if (category == MediaCategory.MOVIES) moviesSearchQuery else tvShowsSearchQuery
        return if (searchQ.isNotBlank()) {
            if (category == MediaCategory.MOVIES) moviesSearchResults else tvShowsSearchResults
        } else {
            if (category == MediaCategory.MOVIES) movies else tvShows
        }
    }

    val availableGenres: List<String>
        get() = getAvailableGenresFor(activeCategory)

    fun getAvailableGenresFor(category: MediaCategory): List<String> {
        val fromData = getRawListFor(category).flatMap { it.categoryNames }.distinct().filter { it.isNotBlank() }
        val fallbackDefaults = listOf("Action", "Adventure", "Animation", "Comedy", "Crime", "Drama", "Fantasy", "Horror", "Romance", "Sci-Fi", "Thriller")
        return (fromData + fallbackDefaults).distinct().sorted()
    }

    val currentDisplayList: List<MovieDTO>
        get() = getDisplayListFor(activeCategory)

    fun getDisplayListFor(category: MediaCategory): List<MovieDTO> {
        return getRawListFor(category).asSequence()
            .filter { item ->
                selectedGenre == null || item.categoryNames.any { it.equals(selectedGenre, ignoreCase = true) }
            }
            .filter { item ->
                minRating <= 0.0 || (item.rating ?: 0.0) >= minRating
            }
            .let { seq ->
                when (sortOrder) {
                    MovieSortOrder.TOP_RATED -> seq.sortedByDescending { it.rating ?: 0.0 }
                    MovieSortOrder.NEWEST -> seq.sortedByDescending { it.displayYear }
                    MovieSortOrder.TITLE_AZ -> seq.sortedBy { it.displayTitle.lowercase() }
                    MovieSortOrder.DEFAULT -> seq
                }
            }
            .toList()
    }

    val currentHasMore: Boolean
        get() = if (isSearchActive) {
            false
        } else {
            if (activeCategory == MediaCategory.MOVIES) moviesHasMore else tvShowsHasMore
        }

    val isSearchEmpty: Boolean
        get() = isSearchActive && !isSearching && currentDisplayList.isEmpty()

    val isFilterEmpty: Boolean
        get() = isFilterEmptyFor(activeCategory)

    fun isFilterEmptyFor(category: MediaCategory): Boolean {
        val raw = getRawListFor(category)
        return isFilterActive && !isInitialLoading && getDisplayListFor(category).isEmpty() && raw.isNotEmpty()
    }
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
    private var fetchMoviesJob: kotlinx.coroutines.Job? = null
    private var fetchTvShowsJob: kotlinx.coroutines.Job? = null

    init {
        loadInitialFeeds()
        setupSearch()
    }

    private fun loadInitialFeeds() {
        fetchMoviesPage(1)
        fetchTvShowsPage(1)
    }

    private fun setupSearch() {
        viewModelScope.launch(Dispatchers.Default) {
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
                            val currentQuery = _searchQuery.value.trim()
                            val ranked = com.movieapp.features.search.SearchRanker.rank(filtered, currentQuery)
                            _uiState.update { current ->
                                if (category == MediaCategory.MOVIES) {
                                    current.copy(moviesSearchResults = ranked, isSearching = false)
                                } else {
                                    current.copy(tvShowsSearchResults = ranked, isSearching = false)
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
     * Filters media items by selected genre. Passing null or "All" shows all genres.
     */
    fun selectGenre(genre: String?) {
        val clean = if (genre == null || genre.equals("All", ignoreCase = true) || genre.isBlank()) null else genre
        _uiState.update { it.copy(selectedGenre = clean) }
        checkAutoPagination()
    }

    /**
     * Filters media items by minimum rating (e.g. 6.0, 7.0, 8.0). 0.0 shows all.
     */
    fun selectMinRating(rating: Double) {
        val clean = if (rating <= 0.0) 0.0 else rating
        _uiState.update { it.copy(minRating = clean) }
        checkAutoPagination()
    }

    /**
     * Sets the sort order for movies and TV shows.
     */
    fun selectSortOrder(order: MovieSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    /**
     * Resets all genre, rating, and sort filters to defaults.
     */
    fun resetFilters() {
        _uiState.update { it.copy(selectedGenre = null, minRating = 0.0, sortOrder = MovieSortOrder.DEFAULT) }
    }

    private fun checkAutoPagination() {
        val state = _uiState.value
        if (state.isFilterActive && state.currentDisplayList.size < 8 && state.currentHasMore && !state.isPaginating && !state.isInitialLoading) {
            loadNextPage(state.activeCategory)
        }
    }

    /**
     * Pull-to-refresh: resets page to 1 and reloads current active category.
     */
    fun refresh(targetCategory: MediaCategory? = null) {
        val state = _uiState.value
        val cat = targetCategory ?: state.activeCategory
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        if (cat == MediaCategory.MOVIES) {
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
    fun loadNextPage(targetCategory: MediaCategory? = null) {
        val state = _uiState.value
        val cat = targetCategory ?: state.activeCategory
        val hasMore = if (cat == MediaCategory.MOVIES) state.moviesHasMore else state.tvShowsHasMore
        val isSearch = if (cat == MediaCategory.MOVIES) state.moviesSearchQuery.isNotBlank() else state.tvShowsSearchQuery.isNotBlank()
        if (state.isInitialLoading || state.isPaginating || !hasMore || isSearch) return

        if (cat == MediaCategory.MOVIES) {
            fetchMoviesPage(state.moviesPage + 1)
        } else {
            fetchTvShowsPage(state.tvShowsPage + 1)
        }
    }

    /**
     * Retries loading after an error.
     */
    fun retry(targetCategory: MediaCategory? = null) {
        val state = _uiState.value
        val cat = targetCategory ?: state.activeCategory
        if (cat == MediaCategory.MOVIES) {
            fetchMoviesPage(if (state.movies.isEmpty()) 1 else state.moviesPage + 1)
        } else {
            fetchTvShowsPage(if (state.tvShows.isEmpty()) 1 else state.tvShowsPage + 1)
        }
    }

    private fun fetchMoviesPage(page: Int, isRefresh: Boolean = false) {
        if (isRefresh) {
            fetchMoviesJob?.cancel()
        }
        fetchMoviesJob = viewModelScope.launch(Dispatchers.Default) {
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
                        checkAutoPagination()
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
        if (isRefresh) {
            fetchTvShowsJob?.cancel()
        }
        fetchTvShowsJob = viewModelScope.launch(Dispatchers.Default) {
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
                        checkAutoPagination()
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
