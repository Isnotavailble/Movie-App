package com.movieapp.features.moviedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the detail screen.
 */
data class MovieDetailUiState(
    val detail: MovieDetailDTO? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isTvShow: Boolean = false,
    val selectedSeasonNumber: Int = 1,
    val isBookmarked: Boolean = false
) {
    val activeSeason: SeasonDTO?
        get() = detail?.safeSeasons?.find { it.seasonNumber == selectedSeasonNumber }
            ?: detail?.safeSeasons?.firstOrNull()
}

/**
 * State holder managing title details, storyline, and TV season selection.
 * Note: The Download Feature is excluded.
 */
class MovieDetailViewModel(
    private val repository: MovieDetailRepository = MovieDetailRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    private var currentSlug: String = ""
    private var currentIsTv: Boolean = false

    private val detailCache = mutableMapOf<String, MovieDetailDTO>()

    private var detailJob: kotlinx.coroutines.Job? = null
    private var bookmarkJob: kotlinx.coroutines.Job? = null

    /**
     * Loads title details based on slug and media category.
     * Cancels pending fetch operations and immediately hydrates from memory cache if available,
     * completely eliminating skeleton loading for already fetched titles.
     */
    fun loadDetail(slug: String, isTvShow: Boolean, force: Boolean = false) {
        val isDifferentTitle = currentSlug != slug || currentIsTv != isTvShow

        if (!isDifferentTitle && !force) {
            if (detailJob?.isActive == true || (_uiState.value.detail != null && _uiState.value.errorMessage == null)) {
                return
            }
        }

        currentSlug = slug
        currentIsTv = isTvShow

        detailJob?.cancel()
        bookmarkJob?.cancel()

        val cachedDetail = detailCache[slug]

        _uiState.update { current ->
            if (cachedDetail != null) {
                // Instantly hydrate already fetched title - no skeleton flicker
                current.copy(
                    detail = cachedDetail,
                    isTvShow = isTvShow,
                    isLoading = false,
                    errorMessage = null,
                    isBookmarked = false,
                    selectedSeasonNumber = cachedDetail.safeSeasons.firstOrNull()?.seasonNumber ?: 1
                )
            } else if (isDifferentTitle) {
                // New title never fetched before: show skeleton while loading
                current.copy(
                    detail = null,
                    isTvShow = isTvShow,
                    isLoading = true,
                    errorMessage = null,
                    isBookmarked = false,
                    selectedSeasonNumber = 1
                )
            } else {
                current.copy(
                    isTvShow = isTvShow,
                    isLoading = true,
                    errorMessage = null
                )
            }
        }

        bookmarkJob = viewModelScope.launch {
            repository.isBookmarked(slug).collect { bookmarked ->
                _uiState.update { it.copy(isBookmarked = bookmarked ?: false) }
            }
        }

        detailJob = viewModelScope.launch {
            val flow = if (isTvShow) {
                repository.getTvShowDetail(slug)
            } else {
                repository.getMovieDetail(slug)
            }

            flow.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Only show loading indicator if no cached detail is already visible
                        if (_uiState.value.detail == null) {
                            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                        }
                    }
                    is Resource.Success -> {
                        val data = resource.data
                        if (data != null) {
                            detailCache[slug] = data
                        }
                        val firstSeason = data?.safeSeasons?.firstOrNull()?.seasonNumber ?: 1
                        _uiState.update {
                            it.copy(
                                detail = data,
                                isLoading = false,
                                errorMessage = null,
                                selectedSeasonNumber = firstSeason
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = if (it.detail != null) null else resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears current detail state and cancels active jobs.
     */
    fun clearDetail() {
        detailJob?.cancel()
        bookmarkJob?.cancel()
        currentSlug = ""
        currentIsTv = false
        _uiState.update { MovieDetailUiState() }
    }

    /**
     * Toggles bookmark state for the current title.
     */
    fun toggleBookmark() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val newState = repository.toggleBookmark(detail, _uiState.value.isTvShow)
            _uiState.update { it.copy(isBookmarked = newState) }
        }
    }

    /**
     * Updates the selected season for TV shows.
     */
    fun selectSeason(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeasonNumber = seasonNumber) }
    }

    /**
     * Retries loading details.
     */
    fun retry() {
        if (currentSlug.isNotBlank()) {
            loadDetail(currentSlug, currentIsTv)
        }
    }
}
