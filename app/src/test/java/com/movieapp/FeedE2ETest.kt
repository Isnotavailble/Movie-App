package com.movieapp

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.movieapp.features.search.SearchRepository
import com.movieapp.features.search.SearchResponseDTO
import com.movieapp.features.movielist.MediaCategory
import com.movieapp.features.movielist.MovieDTO
import com.movieapp.features.movielist.MovieListRepository
import com.movieapp.features.movielist.MovieListResponseDTO
import com.movieapp.features.movielist.MovieListScreen
import com.movieapp.features.movielist.MovieListViewModel
import com.movieapp.network.MovieApiService
import com.movieapp.theme.MovieAppTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-End Compose test suite for the Catalog Feed & Category Tabs flow.
 * Runs on host JVM without requiring an emulator via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeMovies = listOf(
        MovieDTO(rawId = 1L, title = "Inception", slug = "inception", releaseYear = "2010", rawRating = 8.8, mediaType = "movie"),
        MovieDTO(rawId = 2L, title = "Interstellar", slug = "interstellar", releaseYear = "2014", rawRating = 8.7, mediaType = "movie")
    )

    private val fakeTvShows = listOf(
        MovieDTO(rawId = 101L, title = "Breaking Bad", slug = "breaking-bad", releaseYear = "2008", rawRating = 9.5, mediaType = "tv"),
        MovieDTO(rawId = 102L, title = "Chernobyl", slug = "chernobyl", releaseYear = "2019", rawRating = 9.4, mediaType = "tv")
    )

    private val fakeApiService = object : MovieApiService {
        override suspend fun getMovies(page: Int): MovieListResponseDTO {
            return if (page == 1) {
                MovieListResponseDTO(items = fakeMovies, rawCurrentPage = 1, rawTotalPages = 1)
            } else {
                MovieListResponseDTO(items = emptyList(), rawCurrentPage = page, rawTotalPages = 1)
            }
        }
        override suspend fun getTvShows(page: Int): MovieListResponseDTO {
            return if (page == 1) {
                MovieListResponseDTO(items = fakeTvShows, rawCurrentPage = 1, rawTotalPages = 1)
            } else {
                MovieListResponseDTO(items = emptyList(), rawCurrentPage = page, rawTotalPages = 1)
            }
        }
        override suspend fun getMovieDetail(slug: String) = throw UnsupportedOperationException()
        override suspend fun getTvShowDetail(slug: String) = throw UnsupportedOperationException()
        override suspend fun searchTitles(keyword: String, page: Int): SearchResponseDTO {
            val all = (fakeMovies + fakeTvShows).filter { it.title?.contains(keyword, ignoreCase = true) == true }
            return SearchResponseDTO(items = all, rawCurrentPage = 1, rawTotalPages = 1)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private val repository = MovieListRepository(fakeApiService, testDispatcher)
    private val searchRepository = SearchRepository(fakeApiService, testDispatcher)

    private fun createViewModel() = MovieListViewModel(repository, searchRepository, searchDebounceMillis = 0L)

    @Test
    fun feedDisplaysMoviesInitiallyAndSwitchesToTvShows() {
        val viewModel = createViewModel()
        var clickedSlug: String? = null
        var clickedIsTv: Boolean? = null

        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = viewModel,
                    onTitleClick = { slug, isTv ->
                        clickedSlug = slug
                        clickedIsTv = isTv
                    }
                )
            }
        }

        // Verify initial Movies list and in-page search placeholder
        composeTestRule.onNodeWithText("Search movies...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interstellar").assertIsDisplayed()

        // Switch to TV Shows category
        viewModel.selectCategory(MediaCategory.TV_SHOWS)
        composeTestRule.waitForIdle()

        // Verify TV shows are displayed with updated search placeholder
        composeTestRule.onNodeWithText("Search TV shows...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Breaking Bad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chernobyl").assertIsDisplayed()

        // Click on a TV show card
        composeTestRule.onNodeWithText("Breaking Bad").performClick()
        assertEquals("breaking-bad", clickedSlug)
        assertTrue(clickedIsTv == true)
    }

    @Test
    fun inPageSearchFiltersTitlesCorrectly() {
        val viewModel = createViewModel()

        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = viewModel,
                    onTitleClick = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interstellar").assertIsDisplayed()

        // Type query in the in-page search bar
        viewModel.onSearchQueryChange("Inception")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.moviesSearchResults.isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // Verify filtered results: Inception is displayed (in search bar and movie card), Interstellar is not
        composeTestRule.onAllNodesWithText("Inception").assertCountEquals(2)
        composeTestRule.onNodeWithText("Interstellar").assertDoesNotExist()

        // Clear search query
        viewModel.clearSearchQuery()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interstellar").assertIsDisplayed()
    }

    @Test
    fun pullToRefreshTriggersReloadSuccessfully() {
        val viewModel = MovieListViewModel(repository)

        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = viewModel,
                    onTitleClick = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()

        // Trigger refresh directly on ViewModel
        viewModel.refresh()
        composeTestRule.waitForIdle()

        // Verify content remains loaded cleanly
        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
    }
}
