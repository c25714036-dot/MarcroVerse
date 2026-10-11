package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.api.CatalogRepository
import com.example.auth.AuthManager
import com.example.data.FirestoreRepository
import com.example.model.MediaItem
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.EpisodesScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReviewsScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.CineViewModel

enum class CineNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Início", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    EXPLORE("Explorar", Icons.Filled.Search, Icons.Outlined.Search, "nav_explore"),
    LIBRARY("Minha Lista", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_library"),
    REVIEWS("Avaliações", Icons.Filled.RateReview, Icons.Outlined.RateReview, "nav_reviews"),
    PROFILE("Perfil", Icons.Filled.Person, Icons.Outlined.PersonOutline, "nav_profile")
}

sealed class CineScreen {
    data class Tab(val tab: CineNavTab) : CineScreen()
    data class Detail(val returnTab: CineNavTab) : CineScreen()
    data class Episodes(val returnTab: CineNavTab) : CineScreen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: CineViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val firestoreRepo = FirestoreRepository(this@MainActivity)
                val catalogRepo = CatalogRepository(this@MainActivity)
                val authManager = AuthManager(this@MainActivity)
                return CineViewModel(application, firestoreRepo, catalogRepo, authManager) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val isSigningIn by viewModel.isSigningIn.collectAsState()
                val authError by viewModel.authError.collectAsState()

