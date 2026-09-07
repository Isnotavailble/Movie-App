package com.movieapp.features.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.movieapp.data.local.DownloadDao
import com.movieapp.data.local.DownloadEntity
import com.movieapp.features.downloadlinks.DownloadManagerHelper
import com.movieapp.features.downloadlinks.DownloadRepository
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import com.movieapp.util.t
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(
    repository: DownloadRepository? = null,
    dao: DownloadDao? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadRepo = remember(repository, dao, context) {
        repository ?: if (dao != null) {
            DownloadRepository(context, dao)
        } else {
            DownloadRepository(context)
        }
    }

    LaunchedEffect(downloadRepo) {
        downloadRepo.startSync(this)
    }

    val activeDownloads by downloadRepo.activeDownloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val completedDownloads by downloadRepo.completedDownloads.collectAsStateWithLifecycle(initialValue = emptyList())

    val neoColors = MaterialTheme.neoColors
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Currently Downloading, 1: History

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Screen Header
        Text(
            text = t("downloads_title"),
            fontFamily = headerFontFamily(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = neoColors.textPrimary
        )
        Text(
            text = t("downloads_subtitle"),
            fontFamily = bodyFontFamily(),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = neoColors.textSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Tab Selector (Currently Downloading / History)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tabs = listOf(
                Pair(t("currently_downloading"), activeDownloads.size),
                Pair(t("download_history"), completedDownloads.size)
            )

            tabs.forEachIndexed { index, (label, count) ->
                val isSelected = selectedTab == index
                val bg = if (isSelected) neoColors.primary else neoColors.surfaceMuted

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                                    .background(bg, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                                    .background(bg, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                            }
                        )
                        .clickable { selectedTab = index }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = buttonFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = neoColors.textPrimary
                        )
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .background(neoColors.surface, RoundedCornerShape(12.dp))
                                    .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontFamily = badgeFontFamily(),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neoColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            // Active Downloads List
            if (activeDownloads.isEmpty()) {
                EmptyStateCard(
                    title = t("no_active_downloads"),
                    description = t("no_active_downloads_desc")
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activeDownloads, key = { it.downloadId }) { download ->
                        ActiveDownloadCard(
                            download = download,
                            onCancel = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    downloadRepo.cancelDownload(download.downloadId)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Completed Downloads List
            if (completedDownloads.isEmpty()) {
                EmptyStateCard(
                    title = t("no_download_history"),
                    description = t("no_download_history_desc")
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(completedDownloads, key = { it.downloadId }) { download ->
                        CompletedDownloadCard(
                            download = download,
                            onPlay = {
                                DownloadManagerHelper.openDownloadedFile(context, download)
                            },
                            onDelete = {
                                downloadRepo.deleteDownload(download)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(
    download: DownloadEntity,
    onCancel: () -> Unit
) {
    val neoColors = MaterialTheme.neoColors
    val cancelDownloadLabel = t("cancel_download")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = neoColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .padding(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        fontFamily = headerFontFamily(),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = neoColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = download.fileName,
                        fontFamily = badgeFontFamily(),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = neoColors.textSecondary
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Close,
                        contentDescription = t("delete_download"),
                        tint = neoColors.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            val progressFraction = if (download.totalBytes > 0L) {
                (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(4.dp)),
                color = neoColors.primary,
                trackColor = neoColors.surfaceMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${download.formattedDownloadedSize} / ${if (download.totalBytes > 0L) download.formattedTotalSize else "..."}",
                        fontFamily = badgeFontFamily(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = neoColors.textSecondary
                    )
                    Text(
                        text = "${download.progressPercentage}%",
                        fontFamily = badgeFontFamily(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = neoColors.secondary
                    )
                }

                // Prominent Neobrutalist Cancel Download Button
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                        .background(neoColors.error, RoundedCornerShape(8.dp))
                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancel)
                        .semantics {
                            role = Role.Button
                            contentDescription = cancelDownloadLabel
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Close,
                            contentDescription = null,
                            tint = neoColors.onError,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = t("cancel_download"),
                            fontFamily = com.movieapp.theme.buttonFontFamily(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.onError
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedDownloadCard(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val neoColors = MaterialTheme.neoColors

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = neoColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Movie Poster or File Icon
            if (!download.poster.isNullOrBlank()) {
                AsyncImage(
                    model = download.poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 46.dp, height = 64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    fontFamily = headerFontFamily(),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = neoColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(neoColors.secondary, RoundedCornerShape(4.dp))
                            .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = download.formattedTotalSize,
                            fontFamily = badgeFontFamily(),
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.textPrimary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play Button
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neoColors.primary,
                        contentColor = neoColors.textPrimary
                    ),
                    modifier = Modifier.neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Browse,
                        contentDescription = null,
                        tint = neoColors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = t("open_movie"),
                        fontFamily = buttonFontFamily(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = neoColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Close,
                        contentDescription = t("delete_download"),
                        tint = neoColors.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String
) {
    val neoColors = MaterialTheme.neoColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surfaceMuted, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = NeubrutalismIcons.Download,
                contentDescription = null,
                tint = neoColors.textSecondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontFamily = headerFontFamily(),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = neoColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = bodyFontFamily(),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = neoColors.textSecondary
            )
        }
    }
}
