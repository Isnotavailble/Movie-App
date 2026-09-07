package com.movieapp

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.movieapp.data.local.AppDatabase
import com.movieapp.data.local.DownloadDao
import com.movieapp.data.local.DownloadEntity
import com.movieapp.features.downloadlinks.DownloadFallbackDialog
import com.movieapp.features.downloadlinks.DownloadLinkDTO
import com.movieapp.features.downloadlinks.DownloadLinksBottomSheet
import com.movieapp.features.downloads.DownloadsScreen
import com.movieapp.theme.MovieAppTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1200dp")
class DownloadE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var downloadDao: DownloadDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        downloadDao = database.downloadDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    @Trait(category = "positive", secondary = "critical-path")
    fun downloadsScreen_displaysActiveTaskAndSwitchesTabs() {
        runBlocking {
            // Pre-populate with an active downloading task
            val activeTask = DownloadEntity(
                downloadId = 555L,
                title = "Hydra (2025)",
                movieSlug = "hydra-bjcl5wwm",
                poster = "https://example.com/hydra.jpg",
                fileName = "Hydra_720p.mp4",
                totalBytes = 650000000L,
                downloadedBytes = 292500000L,
                status = 2 // STATUS_RUNNING (45%)
            )
            downloadDao.insertOrUpdate(activeTask)
        }

        composeTestRule.setContent {
            MovieAppTheme {
                DownloadsScreen(dao = downloadDao)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify header
        composeTestRule.onNodeWithText("Downloads").assertIsDisplayed()

        // 2. Wait for async Room Flow to emit active download task
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Hydra (2025)")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify active download task is visible under Downloading tab
        composeTestRule.onNodeWithText("Hydra (2025)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hydra_720p.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("45%").assertIsDisplayed()

        // 3. Switch to History tab (tab 1)
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()

        // History is initially empty
        composeTestRule.onNodeWithText("No downloaded movies yet").assertIsDisplayed()
    }

    @Test
    @Trait(category = "positive", secondary = "smoke")
    fun downloadsScreen_displaysCompletedTaskWithPlayAction() {
        runBlocking {
            // Pre-populate with a completed download
            val completedTask = DownloadEntity(
                downloadId = 777L,
                title = "Barreda (2026)",
                movieSlug = "barreda-errkqjhe",
                poster = "https://example.com/barreda.jpg",
                fileName = "Barreda_1080p.mkv",
                totalBytes = 1200000000L,
                downloadedBytes = 1200000000L,
                status = 8, // STATUS_SUCCESSFUL
                completedAt = System.currentTimeMillis()
            )
            downloadDao.insertOrUpdate(completedTask)
        }

        composeTestRule.setContent {
            MovieAppTheme {
                DownloadsScreen(dao = downloadDao)
            }
        }
        composeTestRule.waitForIdle()

        // Switch to History tab
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()

        // Verify completed movie card
        composeTestRule.onNodeWithText("Barreda (2026)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Play Video").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    @Trait(category = "positive", secondary = "end-to-end")
    fun downloadBottomSheet_rendersTelegramAndWebOptions() {
        val testLinks = listOf(
            DownloadLinkDTO(
                id = 10L,
                resolution = "720p",
                size = "620 MB",
                serverName = "MegaUp",
                url = "https://megaup.net/34b80bbb30a44b69fe97c96018651be8/Hydra.2025.720p.WEB-DL.mp4"
            ),
            DownloadLinkDTO(
                id = 11L,
                resolution = "720p",
                size = "620 MB",
                serverName = "Telegram",
                url = "https://t.me/ch003agwpcd/880"
            )
        )

        composeTestRule.setContent {
            MovieAppTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                DownloadLinksBottomSheet(
                    title = "Hydra (2025)",
                    downloadLinks = testLinks,
                    sheetState = sheetState,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify header and links
        composeTestRule.onNodeWithText("Download Links").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hydra (2025)").assertIsDisplayed()
        composeTestRule.onNodeWithText("MegaUp").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Telegram"))[0].performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    @Trait(category = "positive", secondary = "smoke")
    fun downloadBottomSheet_rendersExternalDownloaderActionForWebLinks() {
        val webOnlyLinks = listOf(
            DownloadLinkDTO(
                id = 20L,
                resolution = "1080p",
                size = "1.2 GB",
                serverName = "Usersdrive",
                url = "https://usersdrive.com/sample123.html"
            )
        )

        composeTestRule.setContent {
            MovieAppTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                DownloadLinksBottomSheet(
                    title = "Barreda (2026)",
                    downloadLinks = webOnlyLinks,
                    sheetState = sheetState,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify 1DM / ADM content description is present for web link
        composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription("1DM / ADM")).assertIsDisplayed()
    }

    @Test
    @Trait(category = "positive", secondary = "end-to-end")
    fun downloadFallbackDialog_rendersChoicesAndDispatchesCallbacks() {
        val testLink = DownloadLinkDTO(
            id = 99L,
            resolution = "1080p",
            size = "1.1 GB",
            serverName = "MegaUp",
            url = "https://megaup.net/sample123"
        )

        var browserClicked = false
        var copyClicked = false
        var dismissClicked = false

        composeTestRule.setContent {
            MovieAppTheme {
                DownloadFallbackDialog(
                    link = testLink,
                    onOpenInBrowser = { browserClicked = true },
                    onCopyLink = { copyClicked = true },
                    onDismiss = { dismissClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Dialog Title & Badges
        composeTestRule.onNodeWithText("Download Options").assertIsDisplayed()
        composeTestRule.onNodeWithText("MegaUp").assertIsDisplayed()
        composeTestRule.onNodeWithText("(1080p)").assertIsDisplayed()

        // 2. Verify Choice 1: "Open in Browser to Download"
        composeTestRule.onNodeWithText("Open in Browser to Download").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open in Browser to Download").performClick()
        org.junit.Assert.assertTrue("Open in Browser callback must be invoked", browserClicked)

        // 3. Verify Choice 2: "Copy Link for 1DM / ADM"
        composeTestRule.onNodeWithText("Copy Link for 1DM / ADM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy Link for 1DM / ADM").performClick()
        org.junit.Assert.assertTrue("Copy Link callback must be invoked", copyClicked)

        // 4. Verify Close action
        composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription("Close")).performClick()
        org.junit.Assert.assertTrue("Dismiss callback must be invoked", dismissClicked)
    }
}
