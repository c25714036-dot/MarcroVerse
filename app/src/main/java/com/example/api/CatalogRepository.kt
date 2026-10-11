package com.example.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.CastMember
import com.example.model.EpisodeDetail
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.SeasonDetail
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class CatalogRepository(context: Context? = null) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val tvMazeService: TvMazeService = Retrofit.Builder()
        .baseUrl("https://api.tvmaze.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TvMazeService::class.java)

    private val tmdbService: TmdbService = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TmdbService::class.java)

    // Store custom TMDB API key if set by user in Profile/Settings, defaulting to BuildConfig
    var userTmdbApiKey: String = if (BuildConfig.TMDB_API_KEY == "DEFAULT_TMDB_API_KEY") "" else BuildConfig.TMDB_API_KEY.trim()

    // Memory cache for details to reduce external API requests
    private val detailCache = mutableMapOf<String, MediaItem>()

    suspend fun getFeaturedMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()

        // 1. Try TMDB trending if API key available
        val tmdbKey = userTmdbApiKey.trim()
        if (tmdbKey.isNotEmpty()) {
            try {
                val tmdbTrending = tmdbService.getTrending(apiKey = tmdbKey)
                tmdbTrending.results.take(12).forEach { item ->
                    mapTmdbToMediaItem(item)?.let { list.add(it) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TMDB trending error: ${e.message}")
            }
        }

        // 2. Fetch popular curated series/anime from TVmaze (reliable, completely free)
        if (list.size < 6) {
            try {
                val tvShows = tvMazeService.getPopularShows(page = 0).take(20)
                tvShows.forEach { show ->
                    list.add(mapTvMazeToMediaItem(show))
                }
            } catch (e: Exception) {
                Log.e(TAG, "TVmaze popular error: ${e.message}")
            }
        }

        list
    }

    suspend fun searchCatalog(query: String, filterCategory: MediaType? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<MediaItem>()
        val seenIds = mutableSetOf<String>()

        // 1. Search TMDB if key is available
        val tmdbKey = userTmdbApiKey.trim()
        if (tmdbKey.isNotEmpty()) {
            try {
                val tmdbRes = tmdbService.searchMulti(query = query, apiKey = tmdbKey)
                tmdbRes.results.forEach { item ->
                    val media = mapTmdbToMediaItem(item)
                    if (media != null && seenIds.add(media.id)) {
                        if (filterCategory == null || media.mediaType == filterCategory) {
                            results.add(media)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TMDB search error: ${e.message}")
            }
        }

        // 2. Search TVmaze (real TV shows, animes, doramas)
        try {
            val tvMazeResults = tvMazeService.searchShows(query = query)
            tvMazeResults.forEach { item ->
                val media = mapTvMazeToMediaItem(item.show)
                if (seenIds.add(media.id)) {
                    if (filterCategory == null || media.mediaType == filterCategory) {
                        results.add(media)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TVmaze search error: ${e.message}")
        }

        results
    }

    suspend fun getMediaDetails(mediaId: String): MediaItem? = withContext(Dispatchers.IO) {
        detailCache[mediaId]?.let { return@withContext it }

        if (mediaId.startsWith("tvmaze_")) {
            val showId = mediaId.removePrefix("tvmaze_").toLongOrNull() ?: return@withContext null
            try {
                val fullShow = tvMazeService.getShowDetails(id = showId)
                val media = mapTvMazeToMediaItemWithEmbeds(fullShow)
                detailCache[mediaId] = media
                return@withContext media
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching TVmaze details: ${e.message}")
                return@withContext null
            }
        } else if (mediaId.startsWith("tmdb_")) {
            val parts = mediaId.removePrefix("tmdb_").split("_")
            if (parts.size >= 2) {
                val type = parts[0]
                val id = parts[1].toLongOrNull() ?: return@withContext null
                val tmdbKey = userTmdbApiKey.trim()
                if (tmdbKey.isEmpty()) return@withContext null

                try {
                    if (type == "movie") {
                        val movie = tmdbService.getMovieDetail(movieId = id, apiKey = tmdbKey)
                        val media = mapTmdbMovieDetailToMediaItem(movie)
                        detailCache[mediaId] = media
                        return@withContext media
                    } else {
                        val tv = tmdbService.getTvDetail(seriesId = id, apiKey = tmdbKey)
                        // Fetch seasons
                        val seasonsList = mutableListOf<SeasonDetail>()
                        tv.seasons.forEach { s ->
                            try {
                                val fullSeason = tmdbService.getTvSeason(seriesId = id, seasonNumber = s.seasonNumber, apiKey = tmdbKey)
                                val eps = fullSeason.episodes.map { ep ->
                                    EpisodeDetail(
                                        id = "s${ep.seasonNumber}_e${ep.episodeNumber}",
                                        seasonNumber = ep.seasonNumber,
                                        episodeNumber = ep.episodeNumber,
                                        name = ep.name.ifBlank { "Episódio ${ep.episodeNumber}" },
                                        overview = ep.overview,
                                        airDate = ep.airDate,
                                        isSpecial = ep.seasonNumber == 0,
                                        stillUrl = ep.stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }
                                    )
                                }
                                seasonsList.add(SeasonDetail(
                                    seasonNumber = s.seasonNumber,
                                    name = s.name.ifBlank { "Temporada ${s.seasonNumber}" },
                                    episodeCount = eps.size,
                                    episodes = eps
                                ))
                            } catch (e: Exception) {
                                seasonsList.add(SeasonDetail(
                                    seasonNumber = s.seasonNumber,
                                    name = s.name,
                                    episodeCount = s.episodeCount
                                ))
                            }
                        }
                        val media = mapTmdbTvDetailToMediaItem(tv, seasonsList)
                        detailCache[mediaId] = media
                        return@withContext media
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching TMDB detail: ${e.message}")
                    return@withContext null
                }
            }
        }
        null
    }

    private fun mapTvMazeToMediaItem(show: TvMazeShow): MediaItem {
        val cleanedSummary = cleanHtml(show.summary ?: "")
        val mediaType = inferMediaType(show.type, show.language, show.genres)
        val year = show.premiered?.take(4) ?: ""

        return MediaItem(
            id = "tvmaze_${show.id}",
            source = "tvmaze",
            externalId = show.id.toString(),
            title = show.name,
            originalTitle = show.name,
            overview = cleanedSummary,
            posterUrl = show.image?.original ?: show.image?.medium,
            backdropUrl = show.image?.original,
            genres = show.genres,
            mediaType = mediaType,
            releaseYear = year,
            rating = show.rating?.average,
            runtime = show.runtime?.let { "${it} min" },
            totalEpisodes = 0,
            totalSeasons = 0,
            status = show.status
        )
    }

    private fun mapTvMazeToMediaItemWithEmbeds(show: TvMazeShow): MediaItem {
        val base = mapTvMazeToMediaItem(show)
        val episodes = show.embedded?.episodes ?: emptyList()
        val seasons = show.embedded?.seasons ?: emptyList()
        val cast = show.embedded?.cast?.take(10)?.map {
            CastMember(
                name = it.person.name,
                character = it.character?.name,
                profileUrl = it.person.image?.medium
            )
        } ?: emptyList()

        // Group episodes by season
        val seasonDetails = if (episodes.isNotEmpty()) {
            episodes.groupBy { it.season }.map { (seasonNum, eps) ->
                SeasonDetail(
                    seasonNumber = seasonNum,
                    name = "Temporada $seasonNum",
                    episodeCount = eps.size,
                    episodes = eps.map { ep ->
                        EpisodeDetail(
                            id = "s${ep.season}_e${ep.number ?: 0}",
                            seasonNumber = ep.season,
                            episodeNumber = ep.number ?: 0,
                            name = ep.name.ifBlank { "Episódio ${ep.number ?: 0}" },
                            overview = cleanHtml(ep.summary ?: ""),
                            airDate = ep.airdate,
                            isSpecial = (ep.type.equals("significant_special", true) || ep.season == 0 || ep.number == null),
                            stillUrl = ep.image?.original ?: ep.image?.medium
                        )
                    }
                )
            }.sortedBy { it.seasonNumber }
        } else {
            seasons.map { s ->
                SeasonDetail(
                    seasonNumber = s.number,
                    name = s.name ?: "Temporada ${s.number}",
                    episodeCount = s.episodeOrder ?: 0
                )
            }.sortedBy { it.seasonNumber }
        }

        val totalEpisodes = episodes.count { it.season > 0 }
        val totalSeasons = seasonDetails.count { it.seasonNumber > 0 }

        return base.copy(
            totalEpisodes = if (totalEpisodes > 0) totalEpisodes else seasons.sumOf { it.episodeOrder ?: 0 },
            totalSeasons = if (totalSeasons > 0) totalSeasons else seasons.size,
            cast = cast,
            seasons = seasonDetails
        )
    }

    private fun mapTmdbToMediaItem(item: TmdbMultiItem): MediaItem? {
        val isMovie = item.mediaType == "movie"
        val title = item.title ?: item.name ?: return null
        val original = item.originalTitle ?: item.originalName ?: title
        val year = (item.releaseDate ?: item.firstAirDate)?.take(4) ?: ""
        val poster = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val backdrop = item.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

        val type = if (isMovie) MediaType.MOVIE else MediaType.SERIES
        val id = if (isMovie) "tmdb_movie_${item.id}" else "tmdb_tv_${item.id}"

        return MediaItem(
            id = id,
            source = "tmdb",
            externalId = item.id.toString(),
            title = title,
            originalTitle = original,
            overview = item.overview ?: "",
            posterUrl = poster,
            backdropUrl = backdrop,
            mediaType = type,
            releaseYear = year,
            rating = item.voteAverage
        )
    }

    private fun mapTmdbMovieDetailToMediaItem(movie: TmdbMovieDetail): MediaItem {
        val year = movie.releaseDate?.take(4) ?: ""
        val poster = movie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val backdrop = movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
        val cast = movie.credits?.cast?.take(10)?.map {
            CastMember(
                name = it.name,
                character = it.character,
                profileUrl = it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }
            )
        } ?: emptyList()

        return MediaItem(
            id = "tmdb_movie_${movie.id}",
            source = "tmdb",
            externalId = movie.id.toString(),
            title = movie.title,
            originalTitle = movie.originalTitle ?: movie.title,
            overview = movie.overview ?: "",
            posterUrl = poster,
            backdropUrl = backdrop,
            genres = movie.genres.map { it.name },
            mediaType = MediaType.MOVIE,
            releaseYear = year,
            rating = movie.voteAverage,
            runtime = movie.runtime?.let { "${it} min" },
            status = movie.status,
            cast = cast
        )
    }

    private fun mapTmdbTvDetailToMediaItem(tv: TmdbTvDetail, seasons: List<SeasonDetail>): MediaItem {
        val year = tv.firstAirDate?.take(4) ?: ""
        val poster = tv.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val backdrop = tv.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
        val cast = tv.credits?.cast?.take(10)?.map {
            CastMember(
                name = it.name,
                character = it.character,
                profileUrl = it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }
            )
        } ?: emptyList()

        return MediaItem(
            id = "tmdb_tv_${tv.id}",
            source = "tmdb",
            externalId = tv.id.toString(),
            title = tv.name,
            originalTitle = tv.originalName ?: tv.name,
            overview = tv.overview ?: "",
            posterUrl = poster,
            backdropUrl = backdrop,
            genres = tv.genres.map { it.name },
            mediaType = MediaType.SERIES,
            releaseYear = year,
            rating = tv.voteAverage,
            totalEpisodes = tv.numberOfEpisodes,
            totalSeasons = tv.numberOfSeasons,
            status = tv.status,
            cast = cast,
            seasons = seasons
        )
    }

    private fun inferMediaType(type: String?, language: String?, genres: List<String>): MediaType {
        val isAnime = genres.any { it.equals("Anime", true) || it.equals("Animation", true) } &&
                (language.equals("Japanese", true) || type.equals("Animation", true))
        if (isAnime) return MediaType.ANIME

        val isDorama = (language.equals("Korean", true) || language.equals("Japanese", true)) &&
                genres.any { it.equals("Drama", true) || it.equals("Romance", true) }
        if (isDorama) return MediaType.DORAMA

        if (genres.any { it.equals("Animation", true) }) return MediaType.ANIMATION
        if (genres.any { it.equals("Documentary", true) } || type.equals("Documentary", true)) return MediaType.DOCUMENTARY

        return MediaType.SERIES
    }

    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        val pattern = Pattern.compile("<[^>]*>")
        val matcher = pattern.matcher(html)
        return matcher.replaceAll("").trim()
    }

    companion object {
        private const val TAG = "CatalogRepository"
    }
}
