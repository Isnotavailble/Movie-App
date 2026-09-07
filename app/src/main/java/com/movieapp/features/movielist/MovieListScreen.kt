package com.movieapp.features.movielist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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

import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    viewModel: MovieListViewModel,
    onTitleClick: (slug: String, isTvShow: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val neoColors = MaterialTheme.neoColors

    // Pull-to-refresh state
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    // Continuous Infinite Scrolling detection with distinctUntilChanged (US-03)
    LaunchedEffect(gridState, uiState.activeCategory) {
        snapshotFlow {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 4
        }
        .distinctUntilChanged()
        .collect { shouldPaginate ->
            if (shouldPaginate && !uiState.isPaginating && uiState.currentHasMore) {
                viewModel.loadNextPage()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // In-page search bar for Movies or TV Shows
            val searchPlaceholder = if (uiState.activeCategory == MediaCategory.MOVIES) {
                t("search_movies_placeholder")
            } else {
                t("search_tv_shows_placeholder")
            }

            InPageSearchBar(
                query = uiState.currentSearchQuery,
                placeholder = searchPlaceholder,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onClearClick = { viewModel.clearSearchQuery() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Empty State
            if (uiState.isSearchEmpty) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                            .background(neoColors.surface, RoundedCornerShape(12.dp))
                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Search,
                            contentDescription = null,
                            tint = neoColors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val emptyTitle = if (uiState.activeCategory == MediaCategory.MOVIES) {
                            t("search_no_movies_found")
                        } else {
                            t("search_no_tv_shows_found")
                        }
                        Text(
                            text = emptyTitle,
                            fontFamily = headerFontFamily(),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t("search_empty_desc"),
                            fontFamily = bodyFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = neoColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (uiState.isInitialLoading && uiState.currentDisplayList.isEmpty()) {
                // Initial Loading State with Skeleton Cards
                com.movieapp.theme.MovieListFeedSkeleton(modifier = Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = uiState.currentDisplayList,
                        key = { item ->
                            val idPart = if (item.id != 0L) item.id.toString() else item.slug ?: item.displayTitle
                            "${uiState.activeCategory}_$idPart"
                        },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieGridCard(
                            item = item,
                            onClick = {
                                val slug = item.slug ?: item.id.toString()
                                val isTv = uiState.activeCategory == MediaCategory.TV_SHOWS
                                onTitleClick(slug, isTv)
                            }
                        )
                    }

                    // Inline Pagination Progress Indicator (Spidey Blue) spanning full grid width
                    if (uiState.isPaginating) {
                        item(
                            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                            contentType = "pagination_loader"
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = neoColors.secondary, strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }

            // Inline Error Notice & Retry Button
            uiState.errorMessage?.let { errorText ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow)
                        .background(neoColors.errorBackground, RoundedCornerShape(12.dp))
                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorText,
                            fontFamily = bodyFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NeoButton(
                            onClick = { viewModel.retry() },
                            text = t("try_again"),
                            backgroundColor = neoColors.primary,
                            contentColor = neoColors.textPrimary
                        )
                    }
                }
            }
        }

        // Pull to refresh indicator
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = neoColors.primary,
            contentColor = neoColors.textPrimary
        )
    }
}

/**
 * Accessible Neobrutalist in-page search bar.
 */
@Composable
fun InPageSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neoColors = MaterialTheme.neoColors
    val clearLabel = t("search_clear")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = NeubrutalismIcons.Search,
                contentDescription = null,
                tint = neoColors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontFamily = bodyFontFamily(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = neoColors.textPrimary
                ),
                singleLine = true,
                cursorBrush = SolidColor(neoColors.secondary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontFamily = bodyFontFamily(),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = neoColors.textSecondary
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .semantics {
                        contentDescription = placeholder
                    }
            )
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(onClick = onClearClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = clearLabel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Close,
                        contentDescription = null,
                        tint = neoColors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual Movie or TV Show poster card with Neobrutalism border and hard shadow.
 */
@Composable
private fun MovieGridCard(
    item: MovieDTO,
    onClick: () -> Unit
) {
    val neoColors = MaterialTheme.neoColors
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageRequest = remember(item.poster) {
        coil.request.ImageRequest.Builder(context)
            .data(item.poster)
            .size(coil.size.Dimension(360), coil.size.Dimension(540))
            .precision(coil.size.Precision.INEXACT)
            .build()
    }
    val a11yLabel = "${item.displayTitle}, released in ${item.displayYear}, rating ${item.formattedRating} out of 10"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = a11yLabel
            }
    ) {
        // Poster Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(neoColors.surfaceMuted)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "${item.displayTitle} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Rating Badge with Star Icon (Web Gold)
            val itemRating = item.rating
            if (itemRating != null && itemRating > 0.0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                        .background(neoColors.tertiary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = NeubrutalismIcons.Star,
                        contentDescription = null,
                        tint = NeoBlack,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = item.formattedRating,
                        fontFamily = badgeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.sp,
                        color = NeoBlack
                    )
                }
            }
        }

        // Details Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = item.displayTitle,
                fontFamily = headerFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = neoColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.displayYear,
                    fontFamily = badgeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = neoColors.textSecondary
                )
                val isTv = item.mediaType?.contains("tv", ignoreCase = true) == true
                val typeLabel = if (isTv) t("badge_tv_show") else t("badge_movie")
                val typeBg = if (isTv) neoColors.secondary else neoColors.primary
                Box(
                    modifier = Modifier
                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(4.dp))
                        .background(typeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = typeLabel,
                        fontFamily = badgeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        letterSpacing = 0.sp,
                        color = neoColors.onPrimary
                    )
                }
            }
        }
    }
}
