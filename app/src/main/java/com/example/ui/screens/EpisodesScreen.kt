package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EpisodeDetail
import com.example.model.EpisodeProgress
import com.example.model.LibraryItem
import com.example.model.MediaItem
import com.example.model.SeasonDetail
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.RatingGold
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun EpisodesScreen(
    media: MediaItem,
    libraryItem: LibraryItem?,
    episodesProgress: Map<String, EpisodeProgress>,
    isLoadingEpisodes: Boolean,
    onBackClick: () -> Unit,
    onToggleEpisode: (seasonNumber: Int, episodeNumber: Int, title: String, isSpecial: Boolean, isWatched: Boolean) -> Unit,
    onToggleSeason: (seasonNumber: Int, episodes: List<EpisodeDetail>, markWatched: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    // Map tracking expanded state for each season accordion
    val expandedSeasons = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            media.seasons.forEach { this[it.seasonNumber] = (it.seasonNumber == 1 || media.seasons.size == 1) }
        }
    }

    val regularEpisodes = media.seasons.filter { it.seasonNumber > 0 }.flatMap { it.episodes.filter { ep -> !ep.isSpecial } }
    val totalRegularCount = if (regularEpisodes.isNotEmpty()) regularEpisodes.size else media.totalEpisodes
    val watchedRegularCount = regularEpisodes.count { episodesProgress["s${it.seasonNumber}_e${it.episodeNumber}"]?.watched == true }

    val nextUnwatched = regularEpisodes.firstOrNull {
        episodesProgress["s${it.seasonNumber}_e${it.episodeNumber}"]?.watched != true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("episodes_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .testTag("episodes_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Episódios",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }

        // Overall Progress Banner
        item {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val progress = if (totalRegularCount > 0) {
                        (watchedRegularCount.toFloat() / totalRegularCount.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progresso Geral",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% concluído",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricVioletLight
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ElectricViolet,
                        trackColor = BackgroundDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$watchedRegularCount de $totalRegularCount episódios assistidos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Next episode highlight
                    if (nextUnwatched != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = BackgroundDark,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Próximo a assistir:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary
                                    )
                                    Text(
                                        text = "T${nextUnwatched.seasonNumber}:E${nextUnwatched.episodeNumber} - ${nextUnwatched.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Button(
                                    onClick = {
                                        onToggleEpisode(
                                            nextUnwatched.seasonNumber,
                                            nextUnwatched.episodeNumber,
                                            nextUnwatched.name,
                                            nextUnwatched.isSpecial,
                                            false
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Marcar", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoadingEpisodes) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }
        }

        // Season Accordions
        items(media.seasons, key = { it.seasonNumber }) { season ->
            val isExpanded = expandedSeasons[season.seasonNumber] ?: false
            val seasonEpisodes = season.episodes
            val watchedInSeason = seasonEpisodes.count {
                episodesProgress["s${it.seasonNumber}_e${it.episodeNumber}"]?.watched == true
            }
            val isSeasonFullyWatched = seasonEpisodes.isNotEmpty() && watchedInSeason == seasonEpisodes.size

            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("season_accordion_${season.seasonNumber}")
            ) {
                Column {
                    // Season Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSeasons[season.seasonNumber] = !isExpanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = season.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                if (season.seasonNumber == 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = RatingGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ESPECIAIS",
                                            color = RatingGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$watchedInSeason de ${season.episodeCount} assistidos",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSeasonFullyWatched) StatusGreen else TextTertiary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick button to mark / unmark entire season
                            TextButton(
                                onClick = {
                                    onToggleSeason(
                                        season.seasonNumber,
                                        seasonEpisodes,
                                        !isSeasonFullyWatched
                                    )
                                }
                            ) {
                                Text(
                                    text = if (isSeasonFullyWatched) "Desmarcar todos" else "Marcar todos",
                                    color = ElectricVioletLight,
                                    fontSize = 12.sp
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Episodes List inside Accordion
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (seasonEpisodes.isEmpty()) {
                                Text(
                                    text = "Carregando episódios desta temporada...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            seasonEpisodes.forEach { ep ->
                                val isWatched = episodesProgress["s${ep.seasonNumber}_e${ep.episodeNumber}"]?.watched == true

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onToggleEpisode(
                                                ep.seasonNumber,
                                                ep.episodeNumber,
                                                ep.name,
                                                ep.isSpecial,
                                                isWatched
                                            )
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .testTag("episode_item_s${ep.seasonNumber}_e${ep.episodeNumber}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isWatched,
                                        onCheckedChange = { checked ->
                                            onToggleEpisode(
                                                ep.seasonNumber,
                                                ep.episodeNumber,
                                                ep.name,
                                                ep.isSpecial,
                                                isWatched
                                            )
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ElectricViolet,
                                            checkmarkColor = Color.White,
                                            uncheckedColor = BorderSubtle
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Ep. ${ep.episodeNumber}: ${ep.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isWatched) TextSecondary else TextPrimary,
                                                fontWeight = if (isWatched) FontWeight.Normal else FontWeight.Medium
                                            )
                                        }
                                        if (!ep.airDate.isNullOrBlank()) {
                                            Text(
                                                text = ep.airDate,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextTertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
