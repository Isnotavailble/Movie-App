package com.movieapp.features.downloadlinks

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interactive in-app bottom sheet hosting an embedded WebView for links protected
 * by Cloudflare Turnstile human verification (e.g. MegaUp).
 *
 * Allows the user to tap "Verify you are human". Once passed, the real CDN video stream
 * is intercepted via DownloadListener / WebViewClient, cookies are captured, the sheet
 * automatically dismisses, and the native DownloadManager takes over.
 *
 * If dismissed or cancelled, delegates to onDismissWithFallback to offer:
 * 1. Open in Browser
 * 2. Copy Link for 1DM / ADM
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveDownloadSheet(
    link: DownloadLinkDTO,
    title: String,
    onStreamResolved: (SniffResult) -> Unit,
    onDismissWithFallback: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val neoColors = MaterialTheme.neoColors
    val isResolved = remember { AtomicBoolean(false) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissWithFallback,
        sheetState = sheetState,
        containerColor = neoColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Server Badge
                        Box(
                            modifier = Modifier
                                .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                                .background(neoColors.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = link.cleanServerName,
                                fontFamily = buttonFontFamily(),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = neoColors.textPrimary
                            )
                        }

                        // Resolution Badge
                        link.resolution?.takeIf { it.isNotBlank() }?.let { res ->
                            Box(
                                modifier = Modifier
                                    .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                                    .background(neoColors.secondary, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = res,
                                    fontFamily = badgeFontFamily(),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neoColors.onSecondary
                                )
                            }
                        }

                        // Size Badge
                        link.size?.takeIf { it.isNotBlank() }?.let { sizeStr ->
                            Text(
                                text = "• $sizeStr",
                                fontFamily = badgeFontFamily(),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = neoColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title,
                        fontFamily = headerFontFamily(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = neoColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(neoColors.surfaceMuted, RoundedCornerShape(8.dp))
                        .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                        .clickable { onDismissWithFallback() }
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Close,
                        contentDescription = "Close",
                        tint = neoColors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- USER INSTRUCTION BANNER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(neoColors.surfaceMuted, RoundedCornerShape(10.dp))
                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Cloudflare စစ်ဆေးမှု (I am human) ပေါ်လာပါက နှိပ်ပေးပါ။",
                        fontFamily = bodyFontFamily(),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = neoColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "အတည်ပြုပြီးပါက ဗီဒီယိုကို ဖုန်းထဲသို့ အလိုအလျောက် စတင်ဒေါင်းလုဒ်ဆွဲပေးပါမည်။",
                        fontFamily = bodyFontFamily(),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = neoColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Loading Progress Bar
            if (pageProgress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = neoColors.primary,
                    trackColor = neoColors.surfaceMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- EMBEDDED INTERACTIVE WEBVIEW ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                    .background(neoColors.surface, RoundedCornerShape(12.dp))
                    .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewRef = this

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            try {
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                            } catch (_: Exception) {}

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                userAgentString = WebViewDownloadSniffer.CHROME_USER_AGENT
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            // 1. Intercept direct file download trigger from server
                            setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                                val cleanUrl = downloadUrl.lowercase()

                                // If this is an intermediate MegaUp challenge URL, navigate into it to display Turnstile Captcha
                                if (cleanUrl.contains("download.megaup.net/?url=") ||
                                    (cleanUrl.contains("megaup.net") && (mimetype?.contains("text/html") == true || (contentLength in 1..500_000)))
                                ) {
                                    loadUrl(downloadUrl)
                                    return@DownloadListener
                                }

                                // Only intercept if it is an authentic video stream or binary payload > 5 MB
                                val isAuthenticMedia = WebViewDownloadSniffer.isMediaStream(downloadUrl, mimetype) ||
                                        mimetype?.startsWith("video/") == true ||
                                        mimetype == "application/x-matroska" ||
                                        mimetype == "binary/octet-stream" ||
                                        contentLength > 5 * 1024 * 1024L

                                if (isAuthenticMedia) {
                                    if (isResolved.compareAndSet(false, true)) {
                                        val pageCookies = try { link.url?.let { cookieManager.getCookie(it) } } catch (_: Exception) { null }
                                        val dlCookies = try { cookieManager.getCookie(downloadUrl) } catch (_: Exception) { null }
                                        val mergedCookies = listOfNotNull(pageCookies, dlCookies).flatMap { it.split("; ") }.distinct().joinToString("; ").takeIf { it.isNotBlank() }

                                        val result = SniffResult(
                                            directUrl = downloadUrl,
                                            cookies = mergedCookies,
                                            userAgent = userAgent.takeIf { !it.isNullOrBlank() } ?: WebViewDownloadSniffer.CHROME_USER_AGENT,
                                            mimeType = mimetype,
                                            contentDisposition = contentDisposition,
                                            contentLength = contentLength,
                                            referer = link.url
                                        )
                                        Handler(Looper.getMainLooper()).post {
                                            onStreamResolved(result)
                                        }
                                    }
                                } else {
                                    // Not verified media; continue loading in WebView
                                    loadUrl(downloadUrl)
                                }
                            })

                            // 2. Track page loading progress & eagerly skip countdown timer
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress / 100f
                                    if (newProgress >= 40) {
                                        val fastTimerSkip = """
                                            (function() {
                                                if (typeof seconds !== 'undefined' && typeof display === 'function') {
                                                    seconds = 0;
                                                    display();
                                                    if (typeof countdownTimer !== 'undefined') clearInterval(countdownTimer);
                                                }
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(fastTimerSkip, null)
                                    }
                                }
                            }

                            // 3. Intercept media redirects & inject countdown timer bypass
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return false
                                    val cleanUrl = reqUrl.lowercase()

                                    // Let intermediate Turnstile challenge pages load inside the WebView
                                    if (cleanUrl.contains("download.megaup.net/?url=")) {
                                        return false
                                    }

                                    if (WebViewDownloadSniffer.isMediaStream(reqUrl, null)) {
                                        if (isResolved.compareAndSet(false, true)) {
                                            val pageCookies = try { link.url?.let { cookieManager.getCookie(it) } } catch (_: Exception) { null }
                                            val dlCookies = try { cookieManager.getCookie(reqUrl) } catch (_: Exception) { null }
                                            val mergedCookies = listOfNotNull(pageCookies, dlCookies).flatMap { it.split("; ") }.distinct().joinToString("; ").takeIf { it.isNotBlank() }

                                            val result = SniffResult(
                                                directUrl = reqUrl,
                                                cookies = mergedCookies,
                                                userAgent = WebViewDownloadSniffer.CHROME_USER_AGENT,
                                                referer = link.url
                                            )
                                            Handler(Looper.getMainLooper()).post {
                                                onStreamResolved(result)
                                            }
                                        }
                                        return true
                                    }
                                    return false
                                }

                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: return null
                                    val cleanUrl = reqUrl.lowercase()

                                    if (cleanUrl.contains("download.megaup.net/?url=")) {
                                        return null
                                    }

                                    if (WebViewDownloadSniffer.isMediaStream(reqUrl, null)) {
                                        if (isResolved.compareAndSet(false, true)) {
                                            val pageCookies = try { link.url?.let { cookieManager.getCookie(it) } } catch (_: Exception) { null }
                                            val dlCookies = try { cookieManager.getCookie(reqUrl) } catch (_: Exception) { null }
                                            val mergedCookies = listOfNotNull(pageCookies, dlCookies).flatMap { it.split("; ") }.distinct().joinToString("; ").takeIf { it.isNotBlank() }

                                            val result = SniffResult(
                                                directUrl = reqUrl,
                                                cookies = mergedCookies,
                                                userAgent = WebViewDownloadSniffer.CHROME_USER_AGENT,
                                                referer = link.url
                                            )
                                            Handler(Looper.getMainLooper()).post {
                                                onStreamResolved(result)
                                            }
                                        }
                                    }
                                    return null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Robust countdown timer bypass: zeroes out countdown and reveals download button immediately
                                    val jsTimerBypass = """
                                        (function() {
                                            function bypass() {
                                                if (typeof seconds !== 'undefined' && typeof display === 'function') {
                                                    seconds = 0;
                                                    display();
                                                    if (typeof countdownTimer !== 'undefined') {
                                                        clearInterval(countdownTimer);
                                                    }
                                                }
                                                var btn = document.querySelector('.download-timer a, #btn-download, a.btn-download');
                                                if (btn) {
                                                    btn.classList.remove('disabled');
                                                    btn.removeAttribute('disabled');
                                                    btn.style.display = 'block';
                                                    btn.style.pointerEvents = 'auto';
                                                }
                                            }
                                            bypass();
                                            var bypassInterval = setInterval(bypass, 150);
                                            setTimeout(function() { clearInterval(bypassInterval); }, 6000);
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(jsTimerBypass, null)
                                }
                            }

                            loadUrl(link.url ?: "")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(350.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- FOOTER FALLBACK ACTION ---
            Button(
                onClick = onDismissWithFallback,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = neoColors.surfaceMuted,
                    contentColor = neoColors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel & Choose Other Options (Browser / 1DM)",
                        fontFamily = buttonFontFamily(),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
