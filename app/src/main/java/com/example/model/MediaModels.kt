package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MediaType(val label: String) {
    MOVIE("Filmes"),
    SERIES("Séries"),
    ANIME("Animes"),
    DORAMA("Doramas"),
    ANIMATION("Animações"),
    DOCUMENTARY("Documentários");

    companion object {
        fun fromString(value: String?): MediaType {
            return when (value?.lowercase()) {
                "movie", "filme", "filmes" -> MOVIE
                "anime", "animes" -> ANIME
                "dorama", "doramas" -> DORAMA
                "animation", "animação", "animacao" -> ANIMATION
                "documentary", "documentário", "documentario" -> DOCUMENTARY
                else -> SERIES
            }
        }
    }
}

enum class WatchStatus(val label: String) {
    WANT_TO_WATCH("Quero assistir"),
    WATCHING("Assistindo"),
    COMPLETED("Concluído"),
    PAUSED("Pausado"),
    DROPPED("Abandonado");

    companion object {
        fun fromString(value: String?): WatchStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: WANT_TO_WATCH
        }
    }
}

data class CastMember(
    val name: String,
    val character: String?,
    val profileUrl: String?
)

data class EpisodeDetail(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String = "",
    val airDate: String? = null,
    val isSpecial: Boolean = false,
    val stillUrl: String? = null
)

data class SeasonDetail(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val episodes: List<EpisodeDetail> = emptyList()
)

data class MediaItem(
    val id: String,
    val source: String, // "tvmaze" or "tmdb"
    val externalId: String,
    val title: String,
    val originalTitle: String = "",
    val overview: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val genres: List<String> = emptyList(),
    val mediaType: MediaType = MediaType.SERIES,
    val releaseYear: String = "",
    val rating: Double? = null,
    val runtime: String? = null,
    val totalEpisodes: Int = 0,
    val totalSeasons: Int = 0,
    val status: String? = null,
    val cast: List<CastMember> = emptyList(),
    val seasons: List<SeasonDetail> = emptyList()
)

@IgnoreExtraProperties
data class LibraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val externalId: String = "",
    val source: String = "tvmaze",
    val mediaType: String = "series",
    val title: String = "",
    val originalTitle: String = "",
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseYear: String = "",
    val genres: List<String> = emptyList(),
    val status: String = "WANT_TO_WATCH",
    val totalEpisodes: Int = 0,
    val watchedEpisodes: Int = 0,
    val totalSeasons: Int = 0,
    val isMovie: Boolean = false,
    val movieWatched: Boolean = false,
    val lastWatchedSeason: Int? = null,
    val lastWatchedEpisode: Int? = null,
    val lastWatchedEpisodeName: String? = null,
    val nextEpisodeSeason: Int? = null,
    val nextEpisodeNumber: Int? = null,
    val nextEpisodeName: String? = null,
    val userRating: Double? = null,
    val reviewText: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EpisodeProgress(
    @DocumentId val id: String = "", // e.g. "s1_e1"
    val userId: String = "",
    val mediaId: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val title: String = "",
    val isSpecial: Boolean = false,
    val watched: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReview(
    @DocumentId val id: String = "", // mediaId
    val userId: String = "",
    val mediaId: String = "",
    val mediaTitle: String = "",
    val mediaType: String = "series",
    val posterPath: String? = null,
    val rating: Double = 0.0,
    val review: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
