package com.movieapp.features.downloadlinks

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movieapp.theme.NeoBlack
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import com.movieapp.util.LocalizationManager
import com.movieapp.util.t
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadLinksBottomSheet(
    title: String,
    downloadLinks: List<DownloadLinkDTO>,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val neoColors = MaterialTheme.neoColors
    val coroutineScope = rememberCoroutineScope()

    // Resolution Filter: Extract distinct resolutions (e.g. 720p, 1080p, 4K)
    val availableResolutions = remember(downloadLinks) {
        listOf("All") + downloadLinks.mapNotNull { it.resolution?.trim() }.filter { it.isNotBlank() }.distinct()
    }
    var selectedResolution by remember { mutableStateOf("All") }

    val filteredLinks = remember(downloadLinks, selectedResolution) {
        if (selectedResolution == "All") {
            downloadLinks
        } else {
            downloadLinks.filter { it.resolution?.trim().equals(selectedResolution, ignoreCase = true) }
        }
    }

    var resolvingLinkId by remember { mutableStateOf<Long?>(null) }
    var resolvingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var interactiveLink by remember { mutableStateOf<DownloadLinkDTO?>(null) }
    var fallbackLink by remember { mutableStateOf<DownloadLinkDTO?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            resolvingJob?.cancel()
            resolvingJob = null
            resolvingLinkId = null
            interactiveLink = null
            fallbackLink = null
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = neoColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("download_links"),
                        fontFamily = com.movieapp.theme.headerFontFamily(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = neoColors.textPrimary
                    )
                    Text(
                        text = title,
                        fontFamily = bodyFontFamily(),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = neoColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                }

                if (downloadLinks.size > 1) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                            .background(neoColors.primary, RoundedCornerShape(8.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                val allUrls = downloadLinks.mapNotNull { it.url }.filter { it.isNotBlank() }.joinToString("\n")
                                DownloadManagerHelper.copyLinkToClipboard(context, title, allUrls)
                                Toast.makeText(context, LocalizationManager.getString("all_links_copied"), Toast.LENGTH_SHORT).show()
                            }
                            .semantics { role = Role.Button }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = NeubrutalismIcons.Copy,
                                contentDescription = null,
                                tint = neoColors.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = t("copy_all_links"),
                                fontFamily = com.movieapp.theme.buttonFontFamily(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = neoColors.onPrimary
                            )
                        }
                    }
                }
            }

            // Resolution Filter Chips (720p / 1080p / 4K / All)
            if (availableResolutions.size > 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 14.dp)
                ) {
                    availableResolutions.forEach { res ->
                        val isSelected = res == selectedResolution
                        val label = if (res == "All") t("res_all") else res
                        val bg = if (isSelected) neoColors.primary else neoColors.surfaceMuted

                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                                            .background(bg, RoundedCornerShape(8.dp))
                                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                            .background(bg, RoundedCornerShape(8.dp))
                                            .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                                    }
                                )
                                .clickable { selectedResolution = res }
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = com.movieapp.theme.buttonFontFamily(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) neoColors.onPrimary else neoColors.textPrimary
                            )
                        }
                    }
                }
            }

            if (filteredLinks.isEmpty()) {
                Text(
                    text = t("no_download_links"),
                    fontFamily = bodyFontFamily(),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = neoColors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(filteredLinks) { link ->
                        val isResolving = resolvingLinkId == link.id

                        DownloadLinkCard(
                            link = link,
                            isResolving = isResolving,
                            onCancelResolve = {
                                resolvingJob?.cancel()
                                resolvingJob = null
                                resolvingLinkId = null
                                Toast.makeText(context, LocalizationManager.getString("download_cancelled"), Toast.LENGTH_SHORT).show()
                            },
                            onOpenLink = {
                                if (link.isTelegram) {
                                    // tg:// protocol instant launch
                                    DownloadManagerHelper.openTelegram(context, link.url ?: "")
                                } else if (link.isYoteshin) {
                                    // Yoteshin portal / Yoteshin Drive deep link
                                    val rawUrl = link.url?.trim() ?: ""
                                    val isInstalled = DownloadManagerHelper.isYoteshinDriveInstalled(context)
                                    if (isInstalled) {
                                        coroutineScope.launch {
                                            val deepLink = DownloadManagerHelper.convertToYoteshinDeepLink(rawUrl)
                                                ?: DirectDownloadResolver.extractYoteshinDeepLink(rawUrl)
                                                ?: rawUrl
                                            val launched = DownloadManagerHelper.openYoteshinDrive(context, deepLink)
                                            if (!launched) {
                                                fallbackLink = link
                                            }
                                        }
                                    } else {
                                        // App not installed: show dedicated dialog immediately without 18s wait!
                                        fallbackLink = link
                                    }
                                } else if (link.url?.contains("megaup.net", ignoreCase = true) == true) {
                                    // MegaUp requires interactive Cloudflare Turnstile human verification
                                    interactiveLink = link
                                } else {
                                    // In-App Direct Download with Ad/Timer Bypass Sniffer
                                    val resolvingMsg = LocalizationManager.getString("resolving_link")
                                    resolvingJob?.cancel()
                                    resolvingJob = coroutineScope.launch {
                                        resolvingLinkId = link.id
                                        Toast.makeText(context, resolvingMsg, Toast.LENGTH_SHORT).show()
                                        val result = DirectDownloadResolver.resolveDirectUrl(context, link)
                                        resolvingLinkId = null
                                        resolvingJob = null
                                        result.onSuccess { sniffResult ->
                                            DownloadManagerHelper.startMovieDownloadService(
                                                context = context,
                                                title = title,
                                                directUrl = sniffResult.directUrl,
                                                cookies = sniffResult.cookies,
                                                userAgent = sniffResult.userAgent,
                                                referer = sniffResult.referer ?: link.url
                                            )
                                        }.onFailure {
                                            // Fallback dialog giving user the 2 explicit choices
                                            fallbackLink = link
                                        }
                                    }
                                }
                            },
                            onCopyLink = {
                                coroutineScope.launch {
                                    val rawUrl = link.url?.trim() ?: return@launch
                                    val resolved = DirectDownloadResolver.resolveDirectUrlForCopy(link)
                                    val copyTarget = resolved.getOrDefault(rawUrl)
                                    DownloadManagerHelper.copyLinkToClipboard(context, link.cleanServerName, copyTarget)
                                }
                            },
                            onOpenInDownloader = if (!link.isTelegram) {
                                {
                                    val rawUrl = link.url?.trim() ?: ""
                                    val launched = DownloadManagerHelper.openInExternalDownloader(context, rawUrl)
                                    if (!launched) {
                                        Toast.makeText(context, "1DM/ADM not installed. Opening in browser...", Toast.LENGTH_SHORT).show()
                                        DownloadManagerHelper.openExternalLink(context, link)
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Interactive Cloudflare Turnstile human verification sheet
    interactiveLink?.let { targetLink ->
        InteractiveDownloadSheet(
            link = targetLink,
            title = title,
            onStreamResolved = { sniffResult ->
                interactiveLink = null
                DownloadManagerHelper.startMovieDownloadService(
                    context = context,
                    title = title,
                    directUrl = sniffResult.directUrl,
                    cookies = sniffResult.cookies,
                    userAgent = sniffResult.userAgent,
                    referer = sniffResult.referer ?: targetLink.url
                )
            },
            onDismissWithFallback = {
                interactiveLink = null
                fallbackLink = targetLink
            }
        )
    }

    // 2-choice Fallback Dialog (Open in Browser or Copy Link for 1DM/ADM)
    fallbackLink?.let { targetLink ->
        DownloadFallbackDialog(
            link = targetLink,
            onOpenInBrowser = {
                val lk = targetLink
                fallbackLink = null
                DownloadManagerHelper.openExternalLink(context, lk)
            },
            onCopyLink = {
                val lk = targetLink
                fallbackLink = null
                val rawUrl = lk.url ?: ""
                DownloadManagerHelper.copyLinkToClipboard(context, lk.cleanServerName, rawUrl)
                DownloadManagerHelper.openInExternalDownloader(context, rawUrl)
            },
            onOpenYoteshin = if (targetLink.isYoteshin) {
                {
                    val lk = targetLink
                    fallbackLink = null
                    val rawUrl = lk.url ?: ""
                    coroutineScope.launch {
                        val deepLink = DownloadManagerHelper.convertToYoteshinDeepLink(rawUrl)
                            ?: DirectDownloadResolver.extractYoteshinDeepLink(rawUrl)
                            ?: rawUrl

                        val launched = DownloadManagerHelper.openYoteshinDrive(context, deepLink)
                        if (!launched) {
                            DownloadManagerHelper.openYoteshinPlayStore(context)
                        }
                    }
                }
            } else null,
            onDismiss = {
                fallbackLink = null
            }
        )
    }
}

@Composable
fun DownloadLinkCard(
    link: DownloadLinkDTO,
    isResolving: Boolean = false,
    onCancelResolve: () -> Unit = {},
    onOpenLink: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenInDownloader: (() -> Unit)? = null
) {
    val neoColors = MaterialTheme.neoColors
    val copyLabel = t("copy_link")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // --- TOP ROW: Badges & File Size ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Badges (Server, Resolution, Quality)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Server Badge
                    val serverBg = if (link.isTelegram) neoColors.tertiary else if (link.isYoteshin) neoColors.secondary else neoColors.primary.copy(alpha = 0.2f)
                    val serverTextColor = if (link.isYoteshin) neoColors.onSecondary else neoColors.textPrimary
                    Box(
                        modifier = Modifier
                            .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                            .background(serverBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = link.cleanServerName,
                            fontFamily = buttonFontFamily(),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = serverTextColor,
                            maxLines = 1,
                            softWrap = false
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
                                color = neoColors.onSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Quality Badge (e.g. WEB-DL, BluRay)
                    link.quality?.takeIf { it.isNotBlank() && !it.equals(link.resolution, ignoreCase = true) }?.let { qual ->
                        Box(
                            modifier = Modifier
                                .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                                .background(neoColors.surfaceMuted, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = qual,
                                fontFamily = badgeFontFamily(),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = neoColors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Right: File Size
                link.size?.takeIf { it.isNotBlank() }?.let { sizeStr ->
                    Box(
                        modifier = Modifier
                            .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                            .background(neoColors.surfaceMuted, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = sizeStr,
                            fontFamily = badgeFontFamily(),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- BOTTOM ROW: Action Buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // External Downloader (1DM / ADM) Icon Button
                if (!link.isTelegram && onOpenInDownloader != null) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 42.dp, minHeight = 40.dp)
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                            .background(neoColors.surface, RoundedCornerShape(8.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenInDownloader)
                            .semantics {
                                role = Role.Button
                                contentDescription = "1DM / ADM"
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = neoColors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Copy Link Icon Button
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 42.dp, minHeight = 40.dp)
                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                    .background(neoColors.surface, RoundedCornerShape(8.dp))
                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                    .clickable(onClick = onCopyLink)
                    .semantics {
                        role = Role.Button
                        contentDescription = copyLabel
                    }
                    .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Copy,
                        contentDescription = null,
                        tint = neoColors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Main CTA Button (Direct Download, Telegram, or Yoteshin) or Resolving + Cancel State
                val btnBg = if (link.isTelegram) neoColors.tertiary else if (link.isYoteshin) neoColors.secondary else neoColors.primary
                val btnContent = if (link.isTelegram) NeoBlack else if (link.isYoteshin) neoColors.onSecondary else neoColors.onPrimary

                if (isResolving) {
                    // Resolving progress container + prominent CANCEL button
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp)
                                .background(neoColors.surfaceMuted, RoundedCornerShape(8.dp))
                                .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = neoColors.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = t("resolving_link"),
                                    fontFamily = com.movieapp.theme.buttonFontFamily(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neoColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Explicit Cancel Button during resolve
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                                .background(neoColors.error, RoundedCornerShape(8.dp))
                                .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                                .clickable(onClick = onCancelResolve)
                                .semantics { role = Role.Button }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = NeubrutalismIcons.Close,
                                    contentDescription = null,
                                    tint = neoColors.onError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = t("cancel"),
                                    fontFamily = com.movieapp.theme.buttonFontFamily(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neoColors.onError
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp)
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                            .background(btnBg, RoundedCornerShape(8.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenLink)
                            .semantics { role = Role.Button }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (link.isTelegram) NeubrutalismIcons.Telegram else NeubrutalismIcons.Download,
                                contentDescription = null,
                                tint = btnContent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (link.isTelegram) t("telegram_action") else if (link.isYoteshin) t("yoteshin_action") else t("direct_download"),
                                fontFamily = com.movieapp.theme.buttonFontFamily(),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = btnContent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
