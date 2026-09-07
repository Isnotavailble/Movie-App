package com.movieapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.movieapp.features.moviedetail.EpisodeDTO
import com.movieapp.features.moviedetail.MovieDetailDTO
import com.movieapp.features.moviedetail.MovieDetailRepository
import com.movieapp.features.moviedetail.MovieDetailResponseDTO
import com.movieapp.features.moviedetail.MovieDetailScreen
import com.movieapp.features.moviedetail.MovieDetailViewModel
import com.movieapp.features.moviedetail.SeasonDTO
import com.movieapp.network.MovieApiService
import com.movieapp.theme.MovieAppTheme
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-End Compose test suite for the Title Details & TV Seasons flow.
 * Runs on host JVM without requiring an emulator via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetailE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeTvDetail = MovieDetailDTO(
        id = 101L,
        title = "Stranger Things",
        slug = "stranger-things",
        releaseYear = "2016",
        runtime = "50 min",
        rawRating = 8.7,
        plot = "A young boy vanishes, a secret government lab unleashes something terrifying, and a strange girl with powers appears.",
        rawGenres = listOf("Drama", "Fantasy", "Horror"),
        mediaType = "tv",
        seasons = listOf(
            SeasonDTO(
                rawSeasonNumber = 1,
                name = "Season 1",
                episodes = listOf(
                    EpisodeDTO(rawEpisodeNumber = 1, title = "Chapter One: The Vanishing of Will Byers", runtime = "49 min"),
                    EpisodeDTO(rawEpisodeNumber = 2, title = "Chapter Two: The Weirdo on Maple Street", runtime = "56 min")
                )
            ),
            SeasonDTO(
                rawSeasonNumber = 2,
                name = "Season 2",
                episodes = listOf(
                    EpisodeDTO(rawEpisodeNumber = 1, title = "Chapter One: MADMAX", runtime = "48 min")
                )
            )
        )
    )

    private val fakeApiService = object : MovieApiService {
        override suspend fun getMovies(page: Int) = throw UnsupportedOperationException()
        override suspend fun getTvShows(page: Int) = throw UnsupportedOperationException()
        override suspend fun getMovieDetail(slug: String) = throw UnsupportedOperationException()
        override suspend fun getTvShowDetail(slug: String) = MovieDetailResponseDTO(success = true, message = "TV Show Details", data = fakeTvDetail)
        override suspend fun searchTitles(keyword: String, page: Int) = throw UnsupportedOperationException()
    }

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private val repository = MovieDetailRepository(fakeApiService, testDispatcher)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @org.junit.Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val testImageLoader = coil.ImageLoader.Builder(context)
            .components {
                add(object : coil.intercept.Interceptor {
                    override suspend fun intercept(chain: coil.intercept.Interceptor.Chain): coil.request.ImageResult {
                        return coil.request.SuccessResult(
                            drawable = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
                            request = chain.request,
                            dataSource = coil.decode.DataSource.MEMORY
                        )
                    }
                })
            }
            .build()
        coil.Coil.setImageLoader(testImageLoader)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @org.junit.After
    fun teardown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun detailDisplaysTitleMetadataAndSwitchesSeasons() {
        val viewModel = MovieDetailViewModel(repository)
        var backClicked = false

        viewModel.loadDetail("stranger-things", isTvShow = true)

        composeTestRule.setContent {
            MovieAppTheme {
                MovieDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.detail != null
        }
        composeTestRule.waitForIdle()

        // Verify Title and Metadata
        composeTestRule.onNodeWithText("Stranger Things").assertIsDisplayed()
        composeTestRule.onNodeWithText("2016").assertIsDisplayed()
        composeTestRule.onNodeWithText("Story Summary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("TV Show").assertIsDisplayed()

        // Verify Season 1 Episodes are displayed initially
        composeTestRule.onNodeWithText("Choose Season and Episode").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Chapter One: The Vanishing of Will Byers", substring = true).performScrollTo().assertIsDisplayed()

        // Switch to Season 2
        composeTestRule.onNodeWithText("Season 2", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Verify Season 2 Episode is displayed
        composeTestRule.onNodeWithText("Chapter One: MADMAX", substring = true).performScrollTo().assertIsDisplayed()

        // Click Back to List
        composeTestRule.onNodeWithText("Back to List").assertIsDisplayed().performClick()
        assertTrue(backClicked)
    }
}
