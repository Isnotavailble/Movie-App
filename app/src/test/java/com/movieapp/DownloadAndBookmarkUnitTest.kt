package com.movieapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.movieapp.data.local.AppDatabase
import com.movieapp.data.local.DownloadDao
import com.movieapp.data.local.DownloadEntity
import com.movieapp.data.local.MovieDao
import com.movieapp.data.local.MovieEntity
import com.movieapp.features.downloadlinks.DirectDownloadResolver
import com.movieapp.features.downloadlinks.DownloadManagerHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadAndBookmarkUnitTest {

    private lateinit var database: AppDatabase
    private lateinit var movieDao: MovieDao
    private lateinit var downloadDao: DownloadDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        movieDao = database.movieDao()
        downloadDao = database.downloadDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testTelegramDeepLinkConversion() {
        // Standard channel message link
        val webUrl1 = "https://t.me/ch003agwpcd/881"
        val tgLink1 = DownloadManagerHelper.convertToTelegramDeepLink(webUrl1)
        assertEquals("tg://resolve?domain=ch003agwpcd&post=881", tgLink1)

        // Telegram link without https prefix
        val webUrl2 = "t.me/my_channel/42"
        val tgLink2 = DownloadManagerHelper.convertToTelegramDeepLink(webUrl2)
        assertEquals("tg://resolve?domain=my_channel&post=42", tgLink2)

        // General channel link without post
        val webUrl3 = "https://t.me/my_channel"
        val tgLink3 = DownloadManagerHelper.convertToTelegramDeepLink(webUrl3)
        assertEquals("tg://resolve?domain=my_channel", tgLink3)
    }

    @Test
    fun testDirectDownloadResolverMediaTypeValidation() {
        // Video media streams must be accepted
        assertTrue(DirectDownloadResolver.isMediaType("video/mp4"))
        assertTrue(DirectDownloadResolver.isMediaType("video/x-matroska"))
        assertTrue(DirectDownloadResolver.isMediaType("video/webm"))
        assertTrue(DirectDownloadResolver.isMediaType("application/octet-stream"))
        assertTrue(DirectDownloadResolver.isMediaType("binary/octet-stream"))

        // HTML, plain text, and JSON error pages MUST be rejected (preventing 78 kB HTML downloads)
        assertFalse(DirectDownloadResolver.isMediaType("text/html"))
        assertFalse(DirectDownloadResolver.isMediaType("text/html; charset=UTF-8"))
        assertFalse(DirectDownloadResolver.isMediaType("text/plain"))
        assertFalse(DirectDownloadResolver.isMediaType("application/json"))
        assertFalse(DirectDownloadResolver.isMediaType(""))
    }

    @Test
    fun testDirectDownloadResolverKnownWebPortals() {
        // Known web portals with HTML landing pages
        assertTrue(DirectDownloadResolver.isKnownWebPortal("https://yoteshinportal.cc/hydra-2025-720-p-web-dl-mp-4"))
        assertTrue(DirectDownloadResolver.isKnownWebPortal("https://usersdrive.com/sample999.html"))
        assertTrue(DirectDownloadResolver.isKnownWebPortal("https://bioscopeapp.com/watch/123"))

        // Direct media files
        assertFalse(DirectDownloadResolver.isKnownWebPortal("https://cdn.example.com/movies/hydra_720p.mp4"))
        assertFalse(DirectDownloadResolver.isKnownWebPortal("https://files.storage.com/media/barreda_1080p.mkv"))
    }

    @Test
    fun testDownloadDaoOperations() = runBlocking {
        val activeTask = DownloadEntity(
            downloadId = 101L,
            title = "Hydra",
            movieSlug = "hydra-bjcl5wwm",
            poster = "https://example.com/hydra.jpg",
            fileName = "Hydra.mp4",
            totalBytes = 650000000L,
            downloadedBytes = 130000000L,
            status = 2 // STATUS_RUNNING
        )

        // Insert active download task
        downloadDao.insertOrUpdate(activeTask)

        val activeList = downloadDao.getActiveDownloads().first()
        assertEquals(1, activeList.size)
        assertEquals("Hydra", activeList[0].title)
        assertEquals(20, activeList[0].progressPercentage)
        assertEquals("124.0 MB", activeList[0].formattedDownloadedSize)
        assertTrue(activeList[0].isActive)
        assertFalse(activeList[0].isCompleted)

        // Update progress to 100% and STATUS_SUCCESSFUL (8)
        downloadDao.updateProgress(
            id = 101L,
            downloadedBytes = 650000000L,
            totalBytes = 650000000L,
            status = 8,
            completedAt = System.currentTimeMillis(),
            fileUri = "content://downloads/my_downloads/101"
        )

        // Verify active downloads is now empty
        val updatedActive = downloadDao.getActiveDownloads().first()
        assertTrue(updatedActive.isEmpty())

        // Verify completed downloads contains the task
        val completedList = downloadDao.getCompletedDownloads().first()
        assertEquals(1, completedList.size)
        assertEquals("Hydra", completedList[0].title)
        assertTrue(completedList[0].isCompleted)
        assertEquals("content://downloads/my_downloads/101", completedList[0].fileUri)

        // Delete download
        downloadDao.deleteDownload(101L)
        val afterDelete = downloadDao.getCompletedDownloads().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testRoomCacheAndBookmarkOperations() = runBlocking {
        val sampleMovie = MovieEntity(
            slug = "mayday-2026",
            title = "Mayday",
            poster = "https://example.com/poster.jpg",
            rating = "7.2",
            releaseYear = "2026",
            isTvShow = false,
            plot = "A great adventure movie.",
            jsonDetail = "{}",
            isBookmarked = false,
            bookmarkedAt = 0L
        )

        // Insert cached entity
        movieDao.insertOrUpdate(sampleMovie)
        val retrieved = movieDao.getMovieBySlug("mayday-2026")
        assertNotNull(retrieved)
        assertEquals("Mayday", retrieved?.title)
        assertFalse(retrieved?.isBookmarked ?: true)

        // Bookmark the movie
        val timestamp = System.currentTimeMillis()
        movieDao.updateBookmarkStatus("mayday-2026", true, timestamp)

        // Verify bookmark query
        val isBookmarked = movieDao.isBookmarked("mayday-2026").first()
        assertTrue(isBookmarked == true)

        val bookmarks = movieDao.getBookmarkedMovies().first()
        assertEquals(1, bookmarks.size)
        assertEquals("mayday-2026", bookmarks[0].slug)

        // Unbookmark
        movieDao.updateBookmarkStatus("mayday-2026", false, 0L)
        val unbookmarkedList = movieDao.getBookmarkedMovies().first()
        assertTrue(unbookmarkedList.isEmpty())
    }

    @Test
    fun testUpdateMetadataPreservesBookmarkStatus() = runBlocking {
        val sampleMovie = MovieEntity(
            slug = "interstellar-2014",
            title = "Interstellar",
            poster = "https://example.com/poster.jpg",
            rating = "8.6",
            releaseYear = "2014",
            isTvShow = false,
            plot = "Space travel.",
            jsonDetail = "{}",
            isBookmarked = true,
            bookmarkedAt = 123456789L
        )

        // 1. Insert bookmarked movie
        movieDao.insertOrUpdate(sampleMovie)
        val initial = movieDao.getMovieBySlug("interstellar-2014")
        assertTrue(initial?.isBookmarked == true)
        assertEquals(123456789L, initial?.bookmarkedAt)

        // 2. Metadata update from fresh network response
        val updatedRows = movieDao.updateMetadata(
            slug = "interstellar-2014",
            title = "Interstellar (Updated)",
            poster = "https://example.com/new_poster.jpg",
            rating = "8.7",
            releaseYear = "2014",
            isTvShow = false,
            plot = "Updated plot summary.",
            jsonDetail = "{\"rating\": 8.7}",
            cachedAt = System.currentTimeMillis()
        )
        assertEquals(1, updatedRows)

        // 3. Verify metadata changed, but isBookmarked and bookmarkedAt were preserved!
        val updated = movieDao.getMovieBySlug("interstellar-2014")
        assertNotNull(updated)
        assertEquals("Interstellar (Updated)", updated?.title)
        assertEquals("8.7", updated?.rating)
        assertTrue("isBookmarked must remain true after metadata update", updated?.isBookmarked == true)
        assertEquals("bookmarkedAt must remain 123456789L after metadata update", 123456789L, updated?.bookmarkedAt)
    }
}
