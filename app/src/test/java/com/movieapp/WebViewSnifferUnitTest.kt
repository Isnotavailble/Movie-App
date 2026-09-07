package com.movieapp

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.movieapp.features.downloadlinks.DirectDownloadResolver
import com.movieapp.features.downloadlinks.DownloadLinkDTO
import com.movieapp.features.downloadlinks.DownloadManagerHelper
import com.movieapp.features.downloadlinks.WebViewDownloadSniffer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Trait tag annotation conforming to /test-tagging taxonomy.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Trait(val category: String, val secondary: String = "")

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebViewSnifferUnitTest {

    @Test
    @Trait(category = "positive", secondary = "critical-path")
    fun testSnifferMediaDetection_validVideoMime_recognizedAsDownloadable() {
        // Direct media streams and MIME types
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://storage.com/file", "video/mp4"))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://cdn.example.com/stream", "application/x-matroska"))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://host.net/cdn", "binary/octet-stream"))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://host.net/video.mkv", null))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://host.net/video.mp4?token=123", null))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://host.net/video.webm", null))
    }

    @Test
    @Trait(category = "negative", secondary = "boundary")
    fun testSnifferMediaDetection_htmlAndPlainText_rejected() {
        // Web portal pages, HTML, JSON, and errors must NOT be intercepted as video files
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://megaup.net/34b80bbb", "text/html"))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://megaup.net/page.html", null))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://usersdrive.com/sample.php", null))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://api.example.com/details", "application/json"))
        assertFalse(WebViewDownloadSniffer.isMediaStream("", null))
        assertFalse(WebViewDownloadSniffer.isMediaStream(null, null))
    }

    @Test
    @Trait(category = "negative", secondary = "boundary")
    fun testSnifferMediaDetection_cloudflareChallengesAndHtml_explicitlyRejected() {
        // Cloudflare challenge URLs, Turnstile scripts, and HTML responses must NEVER be recognized as media streams
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://challenges.cloudflare.com/cdn-cgi/challenge-platform/h/g/turnstile/if/ov2/av0/rcv0/0/sample", null))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://challenges.cloudflare.com/turnstile/v0/api.js", "text/javascript"))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://megaup.net/cdn-cgi/challenge-platform/h/b/scripts/p.js", null))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://download.megaup.net/?url=https%3A%2F%2Fstorage.com", "text/html"))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://download.megaup.net/?cf_chl_tk=123", "text/html"))
    }

    @Test
    @Trait(category = "positive", secondary = "boundary")
    fun testSnifferMediaDetection_downloadEndpoints_recognized() {
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://cdn.example.com/get/stream123", "application/octet-stream"))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://s12.megaup.net/cache/video.mp4", "video/mp4"))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://cdn.example.com/download/stream123", "video/mp4"))
        assertTrue(WebViewDownloadSniffer.isMediaStream("https://storage.googleapis.com/movie-bucket/sample", "video/mp4"))
    }

    @Test
    @Trait(category = "negative", secondary = "boundary")
    fun testSnifferMediaDetection_megaupChallengeUrl_rejected() {
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://download.megaup.net/?url=abc", null))
        assertFalse(WebViewDownloadSniffer.isMediaStream("https://download.megaup.net/?url=abc", "text/html"))
    }

    @Test
    @Trait(category = "positive", secondary = "integration")
    fun testMovieDownloadServiceStart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directUrl = "https://cdn.example.com/movies/sample_video.mp4"
        val testCookies = "cf_clearance=abcd1234efgh; session=xyz987"
        val testUserAgent = WebViewDownloadSniffer.CHROME_USER_AGENT

        val downloadId = DownloadManagerHelper.startMovieDownloadService(
            context = context,
            title = "Test Movie Service",
            directUrl = directUrl,
            cookies = testCookies,
            userAgent = testUserAgent
        )
        assertTrue("MovieDownloadService must assign valid task ID (id >= 0)", downloadId >= 0)
    }

    @Test
    @Trait(category = "positive", secondary = "smoke")
    fun testExternalDownloader_packagesConfigured() {
        val pkgs = DownloadManagerHelper.EXTERNAL_DOWNLOADER_PACKAGES
        assertTrue(pkgs.contains("idm.internet.download.manager"))
        assertTrue(pkgs.contains("idm.internet.download.manager.plus"))
        assertTrue(pkgs.contains("com.dv.adm"))
    }

    @Test
    @Trait(category = "negative", secondary = "resilience")
    fun testExternalDownloader_emptyUrl_returnsFalse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(DownloadManagerHelper.openInExternalDownloader(context, ""))
        assertFalse(DownloadManagerHelper.openInExternalDownloader(context, "   "))
    }

    @Test
    @Trait(category = "positive", secondary = "boundary")
    fun testIsKnownWebPortal_excludesDirectMediaUrls() {
        // Direct media files even hosted on portal domains are valid media files
        assertFalse(DirectDownloadResolver.isKnownWebPortal("https://usersdrive.com/files/movie.mp4"))
        assertFalse(DirectDownloadResolver.isKnownWebPortal("https://yoteshinportal.cc/download/movie.mkv"))

        // HTML portal pages
        assertTrue(DirectDownloadResolver.isKnownWebPortal("https://usersdrive.com/sample999.html"))
        assertTrue(DirectDownloadResolver.isKnownWebPortal("https://yoteshinportal.cc/hydra-2025"))
        assertTrue(DirectDownloadResolver.isKnownWebPortal("https://drive.google.com/file/d/123/view"))
    }

    @Test
    @Trait(category = "negative", secondary = "boundary")
    fun testDirectDownloadResolver_emptyUrl_failsGracefully() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val emptyDto = DownloadLinkDTO(url = "")
        val result = DirectDownloadResolver.resolveDirectUrl(context, emptyDto)
        assertTrue(result.isFailure)
        assertEquals("Empty URL", result.exceptionOrNull()?.message)
    }

    @Test
    @Trait(category = "positive", secondary = "critical-path")
    fun testYoteshinDetection_andDeepLinkConversion() {
        val yoteshinDto = DownloadLinkDTO(
            id = 50L,
            serverName = "Yoteshin",
            url = "https://yoteshinportal.cc/uno-2025-720-p-amzn-web-dl-hmtv-nc-mp-4"
        )
        assertTrue("DownloadLinkDTO must identify Yoteshin", yoteshinDto.isYoteshin)

        val directDeepLink = DownloadManagerHelper.convertToYoteshinDeepLink("https://yoteshinportal.cc/drive?p=LNtc_Ndg_")
        assertEquals("yoteshin://yoteshinportal.cc/drive?p=LNtc_Ndg_", directDeepLink)

        val alreadyDeepLink = DownloadManagerHelper.convertToYoteshinDeepLink("yoteshin://yoteshinportal.cc/drive?p=sample123")
        assertEquals("yoteshin://yoteshinportal.cc/drive?p=sample123", alreadyDeepLink)
    }

    @Test
    @Trait(category = "positive", secondary = "boundary")
    fun testMegaUpCopyUrl_preservesWebPortalUrlForExternalDownloader() = runBlocking {
        val megaUpUrl = "https://megaup.net/b4693f8cd7799d1de751a7c9bfa47dc6/Uno.2025.1080p.mkv"
        val dto = DownloadLinkDTO(
            id = 51L,
            serverName = "Megaup",
            url = megaUpUrl
        )
        val copyResult = DirectDownloadResolver.resolveDirectUrlForCopy(dto)
        assertTrue(copyResult.isSuccess)
        assertEquals(megaUpUrl, copyResult.getOrNull())
    }

    @Test
    @Trait(category = "positive", secondary = "resilience")
    fun testCorruptHtmlDownload_detectedAndRejected() {
        val tempHtmlFile = java.io.File.createTempFile("cloudflare_challenge", ".mp4")
        try {
            tempHtmlFile.writeText("<!DOCTYPE html><html lang=\"en-US\"><head><title>Just a moment...</title></head><body>cf-chl</body></html>")
            assertTrue("HTML challenge file must be detected as corrupt", DownloadManagerHelper.isCorruptHtmlDownload(tempHtmlFile))

            val tempBinaryFile = java.io.File.createTempFile("valid_stream", ".mp4")
            try {
                tempBinaryFile.writeBytes(ByteArray(1024) { 0x00 })
                assertFalse("Binary video file must NOT be detected as corrupt HTML", DownloadManagerHelper.isCorruptHtmlDownload(tempBinaryFile))
            } finally {
                tempBinaryFile.delete()
            }
        } finally {
            tempHtmlFile.delete()
        }
    }

    @Test
    @Trait(category = "positive", secondary = "integration")
    fun testRefererHeaderPassedInDownloadManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directUrl = "https://cdn.example.com/movies/sample_video.mp4"
        val referer = "https://megaup.net/sample123"

        val downloadId = DownloadManagerHelper.startNativeDownload(
            context = context,
            title = "Test Movie with Referer",
            directUrl = directUrl,
            referer = referer
        )
        assertTrue("DownloadManager must enqueue with referer (id >= 0)", downloadId >= 0)
    }

    private fun assertNotNull(obj: Any?) {
        assertTrue(obj != null)
    }
}