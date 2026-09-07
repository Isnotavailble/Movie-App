package com.movieapp.features.moviedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.movieapp.features.downloadlinks.DownloadLinkDTO
import com.movieapp.features.downloadlinks.DownloadLinksBottomSheet
import com.movieapp.theme.NeoBlack
import com.movieapp.theme.NeoButton
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import com.movieapp.util.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    viewModel: MovieDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val neoColors = MaterialTheme.neoColors
    val context = androidx.compose.ui.platform.LocalContext.current

    val pullRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        androidx.compose.runtime.LaunchedEffect(true) {
            viewModel.retry()
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            pullRefreshState.endRefresh()
        }
    }

    var showDownloadSheet by remember { mutableStateOf(false) }
    var downloadSheetTitle by remember { mutableStateOf("") }
    var currentDownloadLinks by remember { mutableStateOf<List<DownloadLinkDTO>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDownloadSheet) {
        DownloadLinksBottomSheet(
            title = downloadSheetTitle,
            downloadLinks = currentDownloadLinks,
            sheetState = sheetState,
            onDismiss = { showDownloadSheet = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                        .background(neoColors.surface, RoundedCornerShape(10.dp))
                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                        .clickable(onClick = onBackClick)
                        .semantics { role = Role.Button }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.ArrowLeft,
                            contentDescription = t("back_to_list"),
                            tint = neoColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = t("back_to_list"),
                            fontFamily = buttonFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.textPrimary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bookmark Toggle Button
                    val isBookmarked = uiState.isBookmarked
                    val bookmarkBg = if (isBookmarked) neoColors.primary else neoColors.surface
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                            .background(bookmarkBg, RoundedCornerShape(8.dp))
                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.toggleBookmark()
                                val msgKey = if (!isBookmarked) "bookmark_added" else "bookmark_removed"
                                val toastMsg = com.movieapp.util.LocalizationManager.getString(msgKey)
                                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .semantics {
                                role = Role.Button
                                selected = isBookmarked
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) NeubrutalismIcons.Bookmark else NeubrutalismIcons.BookmarkBorder,
                            contentDescription = if (isBookmarked) t("remove_bookmark") else t("add_bookmark"),
                            tint = if (isBookmarked) neoColors.onPrimary else neoColors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Media Type Badge
                    val badgeText = if (uiState.isTvShow) t("badge_tv_show") else t("badge_movie")
                    val badgeColor = if (uiState.isTvShow) neoColors.secondary else neoColors.primary

                    Box(
                        modifier = Modifier
                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .background(badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontFamily = badgeFontFamily(),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading && uiState.detail == null -> {
                    com.movieapp.theme.MovieDetailSkeleton()
                }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                        .background(neoColors.errorBackground, RoundedCornerShape(12.dp))
                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            fontFamily = bodyFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NeoButton(
                            onClick = { viewModel.retry() },
                            text = t("try_again"),
                            backgroundColor = neoColors.primary,
                            contentColor = neoColors.textPrimary
                        )
                    }
                }
            }
            uiState.detail != null -> {
                val detail = uiState.detail!!

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Poster Banner Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(14.dp))
                            .background(neoColors.surfaceMuted, RoundedCornerShape(14.dp))
                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = detail.backdrop ?: detail.poster,
                            contentDescription = "Banner image for ${detail.displayTitle}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title and Rating Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = detail.displayTitle,
                                fontFamily = headerFontFamily(),
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                color = neoColors.textPrimary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = detail.displayYear,
                                    fontFamily = badgeFontFamily(),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = neoColors.textSecondary
                                )
                                detail.runtime?.let {
                                    Text(text = "•", color = neoColors.textSecondary)
                                    Text(
                                        text = it,
                                        fontFamily = badgeFontFamily(),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = neoColors.textSecondary
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                 .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                                 .background(neoColors.tertiary, RoundedCornerShape(10.dp))
                                 .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                                 .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = NeubrutalismIcons.Star,
                                    contentDescription = null,
                                    tint = NeoBlack,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = detail.formattedRating,
                                    fontFamily = badgeFontFamily(),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack
                                )
                            }
                        }
                    }

                    // Download Button for Movies
                    if (!uiState.isTvShow) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                                .background(neoColors.primary, RoundedCornerShape(10.dp))
                                .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                                .clickable {
                                    downloadSheetTitle = detail.displayTitle
                                    currentDownloadLinks = detail.safeMovieDownloadLinks
                                    showDownloadSheet = true
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = NeubrutalismIcons.Download,
                                    contentDescription = null,
                                    tint = neoColors.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("get_download_links", detail.safeMovieDownloadLinks.size),
                                    fontFamily = buttonFontFamily(),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neoColors.onPrimary
                                )
                            }
                        }
                    }

                    // Genre Badges
                    if (detail.safeGenres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            detail.safeGenres.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                                        .background(neoColors.surfaceMuted, RoundedCornerShape(8.dp))
                                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        fontFamily = badgeFontFamily(),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = neoColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Story Summary Card
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                            .background(neoColors.surface, RoundedCornerShape(12.dp))
                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = t("story_summary"),
                                fontFamily = headerFontFamily(),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = neoColors.textPrimary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detail.plot ?: t("no_summary"),
                                fontFamily = bodyFontFamily(),
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                color = neoColors.textSecondary
                            )
                        }
                    }

                    // TV Shows: Seasons and Episodes
                    if (uiState.isTvShow && detail.safeSeasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = t("choose_season_episode"),
                            fontFamily = headerFontFamily(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = neoColors.textPrimary,
                            modifier = Modifier.semantics { heading() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Season Selector Tabs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            detail.safeSeasons.forEach { season ->
                                val isSelected = season.seasonNumber == uiState.selectedSeasonNumber
                                val bg = if (isSelected) neoColors.border else neoColors.surface
                                val textCol = if (isSelected) neoColors.surface else neoColors.textPrimary

                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                                        .background(bg, RoundedCornerShape(8.dp))
                                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                                        .clickable { viewModel.selectSeason(season.seasonNumber) }
                                        .semantics {
                                            role = Role.Tab
                                            selected = isSelected
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = season.displayName,
                                        fontFamily = buttonFontFamily(),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = textCol
                                    )
                                }
                            }
                        }

                        // Full Season Download Action Button (Batch Download)
                        val activeEpisodes = uiState.activeSeason?.safeEpisodes ?: emptyList()
                        if (activeEpisodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                                    .background(neoColors.secondary, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                                    .clickable {
                                        // Collect all download links for the entire season
                                        val allSeasonLinks = activeEpisodes.flatMap { it.safeDownloadLinks }
                                        downloadSheetTitle = "${detail.displayTitle} - Season ${uiState.selectedSeasonNumber} (All Episodes)"
                                        currentDownloadLinks = allSeasonLinks
                                        showDownloadSheet = true
                                    }
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = NeubrutalismIcons.Download,
                                        contentDescription = null,
                                        tint = neoColors.onSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t("download_full_season", activeEpisodes.size),
                                        fontFamily = buttonFontFamily(),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = neoColors.onSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Episode Cards List with Download Action
                        uiState.activeSeason?.safeEpisodes?.forEach { episode ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                                    .background(neoColors.surface, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                                    .clickable {
                                        downloadSheetTitle = "${detail.displayTitle} - ${episode.displayTitle}"
                                        currentDownloadLinks = episode.safeDownloadLinks
                                        showDownloadSheet = true
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                                                .background(neoColors.primary, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${episode.episodeNumber}",
                                                fontFamily = badgeFontFamily(),
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = neoColors.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = episode.displayTitle,
                                            fontFamily = headerFontFamily(),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = neoColors.textPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        episode.runtime?.let {
                                            Text(
                                                text = it,
                                                fontFamily = badgeFontFamily(),
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = neoColors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Icon(
                                            imageVector = NeubrutalismIcons.Download,
                                            contentDescription = "${t("download_episode")}: ${episode.displayTitle}",
                                            tint = neoColors.textPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    PullToRefreshContainer(
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter),
        containerColor = neoColors.primary,
        contentColor = neoColors.textPrimary
    )
}
}

