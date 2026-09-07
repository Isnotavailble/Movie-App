package com.movieapp.features.downloadlinks

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.movieapp.theme.TelegramBlue
import com.movieapp.theme.WebWhite
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import com.movieapp.util.t
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interactive download screen hosted directly inside the main drawer Column.
 * Retains the drawer's rounded corners, soft gray drag handle, and padding.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveDownloadContent(
    link: DownloadLinkDTO,
    title: String,
    onBack: () -> Unit,
    onStreamResolved: (SniffResult) -> Unit,
    onDismissWithFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neoColors = MaterialTheme.neoColors
    val isResolved = remember { AtomicBoolean(false) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(link) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // --- 1. HEADER ROW: Back Button, Badges, Title, Close Button ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Back to Links Button
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                        .background(neoColors.primary, RoundedCornerShape(8.dp))
                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .semantics { role = Role.Button }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.ArrowLeft,
                            contentDescription = t("back_to_links"),
                            tint = neoColors.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = t("back_to_links"),
                            fontFamily = buttonFontFamily(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.onPrimary
                        )
                    }
                }

                // Server Badge
                val serverBg = if (link.isTelegram) TelegramBlue else if (link.isYoteshin) neoColors.secondary else neoColors.primary.copy(alpha = 0.2f)
                val serverTextColor = if (link.isTelegram) WebWhite else if (link.isYoteshin) neoColors.onSecondary else neoColors.textPrimary
                Box(
                    modifier = Modifier
                        .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                        .background(serverBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = link.cleanServerName,
                        fontFamily = buttonFontFamily(),
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = serverTextColor
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
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.onSecondary
                        )
                    }
                }

                // Movie Title
                Text(
                    text = title,
                    fontFamily = headerFontFamily(),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = neoColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                    .background(neoColors.surfaceMuted, RoundedCornerShape(8.dp))
                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
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

        // --- 2. THIN LOADING PROGRESS INDICATOR ---
        if (pageProgress in 0.01f..0.99f) {
            LinearProgressIndicator(
                progress = { pageProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .height(3.dp),
                color = neoColors.primary,
                trackColor = neoColors.surfaceMuted
            )
        }

        // --- 3. WEBVIEW CONTAINER (Full width inside drawer padding with rounded corners and border) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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

                        isNestedScrollingEnabled = false
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = false

                        setOnTouchListener { view, motionEvent ->
                            when (motionEvent.actionMasked) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    view.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            userAgentString = WebViewDownloadSniffer.CHROME_USER_AGENT
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                            val cleanUrl = downloadUrl.lowercase()

                            val isPortalOrChallenge = downloadUrl.equals(link.url, ignoreCase = true) ||
                                    cleanUrl.contains("download.megaup.net") ||
                                    (cleanUrl.contains("megaup.net") && !cleanUrl.matches(Regex("""^https?://(?:s\d+|storage)\.megaup\.net/.*"""))) ||
                                    ((cleanUrl.contains("://usersdrive.com/") || cleanUrl.contains("://www.usersdrive.com/")) && !cleanUrl.contains("/d/") && !cleanUrl.contains("/files/")) ||
                                    (cleanUrl.contains("megaup.net") && (mimetype?.contains("text/html") == true || (contentLength in 1..500_000))) ||
                                    (cleanUrl.contains("usersdrive.com") && (mimetype?.contains("text/html") == true || (contentLength in 1..500_000)))

                            if (isPortalOrChallenge) {
                                loadUrl(downloadUrl)
                                return@DownloadListener
                            }

                            val isAuthenticMedia = !downloadUrl.equals(link.url, ignoreCase = true) &&
                                    (WebViewDownloadSniffer.isMediaStream(downloadUrl, mimetype) ||
                                    mimetype?.startsWith("video/") == true ||
                                    mimetype == "application/x-matroska" ||
                                    mimetype == "binary/octet-stream" ||
                                    contentLength > 5 * 1024 * 1024L)

                            if (isAuthenticMedia) {
                                if (isResolved.compareAndSet(false, true)) {
                                    val pageCookies = try { link.url?.let { cookieManager.getCookie(it) } } catch (_: Exception) { null }
                                    val rootCookies = try {
                                        if (cleanUrl.contains("usersdrive.com")) {
                                            cookieManager.getCookie("https://usersdrive.com")
                                        } else {
                                            cookieManager.getCookie("https://megaup.net")
                                        }
                                    } catch (_: Exception) { null }
                                    val dlHostCookies = try { cookieManager.getCookie("https://download.megaup.net") } catch (_: Exception) { null }
                                    val dlCookies = try { cookieManager.getCookie(downloadUrl) } catch (_: Exception) { null }
                                    val mergedCookies = listOfNotNull(pageCookies, rootCookies, dlHostCookies, dlCookies)
                                        .flatMap { it.split("; ") }
                                        .distinct()
                                        .joinToString("; ")
                                        .takeIf { it.isNotBlank() }

                                    val result = SniffResult(
                                        directUrl = downloadUrl,
                                        cookies = mergedCookies,
                                        userAgent = userAgent ?: WebViewDownloadSniffer.CHROME_USER_AGENT,
                                        mimeType = mimetype,
                                        contentDisposition = contentDisposition,
                                        contentLength = contentLength,
                                        referer = link.url
                                    )
                                    Handler(Looper.getMainLooper()).post {
                                        onStreamResolved(result)
                                    }
                                }
                            }
                        })

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress / 100f
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                val headers = request.requestHeaders
                                val accept = headers["Accept"]?.lowercase() ?: ""
                                val cleanUrl = reqUrl.lowercase()

                                val isPortalOrChallenge = reqUrl.equals(link.url, ignoreCase = true) ||
                                        cleanUrl.contains("download.megaup.net") ||
                                        (cleanUrl.contains("megaup.net") && !cleanUrl.matches(Regex("""^https?://(?:s\d+|storage)\.megaup\.net/.*"""))) ||
                                        ((cleanUrl.contains("://usersdrive.com/") || cleanUrl.contains("://www.usersdrive.com/")) && !cleanUrl.contains("/d/") && !cleanUrl.contains("/files/"))

                                if (!isPortalOrChallenge &&
                                    !reqUrl.equals(link.url, ignoreCase = true) &&
                                    (WebViewDownloadSniffer.isMediaStream(reqUrl, accept) ||
                                    accept.startsWith("video/") ||
                                    cleanUrl.endsWith(".mp4") ||
                                    cleanUrl.endsWith(".mkv"))
                                ) {
                                    if (isResolved.compareAndSet(false, true)) {
                                        val pageCookies = try { link.url?.let { cookieManager.getCookie(it) } } catch (_: Exception) { null }
                                        val rootCookies = try {
                                            if (cleanUrl.contains("usersdrive.com")) {
                                                cookieManager.getCookie("https://usersdrive.com")
                                            } else {
                                                cookieManager.getCookie("https://megaup.net")
                                            }
                                        } catch (_: Exception) { null }
                                        val dlHostCookies = try { cookieManager.getCookie("https://download.megaup.net") } catch (_: Exception) { null }
                                        val dlCookies = try { cookieManager.getCookie(reqUrl) } catch (_: Exception) { null }
                                        val mergedCookies = listOfNotNull(pageCookies, rootCookies, dlHostCookies, dlCookies)
                                            .flatMap { it.split("; ") }
                                            .distinct()
                                            .joinToString("; ")
                                            .takeIf { it.isNotBlank() }

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
                                    return WebResourceResponse("video/mp4", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                                }
                                return null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val jsAutoBypass = """
                                    (function() {
                                        var autoClicked = false;
                                        var formSubmitted = false;

                                        function runBypass() {
                                            try {
                                                var turnstileFrames = document.querySelectorAll('iframe[src*="cloudflare"], iframe[src*="turnstile"], iframe[src*="challenges"]');
                                                turnstileFrames.forEach(function(frame) {
                                                    try {
                                                        var frameDoc = frame.contentDocument || frame.contentWindow.document;
                                                        var chk = frameDoc.querySelector('input[type="checkbox"], .ctp-checkbox-label, #cf-stage, .mark');
                                                        if (chk) {
                                                            chk.click();
                                                        }
                                                    } catch(e) {}
                                                });
                                                var directTurnstile = document.querySelector('.cf-turnstile, #cf-turnstile, input[type="checkbox"]');
                                                if (directTurnstile) {
                                                    directTurnstile.click();
                                                }
                                            } catch(e) {}

                                            if (typeof seconds !== 'undefined') {
                                                seconds = 0;
                                                if (typeof display === 'function') display();
                                                if (typeof countdownTimer !== 'undefined') clearInterval(countdownTimer);
                                            }
                                            if (typeof countdown !== 'undefined') countdown = 0;
                                            if (typeof count !== 'undefined') count = 0;
                                            if (typeof c !== 'undefined') c = 0;

                                            var megaUpBtn = document.querySelector('.download-timer a, #btn-download, a.btn-download');
                                            if (megaUpBtn) {
                                                megaUpBtn.classList.remove('disabled');
                                                megaUpBtn.removeAttribute('disabled');
                                                megaUpBtn.style.display = 'block';
                                                megaUpBtn.style.pointerEvents = 'auto';

                                                if (!autoClicked) {
                                                    var href = megaUpBtn.getAttribute('href');
                                                    if (href && href.indexOf('download.megaup.net') !== -1) {
                                                        autoClicked = true;
                                                        megaUpBtn.click();
                                                    }
                                                }
                                            }

                                            if (!formSubmitted) {
                                                var freeBtn = document.querySelector('input[name="method_free"], button[name="method_free"], input[value*="Free Download"], input[value*="Free"]');
                                                if (freeBtn) {
                                                    formSubmitted = true;
                                                    freeBtn.click();
                                                    return;
                                                }
                                            }

                                            var usersDriveDlBtn = document.querySelector('#downloadbtn, .downloadbtn, a[href*="/d/"], a[href*="usersdrive.com/d/"]');
                                            if (usersDriveDlBtn) {
                                                usersDriveDlBtn.classList.remove('disabled');
                                                usersDriveDlBtn.removeAttribute('disabled');
                                                usersDriveDlBtn.style.display = 'block';
                                                usersDriveDlBtn.style.pointerEvents = 'auto';

                                                if (!autoClicked) {
                                                    var dlHref = usersDriveDlBtn.getAttribute('href');
                                                    if (dlHref && (dlHref.indexOf('/d/') !== -1 || dlHref.indexOf('.mp4') !== -1 || dlHref.indexOf('.mkv') !== -1)) {
                                                        autoClicked = true;
                                                        usersDriveDlBtn.click();
                                                    }
                                                }
                                            }
                                            try {
                                                var metaViewport = document.querySelector('meta[name="viewport"]');
                                                if (!metaViewport) {
                                                    metaViewport = document.createElement('meta');
                                                    metaViewport.name = 'viewport';
                                                    document.head.appendChild(metaViewport);
                                                }
                                                metaViewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes';
                                            } catch(e) {}

                                            try {
                                                var actionTarget = document.querySelector('.cf-turnstile, iframe[src*="turnstile"], #downloadbtn, .download-timer, a.btn-download');
                                                if (actionTarget) {
                                                    actionTarget.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                                }
                                            } catch(e) {}
                                        }

                                        runBypass();
                                        var intervalId = setInterval(runBypass, 300);
                                        setTimeout(function() { clearInterval(intervalId); }, 10000);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(jsAutoBypass, null)
                            }
                        }

                        loadUrl(link.url ?: "")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