                if (currentUser == null) {
                    AuthGateScreen(
                        isLoading = isSigningIn,
                        errorMessage = authError,
                        onSignInClick = { viewModel.signIn() },
                        onDismissError = { viewModel.clearAuthError() }
                    )
                } else {
                    CineAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CineAppContent(viewModel: CineViewModel) {
    var currentScreen by remember { mutableStateOf<CineScreen>(CineScreen.Tab(CineNavTab.HOME)) }

    val featuredMedia by viewModel.featuredMedia.collectAsState()
    val isFeaturedLoading by viewModel.isFeaturedLoading.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val libraryItems by viewModel.libraryItems.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val isSelectedMediaLoading by viewModel.isSelectedMediaLoading.collectAsState()
    val episodesProgress by viewModel.currentEpisodesProgress.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val showBottomBar = currentScreen is CineScreen.Tab

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        bottomBar = {
            if (showBottomBar) {
                val activeTab = (currentScreen as CineScreen.Tab).tab
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    CineNavTab.entries.forEach { tab ->
                        val isSelected = (activeTab == tab)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = CineScreen.Tab(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) ElectricVioletLight else TextTertiary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricViolet,
                                selectedTextColor = ElectricVioletLight,
                                indicatorColor = ElectricViolet.copy(alpha = 0.2f),
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is CineScreen.Tab -> {
                    when (screen.tab) {
                        CineNavTab.HOME -> {
                            HomeScreen(
                                featuredMedia = featuredMedia,
                                isFeaturedLoading = isFeaturedLoading,
                                continueWatching = continueWatching,
                                onMediaClick = { media ->
                                    viewModel.selectMedia(media)
                                    currentScreen = CineScreen.Detail(CineNavTab.HOME)
                                },
                                onLibraryItemClick = { mediaId ->
                                    val item = libraryItems.firstOrNull { it.id == mediaId }
                                    if (item != null) {
                                        val media = MediaItem(
                                            id = item.id,
                                            source = item.source,
                                            externalId = item.externalId,
                                            title = item.title,
                                            posterUrl = item.posterPath,
                                            backdropUrl = item.backdropPath,
                                            overview = item.overview,
                                            releaseYear = item.releaseYear,
                                            genres = item.genres,
                                            totalEpisodes = item.totalEpisodes,
                                            totalSeasons = item.totalSeasons
                                        )
                                        viewModel.selectMedia(media)
                                        currentScreen = CineScreen.Detail(CineNavTab.HOME)
                                    }
                                },
                                onCategoryClick = { cat ->
                                    viewModel.onCategorySelect(cat)
                                    currentScreen = CineScreen.Tab(CineNavTab.EXPLORE)
                                },
                                onQuickNextEpisode = { item ->
                                    if (item.nextEpisodeSeason != null && item.nextEpisodeNumber != null) {
                                        viewModel.toggleEpisode(
                                            mediaId = item.id,
                                            seasonNumber = item.nextEpisodeSeason,
                                            episodeNumber = item.nextEpisodeNumber,
                                            episodeTitle = item.nextEpisodeName ?: "",
                                            isSpecial = false,
                                            currentlyWatched = false,
                                            seasons = null
                                        )
                                    }
                                }
                            )
                        }

                        CineNavTab.EXPLORE -> {
                            ExploreScreen(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                selectedCategory = selectedCategory,
                                onCategorySelect = { viewModel.onCategorySelect(it) },
                                searchResults = searchResults,
                                isSearching = isSearching,
                                onMediaClick = { media ->
                                    viewModel.selectMedia(media)
                                    currentScreen = CineScreen.Detail(CineNavTab.EXPLORE)
                                }
                            )
                        }

                        CineNavTab.LIBRARY -> {
                            LibraryScreen(
                                libraryItems = libraryItems,
                                onItemClick = { mediaId ->
                                    val item = libraryItems.firstOrNull { it.id == mediaId }
                                    if (item != null) {
                                        val media = MediaItem(
                                            id = item.id,
                                            source = item.source,
                                            externalId = item.externalId,
                                            title = item.title,
                                            posterUrl = item.posterPath,
                                            backdropUrl = item.backdropPath,
                                            overview = item.overview,
                                            releaseYear = item.releaseYear,
                                            genres = item.genres,
                                            totalEpisodes = item.totalEpisodes,
                                            totalSeasons = item.totalSeasons
                                        )
                                        viewModel.selectMedia(media)
                                        currentScreen = CineScreen.Detail(CineNavTab.LIBRARY)
                                    }
                                },
                                onNavigateExplore = {
                                    currentScreen = CineScreen.Tab(CineNavTab.EXPLORE)
                                }
                            )
                        }

                        CineNavTab.REVIEWS -> {
                            ReviewsScreen(
                                reviews = reviews,
                                onReviewMediaClick = { mediaId ->
                                    val rev = reviews.firstOrNull { it.mediaId == mediaId }
                                    if (rev != null) {
                                        val media = MediaItem(
                                            id = rev.mediaId,
                                            source = "tvmaze",
                                            externalId = rev.mediaId,
                                            title = rev.mediaTitle,
                                            posterUrl = rev.posterPath
                                        )
                                        viewModel.selectMedia(media)
                                        currentScreen = CineScreen.Detail(CineNavTab.REVIEWS)
                                    }
                                },
                                onDeleteReview = { mediaId ->
                                    viewModel.deleteReview(mediaId)
                                }
                            )
                        }

                        CineNavTab.PROFILE -> {
                            ProfileScreen(
                                user = currentUser,
                                libraryItems = libraryItems,
                                reviews = reviews,
                                currentTmdbKey = viewModel.catalogRepository.userTmdbApiKey,
                                onSaveTmdbKey = { key ->
                                    viewModel.setCustomTmdbKey(key)
                                },
                                onSignOutClick = {
                                    viewModel.signOut()
                                }
                            )
                        }
                    }
                }

                is CineScreen.Detail -> {
                    val media = selectedMedia
                    if (media != null) {
                        val libItem = libraryItems.firstOrNull { it.id == media.id }
                        val revItem = reviews.firstOrNull { it.mediaId == media.id }

                        DetailScreen(
                            media = media,
                            libraryItem = libItem,
                            userReview = revItem,
                            isLoadingDetails = isSelectedMediaLoading,
                            onBackClick = { currentScreen = CineScreen.Tab(screen.returnTab) },
                            onAddToLibrary = { status ->
                                viewModel.addToLibrary(media, status)
                            },
                            onUpdateStatus = { status ->
                                viewModel.updateStatus(media.id, status)
                            },
                            onRemoveFromLibrary = {
                                viewModel.removeFromLibrary(media.id)
                            },
                            onToggleMovieWatched = { watched ->
                                viewModel.toggleMovieWatched(media.id, watched)
                            },
                            onGoToEpisodes = {
                                currentScreen = CineScreen.Episodes(screen.returnTab)
                            },
                            onSubmitReview = { rating, text ->
                                viewModel.submitReview(media, rating, text)
                            },
                            onDeleteReview = {
                                viewModel.deleteReview(media.id)
                            }
                        )
                    }
                }

                is CineScreen.Episodes -> {
                    val media = selectedMedia
                    if (media != null) {
                        val libItem = libraryItems.firstOrNull { it.id == media.id }
                        EpisodesScreen(
                            media = media,
                            libraryItem = libItem,
                            episodesProgress = episodesProgress,
                            isLoadingEpisodes = isSelectedMediaLoading,
                            onBackClick = { currentScreen = CineScreen.Detail(screen.returnTab) },
                            onToggleEpisode = { sNum, epNum, epTitle, isSpec, currentlyWatched ->
                                viewModel.toggleEpisode(
                                    mediaId = media.id,
                                    seasonNumber = sNum,
                                    episodeNumber = epNum,
                                    episodeTitle = epTitle,
                                    isSpecial = isSpec,
                                    currentlyWatched = currentlyWatched,
                                    seasons = media.seasons
                                )
                            },
                            onToggleSeason = { sNum, episodes, markWatched ->
                                viewModel.toggleEntireSeason(
                                    mediaId = media.id,
                                    seasonNumber = sNum,
                                    episodes = episodes,
                                    markWatched = markWatched,
                                    seasons = media.seasons
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
