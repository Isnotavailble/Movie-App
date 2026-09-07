package com.movieapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.movieapp.features.movielist.MovieDTO
import com.movieapp.features.search.SearchRepository
import com.movieapp.features.search.SearchResponseDTO
import com.movieapp.features.search.SearchScreen
import com.movieapp.features.search.SearchViewModel
import com.movieapp.network.MovieApiService
import com.movieapp.theme.MovieAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-End Compose test suite for the Search & Discovery flow.
 * Runs on host JVM without requiring an emulator via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeSearchResults = listOf(
        MovieDTO(rawId = 10L, title = "Batman Begins", slug = "batman-begins", releaseYear = "2005", rawRating = 8.2, mediaType = "movie"),
        MovieDTO(rawId = 11L, title = "The Batman", slug = "the-batman", releaseYear = "2022", rawRating = 7.9, mediaType = "movie")
    )

    private val fakeApiService = object : MovieApiService {
        override suspend fun getMovies(page: Int) = throw UnsupportedOperationException()
        override suspend fun getTvShows(page: Int) = throw UnsupportedOperationException()
        override suspend fun getMovieDetail(slug: String) = throw UnsupportedOperationException()
        override suspend fun getTvShowDetail(slug: String) = throw UnsupportedOperationException()
        override suspend fun searchTitles(keyword: String, page: Int): SearchResponseDTO {
            return if (keyword.contains("batman", ignoreCase = true)) {
                SearchResponseDTO(items = fakeSearchResults, rawCurrentPage = 1, rawTotalPages = 1)
            } else {
                SearchResponseDTO(items = emptyList(), rawCurrentPage = 1, rawTotalPages = 1)
            }
        }
    }

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private val repository = SearchRepository(fakeApiService, testDispatcher)

    @Test
    fun searchShowsInitialPromptAndFindsTitles() {
        val viewModel = SearchViewModel(repository, debounceTimeoutMillis = 0L)
        var clickedSlug: String? = null

        composeTestRule.setContent {
            MovieAppTheme {
                SearchScreen(
                    viewModel = viewModel,
                    onTitleClick = { slug, _ -> clickedSlug = slug }
                )
            }
        }

        // Verify initial prompt
        composeTestRule.onNodeWithText("Search by Title").assertIsDisplayed()

        // Type search query
        composeTestRule.onNodeWithContentDescription("Search text input field").performTextInput("Batman")

        // Wait for debounce and coroutines
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()

        // Verify results displayed
        composeTestRule.onNodeWithText("Batman Begins").assertIsDisplayed()
        composeTestRule.onNodeWithText("The Batman").assertIsDisplayed()

        // Click search result item
        composeTestRule.onNodeWithText("The Batman").performClick()
        assertEquals("the-batman", clickedSlug)

        // Tap Clear action
        composeTestRule.onNodeWithContentDescription("Clear search text").performClick()
        composeTestRule.waitForIdle()

        // Verify restored to prompt
        composeTestRule.onNodeWithText("Search by Title").assertIsDisplayed()
    }

    @Test
    fun searchShowsHelpfulEmptyStateWhenNoMatchesFound() {
        val viewModel = SearchViewModel(repository, debounceTimeoutMillis = 0L)

        composeTestRule.setContent {
            MovieAppTheme {
                SearchScreen(
                    viewModel = viewModel,
                    onTitleClick = { _, _ -> }
                )
            }
        }

        // Type query that returns empty
        composeTestRule.onNodeWithContentDescription("Search text input field").performTextInput("NonExistentTitle")

        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()

        // Verify polite empty state copy
        composeTestRule.onNodeWithText("No Matches Found").assertIsDisplayed()
    }
}
