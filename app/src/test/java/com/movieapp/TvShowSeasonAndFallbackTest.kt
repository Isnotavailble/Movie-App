package com.movieapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.movieapp.data.local.AppDatabase
import com.movieapp.data.local.MovieDao
import com.movieapp.features.downloadlinks.DownloadLinkDTO
import com.movieapp.features.moviedetail.EpisodeDTO
import com.movieapp.features.moviedetail.MovieDetailDTO
import com.movieapp.features.moviedetail.MovieDetailRepository
import com.movieapp.features.moviedetail.MovieDetailResponseDTO
import com.movieapp.features.moviedetail.MovieDetailUiState
import com.movieapp.features.moviedetail.MovieDetailViewModel
import com.movieapp.features.moviedetail.SeasonDTO
import com.movieapp.features.movielist.MovieListResponseDTO
import com.movieapp.features.search.SearchResponseDTO
import com.movieapp.network.MovieApiService
import com.movieapp.util.Resource
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TvShowSeasonAndFallbackTest {

    private lateinit var database: AppDatabase
    private lateinit var movieDao: MovieDao
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        movieDao = database.movieDao()
    }

    @After
    fun teardown() {
        kotlinx.coroutines.Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testSeasonDTONumberExtractionWhenRawNumberIsNull() {
        // Backend only provides id and name, no season_number field
        val season1 = SeasonDTO(
            id = 7296L,
            name = "Season 1",
            episodes = listOf(
                EpisodeDTO(id = 1L, rawEpisodeNumber = 1, title = "Ep 1")
            )
        )
        val season2 = SeasonDTO(
            id = 8914L,
            name = "Season 2",
            episodes = listOf(
                EpisodeDTO(id = 2L, rawEpisodeNumber = 1, title = "Ep 1")
            )
        )

        assertEquals(7296L, season1.idLong)
        assertEquals(8914L, season2.idLong)
        assertEquals(1, season1.seasonNumber)
        assertEquals(2, season2.seasonNumber)
        assertEquals("Season 1", season1.displayName)
        assertEquals("Season 2", season2.displayName)
    }

    @Test
    fun testSeasonSelectionBySeasonDTOInViewModel() {
        val season1 = SeasonDTO(
            id = 7296L,
            name = "Season 1",
            episodes = listOf(EpisodeDTO(id = 101L, rawEpisodeNumber = 1, title = "S1E1"))
        )
        val season2 = SeasonDTO(
            id = 8914L,
            name = "Season 2",
            episodes = listOf(EpisodeDTO(id = 201L, rawEpisodeNumber = 1, title = "S2E1"))
        )
        val tvDetail = MovieDetailDTO(
            id = 5001L,
            title = "Jet Lag",
            slug = "jet-lag",
            mediaType = "tv-show",
            seasons = listOf(season1, season2)
        )

        // Verify MovieDetailUiState resolves activeSeason by selectedSeasonId
        val stateInitial = MovieDetailUiState(
            detail = tvDetail,
            selectedSeasonId = season1.idLong,
            selectedSeasonNumber = season1.seasonNumber
        )
        assertNotNull(stateInitial.activeSeason)
        assertEquals(7296L, stateInitial.activeSeason?.idLong)
        assertEquals("Season 1", stateInitial.activeSeason?.displayName)
        assertEquals("S1E1", stateInitial.activeSeason?.safeEpisodes?.firstOrNull()?.title)

        // Verify selecting season 2 updates activeSeason correctly
        val stateUpdated = stateInitial.copy(
            selectedSeasonId = season2.idLong,
            selectedSeasonNumber = season2.seasonNumber
        )
        assertNotNull(stateUpdated.activeSeason)
        assertEquals(8914L, stateUpdated.activeSeason?.idLong)
        assertEquals("Season 2", stateUpdated.activeSeason?.displayName)
        assertEquals("S2E1", stateUpdated.activeSeason?.safeEpisodes?.firstOrNull()?.title)

        // Verify ViewModel selectSeason updates uiState
        val viewModel = MovieDetailViewModel()
        viewModel.selectSeason(season2)
        assertEquals(8914L, viewModel.uiState.value.selectedSeasonId)
        assertEquals(2, viewModel.uiState.value.selectedSeasonNumber)
    }

    @Test
    fun testFallbackToSecondaryBackendWhenPrimaryReturns404() = runBlocking {
        val theBearDetail = MovieDetailDTO(
            id = 5097L,
            title = "The Bear",
            slug = "the-bear-cc96s51d",
            mediaType = "tv-show",
            seasons = listOf(
                SeasonDTO(
                    id = 9271L,
                    name = "Season 5",
                    episodes = listOf(
                        EpisodeDTO(
                            id = 1001L,
                            rawEpisodeNumber = 1,
                            title = "Soda",
                            tvDownloadLinks = listOf(
                                DownloadLinkDTO(id = 83759L, serverName = "Yoteshin", url = "https://yoteshinportal.cc/the-bear-s05e01")
                            )
                        )
                    )
                )
            )
        )

        // Primary API (HomieTV) returns null or fails (404)
        val primaryApi = object : MovieApiService {
            override suspend fun getMovies(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getTvShows(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getMovieDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = false, data = null)
            override suspend fun getTvShowDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = false, data = null)
            override suspend fun searchTitles(keyword: String, page: Int): SearchResponseDTO = throw UnsupportedOperationException()
        }

        // Secondary API (YSFlix) has "The Bear"
        val fallbackApi = object : MovieApiService {
            override suspend fun getMovies(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getTvShows(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getMovieDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = false, data = null)
            override suspend fun getTvShowDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = true, data = theBearDetail)
            override suspend fun searchTitles(keyword: String, page: Int): SearchResponseDTO = throw UnsupportedOperationException()
        }

        val repo = MovieDetailRepository(
            apiService = primaryApi,
            ioDispatcher = testDispatcher,
            movieDao = movieDao,
            fallbackApiService = fallbackApi
        )

        val flow = repo.getTvShowDetail("the-bear-cc96s51d")
        val emissions = flow.toList()

        assertTrue(emissions.any { it is Resource.Loading })
        val successResult = emissions.filterIsInstance<Resource.Success<MovieDetailDTO>>().firstOrNull()
        assertNotNull(successResult)
        assertEquals("The Bear", successResult?.data?.displayTitle)
        assertEquals(1, successResult?.data?.safeSeasons?.size)
        assertEquals("Season 5", successResult?.data?.safeSeasons?.firstOrNull()?.displayName)
        assertEquals(1, successResult?.data?.safeSeasons?.firstOrNull()?.safeEpisodes?.size)
        assertEquals(1, successResult?.data?.safeSeasons?.firstOrNull()?.safeEpisodes?.firstOrNull()?.safeDownloadLinks?.size)
    }

    @Test
    fun testHtmlSanitization_stripsAllTagsAndDecodesEntities() {
        val rawHtml = "<p>Welcome to <b>The Bear</b> season 5!</p><br/><div class=\"desc\">Watch Carmy &amp; Sydney run the kitchen. &quot;Yes Chef!&#39;&quot;</div>"
        val dto = MovieDetailDTO(
            id = 5097L,
            title = "The Bear",
            slug = "the-bear-cc96s51d",
            plot = rawHtml
        )

        val clean = dto.cleanPlot
        assertNotNull(clean)
        assertTrue("HTML tags should be stripped", !clean!!.contains("<p>") && !clean.contains("<b>") && !clean.contains("</div>"))
        assertTrue("Entities should be decoded", clean.contains("&") && clean.contains("\"Yes Chef!'\""))
        assertEquals("Welcome to The Bear season 5!\n\nWatch Carmy & Sydney run the kitchen. \"Yes Chef!'\"", clean)
    }

    @Test
    fun testCleanPlotPersistedToDatabase_containsNoHtml() = runTest {
        val rawHtml = "<p>A chaotic kitchen drama.</p><br>Rating &amp; reviews."
        val theBearDetail = MovieDetailDTO(
            id = 5097L,
            title = "The Bear",
            slug = "the-bear-cc96s51d",
            plot = rawHtml,
            seasons = listOf(SeasonDTO(id = 1L, rawSeasonNumber = 1, name = "Season 1"))
        )

        val primaryApi = object : MovieApiService {
            override suspend fun getMovies(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getTvShows(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getMovieDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = false, data = null)
            override suspend fun getTvShowDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = false, data = null)
            override suspend fun searchTitles(keyword: String, page: Int): SearchResponseDTO = throw UnsupportedOperationException()
        }

        val fallbackApi = object : MovieApiService {
            override suspend fun getMovies(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getTvShows(page: Int): MovieListResponseDTO = throw UnsupportedOperationException()
            override suspend fun getMovieDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = false, data = null)
            override suspend fun getTvShowDetail(slug: String): MovieDetailResponseDTO = MovieDetailResponseDTO(success = true, data = theBearDetail)
            override suspend fun searchTitles(keyword: String, page: Int): SearchResponseDTO = throw UnsupportedOperationException()
        }

        val repo = MovieDetailRepository(
            apiService = primaryApi,
            ioDispatcher = testDispatcher,
            movieDao = movieDao,
            fallbackApiService = fallbackApi
        )

        val flow = repo.getTvShowDetail("the-bear-cc96s51d")
        val emissions = flow.toList()
        val success = emissions.filterIsInstance<Resource.Success<MovieDetailDTO>>().firstOrNull()
        assertNotNull(success)

        val cachedEntity = movieDao.getMovieBySlug("the-bear-cc96s51d")
        assertNotNull(cachedEntity)
        assertTrue("Cached plot in Room should not contain HTML tags", cachedEntity?.plot?.contains("<p>") == false)
        assertEquals("A chaotic kitchen drama.\n\nRating & reviews.", cachedEntity?.plot)
    }
}
