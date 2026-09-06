package com.movieapp.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Lightweight Neobrutalist Shimmer modifier built natively with Compose animations.
 * Complies with the Ponytail principle (zero external dependencies).
 */
@Composable
fun Modifier.neoShimmer(): Modifier {
    val neoColors = MaterialTheme.neoColors
    val transition = rememberInfiniteTransition(label = "neo_shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neo_shimmer_translation"
    )

    val shimmerColors = listOf(
        neoColors.surfaceMuted.copy(alpha = 0.85f),
        neoColors.surface.copy(alpha = 0.95f),
        neoColors.surfaceMuted.copy(alpha = 0.85f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 250f, translateAnim - 250f),
        end = Offset(translateAnim, translateAnim)
    )

    return this.background(brush)
}

/**
 * 2-column Grid Skeleton matching MovieListScreen layout.
 */
@Composable
fun MovieListFeedSkeleton(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(6) {
            MovieGridCardSkeleton()
        }
    }
}

/**
 * Individual Movie Card skeleton placeholder.
 */
@Composable
fun MovieGridCardSkeleton() {
    val neoColors = MaterialTheme.neoColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Poster Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .neoShimmer()
        )

        // Title and Year Placeholders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .neoShimmer()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .neoShimmer()
                )
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .neoShimmer()
                )
            }
        }
    }
}

/**
 * Search Result Row Skeleton placeholder.
 */
@Composable
fun SearchResultCardSkeleton() {
    val neoColors = MaterialTheme.neoColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
            .background(neoColors.surface, RoundedCornerShape(12.dp))
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 76.dp)
                .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .neoShimmer()
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text Placeholders
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .neoShimmer()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .neoShimmer()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge placeholder
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 20.dp)
                .clip(RoundedCornerShape(6.dp))
                .neoShimmer()
        )
    }
}

/**
 * Movie Detail Screen Skeleton placeholder.
 */
@Composable
fun MovieDetailSkeleton() {
    val neoColors = MaterialTheme.neoColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(14.dp))
                .background(neoColors.surfaceMuted, RoundedCornerShape(14.dp))
                .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .neoShimmer()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Rating Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .neoShimmer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .neoShimmer()
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 30.dp)
                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .neoShimmer()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Button Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .neoShimmer()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Story Summary Box Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = neoColors.shadow, shape = RoundedCornerShape(12.dp))
                .background(neoColors.surface, RoundedCornerShape(12.dp))
                .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .neoShimmer()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .neoShimmer()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .neoShimmer()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .neoShimmer()
                )
            }
        }
    }
}
