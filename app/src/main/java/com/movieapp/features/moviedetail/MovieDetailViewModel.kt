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

    /**
     * Loads title details based on slug and media category.
     */
    fun loadDetail(slug: String, isTvShow: Boolean) {
        currentSlug = slug
        currentIsTv = isTvShow
        _uiState.update { it.copy(isTvShow = isTvShow, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            repository.isBookmarked(slug).collect { bookmarked ->
                _uiState.update { it.copy(isBookmarked = bookmarked ?: false) }
            }
        }

        viewModelScope.launch {
            val flow = if (isTvShow) {
                repository.getTvShowDetail(slug)
            } else {
                repository.getMovieDetail(slug)
            }

            flow.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is Resource.Success -> {
                        val data = resource.data
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
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
        }
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
