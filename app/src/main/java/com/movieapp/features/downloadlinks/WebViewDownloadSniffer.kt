package com.movieapp.features.downloadlinks

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Encapsulates an intercepted downloadable media stream with authenticated session headers.
 */
data class SniffResult(
    val directUrl: String,
    val cookies: String? = null,
    val userAgent: String? = null,
    val mimeType: String? = null,
    val contentDisposition: String? = null,
    val contentLength: Long = 0L,
    val referer: String? = null
)

/**
 * Native in-app stream interception engine.
 * Leverages Android's built-in Chromium WebView to execute client-side JavaScript,
 * count down waiting timers (e.g. MegaUp 5s), pass Cloudflare challenges, and
 * intercept direct media streams without requiring external Python or scraping dependencies.
 */
object WebViewDownloadSniffer {

    /**
     * Standard Chrome mobile user agent to prevent bot rejection on file hosters.
     */
    const val CHROME_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /**
     * Inspects a target URL and MIME type to determine if it represents an authentic video stream.
     */
    fun isMediaStream(url: String?, mimeType: String?): Boolean {
        val cleanUrl = url?.lowercase()?.trim() ?: ""
        val cleanMime = mimeType?.lowercase()?.trim() ?: ""

        // Immediate rejection for Cloudflare challenge pages, captcha frames, and web pages
        if (cleanUrl.contains("challenges.cloudflare.com") ||
            cleanUrl.contains("cf-chl") ||
            cleanUrl.contains("/cdn-cgi/") ||
            cleanUrl.contains("challenge-platform") ||
            cleanUrl.contains(".html") ||
            cleanUrl.contains(".php") ||
            cleanUrl.contains("download.megaup.net/?url=")
        ) {
            return false
        }

        // Rejection for text/json/html MIME types
        if (cleanMime == "text/html" || cleanMime == "text/plain" || cleanMime == "application/json") {
            return false
        }

        // Immediate MIME type matches
        if (cleanMime.startsWith("video/") ||
            cleanMime == "application/x-matroska" ||
            cleanMime == "binary/octet-stream"
        ) {
            return true
        }

        // Direct media extensions (ignoring query strings and preventing html pages)
        val pathWithoutQuery = cleanUrl.substringBefore("?")
        if (pathWithoutQuery.endsWith(".mp4") ||
            pathWithoutQuery.endsWith(".mkv") ||
            pathWithoutQuery.endsWith(".webm") ||
            pathWithoutQuery.endsWith(".avi")
        ) {
            return true
        }

        // Download endpoints often used by file hosts
        if (cleanUrl.contains("/download/") ||
            cleanUrl.contains("/get/") ||
            cleanUrl.contains("storage.googleapis.com")
        ) {
            if (cleanMime.contains("application/octet-stream") || cleanMime.startsWith("video/")) {
                return true
            }
        }

        return false
    }

    /**
     * Sniffs a URL for a direct media stream using an in-app WebView instance on the main thread.
     */
    suspend fun sniff(
        context: Context,
        pageUrl: String,
        timeoutMs: Long = 18000L
    ): Result<SniffResult> = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())
        val isCompleted = AtomicBoolean(false)

        mainHandler.post {
            var webView: WebView? = null

            fun cleanup() {
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                    webView = null
                } catch (_: Exception) {}
            }

            fun completeWithSuccess(result: SniffResult) {
                if (isCompleted.compareAndSet(false, true)) {
                    mainHandler.removeCallbacksAndMessages(null)
                    cleanup()
                    continuation.resume(Result.success(result))
                }
            }

            fun completeWithFailure(error: Throwable) {
                if (isCompleted.compareAndSet(false, true)) {
                    mainHandler.removeCallbacksAndMessages(null)
                    cleanup()
                    continuation.resume(Result.failure(error))
                }
            }

            // Timeout watchdog
            val timeoutRunnable = Runnable {
                completeWithFailure(Exception("Sniffer timed out after ${timeoutMs / 1000}s without intercepting media stream"))
            }
            mainHandler.postDelayed(timeoutRunnable, timeoutMs)

            continuation.invokeOnCancellation {
                mainHandler.removeCallbacks(timeoutRunnable)
                mainHandler.post { cleanup() }
            }

            try {
                val wv = WebView(context.applicationContext)
                webView = wv

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                try {
                    cookieManager.setAcceptThirdPartyCookies(wv, true)
                } catch (_: Exception) {}

                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = CHROME_USER_AGENT
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // 1. Intercept file downloads triggered via Content-Disposition or download headers
                wv.setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    val cookies = try {
                        cookieManager.getCookie(downloadUrl)
                    } catch (_: Exception) { null }

                    completeWithSuccess(
                        SniffResult(
                            directUrl = downloadUrl,
                            cookies = cookies,
                            userAgent = userAgent.takeIf { !it.isNullOrBlank() } ?: CHROME_USER_AGENT,
                            mimeType = mimetype,
                            contentDisposition = contentDisposition,
                            contentLength = contentLength
                        )
                    )
                })

                // 2. Intercept video navigation and network requests
                wv.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString() ?: return false
                        if (isMediaStream(reqUrl, null)) {
                            val cookies = try { cookieManager.getCookie(reqUrl) } catch (_: Exception) { null }
                            completeWithSuccess(
                                SniffResult(
                                    directUrl = reqUrl,
                                    cookies = cookies,
                                    userAgent = CHROME_USER_AGENT
                                )
                            )
                            return true
                        }
                        return false
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return null
                        if (isMediaStream(reqUrl, null)) {
                            val cookies = try { cookieManager.getCookie(reqUrl) } catch (_: Exception) { null }
                            completeWithSuccess(
                                SniffResult(
                                    directUrl = reqUrl,
                                    cookies = cookies,
                                    userAgent = CHROME_USER_AGENT
                                )
                            )
                        }
                        return null
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Trigger download buttons once page countdown timer elapses
                        val jsClickTrigger = """
                            (function() {
                                var interval = setInterval(function() {
                                    var btn = document.querySelector('#btn-download, a.btn-download, .download-timer a, a[href*="download.megaup.net"], a[href*="/download/"]');
                                    if (btn && btn.offsetParent !== null) {
                                        btn.click();
                                        clearInterval(interval);
                                    }
                                }, 1000);
                                setTimeout(function() { clearInterval(interval); }, 15000);
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(jsClickTrigger, null)
                    }
                }

                wv.loadUrl(pageUrl)
            } catch (e: Exception) {
                completeWithFailure(e)
            }
        }
    }
}