package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LibraryItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.WatchStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    libraryItems: List<LibraryItem>,
    onItemClick: (String) -> Unit,
    onNavigateExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by remember { mutableStateOf<WatchStatus?>(null) }
    var selectedCategory by remember { mutableStateOf<MediaType?>(null) }
    var filterQuery by remember { mutableStateOf("") }

    val filteredItems = libraryItems.filter { item ->
        val matchesStatus = (selectedStatus == null || item.status == selectedStatus?.name)
        val matchesCategory = (selectedCategory == null || item.mediaType.equals(selectedCategory?.name, ignoreCase = true))
        val matchesQuery = (filterQuery.isBlank() || item.title.contains(filterQuery, ignoreCase = true) || item.originalTitle.contains(filterQuery, ignoreCase = true))
        matchesStatus && matchesCategory && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("library_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Minha Biblioteca",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${libraryItems.size} títulos organizados",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        // Quick Filter Input
        if (libraryItems.size > 4) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filtrar na biblioteca...", color = TextTertiary) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextTertiary)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Status Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            item {
                StatusFilterChip(
                    label = "Todos (${libraryItems.size})",
                    isSelected = (selectedStatus == null),
                    onClick = { selectedStatus = null }
                )
            }
            items(WatchStatus.entries) { status ->
                val count = libraryItems.count { it.status == status.name }
                StatusFilterChip(
                    label = "${status.label} ($count)",
                    isSelected = (selectedStatus == status),
                    onClick = { selectedStatus = status }
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                CategoryTagPill(
                    label = "Todas Categorias",
                    isSelected = (selectedCategory == null),
                    onClick = { selectedCategory = null }
                )
            }
            items(MediaType.entries) { cat ->
                CategoryTagPill(
                    label = cat.label,
                    isSelected = (selectedCategory == cat),
                    onClick = { selectedCategory = cat }
                )
            }
        }

        // Grid Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.BookmarkBorder,
                    title = if (libraryItems.isEmpty()) "Sua biblioteca está vazia" else "Nenhum título corresponde aos filtros",
                    subtitle = if (libraryItems.isEmpty()) "Explore filmes, séries e animes para adicioná-los à sua lista." else "Experimente mudar o status ou limpar a busca.",
                    actionButton = if (libraryItems.isEmpty()) {
                        {
                            Button(
                                onClick = onNavigateExplore,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Explorar Catálogo")
                            }
                        }
                    } else null
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val dummyMedia = MediaItem(
                            id = item.id,
                            source = item.source,
                            externalId = item.externalId,
                            title = item.title,
                            posterUrl = item.posterPath,
                            backdropUrl = item.backdropPath,
                            releaseYear = item.releaseYear,
                            mediaType = MediaType.fromString(item.mediaType),
                            genres = item.genres,
                            totalEpisodes = item.totalEpisodes,
                            totalSeasons = item.totalSeasons
                        )
                        MediaCard(
                            media = dummyMedia,
                            onClick = { onItemClick(item.id) },
                            watchedEpisodes = if (!item.isMovie) item.watchedEpisodes else null,
                            totalEpisodes = if (!item.isMovie) item.totalEpisodes else null,
                            status = WatchStatus.fromString(item.status),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) ElectricViolet else SurfaceCard,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ElectricViolet else BorderSubtle),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun CategoryTagPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) ElectricViolet.copy(alpha = 0.2f) else SurfaceCard,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ElectricViolet else BorderSubtle),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) ElectricViolet else TextTertiary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
