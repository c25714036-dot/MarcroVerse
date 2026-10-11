package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.CatalogRepository
import com.example.auth.AuthManager
import com.example.data.FirestoreRepository
import com.example.model.EpisodeDetail
import com.example.model.EpisodeProgress
import com.example.model.LibraryItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.UserReview
import com.example.model.WatchStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CineViewModel(
    application: Application,
    val firestoreRepository: FirestoreRepository,
    val catalogRepository: CatalogRepository,
    val authManager: AuthManager
) : AndroidViewModel(application) {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Library items from Firestore
    private val _libraryItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<LibraryItem>> = _libraryItems.asStateFlow()

    // Reviews from Firestore
    private val _reviews = MutableStateFlow<List<UserReview>>(emptyList())
    val reviews: StateFlow<List<UserReview>> = _reviews.asStateFlow()

    // Continue Watching list
    val continueWatching: StateFlow<List<LibraryItem>> = _libraryItems.map { list ->
        list.filter { it.status == WatchStatus.WATCHING.name || (it.watchedEpisodes > 0 && it.status != WatchStatus.COMPLETED.name) }
            .sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Featured media for Home
    private val _featuredMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val featuredMedia: StateFlow<List<MediaItem>> = _featuredMedia.asStateFlow()
    private val _isFeaturedLoading = MutableStateFlow(false)
    val isFeaturedLoading: StateFlow<Boolean> = _isFeaturedLoading.asStateFlow()

    // Search & Explore
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<MediaType?>(null)
    val selectedCategory: StateFlow<MediaType?> = _selectedCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Detail Screen
    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia: StateFlow<MediaItem?> = _selectedMedia.asStateFlow()

    private val _isSelectedMediaLoading = MutableStateFlow(false)
    val isSelectedMediaLoading: StateFlow<Boolean> = _isSelectedMediaLoading.asStateFlow()

    private val _currentEpisodesProgress = MutableStateFlow<Map<String, EpisodeProgress>>(emptyMap())
    val currentEpisodesProgress: StateFlow<Map<String, EpisodeProgress>> = _currentEpisodesProgress.asStateFlow()

    private var libraryJob: Job? = null
    private var reviewsJob: Job? = null
    private var episodesJob: Job? = null
    private var searchJob: Job? = null

    init {
        // Observe Auth State
        viewModelScope.launch {
            authManager.authStateFlow().collectLatest { user ->
                _currentUser.value = user
                if (user != null) {
                    subscribeToUserData(user.uid)
                } else {
                    unsubscribeUserData()
                }
            }
        }
        loadFeatured()
    }

    private fun subscribeToUserData(userId: String) {
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            firestoreRepository.observeLibrary(userId).collectLatest {
                _libraryItems.value = it
            }
        }

        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            firestoreRepository.observeReviews(userId).collectLatest {
                _reviews.value = it
            }
        }
    }

    private fun unsubscribeUserData() {
        libraryJob?.cancel()
        reviewsJob?.cancel()
        episodesJob?.cancel()
        _libraryItems.value = emptyList()
        _reviews.value = emptyList()
        _currentEpisodesProgress.value = emptyMap()
    }

    fun signIn() {
        viewModelScope.launch {
            _isSigningIn.value = true
            _authError.value = null
            val result = authManager.signInWithGoogle()
            result.onSuccess {
                _currentUser.value = it
                _isSigningIn.value = false
            }.onFailure {
                _isSigningIn.value = false
                _authError.value = it.localizedMessage ?: "Não foi possível autenticar."
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun loadFeatured() {
        viewModelScope.launch {
            _isFeaturedLoading.value = true
            val items = catalogRepository.getFeaturedMedia()
            _featuredMedia.value = items
            _isFeaturedLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce typing
            _isSearching.value = true
            val results = catalogRepository.searchCatalog(query, _selectedCategory.value)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun onCategorySelect(category: MediaType?) {
        _selectedCategory.value = category
        if (_searchQuery.value.isNotBlank()) {
            onSearchQueryChange(_searchQuery.value)
        }
    }

    fun selectMedia(media: MediaItem) {
        _selectedMedia.value = media
        val userId = _currentUser.value?.uid
        if (userId != null) {
            episodesJob?.cancel()
            episodesJob = viewModelScope.launch {
                firestoreRepository.observeEpisodeProgress(userId, media.id).collectLatest {
                    _currentEpisodesProgress.value = it
                }
            }
        }

        // Fetch complete seasons and episodes if not yet loaded
        if (media.seasons.isEmpty() && media.mediaType != MediaType.MOVIE) {
            viewModelScope.launch {
                _isSelectedMediaLoading.value = true
                val detailed = catalogRepository.getMediaDetails(media.id)
                if (detailed != null) {
                    _selectedMedia.value = detailed
                }
                _isSelectedMediaLoading.value = false
            }
        }
    }

    fun addToLibrary(media: MediaItem, status: WatchStatus = WatchStatus.WANT_TO_WATCH) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val item = LibraryItem(
                id = media.id,
                userId = user.uid,
                externalId = media.externalId,
                source = media.source,
                mediaType = media.mediaType.name.lowercase(),
                title = media.title,
                originalTitle = media.originalTitle,
                overview = media.overview,
                posterPath = media.posterUrl,
                backdropPath = media.backdropUrl,
                releaseYear = media.releaseYear,
                genres = media.genres,
                status = status.name,
                totalEpisodes = media.totalEpisodes,
                watchedEpisodes = 0,
                totalSeasons = media.totalSeasons,
                isMovie = (media.mediaType == MediaType.MOVIE),
                movieWatched = false
            )
            firestoreRepository.saveToLibrary(user.uid, item)
        }
    }

    fun updateStatus(mediaId: String, status: WatchStatus) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firestoreRepository.updateStatus(user.uid, mediaId, status)
        }
    }

    fun removeFromLibrary(mediaId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firestoreRepository.removeFromLibrary(user.uid, mediaId)
        }
    }

    fun toggleMovieWatched(mediaId: String, watched: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firestoreRepository.toggleMovieWatched(user.uid, mediaId, watched)
        }
    }

    fun toggleEpisode(
        mediaId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        episodeTitle: String,
        isSpecial: Boolean,
        currentlyWatched: Boolean,
        seasons: List<com.example.model.SeasonDetail>?
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firestoreRepository.setEpisodeWatched(
                userId = user.uid,
                mediaId = mediaId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                isSpecial = isSpecial,
                watched = !currentlyWatched,
                seasons = seasons
            )
        }
    }

    fun toggleEntireSeason(
        mediaId: String,
        seasonNumber: Int,
        episodes: List<EpisodeDetail>,
        markWatched: Boolean,
        seasons: List<com.example.model.SeasonDetail>?
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firestoreRepository.setSeasonWatched(
                userId = user.uid,
                mediaId = mediaId,
                seasonNumber = seasonNumber,
                episodes = episodes,
                watched = markWatched,
                seasons = seasons
            )
        }
    }

    fun submitReview(media: MediaItem, rating: Double, reviewText: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val review = UserReview(
                id = media.id,
                userId = user.uid,
                mediaId = media.id,
                mediaTitle = media.title,
                mediaType = media.mediaType.name.lowercase(),
                posterPath = media.posterUrl,
                rating = rating,
                review = reviewText
            )
            firestoreRepository.saveReview(user.uid, review)
        }
    }

    fun deleteReview(mediaId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firestoreRepository.deleteReview(user.uid, mediaId)
        }
    }

    fun setCustomTmdbKey(apiKey: String) {
        catalogRepository.userTmdbApiKey = apiKey
        loadFeatured()
    }
}
