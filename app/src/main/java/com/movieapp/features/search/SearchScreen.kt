package com.movieapp.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.movieapp.features.movielist.MovieDTO
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
fun SearchScreen(
    viewModel: SearchViewModel,
    onTitleClick: (slug: String, isTvShow: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val neoColors = MaterialTheme.neoColors

    val pullRefreshState = rememberPullToRefreshState()
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
            Text(
                text = t("search_heading"),
                fontFamily = headerFontFamily(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = neoColors.textPrimary
            )

        Spacer(modifier = Modifier.height(6.dp))

        // Accessible Search Bar with Clear Button (US-04)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                .background(neoColors.surface, RoundedCornerShape(12.dp))
                .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neubrutalism Search Leading Icon
            Icon(
                imageVector = NeubrutalismIcons.Search,
                contentDescription = null,
                tint = neoColors.textPrimary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                textStyle = TextStyle(
                    fontFamily = bodyFontFamily(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = neoColors.textPrimary
                ),
                cursorBrush = SolidColor(neoColors.textPrimary),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search text input field" },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = t("search_placeholder"),
                                fontFamily = bodyFontFamily(),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = neoColors.textSecondary
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (uiState.query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .background(neoColors.border, RoundedCornerShape(8.dp))
                        .clickable { viewModel.clearQuery() }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Clear search text"
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Close,
                            contentDescription = null,
                            tint = neoColors.surface,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = t("search_clear"),
                            color = neoColors.surface,
                            fontFamily = buttonFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = t("search_note"),
            fontFamily = bodyFontFamily(),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            color = neoColors.textSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area: Loading, Initial Prompt, Empty State, Error, or Results
        when {
            uiState.isLoading -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(4) {
                        com.movieapp.theme.SearchResultCardSkeleton()
                    }
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow)
                        .background(neoColors.errorBackground, RoundedCornerShape(12.dp))
                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            fontFamily = bodyFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NeoButton(
                            onClick = { viewModel.retry() },
                            text = t("try_again"),
                            backgroundColor = neoColors.primary,
                            contentColor = neoColors.textPrimary
                        )
                    }
                }
            }
            uiState.isEmptyResult -> {
                // Empty State with guidance (US-05)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                        .background(neoColors.surfaceMuted, RoundedCornerShape(12.dp))
                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = t("search_empty_title"),
                            fontFamily = headerFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t("search_empty_desc"),
                            fontFamily = bodyFontFamily(),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = neoColors.textSecondary
                        )
                    }
                }
            }
            uiState.isInitial -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                        .background(neoColors.surfaceMuted, RoundedCornerShape(12.dp))
                        .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = t("search_prompt_title"),
                            fontFamily = headerFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t("search_prompt_desc"),
                            fontFamily = bodyFontFamily(),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = neoColors.textSecondary
                        )
                    }
                }
            }
            else -> {
                // Search Results List
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = uiState.results,
                        key = { "${it.id}_${it.slug}_${it.displayTitle}" },
                        contentType = { "search_card" }
                    ) { item ->
                        SearchResultCard(
                            item = item,
                            onClick = {
                                val isTv = item.mediaType?.contains("tv", ignoreCase = true) == true
                                val slug = item.slug ?: item.id.toString()
                                onTitleClick(slug, isTv)
                            }
                        )
                    }
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

/**
 * Result row card featuring movie poster, metadata, and category tag.
 */
@Composable
private fun SearchResultCard(
    item: MovieDTO,
    onClick: () -> Unit
) {
    val neoColors = MaterialTheme.neoColors
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageRequest = remember(item.poster) {
        coil.request.ImageRequest.Builder(context)
            .data(item.poster)
            .size(coil.size.Dimension(120), coil.size.Dimension(170))
            .precision(coil.size.Precision.INEXACT)
            .build()
    }
    val a11yLabel = "${item.displayTitle}, released in ${item.displayYear}, rating ${item.formattedRating}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .semantics {
                role = Role.Button
                contentDescription = a11yLabel
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 76.dp)
                .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(neoColors.surfaceMuted)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "${item.displayTitle} thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                fontFamily = headerFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = neoColors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.displayYear} • Rating ${item.formattedRating}",
                fontFamily = badgeFontFamily(),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = neoColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge [Movie] or [TV Show]
        val isTv = item.mediaType?.contains("tv", ignoreCase = true) == true
        val badgeText = if (isTv) t("badge_tv_show") else t("badge_movie")
        val badgeColor = if (isTv) neoColors.secondary else neoColors.tertiary

        Box(
            modifier = Modifier
                .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                .background(badgeColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.5.dp)
        ) {
            Text(
                text = badgeText,
                fontFamily = badgeFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.sp,
                color = neoColors.textPrimary
            )
        }
    }
}
