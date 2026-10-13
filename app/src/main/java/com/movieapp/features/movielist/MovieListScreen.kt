package com.movieapp.features.movielist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
    modifier: Modifier = Modifier,
    category: MediaCategory? = null,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val neoColors = MaterialTheme.neoColors
    val targetCategory = category ?: uiState.activeCategory

    // Pull-to-refresh state
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh(targetCategory)
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    // Continuous Infinite Scrolling detection with distinctUntilChanged (US-03)
    // Pre-triggers 10 items (5 rows in a 2-col grid) before the bottom for smooth background pagination
    LaunchedEffect(gridState, targetCategory) {
        snapshotFlow {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 10
        }
        .distinctUntilChanged()
        .collect { shouldPaginate ->
            val hasMore = if (targetCategory == MediaCategory.MOVIES) uiState.moviesHasMore else uiState.tvShowsHasMore
            val isSearch = if (targetCategory == MediaCategory.MOVIES) uiState.moviesSearchQuery.isNotBlank() else uiState.tvShowsSearchQuery.isNotBlank()
            if (shouldPaginate && !uiState.isPaginating && hasMore && !isSearch) {
                viewModel.loadNextPage(targetCategory)
            }
        }
    }

    val searchQuery = if (targetCategory == MediaCategory.MOVIES) uiState.moviesSearchQuery else uiState.tvShowsSearchQuery
    val isSearchActive = searchQuery.isNotBlank()
    val displayList = uiState.getDisplayListFor(targetCategory)
    val isSearchEmpty = isSearchActive && !uiState.isSearching && displayList.isEmpty()
    val isFilterEmpty = uiState.isFilterEmptyFor(targetCategory)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(neoColors.background)
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            // In-page search bar for Movies or TV Shows
            val searchPlaceholder = if (targetCategory == MediaCategory.MOVIES) {
                t("search_movies_placeholder")
            } else {
                t("search_tv_shows_placeholder")
            }

            InPageSearchBar(
                query = searchQuery,
                placeholder = searchPlaceholder,
                onQueryChange = {
                    viewModel.selectCategory(targetCategory)
                    viewModel.onSearchQueryChange(it)
                },
                onClearClick = {
                    viewModel.selectCategory(targetCategory)
                    viewModel.clearSearchQuery()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Genre, Rating and Sort Controls
            FilterAndSortBar(
                availableGenres = uiState.getAvailableGenresFor(targetCategory),
                selectedGenre = uiState.selectedGenre,
                onSelectGenre = { viewModel.selectGenre(it) },
                selectedRating = uiState.minRating,
                onSelectRating = { viewModel.selectMinRating(it) },
                sortOrder = uiState.sortOrder,
                onSelectSortOrder = { viewModel.selectSortOrder(it) },
                isFilterActive = uiState.isFilterActive,
                onResetFilters = { viewModel.resetFilters() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Empty State
            if (isSearchEmpty) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
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
                        val emptyTitle = if (targetCategory == MediaCategory.MOVIES) {
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
            } else if (isFilterEmpty) {
                FilterEmptyState(
                    onResetClick = { viewModel.resetFilters() },
                    modifier = Modifier.weight(1f)
                )
            } else if (uiState.isInitialLoading && displayList.isEmpty()) {
                // Initial Loading State with Skeleton Cards
                com.movieapp.theme.MovieListFeedSkeleton(modifier = Modifier.weight(1f))
            } else if (uiState.errorMessage != null && displayList.isEmpty()) {
                // Full screen scrollable error container so pull-to-refresh works when list is empty
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
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
                                text = uiState.errorMessage ?: "",
                                fontFamily = bodyFontFamily(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = neoColors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            NeoButton(
                                onClick = { viewModel.retry(targetCategory) },
                                text = t("try_again"),
                                backgroundColor = neoColors.primary,
                                contentColor = neoColors.textPrimary
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val isTv = targetCategory == MediaCategory.TV_SHOWS
                    items(
                        items = displayList,
                        key = { item ->
                            val idPart = if (item.id != 0L) item.id.toString() else item.slug ?: item.displayTitle
                            "${targetCategory.name}_$idPart"
                        },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieGridCard(
                            item = item,
                            isTv = isTv,
                            onTitleClick = onTitleClick
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

            // Inline Error Notice & Retry Button (only when list already has items)
            if (uiState.errorMessage != null && displayList.isNotEmpty()) {
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
                                onClick = { viewModel.retry(targetCategory) },
                                text = t("try_again"),
                                backgroundColor = neoColors.primary,
                                contentColor = neoColors.textPrimary
                            )
                        }
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
    isTv: Boolean,
    onTitleClick: (slug: String, isTvShow: Boolean) -> Unit
) {
    val neoColors = MaterialTheme.neoColors
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageRequest = remember(item.poster) {
        coil.request.ImageRequest.Builder(context)
            .data(item.poster)
            .size(coil.size.Dimension(360), coil.size.Dimension(540))
            .precision(coil.size.Precision.INEXACT)
            .crossfade(150)
            .build()
    }
    val a11yLabel = "${item.displayTitle}, released in ${item.displayYear}, rating ${item.formattedRating} out of 10"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .clickable {
                val slug = item.slug ?: item.id.toString()
                onTitleClick(slug, isTv)
            }
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
                val isItemTv = item.mediaType?.contains("tv", ignoreCase = true) == true
                val typeLabel = if (isItemTv) t("badge_tv_show") else t("badge_movie")
                val typeBg = if (isItemTv) neoColors.secondary else neoColors.primary
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

/**
 * Neobrutalist filter and sort bar containing horizontal genre chips,
 * rating thresholds (All, 6+, 7+, 8+), and a vector sort dropdown.
 */
@Composable
fun FilterAndSortBar(
    availableGenres: List<String>,
    selectedGenre: String?,
    onSelectGenre: (String?) -> Unit,
    selectedRating: Double,
    onSelectRating: (Double) -> Unit,
    sortOrder: MovieSortOrder,
    onSelectSortOrder: (MovieSortOrder) -> Unit,
    isFilterActive: Boolean,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neoColors = MaterialTheme.neoColors
    var isSortExpanded by remember { mutableStateOf(false) }

    val resetLabel = t("filter_reset")
    val sortLabel = t("sort_label")
    val allGenreLabel = t("filter_genre_all")
    val allRatingLabel = t("filter_rating_all")
    val rating6Label = t("filter_rating_6")
    val rating7Label = t("filter_rating_7")
    val rating8Label = t("filter_rating_8")
    val sortDefaultLabel = t("sort_default")
    val sortTopRatedLabel = t("sort_top_rated")
    val sortNewestLabel = t("sort_newest")
    val sortTitleAzLabel = t("sort_title_az")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Horizontal scrollable Genre Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
        ) {
            val isAllSelected = selectedGenre == null
            item(key = "genre_all") {
                NeobrutalistChip(
                    text = allGenreLabel,
                    isSelected = isAllSelected,
                    onClick = { onSelectGenre(null) },
                    selectedBgColor = neoColors.primary,
                    selectedTextColor = neoColors.onPrimary
                )
            }

            items(availableGenres, key = { "genre_$it" }) { genre ->
                val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                NeobrutalistChip(
                    text = genre,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) onSelectGenre(null) else onSelectGenre(genre)
                    },
                    selectedBgColor = neoColors.primary,
                    selectedTextColor = neoColors.onPrimary
                )
            }
        }

        // 2. Rating chips & Sort Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rating filters: All, 6+, 7+, 8+
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NeobrutalistRatingChip(
                    text = allRatingLabel,
                    isSelected = selectedRating <= 0.0,
                    showStar = false,
                    onClick = { onSelectRating(0.0) }
                )

                NeobrutalistRatingChip(
                    text = rating6Label,
                    isSelected = selectedRating == 6.0,
                    showStar = true,
                    onClick = { onSelectRating(if (selectedRating == 6.0) 0.0 else 6.0) }
                )

                NeobrutalistRatingChip(
                    text = rating7Label,
                    isSelected = selectedRating == 7.0,
                    showStar = true,
                    onClick = { onSelectRating(if (selectedRating == 7.0) 0.0 else 7.0) }
                )

                NeobrutalistRatingChip(
                    text = rating8Label,
                    isSelected = selectedRating == 8.0,
                    showStar = true,
                    onClick = { onSelectRating(if (selectedRating == 8.0) 0.0 else 8.0) }
                )
            }

            // Right side: Reset (if active) & Sort Dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isFilterActive) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                            .background(neoColors.surface, RoundedCornerShape(8.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable(onClick = onResetFilters)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = resetLabel
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Close,
                            contentDescription = null,
                            tint = neoColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Sort Dropdown Button
                Box {
                    val isCustomSort = sortOrder != MovieSortOrder.DEFAULT
                    val sortBg = if (isCustomSort) neoColors.secondary else neoColors.surface
                    val sortContentColor = if (isCustomSort) neoColors.onPrimary else neoColors.textPrimary

                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                            .background(sortBg, RoundedCornerShape(8.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable { isSortExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = sortLabel
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Sort,
                            contentDescription = null,
                            tint = sortContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (sortOrder) {
                                MovieSortOrder.DEFAULT -> sortLabel
                                MovieSortOrder.TOP_RATED -> sortTopRatedLabel
                                MovieSortOrder.NEWEST -> sortNewestLabel
                                MovieSortOrder.TITLE_AZ -> sortTitleAzLabel
                            },
                            fontFamily = buttonFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = sortContentColor
                        )
                    }

                    DropdownMenu(
                        expanded = isSortExpanded,
                        onDismissRequest = { isSortExpanded = false },
                        modifier = Modifier
                            .background(neoColors.surface, RoundedCornerShape(8.dp))
                            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                    ) {
                        SortMenuItem(
                            text = sortDefaultLabel,
                            isSelected = sortOrder == MovieSortOrder.DEFAULT,
                            onClick = {
                                onSelectSortOrder(MovieSortOrder.DEFAULT)
                                isSortExpanded = false
                            }
                        )
                        SortMenuItem(
                            text = sortTopRatedLabel,
                            isSelected = sortOrder == MovieSortOrder.TOP_RATED,
                            onClick = {
                                onSelectSortOrder(MovieSortOrder.TOP_RATED)
                                isSortExpanded = false
                            }
                        )
                        SortMenuItem(
                            text = sortNewestLabel,
                            isSelected = sortOrder == MovieSortOrder.NEWEST,
                            onClick = {
                                onSelectSortOrder(MovieSortOrder.NEWEST)
                                isSortExpanded = false
                            }
                        )
                        SortMenuItem(
                            text = sortTitleAzLabel,
                            isSelected = sortOrder == MovieSortOrder.TITLE_AZ,
                            onClick = {
                                onSelectSortOrder(MovieSortOrder.TITLE_AZ)
                                isSortExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Neobrutalist filter empty state with a Reset Filters button.
 */
@Composable
private fun FilterEmptyState(
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neoColors = MaterialTheme.neoColors
    val emptyTitle = t("filter_no_results")
    val emptyDesc = t("filter_empty_desc")
    val resetLabel = t("filter_reset")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                imageVector = NeubrutalismIcons.Filter,
                contentDescription = null,
                tint = neoColors.textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = emptyTitle,
                fontFamily = headerFontFamily(),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = neoColors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = emptyDesc,
                fontFamily = bodyFontFamily(),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = neoColors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeoButton(
                onClick = onResetClick,
                text = resetLabel,
                backgroundColor = neoColors.primary,
                contentColor = neoColors.onPrimary
            )
        }
    }
}

@Composable
private fun SortMenuItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val neoColors = MaterialTheme.neoColors
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontFamily = bodyFontFamily(),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) neoColors.primary else neoColors.textPrimary
            )
        },
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = NeubrutalismIcons.Check,
                    contentDescription = null,
                    tint = neoColors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
    )
}

@Composable
private fun NeobrutalistChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedBgColor: androidx.compose.ui.graphics.Color,
    selectedTextColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val neoColors = MaterialTheme.neoColors
    val bg = if (isSelected) selectedBgColor else neoColors.surface
    val textColor = if (isSelected) selectedTextColor else neoColors.textPrimary
    val borderWidth = if (isSelected) 2.dp else 1.5.dp
    val shadowOffset = if (isSelected) 2.dp else 1.5.dp

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .neoBorder(width = borderWidth, color = neoColors.border, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = text
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = badgeFontFamily(),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

@Composable
private fun NeobrutalistRatingChip(
    text: String,
    isSelected: Boolean,
    showStar: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neoColors = MaterialTheme.neoColors
    val bg = if (isSelected) neoColors.tertiary else neoColors.surface
    val textColor = if (isSelected) NeoBlack else neoColors.textPrimary
    val borderWidth = if (isSelected) 2.dp else 1.5.dp
    val shadowOffset = if (isSelected) 2.dp else 1.5.dp

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 34.dp)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .neoBorder(width = borderWidth, color = neoColors.border, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .semantics {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = "$text rating"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (showStar) {
            Icon(
                imageVector = NeubrutalismIcons.Star,
                contentDescription = null,
                tint = if (isSelected) NeoBlack else neoColors.tertiary,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            fontFamily = badgeFontFamily(),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.5.sp,
            color = textColor
        )
    }
}
