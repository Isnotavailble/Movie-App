package com.movieapp.features.bookmarks

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.movieapp.MovieApplication
import com.movieapp.data.local.MovieDao
import com.movieapp.data.local.MovieEntity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    onTitleClick: (slug: String, isTvShow: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dao: MovieDao = MovieApplication.instance.database.movieDao()
) {
    val neoColors = MaterialTheme.neoColors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rawBookmarks by dao.getBookmarkedMovies().collectAsStateWithLifecycle(initialValue = emptyList())
    var locallyRemovedSlugs by remember { mutableStateOf(setOf<String>()) }
    val bookmarkedMovies = remember(rawBookmarks, locallyRemovedSlugs) {
        rawBookmarks.filter { it.slug !in locallyRemovedSlugs }
    }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            delay(300)
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Heading
            Text(
                text = t("bookmarks_title"),
                fontFamily = headerFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = neoColors.textPrimary
            )
            Text(
                text = t("bookmarks_subtitle"),
                fontFamily = bodyFontFamily(),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = neoColors.textSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            if (bookmarkedMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
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
                            imageVector = NeubrutalismIcons.BookmarkBorder,
                            contentDescription = null,
                            tint = neoColors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t("bookmarks_empty_title"),
                            fontFamily = headerFontFamily(),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = neoColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t("bookmarks_empty_desc"),
                            fontFamily = bodyFontFamily(),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = neoColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = bookmarkedMovies,
                        key = { it.slug },
                        contentType = { "bookmark_card" }
                    ) { movie ->
                        BookmarkMovieCard(
                            item = movie,
                            onClick = { onTitleClick(movie.slug, movie.isTvShow) },
                            onRemoveClick = {
                                locallyRemovedSlugs = locallyRemovedSlugs + movie.slug
                                val toastMsg = LocalizationManager.getString("bookmark_removed")
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    dao.updateBookmarkStatus(slug = movie.slug, isBookmarked = false, bookmarkedAt = 0L)
                                }
                            }
                        )
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

@Composable
private fun BookmarkMovieCard(
    item: MovieEntity,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val neoColors = MaterialTheme.neoColors
    val badgeLabel = if (item.isTvShow) t("badge_tv_show") else t("badge_movie")
    val a11yLabel = "${item.title}, ${item.releaseYear ?: ""}, $badgeLabel"
    val removeBookmarkLabel = "${t("remove_bookmark")}: ${item.title}"
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageRequest = remember(item.poster) {
        coil.request.ImageRequest.Builder(context)
            .data(item.poster)
            .size(coil.size.Dimension(360), coil.size.Dimension(540))
            .precision(coil.size.Precision.INEXACT)
            .build()
    }

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(neoColors.surfaceMuted)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "${item.title} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // One-tap unbookmark button (US-14) with a11y 48dp touch target
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(6.dp))
                    .background(neoColors.surface, RoundedCornerShape(6.dp))
                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(6.dp))
                    .clickable(onClick = onRemoveClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = removeBookmarkLabel
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NeubrutalismIcons.Bookmark,
                    contentDescription = null,
                    tint = neoColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Rating badge if available
            item.rating?.takeIf { it.isNotBlank() }?.let { rating ->
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
                        text = rating,
                        fontFamily = badgeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.sp,
                        color = NeoBlack
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = item.title,
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
                    text = item.releaseYear ?: "Unknown",
                    fontFamily = badgeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = neoColors.textSecondary
                )
                val isTv = item.isTvShow
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
