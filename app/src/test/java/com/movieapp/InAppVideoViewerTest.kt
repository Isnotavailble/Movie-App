package com.movieapp

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.movieapp.data.local.DownloadEntity
import com.movieapp.features.downloads.InAppVideoViewerModal
import com.movieapp.features.downloads.VideoSource
import com.movieapp.features.downloads.formatDurationMs
import com.movieapp.features.downloads.resolveVideoPlaybackUri
import com.movieapp.features.downloads.resolveVideoSource
import com.movieapp.theme.MovieAppTheme
import com.movieapp.util.AppLanguage
import com.movieapp.util.LocalizationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1200dp")
class InAppVideoViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFormatDurationMs() {
        assertEquals("00:00", formatDurationMs(0L))
        assertEquals("00:00", formatDurationMs(-500L))
        assertEquals("00:45", formatDurationMs(45_000L))
        assertEquals("02:05", formatDurationMs(125_000L))
        assertEquals("01:01:05", formatDurationMs(3_665_000L))
        assertEquals("02:20:00", formatDurationMs(8_400_000L))
    }

    @Test
    fun testResolveVideoSource() {
        // 1. Missing file & missing URI -> NotFound
        val downloadMissing = DownloadEntity(
            downloadId = 100L,
            title = "Missing Movie",
            fileName = "non_existent_file_${System.currentTimeMillis()}.mp4",
            fileUri = null
        )
        val source1 = resolveVideoSource(downloadMissing)
        assertTrue(source1 is VideoSource.NotFound)

        // 2. Missing file on disk but has valid content/file URI -> UriSource
        val downloadUri = DownloadEntity(
            downloadId = 101L,
            title = "Uri Movie",
            fileName = "dummy_${System.currentTimeMillis()}.mp4",
            fileUri = "content://media/external/video/media/42"
        )
        val source2 = resolveVideoSource(downloadUri)
        assertTrue(source2 is VideoSource.UriSource)
        assertEquals("content://media/external/video/media/42", (source2 as VideoSource.UriSource).uri.toString())

        // 3. Existing file in Downloads directory -> LocalFile
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        val tempFile = File(downloadsDir, "test_local_movie_${System.currentTimeMillis()}.mp4")
        try {
            tempFile.writeText("fake video content for test")
            val downloadFile = DownloadEntity(
                downloadId = 102L,
                title = "Local Movie",
                fileName = tempFile.name,
                fileUri = null
            )
            val source3 = resolveVideoSource(downloadFile)
            assertTrue(source3 is VideoSource.LocalFile)
            assertEquals(tempFile.absolutePath, (source3 as VideoSource.LocalFile).file.absolutePath)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Test
    fun testResolveVideoPlaybackUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Missing file & missing URI -> null
        val downloadMissing = DownloadEntity(
            downloadId = 200L,
            title = "Missing Video",
            fileName = "missing_${System.currentTimeMillis()}.mp4",
            fileUri = null
        )
        val uri1 = resolveVideoPlaybackUri(context, downloadMissing)
        assertNull(uri1)

        // 2. Has valid content URI
        val downloadContentUri = DownloadEntity(
            downloadId = 201L,
            title = "Content URI Video",
            fileName = "dummy_${System.currentTimeMillis()}.mp4",
            fileUri = "content://media/external/video/media/100"
        )
        val uri2 = resolveVideoPlaybackUri(context, downloadContentUri)
        assertEquals("content://media/external/video/media/100", uri2.toString())
    }

    @Test
    fun testVideoPlayerLocalizationKeys() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LocalizationManager.initialize(context)

        // Test English
        LocalizationManager.setLanguage(AppLanguage.EN)
        assertEquals("Close player", LocalizationManager.getString("video_player_close"))
        assertEquals("Play", LocalizationManager.getString("video_player_play"))
        assertEquals("Pause", LocalizationManager.getString("video_player_pause"))
        assertEquals("Rewind 10 seconds", LocalizationManager.getString("video_player_rewind"))
        assertEquals("Forward 10 seconds", LocalizationManager.getString("video_player_forward"))
        assertEquals("External", LocalizationManager.getString("video_player_external"))
        assertEquals("Full screen", LocalizationManager.getString("video_player_fullscreen"))
        assertEquals("Exit full screen", LocalizationManager.getString("video_player_exit_fullscreen"))
        assertEquals("Rotate screen", LocalizationManager.getString("video_player_rotate"))
        assertEquals("Aspect ratio", LocalizationManager.getString("video_player_aspect_ratio"))
        assertEquals("Fit", LocalizationManager.getString("video_player_aspect_fit"))
        assertEquals("Crop to Fill", LocalizationManager.getString("video_player_aspect_zoom"))
        assertEquals("Stretch", LocalizationManager.getString("video_player_aspect_stretch"))
        assertEquals("Playback speed", LocalizationManager.getString("video_player_speed"))

        // Test Myanmar
        LocalizationManager.setLanguage(AppLanguage.MY)
        assertEquals("ဗီဒီယိုပိတ်မည်", LocalizationManager.getString("video_player_close"))
        assertEquals("ဖွင့်မည်", LocalizationManager.getString("video_player_play"))
        assertEquals("ခေတ္တရပ်မည်", LocalizationManager.getString("video_player_pause"))
        assertEquals("၁၀ စက္ကန့် နောက်သို့", LocalizationManager.getString("video_player_rewind"))
        assertEquals("၁၀ စက္ကန့် ရှေ့သို့", LocalizationManager.getString("video_player_forward"))
        assertEquals("အခြားအက်ပ်", LocalizationManager.getString("video_player_external"))
        assertEquals("မျက်နှာပြင်ပြည့်", LocalizationManager.getString("video_player_fullscreen"))
        assertEquals("မျက်နှာပြင်ပြည့်မှ ထွက်မည်", LocalizationManager.getString("video_player_exit_fullscreen"))
        assertEquals("မျက်နှာပြင် လှည့်မည်", LocalizationManager.getString("video_player_rotate"))
        assertEquals("ဗီဒီယို အချိုးအစား", LocalizationManager.getString("video_player_aspect_ratio"))
        assertEquals("အံဝင်ခွင်ကျ", LocalizationManager.getString("video_player_aspect_fit"))
        assertEquals("မျက်နှာပြင်အပြည့်", LocalizationManager.getString("video_player_aspect_zoom"))
        assertEquals("ဆန့်ထွက်", LocalizationManager.getString("video_player_aspect_stretch"))
        assertEquals("အမြန်နှုန်း", LocalizationManager.getString("video_player_speed"))
    }

    @Test
    fun testInAppVideoViewerModal_rendersAndDismisses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LocalizationManager.initialize(context)
        LocalizationManager.setLanguage(AppLanguage.EN)

        var dismissed = false

        val download = DownloadEntity(
            downloadId = 999L,
            title = "Spider-Man: Beyond the Spider-Verse",
            fileName = "SpiderMan_1080p.mp4",
            fileUri = "content://media/external/video/media/999",
            totalBytes = 2_147_483_648L,
            downloadedBytes = 2_147_483_648L,
            status = 8 // STATUS_SUCCESSFUL
        )

        composeTestRule.setContent {
            MovieAppTheme {
                InAppVideoViewerModal(
                    download = download,
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Check that modal UI elements are rendered
        composeTestRule.onNodeWithText("Spider-Man: Beyond the Spider-Verse").assertIsDisplayed()

        // Click close button
        val closeDesc = LocalizationManager.getString("video_player_close")
        composeTestRule.onNodeWithContentDescription(closeDesc).performClick()

        // Verify onDismiss callback fired
        assertTrue(dismissed)
    }

    @Test
    fun testInAppVideoViewerModal_newControlsRenderAndInteract() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LocalizationManager.initialize(context)
        LocalizationManager.setLanguage(AppLanguage.EN)

        val download = DownloadEntity(
            downloadId = 999L,
            title = "Spider-Man: Beyond the Spider-Verse",
            fileName = "SpiderMan_1080p.mp4",
            fileUri = "content://media/external/video/media/999",
            totalBytes = 2_147_483_648L,
            downloadedBytes = 2_147_483_648L,
            status = 8
        )

        composeTestRule.setContent {
            MovieAppTheme {
                InAppVideoViewerModal(
                    download = download,
                    onDismiss = {}
                )
            }
        }

        // 1. Fullscreen Button (renders and toggles between Fullscreen and Exit Fullscreen)
        val fullDesc = LocalizationManager.getString("video_player_fullscreen")
        val exitFullDesc = LocalizationManager.getString("video_player_exit_fullscreen")
        val fullNode = composeTestRule.onNodeWithContentDescription(fullDesc)
        fullNode.assertIsDisplayed()
        fullNode.performClick()

        // After clicking, button content description becomes "Exit full screen"
        val exitFullNode = composeTestRule.onNodeWithContentDescription(exitFullDesc)
        exitFullNode.assertIsDisplayed()
        exitFullNode.performClick()
        composeTestRule.onNodeWithContentDescription(fullDesc).assertIsDisplayed()

        // 2. Rotate Screen Button (renders and can be clicked)
        val rotateDesc = LocalizationManager.getString("video_player_rotate")
        composeTestRule.onNodeWithContentDescription(rotateDesc).assertIsDisplayed().performClick()

        // 3. Aspect Ratio Button (renders and can be clicked to cycle modes)
        val aspectPrefix = LocalizationManager.getString("video_player_aspect_ratio")
        val aspectFit = LocalizationManager.getString("video_player_aspect_fit")
        val aspectDesc = "$aspectPrefix: $aspectFit"
        composeTestRule.onNodeWithContentDescription(aspectDesc).assertIsDisplayed().performClick()

        // 4. Playback Speed Button (renders 1.0x by default, clicking cycles to 1.25x)
        val speedPrefix = LocalizationManager.getString("video_player_speed")
        composeTestRule.onNodeWithContentDescription("$speedPrefix 1.0x").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription("$speedPrefix 1.25x").assertIsDisplayed()
    }

    @Test
    fun testInAppVideoViewerModal_errorStateRendersAndDismisses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LocalizationManager.initialize(context)
        LocalizationManager.setLanguage(AppLanguage.EN)

        var dismissed = false

        // Download with missing file and no fileUri triggers error card overlay
        val missingDownload = DownloadEntity(
            downloadId = 888L,
            title = "Spider-Man Missing",
            fileName = "non_existent_${System.currentTimeMillis()}.mp4",
            fileUri = null,
            totalBytes = 1_000_000L,
            downloadedBytes = 1_000_000L,
            status = 8
        )

        composeTestRule.setContent {
            MovieAppTheme {
                InAppVideoViewerModal(
                    download = missingDownload,
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Check error text is displayed
        val errorText = LocalizationManager.getString("video_player_error")
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()

        // Click cancel button (icon-only with accessibility contentDescription)
        val cancelText = LocalizationManager.getString("cancel")
        composeTestRule.onNodeWithContentDescription(cancelText).performClick()

        assertTrue(dismissed)
    }
}
