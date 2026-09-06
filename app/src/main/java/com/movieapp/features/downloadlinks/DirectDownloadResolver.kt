package com.movieapp.features.downloadlinks

import android.content.Context
import com.movieapp.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Lightweight resolver that extracts and validates direct downloadable video file URLs.
 * Bypasses countdown timers and prevents corrupt 78 kB HTML downloads.
 */
object DirectDownloadResolver {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Resolves a download link using the in-app WebView sniffer with native Chromium execution.
     * Captures direct video streams, authenticated cookies, and user-agent headers.
     */
    suspend fun resolveDirectUrl(context: Context, downloadLink: DownloadLinkDTO): Result<SniffResult> = withContext(Dispatchers.IO) {
        val rawUrl = downloadLink.url?.trim() ?: return@withContext Result.failure(Exception("Empty URL"))
        if (rawUrl.isBlank()) return@withContext Result.failure(Exception("Empty URL"))

        try {
            // Fast path: if rawUrl is already a verified direct media stream
            if (verifyDirectMediaUrl(rawUrl)) {
                return@withContext Result.success(SniffResult(directUrl = rawUrl))
            }

            // Web portals requiring dedicated apps or logins (e.g. Yoteshin, Drive) should not wait 18s in sniffer
            if (downloadLink.isYoteshin || isKnownWebPortal(rawUrl)) {
                return@withContext Result.failure(Exception("Portal requires dedicated app or browser authentication"))
            }

            // In-app WebView stream sniffer (handles MegaUp timer, JS tokens, Cloudflare)
            val sniffResult = WebViewDownloadSniffer.sniff(context, rawUrl)
            if (sniffResult.isSuccess) {
                return@withContext sniffResult
            }

            // Fallback: test static MegaUp link extraction
            if (rawUrl.contains("megaup.net", ignoreCase = true)) {
                val directCandidate = extractMegaUpDirectLink(rawUrl)
                if (directCandidate.isSuccess) {
                    val candidateUrl = directCandidate.getOrThrow()
                    if (verifyDirectMediaUrl(candidateUrl)) {
                        return@withContext Result.success(SniffResult(directUrl = candidateUrl))
                    }
                }
            }

            Result.failure(sniffResult.exceptionOrNull() ?: Exception("Direct video stream not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Legacy resolver overload without Context (used in unit tests / fast path).
     */
    suspend fun resolveDirectUrl(downloadLink: DownloadLinkDTO): Result<String> = withContext(Dispatchers.IO) {
        val rawUrl = downloadLink.url?.trim() ?: return@withContext Result.failure(Exception("Empty URL"))
        if (rawUrl.isBlank()) return@withContext Result.failure(Exception("Empty URL"))

        try {
            if (rawUrl.contains("megaup.net", ignoreCase = true)) {
                val directCandidate = extractMegaUpDirectLink(rawUrl)
                if (directCandidate.isSuccess) {
                    val candidateUrl = directCandidate.getOrThrow()
                    if (verifyDirectMediaUrl(candidateUrl)) {
                        Result.success(candidateUrl)
                    } else {
                        Result.failure(Exception("Protected stream requires browser to complete full download"))
                    }
                } else {
                    directCandidate
                }
            } else if (isKnownWebPortal(rawUrl)) {
                Result.failure(Exception("Portal requires authentication to download full movie"))
            } else {
                if (verifyDirectMediaUrl(rawUrl)) {
                    Result.success(rawUrl)
                } else {
                    Result.failure(Exception("URL is not a direct video stream"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves a download link specifically for copying to clipboard.
     * Preserves web page URLs for protected hosts (MegaUp, Yoteshin) so external downloaders (1DM, ADM)
     * can negotiate session authentication in their built-in browser instead of failing with 403 HTML pages.
     */
    suspend fun resolveDirectUrlForCopy(downloadLink: DownloadLinkDTO): Result<String> = withContext(Dispatchers.IO) {
        val rawUrl = downloadLink.url?.trim() ?: return@withContext Result.failure(Exception("Empty URL"))
        Result.success(rawUrl)
    }

    /**
     * Extracts proprietary yoteshin:// deep link from a yoteshinportal.cc landing page.
     */
    fun extractYoteshinDeepLink(pageUrl: String): String? {
        val request = Request.Builder()
            .url(pageUrl)
            .header("User-Agent", Constants.USER_AGENT)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null
                val pattern = Pattern.compile("(yoteshin://yoteshinportal\\.cc/drive\\?p=[^'\"\\s<>]+)")
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    matcher.group(1)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks whether a URL belongs to a known web portal that hosts HTML landing pages.
     */
    fun isKnownWebPortal(url: String): Boolean {
        val lower = url.lowercase().substringBefore("?")
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".avi")) return false
        return lower.contains("yoteshinportal.cc") ||
                lower.contains("usersdrive.com") ||
                lower.contains("bioscopeapp.com") ||
                lower.contains("drive.google.com/file")
    }

    /**
     * Extracts direct https://download.megaup.net/?url=... link from MegaUp HTML, bypassing the 5-second countdown timer.
     */
    fun extractMegaUpDirectLink(pageUrl: String): Result<String> {
        val request = Request.Builder()
            .url(pageUrl)
            .header("User-Agent", Constants.USER_AGENT)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP error ${response.code}"))
                }

                val html = response.body?.string() ?: return Result.failure(Exception("Empty response body"))

                // Match href='https://download.megaup.net/?url=...'
                val pattern = Pattern.compile("(https://download\\.megaup\\.net/\\?url=[^'\"\\s<>]+)")
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val directUrl = matcher.group(1) ?: ""
                    if (directUrl.isNotBlank()) return Result.success(directUrl)
                }

                // Fallback: check alternate download link pattern
                val fallbackPattern = Pattern.compile("href=[\"'](https?://[^\"']+/(?:download|get)/[^\"']+)[\"']")
                val fallbackMatcher = fallbackPattern.matcher(html)
                if (fallbackMatcher.find()) {
                    val fallbackUrl = fallbackMatcher.group(1) ?: ""
                    if (fallbackUrl.isNotBlank()) return Result.success(fallbackUrl)
                }

                Result.failure(Exception("Direct download link not found in page"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Preflight check to verify that a target URL actually serves an authentic video stream
     * and is NOT an HTML error or portal page.
     */
    fun verifyDirectMediaUrl(url: String): Boolean {
        // First try HTTP HEAD request
        val headRequest = Request.Builder()
            .url(url)
            .header("User-Agent", Constants.USER_AGENT)
            .head()
            .build()

        try {
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type")?.lowercase() ?: ""
                    if (isMediaType(contentType)) {
                        return true
                    }
                    if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                        return false
                    }
                }
            }
        } catch (_: Exception) {
            // If HEAD fails or is rejected (405), fallback to partial GET range check
        }

        // Fallback: Range GET request for first 1024 bytes
        val rangeRequest = Request.Builder()
            .url(url)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Range", "bytes=0-1024")
            .build()

        return try {
            client.newCall(rangeRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    val contentType = response.header("Content-Type")?.lowercase() ?: ""
                    isMediaType(contentType)
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Determines whether a Content-Type header corresponds to a valid video or binary stream.
     */
    fun isMediaType(contentType: String): Boolean {
        if (contentType.isBlank()) return false
        val lower = contentType.lowercase()
        if (lower.contains("text/html") || lower.contains("text/plain") || lower.contains("application/json")) {
            return false
        }
        return lower.startsWith("video/") ||
                lower.contains("application/octet-stream") ||
                lower.contains("application/x-matroska") ||
                lower.contains("binary/octet-stream") ||
                lower.contains("application/vnd.android.package-archive")
    }
}
