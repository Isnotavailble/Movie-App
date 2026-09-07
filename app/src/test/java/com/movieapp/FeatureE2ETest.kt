package com.movieapp

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.movieapp.data.local.AppDatabase
import com.movieapp.data.local.MovieDao
import com.movieapp.features.bookmarks.BookmarkScreen
import com.movieapp.features.downloadlinks.DownloadLinkDTO
import com.movieapp.features.downloadlinks.DownloadLinksBottomSheet
import com.movieapp.features.moviedetail.MovieDetailDTO
import com.movieapp.features.moviedetail.MovieDetailRepository
import com.movieapp.features.moviedetail.MovieDetailResponseDTO
import com.movieapp.features.moviedetail.MovieDetailScreen
import com.movieapp.features.moviedetail.MovieDetailViewModel
import com.movieapp.network.MovieApiService
import com.movieapp.theme.MovieAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-End Compose tests for newly added features:
 * 1. Bookmarks flow: Adding/Removing bookmark from Detail and observing on BookmarkScreen.
 * 2. DownloadLinksBottomSheet flow: Resolution filtering chips and direct download buttons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1200dp")
class FeatureE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var movieDao: MovieDao

    private val sampleMovieDetail = MovieDetailDTO(
        id = 202L,
        title = "Inception",
        slug = "inception-2010",
        releaseYear = "2010",
        runtime = "148 min",
        rawRating = 8.8,
        plot = "A thief who steals corporate secrets through dream-sharing technology.",
        rawGenres = listOf("Action", "Sci-Fi"),
        mediaType = "movie",
        seasons = emptyList()
    )

    private val fakeApiService = object : MovieApiService {
        override suspend fun getMovies(page: Int) = throw UnsupportedOperationException()
        override suspend fun getTvShows(page: Int) = throw UnsupportedOperationException()
        override suspend fun getMovieDetail(slug: String) = MovieDetailResponseDTO(success = true, message = "Movie Details", data = sampleMovieDetail)
        override suspend fun getTvShowDetail(slug: String) = throw UnsupportedOperationException()
        override suspend fun searchTitles(keyword: String, page: Int) = throw UnsupportedOperationException()
    }

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
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

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        movieDao = database.movieDao()
    }

    @After
    fun teardown() {
        composeTestRule.waitForIdle()
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun bookmarkFlow_toggleInDetailAndVerifyInBookmarkScreen() {
        val repository = MovieDetailRepository(fakeApiService, testDispatcher, movieDao)
        val viewModel = MovieDetailViewModel(repository)

        // 1. Load detail screen and display
        viewModel.loadDetail("inception-2010", isTvShow = false)

        var currentScreen by mutableStateOf("detail")
        var clickedSlug = ""

        composeTestRule.setContent {
            MovieAppTheme {
                if (currentScreen == "detail") {
                    MovieDetailScreen(
                        viewModel = viewModel,
                        onBackClick = {}
                    )
                } else {
                    BookmarkScreen(
                        onTitleClick = { slug, _ -> clickedSlug = slug },
                        dao = movieDao
                    )
                }
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.detail != null
        }
        composeTestRule.waitForIdle()

        // Verify title displayed
        composeTestRule.onNodeWithText("Inception", substring = true).assertIsDisplayed()

        // Verify initially not bookmarked (in top bar)
        composeTestRule.onNodeWithContentDescription("Add to bookmarks").assertIsDisplayed()

        // 2. Click Bookmark toggle button
        composeTestRule.onNodeWithContentDescription("Add to bookmarks").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.isBookmarked
        }
        composeTestRule.waitForIdle()

        // Verify toggle state changed
        composeTestRule.onNodeWithContentDescription("Remove from bookmarks").assertIsDisplayed()

        // Verify in Room database that it is actually persisted
        val isBookmarkedInDb = runBlocking { movieDao.isBookmarked("inception-2010").first() }
        assertTrue(isBookmarkedInDb == true)

        // 3. Switch to BookmarkScreen and verify "Inception" is listed
        currentScreen = "bookmarks"
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Inception")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // Verify title exists in the bookmarks grid
        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
        composeTestRule.onNodeWithText("2010").assertIsDisplayed()

        // Click the bookmarked item card
        composeTestRule.onNodeWithText("Inception").performClick()
        assertEquals("inception-2010", clickedSlug)
    }

    @Test
    fun unbookmarkOnBookmarkScreen_immediatelyRemovesTitleAndUpdatesDatabase() {
        val sampleMovie = com.movieapp.data.local.MovieEntity(
            slug = "inception-2010",
            title = "Inception",
            poster = "https://example.com/poster.jpg",
            rating = "8.8",
            releaseYear = "2010",
            isTvShow = false,
            plot = "A thief who steals corporate secrets.",
            jsonDetail = "{}",
            isBookmarked = true,
            bookmarkedAt = System.currentTimeMillis()
        )
        runBlocking { movieDao.insertOrUpdate(sampleMovie) }
        var showBookmarkScreen by mutableStateOf(true)
        composeTestRule.setContent {
            MovieAppTheme {
                if (showBookmarkScreen) {
                    BookmarkScreen(
                        onTitleClick = { _, _ -> },
                        dao = movieDao
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Verify Inception is displayed in bookmarks
        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()

        // Click the unbookmark button
        composeTestRule.onNodeWithContentDescription("Remove from bookmarks", substring = true).performClick()
        composeTestRule.waitForIdle()

        // Verify Inception is immediately no longer displayed on screen
        composeTestRule.onNodeWithText("Inception").assertDoesNotExist()

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("No Bookmarks Yet").assertIsDisplayed()

        // Wait until Room database has processed the unbookmark
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            runBlocking { movieDao.getBookmarkedMovies().first().isEmpty() }
        }
        composeTestRule.waitForIdle()

        // Verify in Room database that it is updated to false
        val isBookmarkedInDb = runBlocking { movieDao.isBookmarked("inception-2010").first() }
        assertTrue(isBookmarkedInDb == false)

        // Dispose composable to cleanly stop Room flow observation before teardown
        showBookmarkScreen = false
        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun downloadBottomSheet_resolutionFilteringAndButtons() {
        val testLinks = listOf(
            DownloadLinkDTO(
                id = 1L,
                resolution = "720p",
                size = "850 MB",
                serverName = "MegaUp",
                url = "https://megaup.net/sample720"
            ),
            DownloadLinkDTO(
                id = 2L,
                resolution = "1080p",
                size = "2.1 GB",
                serverName = "Yoteshin Portal",
                url = "https://yoteshin.net/sample1080"
            ),
            DownloadLinkDTO(
                id = 3L,
                resolution = "4K",
                size = "6.5 GB",
                serverName = "Telegram",
                url = "https://t.me/ch003agwpcd/100"
            )
        )

        composeTestRule.setContent {
            MovieAppTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                DownloadLinksBottomSheet(
                    title = "Inception (2010)",
                    downloadLinks = testLinks,
                    sheetState = sheetState,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify header
        composeTestRule.onNodeWithText("Download Links").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inception (2010)").assertIsDisplayed()

        // Verify Resolution Chips exist (use role Tab to distinguish from card badge)
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("720p"))[0].assertIsDisplayed()
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("1080p"))[0].assertIsDisplayed()
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("4K"))[0].assertIsDisplayed()

        // Verify initial state shows all links (perform scroll to ensure off-screen items are brought into view)
        composeTestRule.onNodeWithText("MegaUp").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Yoteshin Portal").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Telegram"))[0].performScrollTo().assertIsDisplayed()

        // Filter by 1080p
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("1080p"))[0].performClick()
        composeTestRule.waitForIdle()

        // 1080p item should still be visible
        composeTestRule.onNodeWithText("Yoteshin Portal").performScrollTo().assertIsDisplayed()
        // Other items should be filtered out
        composeTestRule.onNodeWithText("MegaUp").assertDoesNotExist()
        composeTestRule.onNodeWithText("Telegram").assertDoesNotExist()

        // Filter by 4K (Telegram link)
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("4K"))[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Telegram"))[0].performScrollTo().assertIsDisplayed()
    }
}
