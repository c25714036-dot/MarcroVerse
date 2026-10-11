package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.LibraryItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.WatchStatus
import com.example.ui.components.MediaCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.RatingGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    featuredMedia: List<MediaItem>,
    isFeaturedLoading: Boolean,
    continueWatching: List<LibraryItem>,
    onMediaClick: (MediaItem) -> Unit,
    onLibraryItemClick: (String) -> Unit,
    onCategoryClick: (MediaType) -> Unit,
    onQuickNextEpisode: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Hero Spotlight
        if (featuredMedia.isNotEmpty()) {
            val hero = featuredMedia.first()
            item {
                HeroBanner(
                    media = hero,
                    onClick = { onMediaClick(hero) }
                )
            }
        } else if (isFeaturedLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }
        }

        // 2. Continue Watching (Personal tracking section)
        if (continueWatching.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Continue Assistindo")
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(continueWatching, key = { it.id }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = { onLibraryItemClick(item.id) },
                            onNextClick = { onQuickNextEpisode(item) }
                        )
                    }
                }
            }
        }

        // 3. Category Quick Filter Chips
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(title = "Explorar Categorias")
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MediaType.entries) { category ->
                    CategoryPill(
                        category = category,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }

        // 4. Em Alta / Populares
        if (featuredMedia.size > 1) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Em Alta & Populares")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(featuredMedia.drop(1), key = { it.id }) { media ->
                        MediaCard(
                            media = media,
                            onClick = { onMediaClick(media) }
                        )
                    }
                }
            }
        }

        // 5. TMDB & TVmaze Attribution
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Catálogo em Tempo Real",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dados e imagens de entretenimento fornecidos pelas APIs oficiais do TMDB (The Movie Database) e TVmaze.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    media: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clickable(onClick = onClick)
            .testTag("hero_banner")
    ) {
        val imageUrl = media.backdropUrl ?: media.posterUrl
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundDark.copy(alpha = 0.5f),
                            BackgroundDark
                        ),
                        startY = 60f
                    )
                )
        )

        // Hero info content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(
                color = ElectricViolet,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "DESTAQUE • ${media.mediaType.label.uppercase()}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Text(
                text = media.title,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (media.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.genres.take(3).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ver Detalhes",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: LibraryItem,
    onClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("continue_watching_${item.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
            ) {
                if (!item.posterPath.isNullOrBlank()) {
                    AsyncImage(
                        model = item.posterPath,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.isMovie) {
                    Text(
                        text = if (item.movieWatched) "Filme assistido" else "Em andamento",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.movieWatched) Color(0xFF10B981) else ElectricVioletLight
                    )
                } else {
                    val nextText = if (item.nextEpisodeSeason != null && item.nextEpisodeNumber != null) {
                        "Próximo: T${item.nextEpisodeSeason} E${item.nextEpisodeNumber}"
                    } else if (item.lastWatchedSeason != null && item.lastWatchedEpisode != null) {
                        "Último: T${item.lastWatchedSeason} E${item.lastWatchedEpisode}"
                    } else {
                        "${item.watchedEpisodes} de ${item.totalEpisodes} eps"
                    }
                    Text(
                        text = nextText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricVioletLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!item.isMovie && item.totalEpisodes > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            val progress = (item.watchedEpisodes.toFloat() / item.totalEpisodes.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = ElectricViolet,
                trackColor = BackgroundDark
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.watchedEpisodes}/${item.totalEpisodes} assistidos",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricVioletLight,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    category: MediaType,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = category.label,
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
